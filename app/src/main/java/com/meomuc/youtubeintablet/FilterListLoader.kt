package com.meomuc.youtubeintablet

import android.util.Log
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Nạp danh sách lọc EasyList và EasyPrivacy (ADR 0004).
 * Dùng cache trong thư mục cache của app; làm mới sau 7 ngày. Không gọi trên luồng chính.
 */
object FilterListLoader {

    private const val TAG = "AdBlock"
    private const val CACHE_FILE = "adblock-filters.txt"
    private const val MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000

    private val SOURCES = listOf(
        "https://easylist.to/easylist/easylist.txt",
        "https://easylist.to/easylist/easyprivacy.txt",
    )

    /**
     * Trả về nội dung danh sách lọc: cache còn mới thì dùng cache; nếu không thì tải lại.
     * Tải lỗi thì dùng cache cũ; không có cache thì trả về chuỗi rỗng (chưa chặn gì).
     */
    fun load(cacheDir: File): String {
        val file = File(cacheDir, CACHE_FILE)
        val fresh = file.exists() && System.currentTimeMillis() - file.lastModified() < MAX_AGE_MS
        if (!fresh) {
            try {
                // Tải đủ mọi nguồn rồi mới ghi, để không ghi cache dở dang.
                val text = SOURCES.joinToString("\n") { download(it) }
                file.writeText(text)
                Log.i(TAG, "filters downloaded chars=${text.length}")
            } catch (e: IOException) {
                Log.w(TAG, "filters download failed: ${e.message}")
            }
        }
        return if (file.exists()) file.readText() else ""
    }

    private fun download(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        try {
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
