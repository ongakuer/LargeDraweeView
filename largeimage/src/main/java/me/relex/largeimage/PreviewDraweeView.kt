package me.relex.largeimage

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.facebook.drawee.generic.GenericDraweeHierarchy
import com.facebook.drawee.view.SimpleDraweeView

internal class PreviewDraweeView : SimpleDraweeView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context, attrs, defStyle
    )

    constructor(
        context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    constructor(context: Context, hierarchy: GenericDraweeHierarchy) : super(context, hierarchy)

    // Disable Touch Event
    override fun dispatchTouchEvent(event: MotionEvent): Boolean = true
}
