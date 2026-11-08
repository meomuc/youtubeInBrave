package com.meomuc.youtubeintablet

import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.R as MaterialR

/**
 * Bảng hiển thị nhanh (ticket #10), Material 3: thẻ nổi với bốn công tắc, mở từ nút ☰ góc dưới bên trái.
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

        val card = MaterialCardView(context).apply {
            radius = dp(CARD_RADIUS_DP).toFloat()
            // Chạm vào thẻ không lan xuống lớp mờ, nên không làm bảng đóng.
            isClickable = true
        }
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(CONTENT_PAD_DP), dp(CONTENT_PAD_DP), dp(CONTENT_PAD_DP), dp(CONTENT_PAD_DP / 2))
        }
        content.addView(TextView(context).apply {
            text = "Hiển thị nhanh"
            TextViewCompat.setTextAppearance(this, MaterialR.style.TextAppearance_Material3_TitleLarge)
            setTextColor(colorOnSurface())
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(CONTENT_PAD_DP / 2)
        })
        content.addView(row(R.drawable.ic_ph_video, "Shorts", "Mặc định tắt", initial.showShorts) { v ->
            update(options.copy(showShorts = v))
        })
        content.addView(row(R.drawable.ic_ph_queue, "Hàng đợi", "Mặc định bật", initial.showQueue) { v ->
            update(options.copy(showQueue = v))
        })
        content.addView(row(R.drawable.ic_ph_chat_circle, "Bình luận", "Mặc định bật", initial.showComments) { v ->
            update(options.copy(showComments = v))
        })
        content.addView(row(R.drawable.ic_ph_squares_four, "Video đề xuất", "Mặc định bật", initial.showRelated) { v ->
            update(options.copy(showRelated = v))
        })
        card.addView(content)
        addView(
            card,
            LayoutParams(dp(CARD_WIDTH_DP), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.START).apply {
                setMargins(dp(MENU_OFFSET_DP), 0, 0, dp(CARD_BOTTOM_MARGIN_DP))
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

    /**
     * Một hàng công tắc: icon Phosphor, nhãn và mô tả, công tắc Material 3 bên phải.
     * Cả hàng là vùng chạm (từ 56dp) và có hiệu ứng gợn khi bấm.
     */
    private fun row(
        iconRes: Int,
        label: String,
        hint: String,
        checked: Boolean,
        onToggle: (Boolean) -> Unit,
    ): View {
        val context = context
        val toggle = MaterialSwitch(context).apply {
            isChecked = checked
            contentDescription = label
            setOnCheckedChangeListener { _, isChecked -> onToggle(isChecked) }
        }
        val icon = ImageView(context).apply {
            setImageResource(iconRes)
            imageTintList = ColorStateList.valueOf(colorOnSurfaceVariant())
            contentDescription = null
        }
        val texts = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(context).apply {
                text = label
                TextViewCompat.setTextAppearance(this, MaterialR.style.TextAppearance_Material3_BodyLarge)
                setTextColor(colorOnSurface())
            })
            addView(TextView(context).apply {
                text = hint
                TextViewCompat.setTextAppearance(this, MaterialR.style.TextAppearance_Material3_BodyMedium)
                setTextColor(colorOnSurfaceVariant())
            })
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(ROW_MIN_HEIGHT_DP)
            setPadding(0, dp(ROW_PAD_DP), 0, dp(ROW_PAD_DP))
            isClickable = true
            background = rippleBackground()
            setOnClickListener { toggle.toggle() }
            addView(icon, LinearLayout.LayoutParams(dp(ICON_SIZE_DP), dp(ICON_SIZE_DP)).apply {
                rightMargin = dp(ICON_GAP_DP)
            })
            addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(toggle)
        }
    }

    /** Hiệu ứng gợn chuẩn của hệ thống khi chạm vào hàng. */
    private fun rippleBackground() = context.obtainStyledAttributes(
        intArrayOf(android.R.attr.selectableItemBackground)
    ).let { typed ->
        val drawable = typed.getDrawable(0)
        typed.recycle()
        drawable
    }

    private fun colorOnSurface(): Int =
        MaterialColors.getColor(this, MaterialR.attr.colorOnSurface)

    private fun colorOnSurfaceVariant(): Int =
        MaterialColors.getColor(this, MaterialR.attr.colorOnSurfaceVariant)

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

    private companion object {
        /** Lớp phủ khoảng 32% theo hướng dẫn Material cho hộp thoại và bảng nổi. */
        const val SCRIM_COLOR = 0x52000000
        const val CARD_WIDTH_DP = 320
        const val CARD_RADIUS_DP = 28
        const val CARD_BOTTOM_MARGIN_DP = 24
        /** Đặt bảng bên phải nút ☰ (kích thước nút 56dp + lề 24dp, như trước). */
        const val MENU_OFFSET_DP = 96
        const val CONTENT_PAD_DP = 24
        const val ROW_MIN_HEIGHT_DP = 56
        const val ROW_PAD_DP = 8
        const val ICON_SIZE_DP = 24
        const val ICON_GAP_DP = 16
    }
}
