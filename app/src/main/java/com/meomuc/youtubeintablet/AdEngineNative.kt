package com.meomuc.youtubeintablet

/**
 * Khai báo hàm native trong native/adblock-jni/src/lib.rs (ADR 0004).
 * Đừng gọi trực tiếp; dùng [AdEngine] để có đồng bộ hóa và giải phóng handle đúng cách.
 */
object AdEngineNative {

    init {
        System.loadLibrary("adblock_jni")
    }

    @JvmStatic
    external fun nativeCreate(rules: String): Long

    @JvmStatic
    external fun nativeShouldBlock(handle: Long, url: String, sourceUrl: String, requestType: String): Boolean

    @JvmStatic
    external fun nativeDestroy(handle: Long)
}
