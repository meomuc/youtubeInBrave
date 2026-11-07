package com.meomuc.youtubeintablet

import java.net.URLDecoder
import java.net.URLEncoder

/** Một mục hàng đợi: id video YouTube và tiêu đề để hiển thị (ticket #13). */
data class QueueItem(val videoId: String, val title: String)

/**
 * Vị trí đích khi kéo một mục (ticket #15): dịch chuyển theo số dòng đã vượt qua, kẹp trong danh sách.
 * Chiều cao dòng không hợp lệ (<= 0) thì giữ nguyên vị trí.
 */
fun dropTargetIndex(from: Int, offsetPx: Float, rowHeightPx: Float, count: Int): Int {
    if (rowHeightPx <= 0f || count <= 0) return from
    val rows = Math.round(offsetPx / rowHeightPx)
    return (from + rows).coerceIn(0, count - 1)
}

/** Id video YouTube gồm đúng 11 ký tự chữ, số, `_` hoặc `-`. Dùng để kiểm tra dữ liệu từ trang web. */
fun isValidVideoId(id: String): Boolean = VIDEO_ID_REGEX.matches(id)

private val VIDEO_ID_REGEX = Regex("[A-Za-z0-9_-]{11}")

/**
 * Hàng đợi phát cục bộ, tách khỏi playlist YouTube (ticket #13). Mục đầu danh sách là mục phát kế tiếp.
 * Không chứa id trùng: thêm lại một video đã có thì giữ nguyên vị trí cũ.
 * Lớp thuần Kotlin, không gọi API Android nên kiểm thử được bằng unit test.
 */
class PlayQueue(initial: List<QueueItem> = emptyList()) {

    private val list = initial.distinctBy { it.videoId }.toMutableList()

    val items: List<QueueItem>
        get() = list.toList()

    val isEmpty: Boolean
        get() = list.isEmpty()

    /** Thêm vào cuối hàng đợi; video đã có thì bỏ qua. */
    fun append(item: QueueItem) {
        if (list.none { it.videoId == item.videoId }) list.add(item)
    }

    /** Đưa mục lên đầu hàng đợi (Phát tiếp theo); mục đã có thì chuyển lên đầu, không nhân đôi. */
    fun playNext(item: QueueItem) {
        list.removeAll { it.videoId == item.videoId }
        list.add(0, item)
    }

    /** Lấy và bỏ mục đầu hàng đợi; hàng đợi rỗng thì trả null. */
    fun pollHead(): QueueItem? = if (list.isEmpty()) null else list.removeAt(0)

    /**
     * Quyết định khi một video kết thúc (ticket #14). Hàng đợi tắt thì không đụng tới danh sách và trả null.
     * Hàng đợi bật thì lấy mục đầu để phát; hết mục thì trả null, nghĩa là dừng.
     */
    fun takeNextOnEnd(queueOn: Boolean): QueueItem? = if (queueOn) pollHead() else null

    /** Chuyển mục từ vị trí [from] sang [to]. Chỉ số không hợp lệ thì bỏ qua. */
    fun move(from: Int, to: Int) {
        if (from !in list.indices || to !in list.indices || from == to) return
        list.add(to, list.removeAt(from))
    }

    /** Xóa mục tại vị trí [index]; chỉ số không hợp lệ thì bỏ qua. */
    fun removeAt(index: Int) {
        if (index in list.indices) list.removeAt(index)
    }
}

/**
 * Mã hóa hàng đợi thành chuỗi để lưu trong SharedPreferences. Mỗi mục một dòng,
 * hai trường cách nhau bằng tab; từng trường được URL-encode nên tab và xuống dòng trong tiêu đề không làm lệch dữ liệu.
 */
object QueueCodec {

    fun encode(items: List<QueueItem>): String =
        items.joinToString("\n") { enc(it.videoId) + "\t" + enc(it.title) }

    /** Đọc lại chuỗi đã mã hóa; dòng sai định dạng hoặc id video không hợp lệ thì bỏ qua. */
    fun decode(text: String): List<QueueItem> =
        text.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("\t")
                if (parts.size != 2) return@mapNotNull null
                val id = dec(parts[0])
                if (isValidVideoId(id)) QueueItem(id, dec(parts[1])) else null
            }
            .toList()

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun dec(s: String) = URLDecoder.decode(s, "UTF-8")
}
