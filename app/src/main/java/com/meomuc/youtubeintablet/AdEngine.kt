package com.meomuc.youtubeintablet

/**
 * Engine chặn quảng cáo (ADR 0004). Engine native không thread-safe nên mọi lần gọi
 * được đồng bộ hóa. Gọi [close] khi không dùng nữa.
 */
class AdEngine private constructor(private var handle: Long) {

    @Synchronized
    fun shouldBlock(url: String, sourceUrl: String, requestType: String): Boolean {
        if (handle == 0L) return false
        return AdEngineNative.nativeShouldBlock(handle, url, sourceUrl, requestType)
    }

    @Synchronized
    fun close() {
        if (handle != 0L) {
            AdEngineNative.nativeDestroy(handle)
            handle = 0L
        }
    }

    companion object {
        /** Tạo engine từ văn bản danh sách lọc; trả về null nếu native không tạo được. */
        fun create(rules: String): AdEngine? {
            val handle = AdEngineNative.nativeCreate(rules)
            return if (handle == 0L) null else AdEngine(handle)
        }
    }
}
