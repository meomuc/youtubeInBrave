package com.meomuc.youtubeintablet

import android.content.Context

/** Đọc script JS tĩnh đóng gói trong assets/js (Bước 1: tách JS khỏi mã Kotlin). */
object JsAssets {

    /** Trả về nội dung của `assets/js/<name>`, UTF-8. */
    fun read(context: Context, name: String): String =
        context.assets.open("js/$name").bufferedReader(Charsets.UTF_8).use { it.readText() }
}
