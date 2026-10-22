package com.weatherapp.libre.alerts.providers.meteoalarm

import com.weatherapp.libre.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

class MeteoGateHeaderInterceptor(
    private val apiKey: String = BuildConfig.METEOGATE_API_KEY
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("apikey", apiKey)
            .addHeader("Accept", "application/json")
            .build()
        return chain.proceed(request)
    }
}

interface MeteoGateApi {
    @GET("collections/warnings/position")
    suspend fun getWarningsForPosition(
        @Query("coords") coords: String
    ): MeteoGateFeatureCollection

    @GET("collections/warnings/items")
    suspend fun getWarningsItems(
        @Query("bbox") bbox: String? = null
    ): MeteoGateFeatureCollection
}

object MeteoGateClient {
    const val BASE_URL = "https://api.meteogate.eu/warnings/"

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(MeteoGateHeaderInterceptor(BuildConfig.METEOGATE_API_KEY))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: MeteoGateApi by lazy {
        retrofit.create(MeteoGateApi::class.java)
    }
}
