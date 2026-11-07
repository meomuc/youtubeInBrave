package com.meomuc.youtubeintablet

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

/**
 * Bảng hiển thị nhanh (ticket #10): 4 mục bật/tắt, mở từ nút ☰ góc dưới bên trái.
 * Bấm ra ngoài bảng thì đóng. Mỗi lần đổi một mục gọi [onChange] với lựa chọn mới.
 */
class QuickPanel(
    context: Context,
    initial: DisplayOptions,
    private val onChange: (DisplayOptions) -> Unit,
) : FrameLayout(context) {

    /** Lựa chọn hiện tại của bảng. */
    var options: DisplayOptions = initial
        private set

    val isOpen: Boolean
        get() = visibility == View.VISIBLE

    init {
        visibility = View.GONE
        // Lớp mờ phủ toàn màn hình: chạm vào đây thì đóng bảng.
        setBackgroundColor(SCRIM_COLOR)
        isClickable = true
        setOnClickListener { hide() }

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(CARD_COLOR)
            setPadding(dp(16), dp(16), dp(16), dp(16))
            // Chạm vào thẻ không lan xuống lớp mờ, nên không làm bảng đóng.
            isClickable = true
        }
        card.addView(TextView(context).apply {
            text = "Hiển thị nhanh"
            setTextColor(TEXT_COLOR)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        card.addView(row("Shorts", "Mặc định tắt", initial.showShorts) { v ->
            update(options.copy(showShorts = v))
        })
        card.addView(row("Hàng đợi", "Mặc định bật", initial.showQueue) { v ->
            update(options.copy(showQueue = v))
        })
        card.addView(row("Bình luận", "Mặc định bật", initial.showComments) { v ->
            update(options.copy(showComments = v))
        })
        card.addView(row("Video đề xuất", "Mặc định bật", initial.showRelated) { v ->
            update(options.copy(showRelated = v))
        })
        addView(
            card,
            LayoutParams(dp(300), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.START).apply {
                setMargins(dp(96), 0, 0, dp(24))
            }
        )
    }

    fun show() {
        visibility = View.VISIBLE
    }

    fun hide() {
        visibility = View.GONE
    }

    private fun update(value: DisplayOptions) {
        options = value
        onChange(value)
    }

    /** Một hàng: nhãn + mô tả bên trái, công tắc bên phải. Chạm cả hàng để đổi công tắc. */
    private fun row(
        label: String,
        hint: String,
        checked: Boolean,
        onToggle: (Boolean) -> Unit,
    ): View {
        val context = context
        val toggle = Switch(context).apply {
            isChecked = checked
            contentDescription = label
            setOnCheckedChangeListener { _, isChecked -> onToggle(isChecked) }
        }
        val texts = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(context).apply {
                text = label
                setTextColor(TEXT_COLOR)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            })
            addView(TextView(context).apply {
                text = hint
                setTextColor(HINT_COLOR)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            })
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, dp(12))
            isClickable = true
            setOnClickListener { toggle.toggle() }
            addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(toggle)
        }
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

    private companion object {
        const val SCRIM_COLOR = 0x33000000
        const val CARD_COLOR = 0xFFFFFFFF.toInt()
        const val TEXT_COLOR = 0xFF222222.toInt()
        const val HINT_COLOR = 0xFF666666.toInt()
    }
}
