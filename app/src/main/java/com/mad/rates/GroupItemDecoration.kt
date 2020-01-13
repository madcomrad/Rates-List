package com.mad.rates

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView

class HeaderDecoration(val createHeaderView: (Context) -> HeaderView) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        val topOffset = if (position != RecyclerView.NO_POSITION && position == 0) {
            getHeaderView(parent).measuredHeight
        } else {
            0
        }
        outRect.set(0, topOffset, 0, 0)
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val child = if (parent.childCount > 0) parent.getChildAt(0) else return
        val adapterPosition = parent.getChildAdapterPosition(child)
        if (adapterPosition != 0) return

        c.save()
        val header = getHeaderView(parent)
        val headerTop = (child.y.toInt() - header.measuredHeight)
        c.translate(0f, headerTop.toFloat())
        header.draw(c)
        c.restore()
    }

    private fun getHeaderView(parent: RecyclerView): View {
        val header = createHeaderView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        val widthSpec = View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        header.measure(widthSpec, heightSpec)
        header.layout(0, 0, header.measuredWidth, header.measuredHeight)
        return header
    }
}

class HeaderView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = R.attr.headerStyle) : AppCompatTextView(context, attrs, defStyleAttr)