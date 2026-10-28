package com.weatherapp.libre.alerts.providers.meteoalarm

import android.util.Log
import com.weatherapp.libre.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

class MeteoGateHeaderInterceptor(
    private val apiKey: String = BuildConfig.METEOGATE_API_KEY
) : Interceptor {
    init {
        if (apiKey.isBlank()) {
            Log.w("MeteoGateClient", "EU alerts are disabled due to the missing key")
            throw IllegalStateException("MeteoGate API key is missing from BuildConfig")
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("apikey", apiKey)
            .addHeader("Accept", "application/json")
            .build()
        return chain.proceed(request)
    }
}

interface MeteoGateApi {
    @GET("collections/warnings/locations/{countryCode}")
    suspend fun getWarningsForCountry(
        @Path("countryCode") countryCode: String,
        @Query("datetime") datetime: String
    ): MeteoGateFeatureCollection

    @GET
    suspend fun getDetailedCapJson(@Url url: String): MeteoGateDetailedCap
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
