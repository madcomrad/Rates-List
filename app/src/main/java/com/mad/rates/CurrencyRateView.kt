package com.mad.rates

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding

class CurrencyRateView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : ConstraintLayout(context, attrs, defStyleAttr) {

    val iconView: ImageView by lazy { findViewById<ImageView>(R.id.icon) }
    val titleView: TextView by lazy { findViewById<TextView>(R.id.title) }
    val subTitleView: TextView by lazy { findViewById<TextView>(R.id.subtitle) }
    val rateEditView: AppCompatEditText by lazy { findViewById<AppCompatEditText>(R.id.rateEdit) }

    init {
        LayoutInflater.from(context).inflate(R.layout.currency_rate_item, this)
        setPadding(resources.getDimensionPixelSize(R.dimen.margin_medium))
        setBackgroundColor(ContextCompat.getColor(context, R.color.backgroundPrimary))
    }
}