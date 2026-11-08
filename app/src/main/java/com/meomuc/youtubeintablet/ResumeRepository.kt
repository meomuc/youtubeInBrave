package com.meomuc.youtubeintablet

/** Nơi lưu vị trí xem dở (Bước 2). Tầng Android triển khai bằng SharedPreferences ([ResumeStore]). */
interface ResumeRepository {
    fun load(): ResumePoint?
    fun save(point: ResumePoint)
    fun clearIf(videoId: String?)
}
