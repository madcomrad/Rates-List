package com.mad.network


import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.net.ssl.HttpsURLConnection


class RatesTest {

//    @get:Rule
//    var rule: TestRule = InstantTaskExecutorRule()
//
//    private val mainDispatcher = TestCoroutineDispatcher()

    private val mockWebServer = MockWebServer()

    private val gson = Gson()

    private val dispatcher: Dispatcher = object : Dispatcher() {
        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse {
            when (request.path) {
                "/latest?base=EUR" -> return MockResponse().setResponseCode(HttpsURLConnection.HTTP_OK).setBody(gson.toJson(testSuccessRatesModelForEur))
                "/latest?base=AUD" -> return MockResponse().setResponseCode(HttpsURLConnection.HTTP_OK).setBody(gson.toJson(testSuccessRatesModelForAud))
            }
            return MockResponse().setResponseCode(404)
        }
    }

    @Before
    fun setUp() {
//        Dispatchers.setMain(mainDispatcher)
        mockWebServer.start()
        mockWebServer.dispatcher = dispatcher
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
//        Dispatchers.resetMain()
    }

    @Test
    fun requestTest() {
        val networkApiProvider: NetworkApiProviderInterface = TestNetworkApiProvider(mockWebServer.url("").toString())
        runBlocking {
            val responseEur = networkApiProvider.ratesAPI.getRates("EUR")
            mockWebServer.takeRequest().also { recordedRequest ->
                Assert.assertEquals(recordedRequest.method, "GET")
                Assert.assertEquals(recordedRequest.path, "/latest?base=EUR")
            }
            Assert.assertEquals(responseEur.isSuccessful, true)
            var ratesModel = responseEur.body()
            Assert.assertNotNull(ratesModel)
            Assert.assertEquals(ratesModel?.base, "EUR")
            Assert.assertTrue(ratesModel?.rates?.containsKey("EUR") == false)
            Assert.assertEquals(ratesModel, testSuccessRatesModelForEur)

            val responseAud = networkApiProvider.ratesAPI.getRates("AUD")
            mockWebServer.takeRequest().also { recordedRequest ->
                Assert.assertEquals(recordedRequest.method, "GET")
                Assert.assertEquals(recordedRequest.path, "/latest?base=AUD")
            }
            Assert.assertEquals(responseAud.isSuccessful, true)
            ratesModel = responseAud.body()
            Assert.assertNotNull(ratesModel)
            Assert.assertEquals(ratesModel?.base, "AUD")
            Assert.assertTrue(ratesModel?.rates?.containsKey("AUD") == false)
            Assert.assertEquals(ratesModel, testSuccessRatesModelForAud)
        }
    }

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

class TestNetworkApiProvider(ratesBaseUrl: String) : NetworkApiProviderInterface {
    override val ratesAPI: RatesAPI = Retrofit.Builder().apply {
        baseUrl(ratesBaseUrl)
        client(OkHttpClient.Builder().build())
        addConverterFactory(GsonConverterFactory.create(Gson()))
    }.build().create(RatesAPI::class.java)
}
