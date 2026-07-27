package com.dinner.crimeapp.data

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object NominatimApi {
    private val userAgentInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("User-Agent", "com.dinner.crimeapp")
            .build()
        chain.proceed(request)
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(userAgentInterceptor)
        .build()

    private val moshi = Moshi.Builder().build()

    val service: NominatimApiService = Retrofit.Builder()
        .baseUrl("https://nominatim.openstreetmap.org/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(NominatimApiService::class.java)
}
