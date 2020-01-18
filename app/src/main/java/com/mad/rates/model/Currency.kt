package com.mad.rates.model

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources
import com.mad.network.RatesModel
import com.mad.rates.R
import java.math.BigDecimal

const val USD_SYMBOL = "USD"
const val USD_NAME = "US Dollar"

const val EUR_SYMBOL = "EUR"
const val EUR_NAME = "Euro"

const val AUD_SYMBOL = "AUD"
const val AUD_NAME = "Australian Dollar"

const val BGN_SYMBOL = "BGN"
const val BGN_NAME = "Bulgarian Lev"

const val GBP_SYMBOL = "GBP"
const val GBP_NAME = "Pound Sterling"

const val BRL_SYMBOL = "BRL"
const val BRL_NAME = "Brazilian Real"

const val CAD_SYMBOL = "CAD"
const val CAD_NAME = "Canadian Dollar"

const val CHF_SYMBOL = "CHF"
const val CHF_NAME = "Swiss Franc"

const val CNY_SYMBOL = "CNY"
const val CNY_NAME = "Yuan Renminbi"

const val CZK_SYMBOL = "CZK"
const val CZK_NAME = "Czech Koruna"

const val DKK_SYMBOL = "DKK"
const val DKK_NAME = "Danish Krone"

const val HKD_SYMBOL = "HKD"
const val HKD_NAME = "Hong Kong Dollar"

const val HRK_SYMBOL = "HRK"
const val HRK_NAME = "Croatian Kuna"

const val HUF_SYMBOL = "HUF"
const val HUF_NAME = "Forint"

const val IDR_SYMBOL = "IDR"
const val IDR_NAME = "Rupiah"

const val ILS_SYMBOL = "ILS"
const val ILS_NAME = "New Israeli Sheqel"

const val INR_SYMBOL = "INR"
const val INR_NAME = "Indian Rupee"

const val ISK_SYMBOL = "ISK"
const val ISK_NAME = "Iceland Krona"

const val JPY_SYMBOL = "JPY"
const val JPY_NAME = "Yen"

const val KRW_SYMBOL = "KRW"
const val KRW_NAME = "Won"

const val MXN_SYMBOL = "MXN"
const val MXN_NAME = "Mexican Peso"

const val MYR_SYMBOL = "MYR"
const val MYR_NAME = "Malaysian Ringgit"

const val NOK_SYMBOL = "NOK"
const val NOK_NAME = "Norwegian Krone"

const val NZD_SYMBOL = "NZD"
const val NZD_NAME = "New Zealand Dollar"

const val PHP_SYMBOL = "PHP"
const val PHP_NAME = "Philippine Peso"

const val PLN_SYMBOL = "PLN"
const val PLN_NAME = "Zloty"

const val RON_SYMBOL = "RON"
const val RON_NAME = "New Romanian Leu"

const val RUB_SYMBOL = "RUB"
const val RUB_NAME = "Russian Ruble"

const val SEK_SYMBOL = "SEK"
const val SEK_NAME = "Swedish Krona"

const val SGD_SYMBOL = "SGD"
const val SGD_NAME = "Singapore Dollar"

const val THB_SYMBOL = "THB"
const val THB_NAME = "Baht"

const val TRY_SYMBOL = "TRY"
const val TRY_NAME = "Turkish Lira"

const val ZAR_SYMBOL = "ZAR"
const val ZAR_NAME = "Rand"

val currencyFlagRes: Map<String, Int> = mapOf(
    USD_SYMBOL to R.drawable.ic_usd, EUR_SYMBOL to R.drawable.ic_eur, CAD_SYMBOL to R.drawable.ic_cad, SEK_SYMBOL to R.drawable.ic_sek)

val currencyName: Map<String, String> = mapOf(
    ZAR_SYMBOL to ZAR_NAME,
    TRY_SYMBOL to TRY_NAME,
    THB_SYMBOL to THB_NAME,
    SGD_SYMBOL to SGD_NAME,
    SEK_SYMBOL to SEK_NAME,
    RUB_SYMBOL to RUB_NAME,
    RON_SYMBOL to RON_NAME,
    PLN_SYMBOL to PLN_NAME,
    PHP_SYMBOL to PHP_NAME,
    NZD_SYMBOL to NZD_NAME,
    NOK_SYMBOL to NOK_NAME,
    MYR_SYMBOL to MYR_NAME,
    MXN_SYMBOL to MXN_NAME,
    KRW_SYMBOL to KRW_NAME,
    JPY_SYMBOL to JPY_NAME,
    ISK_SYMBOL to ISK_NAME,
    INR_SYMBOL to INR_NAME,
    ILS_SYMBOL to ILS_NAME,
    IDR_SYMBOL to IDR_NAME,
    HUF_SYMBOL to HUF_NAME,
    HRK_SYMBOL to HRK_NAME,
    HKD_SYMBOL to HKD_NAME,
    DKK_SYMBOL to DKK_NAME,
    CZK_SYMBOL to CZK_NAME,
    CNY_SYMBOL to CNY_NAME,
    CHF_SYMBOL to CHF_NAME,
    CAD_SYMBOL to CAD_NAME,
    BRL_SYMBOL to BRL_NAME,
    GBP_SYMBOL to GBP_NAME,
    BGN_SYMBOL to BGN_NAME,
    AUD_SYMBOL to AUD_NAME,
    EUR_SYMBOL to EUR_NAME,
    USD_SYMBOL to USD_NAME)

inline class Currency(val symbol: String) {
    fun getName() = currencyName[symbol] ?: ""

    fun getFlagDrawable(context: Context) = currencyFlagRes[symbol]?.let { resId -> AppCompatResources.getDrawable(context, resId) }
}

data class CurrencyRateInfo(val currency: Currency, val rate: BigDecimal, val multiplier: BigDecimal?)

fun RatesModel.toCurrencyRateInfoList(base: CurrencyRateInfo) = rates.map { (currencySymbol, rate) ->
    CurrencyRateInfo(Currency(currencySymbol), rate.toBigDecimal(), base.multiplier)
}

