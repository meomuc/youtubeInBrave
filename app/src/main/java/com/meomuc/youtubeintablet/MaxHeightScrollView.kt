package com.meomuc.youtubeintablet

import android.content.Context
import android.widget.ScrollView

/**
 * ScrollView có chiều cao tối đa [maxHeightPx]: nội dung ngắn thì bọc vừa nội dung,
 * nội dung dài thì dừng ở mức tối đa và cuộn. Dùng cho danh sách trong bảng hàng đợi.
 */
class MaxHeightScrollView(
    context: Context,
    private val maxHeightPx: Int,
) : ScrollView(context) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (measuredHeight > maxHeightPx) {
            setMeasuredDimension(measuredWidth, maxHeightPx)
        }
    }
}
