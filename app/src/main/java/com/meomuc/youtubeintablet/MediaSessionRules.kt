package com.meomuc.youtubeintablet

/** Lệnh điều khiển phát đến từ MediaSession (nút vô lăng, tai nghe, điều khiển hệ thống). */
enum class MediaCommand { PLAY, PAUSE, NEXT, PREVIOUS }

/**
 * Quy tắc thuần: đổi lệnh MediaSession thành script chạy trên trang YouTube.
 * Script không dùng innerHTML (Trusted Types) và không phụ thuộc Android.
 */
object MediaSessionRules {

    fun scriptFor(command: MediaCommand): String = when (command) {
        MediaCommand.PLAY ->
            "(function(){var v=document.querySelector('video');if(v){window.__ytTabletUserPaused=false;v.play();}})();"
        MediaCommand.PAUSE ->
            "(function(){var v=document.querySelector('video');if(v){window.__ytTabletUserPaused=true;v.pause();}})();"
        // Bấm nút "Video tiếp theo" của YouTube, nếu có.
        MediaCommand.NEXT ->
            "(function(){var b=document.querySelector('.ytp-next-button');if(b){b.click();}})();"
        // Quá 3 giây thì tua về đầu; gần đầu thì quay lại trang trước.
        MediaCommand.PREVIOUS ->
            "(function(){var v=document.querySelector('video');if(!v){return;}" +
                "if(v.currentTime>3){v.currentTime=0;}else{history.back();}})();"
    }
}
