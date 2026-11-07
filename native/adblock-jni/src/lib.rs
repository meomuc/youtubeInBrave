//! Cầu JNI cho engine chặn quảng cáo adblock-rust (ADR 0004).
//!
//! Kotlin giữ một handle (con trỏ) tới `Engine`. `Engine` dùng `RefCell` bên trong,
//! nên phía Kotlin phải đồng bộ hóa mọi lần gọi; xem `AdEngine.kt`.

use adblock::engine::Engine;
use adblock::lists::{FilterSet, ParseOptions};
use adblock::request::Request;
use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jlong, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;

/// Tạo engine từ văn bản danh sách lọc (định dạng Adblock Plus).
pub fn build_engine(list_text: &str) -> Engine {
    let mut filter_set = FilterSet::new(false);
    filter_set.add_filter_list(list_text.to_string(), ParseOptions::default());
    Engine::new_with_filter_set(filter_set)
}

/// Quyết định có chặn request không.
/// `request_type` theo quy ước của adblock: "document", "subdocument", "script",
/// "image", "stylesheet", "xmlhttprequest", "media", "font", "other"...
pub fn is_blocked(engine: &Engine, url: &str, source_url: &str, request_type: &str) -> bool {
    match Request::new(url, source_url, request_type, "GET") {
        Ok(request) => engine.check_network_request(&request).should_block(),
        Err(_) => false,
    }
}

// ---- Hàm JNI: lớp Kotlin AdEngineNative (package com.meomuc.youtubeintablet) ----

/// Tạo engine; trả về handle (0 nếu lỗi). Kotlin phải gọi `nativeDestroy` khi xong.
#[no_mangle]
pub extern "system" fn Java_com_meomuc_youtubeintablet_AdEngineNative_nativeCreate(
    mut env: JNIEnv,
    _class: JClass,
    rules: JString,
) -> jlong {
    let text: String = match env.get_string(&rules) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };
    Box::into_raw(Box::new(build_engine(&text))) as jlong
}

/// Kiểm tra một request. Kotlin phải đồng bộ hóa mọi lần gọi (Engine không thread-safe).
#[no_mangle]
pub extern "system" fn Java_com_meomuc_youtubeintablet_AdEngineNative_nativeShouldBlock(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    url: JString,
    source_url: JString,
    request_type: JString,
) -> jboolean {
    if handle == 0 {
        return JNI_FALSE;
    }
    let url: String = match env.get_string(&url) {
        Ok(s) => s.into(),
        Err(_) => return JNI_FALSE,
    };
    let source_url: String = match env.get_string(&source_url) {
        Ok(s) => s.into(),
        Err(_) => return JNI_FALSE,
    };
    let request_type: String = match env.get_string(&request_type) {
        Ok(s) => s.into(),
        Err(_) => return JNI_FALSE,
    };
    // SAFETY: handle lấy từ nativeCreate và chưa bị nativeDestroy (Kotlin đảm bảo).
    let engine = unsafe { &*(handle as *const Engine) };
    if is_blocked(engine, &url, &source_url, &request_type) {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

/// Giải phóng engine.
#[no_mangle]
pub extern "system" fn Java_com_meomuc_youtubeintablet_AdEngineNative_nativeDestroy(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle != 0 {
        // SAFETY: handle lấy từ nativeCreate, và chỉ giải phóng một lần (Kotlin đảm bảo).
        unsafe {
            drop(Box::from_raw(handle as *mut Engine));
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const RULES: &str = "||doubleclick.net^\n@@||doubleclick.net/allowed^\n";
    const PAGE: &str = "https://www.youtube.com/watch?v=abc";

    #[test]
    fn blocks_request_matching_network_rule() {
        let engine = build_engine(RULES);
        assert!(is_blocked(&engine, "https://googleads.doubleclick.net/pagead/x", PAGE, "script"));
    }

    #[test]
    fn allows_request_matching_exception_rule() {
        let engine = build_engine(RULES);
        assert!(!is_blocked(&engine, "https://doubleclick.net/allowed/y", PAGE, "script"));
    }

    #[test]
    fn allows_unrelated_request() {
        let engine = build_engine(RULES);
        assert!(!is_blocked(&engine, "https://www.youtube.com/s/player.js", PAGE, "script"));
    }

    #[test]
    fn allows_malformed_url_without_panic() {
        let engine = build_engine(RULES);
        assert!(!is_blocked(&engine, "not a url", PAGE, "other"));
    }
}
