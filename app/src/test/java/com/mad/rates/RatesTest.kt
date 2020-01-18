package com.mad.rates

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.mad.network.RatesModel
import com.mad.rates.model.Currency
import com.mad.rates.model.CurrencyRateInfo
import com.mad.rates.repository.RatesRepositoryInterface
import com.mad.rates.utils.MainCoroutineScopeRule
import com.mad.rates.utils.captureValues
import com.mad.rates.utils.getValueForTest
import com.mad.rates.viewmodel.RatesViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.*
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import retrofit2.Response
import java.lang.IllegalStateException
import java.math.BigDecimal
import java.net.UnknownHostException
import kotlin.random.Random
import kotlin.random.nextUInt

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class RatesTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    private lateinit var viewModel: RatesViewModel

    private val successViewModelGeneratedRates
        get() = RatesViewModel(object : RatesRepositoryInterface {
            override suspend fun getRates(base: String): Response<RatesModel> = Response.success(generateRatesModel(base))
        }, coroutineScope.dispatcher)

    private val successViewModelFixedRates
        get() = RatesViewModel(object : RatesRepositoryInterface {
            override suspend fun getRates(base: String): Response<RatesModel> {
                val ratesModel = when (base) {
                    "EUR" -> testSuccessRatesModelForEur
                    "AUD" -> testSuccessRatesModelForAud
                    else -> throw IllegalStateException("Wrong base")
                }
                return Response.success(ratesModel)
            }
        }, coroutineScope.dispatcher)

    private val failureViewModel
        get() = RatesViewModel(object : RatesRepositoryInterface {
            override suspend fun getRates(base: String): Response<RatesModel> {
                throw UnknownHostException()
            }
        }, coroutineScope.dispatcher)

    @Test
    fun testInitialValues() = coroutineScope.runBlockingTest {
        viewModel = successViewModelGeneratedRates
        Assert.assertNull(viewModel.rates.getValueForTest())
        Assert.assertNull(viewModel.failure.getValueForTest())
    }

    @Test
    fun setBaseSuccess() = coroutineScope.runBlockingTest {
        viewModel = successViewModelGeneratedRates
        val base = CurrencyRateInfo(Currency("EUR"), BigDecimal.ONE, BigDecimal.ONE)
        viewModel.setBase(base)
        viewModel.queryChannel.offer(base)
        val currencyRates = viewModel.rates.getValueForTest()
        Assert.assertNotNull(currencyRates)
        Assert.assertEquals(currencyRates?.first()?.currency?.symbol, "EUR")
        Assert.assertEquals(currencyRates?.size, listOfCurrencySymbols.size)
        Assert.assertFalse(viewModel.failure.getValueForTest() ?: true)
    }

    @Test
    fun setBaseFailure() = coroutineScope.runBlockingTest {
        viewModel = failureViewModel
        val base = CurrencyRateInfo(Currency("EUR"), BigDecimal.ONE, BigDecimal.ONE)
        viewModel.setBase(base)
        viewModel.queryChannel.offer(base)
        Assert.assertNull(viewModel.rates.getValueForTest())
        Assert.assertTrue(viewModel.failure.getValueForTest() ?: false)
    }

    @Test
    fun setBaseRepeatedlySuccess() = coroutineScope.runBlockingTest {
        viewModel = successViewModelGeneratedRates
        viewModel.rates.captureValues {
            repeat(3) { i ->
                val base = CurrencyRateInfo(Currency(listOfCurrencySymbols[i]), BigDecimal.ONE, BigDecimal.ONE)
                viewModel.setBase(base)
                viewModel.queryChannel.offer(base)
            }

            Assert.assertFalse(viewModel.failure.getValueForTest() ?: true)
            Assert.assertTrue(values.isNotEmpty())
            Assert.assertTrue(values.size == 3)
            values.forEachIndexed { i, currencyRates ->
                Assert.assertEquals(currencyRates?.first()?.currency?.symbol, listOfCurrencySymbols[i])
                Assert.assertEquals(currencyRates?.size, listOfCurrencySymbols.size)
            }
        }
    }

    @ExperimentalUnsignedTypes
    @Test
    fun setMultiplier() = coroutineScope.runBlockingTest {
        viewModel = successViewModelFixedRates

        val initialMultiplier = BigDecimal.ONE
        val base = CurrencyRateInfo(Currency("EUR"), BigDecimal.ONE, initialMultiplier)
        viewModel.setBase(base)
        viewModel.queryChannel.offer(base)
        Assert.assertTrue(viewModel.rates.getValueForTest()?.all { it.multiplier == initialMultiplier } ?: false)
        viewModel.rates.captureValues {
            repeat(10) {
                viewModel.setMultiplier(Random.nextUInt().toString().toBigDecimal())
            }
            Assert.assertTrue(values.all { currencyRates -> currencyRates?.all { it.multiplier == initialMultiplier } ?: false })
        }
    }

    @Test
    fun checkMultiplierOnBaseChange() = coroutineScope.runBlockingTest {
        viewModel = successViewModelFixedRates

        val initialMultiplier = BigDecimal.TEN
        val base = CurrencyRateInfo(Currency("EUR"), BigDecimal.ONE, initialMultiplier)
        viewModel.setBase(base)
        viewModel.queryChannel.offer(base)

        var currencyRateAUD = viewModel.rates.getValueForTest()?.find { it.currency.symbol == "AUD" }
        val audRate = currencyRateAUD?.rate
        viewModel.setBase(requireNotNull(currencyRateAUD))
        viewModel.queryChannel.offer(requireNotNull(currencyRateAUD))

        currencyRateAUD = viewModel.rates.getValueForTest()?.find { it.currency.symbol == "AUD" }
        Assert.assertEquals(currencyRateAUD?.multiplier, initialMultiplier * requireNotNull(audRate))
    }

    private fun generateRatesModel(base: String): RatesModel =
        RatesModel(base = base, date = "2018-09-06", rates = listOfCurrencySymbols.toMutableList().apply { remove(base) }.associateWith { Random.nextFloat() })

    private val listOfCurrencySymbols = listOf(
        "AUD",
        "BGN",
        "BRL",
        "CAD",
        "CHF",
        "CNY",
        "CZK",
        "DKK",
        "GBP",
        "HKD",
        "HRK",
        "HUF",
        "IDR",
        "ILS",
        "INR",
        "ISK",
        "JPY",
        "KRW",
        "MXN",
        "MYR",
        "NOK",
        "NZD",
        "PHP",
        "PLN",
        "RON",
        "RUB",
        "SEK",
        "SGD",
        "THB",
        "TRY",
        "USD",
        "ZAR",
        "EUR")

    private val testSuccessRatesModelForEur = RatesModel(
        "EUR", "2018-09-06", mapOf(
            "AUD" to 1.6138f,
            "BGN" to 1.9527f,
            "BRL" to 4.7842f,
            "CAD" to 1.5314f,
            "CHF" to 1.1257f,
            "CNY" to 7.9324f,
            "CZK" to 25.674f,
            "DKK" to 7.4448f,
            "GBP" to 0.89681f,
            "HKD" to 9.1179f,
            "HRK" to 7.4223f,
            "HUF" to 325.97f,
            "IDR" to 17296.0f,
            "ILS" to 4.164f,
            "INR" to 83.584f,
            "ISK" to 127.6f,
            "JPY" to 129.34f,
            "KRW" to 1302.7f,
            "MXN" to 22.33f,
            "MYR" to 4.8043f,
            "NOK" to 9.7604f,
            "NZD" to 1.7605f,
            "PHP" to 62.492f,
            "PLN" to 4.3114f,
            "RON" to 4.6311f,
            "RUB" to 79.448f,
            "SEK" to 10.574f,
            "SGD" to 1.5975f,
            "THB" to 38.069f,
            "TRY" to 7.6161f,
            "USD" to 1.1615f,
            "ZAR" to 17.795f))

    private val testSuccessRatesModelForAud = RatesModel(
        "AUD", "2018-09-06", mapOf(
            "BGN" to 1.2089f,
            "BRL" to 2.9619f,
            "CAD" to 0.94809f,
            "CHF" to 0.69692f,
            "CNY" to 4.911f,
            "CZK" to 15.895f,
            "DKK" to 4.6091f,
            "GBP" to 0.55522f,
            "HKD" to 5.6449f,
            "HRK" to 4.5951f,
            "HUF" to 201.81f,
            "IDR" to 10708.0f,
            "ILS" to 2.5779f,
            "INR" to 51.748f,
            "ISK" to 78.996f,
            "JPY" to 80.079f,
            "KRW" to 806.48f,
            "MXN" to 13.825f,
            "MYR" to 2.9744f,
            "NOK" to 6.0427f,
            "NZD" to 1.09f,
            "PHP" to 38.69f,
            "PLN" to 2.6692f,
            "RON" to 2.8672f,
            "RUB" to 49.187f,
            "SEK" to 6.5466f,
            "SGD" to 0.98899f,
            "THB" to 23.569f,
            "TRY" to 4.7152f,
            "USD" to 0.71913f,
            "ZAR" to 11.017f,
            "EUR" to 0.61812f))
}