package com.mad.rates.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mad.rates.*
import com.mad.rates.model.Currency
import com.mad.rates.model.CurrencyRateInfo
import com.mad.rates.model.EUR_SYMBOL
import com.mad.rates.viewmodel.RatesViewModel
import com.mad.rates.viewmodel.RatesViewModelFactory
import com.mad.rates.widget.CurrencyRateView
import kotlinx.android.synthetic.main.activity_rates.*
import kotlinx.android.synthetic.main.currency_rate_item.view.*
import kotlinx.android.synthetic.main.rates_layout.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

@UseExperimental(ExperimentalCoroutinesApi::class)
class RatesActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "RatesActivity"

        // 10 decimals before dot and 2 after
        private const val MAX_VALUE_LENGTH = 13

        private const val INITIAL_BASE_SYMBOL = EUR_SYMBOL
        private const val INITIAL_RATE_MULTIPLIER = 1f
        private const val INITIAL_RATE_VALUE = 1f
    }

    private val ratesViewModel: RatesViewModel by lazy {
        ViewModelProvider(this, RatesViewModelFactory()).get(RatesViewModel::class.java)
    }

    private lateinit var currencyRatesAdapter: CurrencyRatesAdapter

    private val onItemClickListener = View.OnClickListener { view ->
        if (view.tag == null || view.tag !is ViewHolder) return@OnClickListener
        val position = (view.tag as ViewHolder).adapterPosition

        if (position > 0) {
            ratesViewModel.setBase(currencyRatesAdapter.currencyRates[position])
            ratesViewModel.startRepeatingRequests()
        }

        view.rateEdit.apply {
            showKeyboard()
            setSelection(text?.length ?: 0)
        }
    }

    private val onMultiplierChange: (EditText, Editable?) -> Unit = { editText, input ->
        if (editText.isFocused) {
            val multiplier = if (input.isNullOrBlank()) null else input.toString().toBigDecimal()
            ratesViewModel.setMultiplier(multiplier)
            ratesViewModel.startRepeatingRequests()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rates)
        contentContainer.showLoad()

        ratesRecycler.apply {
            layoutManager = LinearLayoutManager(context)
            currencyRatesAdapter = CurrencyRatesAdapter(onItemClickListener, onMultiplierChange).apply {
                registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                    override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                        (layoutManager as LinearLayoutManager).scrollToPosition(0)
                    }
                })
            }
            adapter = currencyRatesAdapter
            hideSoftKeyboardOnScroll(ratesRecycler)
        }

        ratesViewModel.rates.observeNonNull(this) { rates ->
            contentContainer.showContent()
            currencyRatesAdapter.update(rates)
        }

        ratesViewModel.failure.observeNonNull(this) { isFailure ->
            if (isFailure) contentContainer.showError()
        }

        ratesViewModel.setBase(
            CurrencyRateInfo(Currency(INITIAL_BASE_SYMBOL), INITIAL_RATE_VALUE.toBigDecimal(), INITIAL_RATE_MULTIPLIER.toBigDecimal()))
        ratesViewModel.startRepeatingRequests()
    }

    private inner class CurrencyRatesAdapter(val onClickListener: View.OnClickListener, val onMultiplierChange: (EditText, Editable?) -> Unit) : RecyclerView.Adapter<ViewHolder>() {

        private val diffCallback by lazy { CurrencyRatesDiffCallback() }

        var currencyRates = mutableListOf<CurrencyRateInfo>()

        override fun getItemCount(): Int = currencyRates.size

        fun update(newRates: List<CurrencyRateInfo>) {
            if (currencyRates.isEmpty()) {
                currencyRates.addAll(newRates)
                notifyDataSetChanged()
            } else {
                lifecycleScope.launch {
                    val diffResult = withContext(Dispatchers.IO) {
                        DiffUtil.calculateDiff(diffCallback.apply {
                            this.newItems = newRates
                            this.oldItems = currencyRates.toList()
                        })
                    }
                    currencyRates.apply {
                        clear()
                        addAll(newRates)
                    }
                    diffResult.dispatchUpdatesTo(this@CurrencyRatesAdapter)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(CurrencyRateView(parent.context).apply {
                rateEdit.filters = mutableListOf(
                    LeadingZeroesInputFilter(), CurrencyFormatInputFilter(), InputFilter.LengthFilter(MAX_VALUE_LENGTH)).toTypedArray()
                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            }, onClickListener, onMultiplierChange)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(currencyRates[position])
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
            payloads.takeIf { it.isNotEmpty() }?.first().also { payload ->
                when (payload) {
                    is ChangePayloadType.Rate -> holder.bind(payload)
                    null -> super.onBindViewHolder(holder, position, payloads)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private inner class ViewHolder(item: CurrencyRateView, onClickListener: View.OnClickListener, onMultiplierChange: (EditText, Editable?) -> Unit) : RecyclerView.ViewHolder(item) {

        val textWatcher: TextWatcher

        init {
            with(itemView as CurrencyRateView) {
                tag = this@ViewHolder
                setOnClickListener(onClickListener)
                rateEditView.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) this@with.performClick()
                }
                textWatcher = rateEditView.doAfterTextChanged { input ->
                    if (rateEditView.isFocused) {
                        if (input != null && input.isNotEmpty() && input.length == 1 && input.first().isPoint()) {
                            rateEditView.text = null
                        }
                        else if (input != null && input.isNotEmpty() && input.first().isPoint()) {
                            rateEditView.setText("$ZERO$input")
                        } else {
                            onMultiplierChange(rateEditView, input)
                        }
                    }
                }
            }
        }

        fun bind(currencyRateInfo: CurrencyRateInfo) {
            val (currency, rate, multiplier) = currencyRateInfo
            with(itemView as CurrencyRateView) {
                titleView.text = currency.symbol
                subTitleView.text = currency.getName()
                iconView.setImageDrawable(currency.getFlagDrawable(context))
                rateEditView.setTextQuietly(multiplier?.let { rate.multiply(it).toString() }, textWatcher)
            }
        }

        fun bind(payload: ChangePayloadType.Rate) {
            (itemView as CurrencyRateView).rateEditView.setTextQuietly(payload.multiplier?.let { payload.rate.multiply(it).toString() }, textWatcher)
        }
    }

    private inner class CurrencyRatesDiffCallback : DiffUtil.Callback() {

        lateinit var newItems: List<CurrencyRateInfo>
        lateinit var oldItems: List<CurrencyRateInfo>

        override fun getOldListSize(): Int = oldItems.size
        override fun getNewListSize(): Int = newItems.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val newItem = if (newItems.size > newItemPosition) newItems[newItemPosition] else return false
            val oldItem = if (oldItems.size > oldItemPosition) oldItems[oldItemPosition] else return false
            return oldItem.currency.symbol == newItem.currency.symbol
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val newItem = if (newItems.size > newItemPosition) newItems[newItemPosition] else return false
            val oldItem = if (oldItems.size > oldItemPosition) oldItems[oldItemPosition] else return false
            return oldItem == newItem
        }

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            val newItem = if (newItems.size > newItemPosition) newItems[newItemPosition] else return null
            val oldItem = if (oldItems.size > oldItemPosition) oldItems[oldItemPosition] else return null
            return if (oldItem.multiplier != newItem.multiplier || oldItem.rate != newItem.rate) {
                if (newItemPosition != 0) ChangePayloadType.Rate(
                    newItem.rate, newItem.multiplier) else ChangePayloadType.Skip
            } else null
        }
    }

    sealed class ChangePayloadType {
        class Rate(val rate: BigDecimal, val multiplier: BigDecimal?) : ChangePayloadType()
        object Skip : ChangePayloadType()
    }
}