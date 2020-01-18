package com.mad.rates

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.text.InputFilter
import android.text.Spanned
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Response
import java.math.BigDecimal

private const val EMPTY_STRING = ""

private const val DOT = '.'
private const val COMMA = ','
const val ZERO = '0'

sealed class Result<T> {
    class Success<T>(val data: T) : Result<T>()
    class Failure<T> : Result<T>()
}

fun <T> Response<T>.toResult(): Result<T> = if (this.isSuccessful && this.body() != null) {
    Result.Success(body()!!)
} else {
    Result.Failure()
}

fun <T> LiveData<T>.observeNonNull(owner: LifecycleOwner, f: (T) -> Unit) {
    this.observe(owner, Observer<T> { t -> t?.let(f) })
}

fun Char.isPoint(): Boolean = this == DOT || this == COMMA

class LeadingZeroesInputFilter : InputFilter {
    override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
        if (end > start) {
            val resultingTxt = (dest.substring(0, dstart) + source.subSequence(start, end) + dest.substring(dend))
            if (resultingTxt.length > 1 && resultingTxt[0] == ZERO && !resultingTxt[1].isPoint()) {
                return dest.subSequence(dstart, dend)
            }
        }
        return null
    }
}

class CurrencyFormatInputFilter : InputFilter {
    companion object {
        private const val DIGITS_AFTER_DOT = 2
    }

    override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
        if (end > start) {
            val resultingTxt = (dest.substring(0, dstart) + source.subSequence(start, end) + dest.substring(dend))
            return resultingTxt.indexOfFirst { char -> char.isPoint() }.takeIf { it != -1 }?.let { pointIndex ->
                if (resultingTxt.substring(pointIndex).length - 1 > DIGITS_AFTER_DOT) {
                    if (dest.isEmpty() || (dstart == 0 && dend == dest.lastIndex)) {
                        try {
                            resultingTxt.toBigDecimal().setScale(DIGITS_AFTER_DOT, BigDecimal.ROUND_HALF_DOWN).toString()
                        } catch (e: Exception) {
                            EMPTY_STRING
                        }
                    } else {
                        EMPTY_STRING
                    }
                } else {
                    null
                }
            }
        }
        return null
    }
}

fun View.hideSoftKeyboard() {
    (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(windowToken, 0)
}

fun View.showKeyboard() {
    postDelayed(
        { if (requestFocus()) (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(this, InputMethodManager.SHOW_IMPLICIT) },
        resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
}

fun Activity.hideSoftKeyboardOnScroll(recyclerView: RecyclerView) {
    val hideKeyboardOnScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                recyclerView.hideSoftKeyboard()
            }
        }
    }

    val rootView = window.findViewById<ViewGroup>(Window.ID_ANDROID_CONTENT)
    val screenHeight = DisplayMetrics().let {
        windowManager.defaultDisplay.getMetrics(it)
        it.heightPixels
    }
    rootView.viewTreeObserver.addOnGlobalLayoutListener {
        val rect = Rect().apply { rootView.getWindowVisibleDisplayFrame(this) }
        val keyboardHeight = screenHeight - rect.bottom
        // 0.15 ratio is perhaps enough to determine keyboard height.
        if (keyboardHeight > screenHeight * 0.15) {
            recyclerView.addOnScrollListener(hideKeyboardOnScrollListener)
        } else {
            recyclerView.removeOnScrollListener(hideKeyboardOnScrollListener)
        }
    }
}

fun EditText.setTextQuietly(text: CharSequence?, textWatcher: TextWatcher) {
    removeTextChangedListener(textWatcher)
    setText(text)
    addTextChangedListener(textWatcher)
}