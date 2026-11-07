package com.meomuc.youtubeintablet

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Bảng hàng đợi (ticket #13): liệt kê [queue] theo thứ tự phát, mở từ nút "Hàng đợi" góc dưới bên trái.
 * Mọi thay đổi gọi [onChanged] để MainActivity lưu lại và cập nhật nút.
 */
class QueuePanel(
    context: Context,
    private val queue: PlayQueue,
    private val onChanged: () -> Unit,
) : FrameLayout(context) {

    private val listContainer = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }

    val isOpen: Boolean
        get() = visibility == View.VISIBLE

    init {
        visibility = View.GONE
        // Lớp mờ phủ toàn màn hình: chạm vào đây thì đóng bảng.
        setBackgroundColor(SCRIM_COLOR)
        isClickable = true
        setOnClickListener { hide() }

        val header = TextView(context).apply {
            text = "Hàng đợi"
            setTextColor(TEXT_COLOR)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(CARD_COLOR)
            setPadding(dp(16), dp(16), dp(16), dp(16))
            // Chạm vào thẻ không lan xuống lớp mờ, nên không làm bảng đóng.
            isClickable = true
            addView(header)
            addView(
                ScrollView(context).apply { addView(listContainer) },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            )
        }
        addView(
            card,
            LayoutParams(dp(360), (context.resources.displayMetrics.heightPixels * 0.7f).toInt(), Gravity.BOTTOM or Gravity.START).apply {
                setMargins(dp(96), 0, 0, dp(24))
            }
        )
    }

    fun show() {
        render()
        visibility = View.VISIBLE
    }

    fun hide() {
        visibility = View.GONE
    }

    /** Vẽ lại danh sách theo trạng thái hiện tại của [queue]. */
    fun render() {
        listContainer.removeAllViews()
        val items = queue.items
        if (items.isEmpty()) {
            listContainer.addView(TextView(context).apply {
                text = "Hàng đợi trống. Mở một video và chọn Thêm vào hàng đợi."
                setTextColor(HINT_COLOR)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setPadding(0, dp(12), 0, dp(12))
            })
            return
        }
        items.forEachIndexed { index, item ->
            listContainer.addView(row(index, item))
        }
    }

    /** Một mục: số thứ tự, tiêu đề và nút Phát tiếp theo (đưa lên đầu hàng đợi). */
    private fun row(index: Int, item: QueueItem): View {
        val label = TextView(context).apply {
            text = "${index + 1}. ${item.title.ifBlank { item.videoId }}"
            setTextColor(TEXT_COLOR)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        }
        val playNext = Button(context).apply {
            text = "Phát tiếp theo"
            isEnabled = index > 0
            setOnClickListener {
                queue.playNext(item)
                onChanged()
            }
        }
        val remove = Button(context).apply {
            text = "Xóa"
            contentDescription = "Xóa khỏi hàng đợi: ${item.title.ifBlank { item.videoId }}"
            setOnClickListener {
                queue.removeAt(index)
                onChanged()
            }
        }
        // Tay cầm kéo đổi thứ tự (ticket #15). Chạm xuống thì ngăn ScrollView cuộn.
        val handle = TextView(context).apply {
            text = "Kéo"
            contentDescription = "Kéo để đổi thứ tự"
            setTextColor(HINT_COLOR)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setOnTouchListener { v, event -> onHandleTouch(v, event, index) }
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
            addView(label, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(handle)
            addView(playNext)
            addView(remove)
        }
    }

    /** Kéo dòng [index]: dòng đi theo ngón tay, thả xuống thì đổi vị trí trong [queue]. */
    private fun onHandleTouch(handle: View, event: MotionEvent, index: Int): Boolean {
        val row = handle.parent as? View ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragStartY = event.rawY
                row.parent?.requestDisallowInterceptTouchEvent(true)
                row.alpha = DRAG_ALPHA
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                row.translationY = event.rawY - dragStartY
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val target = dropTargetIndex(
                    from = index,
                    offsetPx = event.rawY - dragStartY,
                    rowHeightPx = row.height.toFloat(),
                    count = queue.items.size,
                )
                row.translationY = 0f
                row.alpha = 1f
                if (event.actionMasked == MotionEvent.ACTION_UP && target != index) {
                    queue.move(index, target)
                    onChanged()
                } else {
                    render()
                }
                return true
            }
        }
        return false
    }

    /** Tọa độ Y tuyệt đối lúc bắt đầu kéo. */
    private var dragStartY = 0f

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

    private companion object {
        const val SCRIM_COLOR = 0x33000000
        const val CARD_COLOR = 0xFFFFFFFF.toInt()
        const val TEXT_COLOR = 0xFF222222.toInt()
        const val HINT_COLOR = 0xFF666666.toInt()
        const val DRAG_ALPHA = 0.7f
    }
}
