package com.mad.rates

import android.util.Log
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

private const val TAG = "NetworkApiProvider"

private const val RATES_BASE_URL = "https://revolut.duckdns.org/"

class NetworkApiProvider(ratesBaseUrl: String = RATES_BASE_URL) : NetworkApiProviderInterface {

    override val ratesAPI: RatesAPI = Retrofit.Builder().apply {
        baseUrl(ratesBaseUrl)
        client(okHttpClient)
        addConverterFactory(gsonConverterFactory)
    }.build().create(RatesAPI::class.java)
}

interface NetworkApiProviderInterface {
    val ratesAPI: RatesAPI
}

private val gsonConverterFactory: GsonConverterFactory = GsonConverterFactory.create(Gson())
private val okHttpClient = OkHttpClient.Builder().apply {
    readTimeout(10, TimeUnit.SECONDS)
    writeTimeout(10, TimeUnit.SECONDS)
    addInterceptor(HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
        override fun log(message: String) {
            Log.d(TAG, message)
        }
    }).apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    })
}.build()


interface RatesAPI {

    @GET("latest")
    suspend fun getRates(@Query("base") base: String): Response<RatesModel>
}