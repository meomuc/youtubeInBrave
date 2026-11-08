package com.meomuc.youtubeintablet

import android.content.Context
import android.content.res.ColorStateList
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.R as MaterialR

/**
 * Bảng hàng đợi (ticket #13), Material 3: liệt kê [queue] theo thứ tự phát, mở từ nút "Hàng đợi" góc dưới bên trái.
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
            TextViewCompat.setTextAppearance(this, MaterialR.style.TextAppearance_Material3_TitleLarge)
            setTextColor(colorOnSurface())
        }
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(CONTENT_PAD_DP), dp(CONTENT_PAD_DP), dp(CONTENT_PAD_DP), dp(CONTENT_PAD_DP / 2))
            addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(CONTENT_PAD_DP / 2)
            })
            addView(
                ScrollView(context).apply { addView(listContainer) },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            )
        }
        val card = MaterialCardView(context).apply {
            radius = dp(CARD_RADIUS_DP).toFloat()
            // Chạm vào thẻ không lan xuống lớp mờ, nên không làm bảng đóng.
            isClickable = true
            addView(content)
        }
        addView(
            card,
            LayoutParams(dp(CARD_WIDTH_DP), (context.resources.displayMetrics.heightPixels * 0.7f).toInt(), Gravity.BOTTOM or Gravity.START).apply {
                setMargins(dp(MENU_OFFSET_DP), 0, 0, dp(CARD_BOTTOM_MARGIN_DP))
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
                TextViewCompat.setTextAppearance(this, MaterialR.style.TextAppearance_Material3_BodyMedium)
                setTextColor(colorOnSurfaceVariant())
                setPadding(0, dp(12), 0, dp(12))
            })
            return
        }
        items.forEachIndexed { index, item ->
            listContainer.addView(row(index, item))
        }
    }

    /** Một mục: số thứ tự, tiêu đề, tay cầm kéo, nút Phát tiếp theo và nút Xóa. */
    private fun row(index: Int, item: QueueItem): View {
        val name = item.title.ifBlank { item.videoId }
        val label = TextView(context).apply {
            text = "${index + 1}. $name"
            TextViewCompat.setTextAppearance(this, MaterialR.style.TextAppearance_Material3_BodyLarge)
            setTextColor(colorOnSurface())
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        }
        val playNext = iconButton(R.drawable.ic_ph_skip_forward, "Phát tiếp theo: $name").apply {
            isEnabled = index > 0
            alpha = if (index > 0) 1f else DISABLED_ALPHA
            setOnClickListener {
                queue.playNext(item)
                onChanged()
            }
        }
        val remove = iconButton(R.drawable.ic_ph_trash, "Xóa khỏi hàng đợi: $name").apply {
            setOnClickListener {
                queue.removeAt(index)
                onChanged()
            }
        }
        // Tay cầm kéo đổi thứ tự (ticket #15). Chạm xuống thì ngăn ScrollView cuộn.
        val handle = ImageView(context).apply {
            setImageResource(R.drawable.ic_ph_dots_six_vertical)
            imageTintList = ColorStateList.valueOf(colorOnSurfaceVariant())
            contentDescription = "Kéo để đổi thứ tự: $name"
            tooltipText = "Kéo để đổi thứ tự"
            setPadding(dp(HANDLE_PAD_DP), dp(HANDLE_PAD_DP), dp(HANDLE_PAD_DP), dp(HANDLE_PAD_DP))
            setOnTouchListener { v, event -> onHandleTouch(v, event, index) }
        }
        val size = dp(TOUCH_SIZE_DP)
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(ROW_MIN_HEIGHT_DP)
            setPadding(0, dp(4), 0, dp(4))
            addView(label, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(handle, LinearLayout.LayoutParams(size, size))
            addView(playNext, LinearLayout.LayoutParams(size, size).apply { leftMargin = dp(MARGIN_DP) })
            addView(remove, LinearLayout.LayoutParams(size, size).apply { leftMargin = dp(MARGIN_DP) })
        }
    }

    /** Nút chỉ có icon Phosphor, theo kiểu nút icon tonal của Material 3; có nhãn đọc màn hình và tooltip. */
    private fun iconButton(iconRes: Int, label: String): MaterialButton =
        MaterialButton(context, null, MaterialR.attr.materialIconButtonFilledTonalStyle).apply {
            setIconResource(iconRes)
            iconSize = dp(ICON_SIZE_DP)
            iconPadding = 0
            iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            text = null
            minimumWidth = dp(TOUCH_SIZE_DP)
            minimumHeight = dp(TOUCH_SIZE_DP)
            contentDescription = label
            tooltipText = label
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

    private fun colorOnSurface(): Int =
        MaterialColors.getColor(this, MaterialR.attr.colorOnSurface)

    private fun colorOnSurfaceVariant(): Int =
        MaterialColors.getColor(this, MaterialR.attr.colorOnSurfaceVariant)

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

    private companion object {
        /** Lớp phủ khoảng 32% theo hướng dẫn Material cho hộp thoại và bảng nổi. */
        const val SCRIM_COLOR = 0x52000000
        const val CARD_WIDTH_DP = 360
        const val CARD_RADIUS_DP = 28
        const val CARD_BOTTOM_MARGIN_DP = 24
        /** Đặt bảng bên phải nút ☰ (kích thước nút 56dp + lề 24dp, như trước). */
        const val MENU_OFFSET_DP = 96
        const val CONTENT_PAD_DP = 24
        const val ROW_MIN_HEIGHT_DP = 56
        const val DRAG_ALPHA = 0.7f
        const val DISABLED_ALPHA = 0.38f
        /** Cỡ chạm tối thiểu theo Material: 48dp; dùng 48dp cho nút và tay cầm. */
        const val TOUCH_SIZE_DP = 48
        const val ICON_SIZE_DP = 22
        const val HANDLE_PAD_DP = 10
        const val MARGIN_DP = 8
    }
}
