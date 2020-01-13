package com.mad.rates

import android.content.Context
import android.util.AttributeSet
import android.widget.ViewAnimator

class RatesViewAnimator @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ViewAnimator(context, attrs) {

    init {
        setInAnimation(context, android.R.anim.fade_in)
        setOutAnimation(context, android.R.anim.fade_out)
    }

    fun showLoad() = changeViewToIndex(LOAD_VIEW_INDEX)

    fun showContent() = changeViewToIndex(CONTENT_VIEW_INDEX)

    fun showError() = changeViewToIndex(ERROR_VIEW_INDEX)

    private fun changeViewToIndex(index: Int) {
        if (displayedChild != index) {
            displayedChild = index
        }
    }

    companion object {
        private const val LOAD_VIEW_INDEX = 0
        private const val CONTENT_VIEW_INDEX = 1
        private const val ERROR_VIEW_INDEX = 2
    }
}