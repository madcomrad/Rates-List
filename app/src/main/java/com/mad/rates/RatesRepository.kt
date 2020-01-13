package com.mad.rates

class RatesRepository(private val apiProvider: NetworkApiProviderInterface = NetworkApiProvider()) : RatesRepositoryInterface {
    override suspend fun getRates(base: String) = apiProvider.ratesAPI.getRates(base).toResult()
}

interface RatesRepositoryInterface {
    suspend fun getRates(base: String): Result<RatesModel>
}