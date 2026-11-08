package com.meomuc.youtubeintablet

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Log chẩn đoán tạm thời (lỗi tiếng bị đứt khi PiP). Ghi toàn bộ phiên ra Download/vf8-diag.txt
 * để lấy file mà không cần adb. Chỉ ghi trên Android 10 trở lên; máy cũ hơn bỏ qua.
 * Gỡ bỏ khi đã tìm ra nguyên nhân.
 */
object DiagLog {
    private const val TAG = "DiagLog"
    private const val FILE_NAME = "vf8-diag.txt"
    private const val MAX_LINES = 3000

    private val lines = ArrayList<String>()
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    @Volatile private var appContext: Context? = null

    fun start(context: Context) {
        appContext = context.applicationContext
        log("=== phiên mới, ${Build.MODEL}, Android ${Build.VERSION.RELEASE} ===")
    }

    @Synchronized
    fun log(message: String) {
        lines.add("${timeFormat.format(Date())} $message")
        if (lines.size > MAX_LINES) lines.removeAt(0)
        flush()
    }

    /** Ghi đè cùng một file trong Download mỗi lần có sự kiện mới. */
    private fun flush() {
        val ctx = appContext ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        try {
            val resolver = ctx.contentResolver
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val existing = resolver.query(
                collection,
                arrayOf(MediaStore.Downloads._ID),
                "${MediaStore.Downloads.DISPLAY_NAME}=?",
                arrayOf(FILE_NAME),
                null
            )?.use { c -> if (c.moveToFirst()) ContentUris.withAppendedId(collection, c.getLong(0)) else null }
            val uri = existing ?: resolver.insert(collection, ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, FILE_NAME)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, "Download")
            }) ?: return
            resolver.openOutputStream(uri, "wt")?.use { it.write(lines.joinToString("\n").toByteArray()) }
        } catch (e: Exception) {
            Log.w(TAG, "ghi log thất bại", e)
        }
    }
}
