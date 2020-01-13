package com.mad.rates

import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.math.BigDecimal

@UseExperimental(ExperimentalCoroutinesApi::class, FlowPreview::class)
class RatesViewModel(private val ratesRepository: RatesRepositoryInterface = RatesRepository(), private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) : ViewModel() {

    companion object {
        private const val TAG = "RatesViewModel"
        private const val RATES_REQUEST_INTERVAL = 1000L
    }

    private var updateJob: Job? = null

    @VisibleForTesting
    private val queryChannel = BroadcastChannel<CurrencyRateInfo>(Channel.CONFLATED)

    private lateinit var baseCurrencyRateInfo: CurrencyRateInfo

    @VisibleForTesting
    private val internalRatesResult: LiveData<Result<RatesModel>> = queryChannel.asFlow().mapLatest { base ->
        try {
            withContext(ioDispatcher) { ratesRepository.getRates(base.currency.symbol) }
        } catch (e: Throwable) {
            if (e !is CancellationException) Result.Failure<RatesModel>() else throw e
        }
    }.catch {
        emit(Result.Failure())
    }.asLiveData()

    val failure = internalRatesResult.map { result -> result is Result.Failure }

    val rates = MediatorLiveData<List<CurrencyRateInfo>>().apply {
        addSource(internalRatesResult) { result ->
            if (result is Result.Success) updateRates(result.data)
        }
    }

    private fun MediatorLiveData<List<CurrencyRateInfo>>.updateRates(rates: RatesModel) {
        val previousCurrencyRates = value

        value = if (previousCurrencyRates == null) {
            rates.toCurrencyRateInfoList(baseCurrencyRateInfo).toMutableList()
        } else {
            val orderMap = previousCurrencyRates.withIndex().associate { indexedValue ->
                indexedValue.value.currency.symbol to indexedValue.index
            }

            rates.toCurrencyRateInfoList(baseCurrencyRateInfo).sortedBy { currencyRateInfo ->
                orderMap[currencyRateInfo.currency.symbol]
            }.toMutableList().also {
                it.forEach { cr ->
                    Log.d(TAG, "$cr")
                }
            }
        }.apply { add(0, baseCurrencyRateInfo) }
    }

    fun setMultiplier(multiplier: BigDecimal?) {
        baseCurrencyRateInfo = baseCurrencyRateInfo.copy(multiplier = multiplier)
        Log.d(TAG, "setMultiplier $baseCurrencyRateInfo")
        startRepeatingRequests()
    }

    private fun startRepeatingRequests() {
        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            while (isActive) {
                Log.d(TAG, "queryChannel send $baseCurrencyRateInfo")
                queryChannel.offer(baseCurrencyRateInfo)
                delay(RATES_REQUEST_INTERVAL)
            }
        }
    }

    fun setBase(currencyRateInfo: CurrencyRateInfo) {
        baseCurrencyRateInfo = currencyRateInfo.copy(rate = BigDecimal.ONE, multiplier = currencyRateInfo.multiplier?.let { currencyRateInfo.rate * it })
        Log.d(TAG, "setBase $baseCurrencyRateInfo")
        startRepeatingRequests()
    }

}

@Suppress("UNCHECKED_CAST")
class RatesViewModelFactory(private val apiProvider: NetworkApiProviderInterface = NetworkApiProvider(), private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RatesViewModel::class.java)) {
            return RatesViewModel(RatesRepository(apiProvider), ioDispatcher) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}