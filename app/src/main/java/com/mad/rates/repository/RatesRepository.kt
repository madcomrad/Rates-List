package com.mad.rates.repository

import com.mad.network.RatesModel
import retrofit2.Response

class RatesRepository(private val apiProvider: com.mad.network.NetworkApiProviderInterface = com.mad.network.NetworkApiProvider()) : RatesRepositoryInterface {
    override suspend fun getRates(base: String) = apiProvider.ratesAPI.getRates(base)
}

interface RatesRepositoryInterface {
    suspend fun getRates(base: String): Response<RatesModel>
}