package com.meomuc.youtubeintablet

/** Test double trong bộ nhớ cho [ResumeRepository]; không thuộc mã production. */
class InMemoryResumeRepository : ResumeRepository {
    private var point: ResumePoint? = null

    override fun load(): ResumePoint? = point

    override fun save(point: ResumePoint) {
        this.point = point
    }

    override fun clearIf(videoId: String?) {
        if (ResumeRules.shouldClear(point?.videoId, videoId)) point = null
    }
}
