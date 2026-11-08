package com.meomuc.youtubeintablet

/** Nơi lưu vị trí xem dở. Tầng Android triển khai bằng SharedPreferences ([ResumeStore]). */
interface ResumeRepository {
    fun load(): ResumePoint?
    fun save(point: ResumePoint)
    fun clearIf(videoId: String?)
}
