package com.dinner.crimeapp.data

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object PoliceApi {
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        })
        .build()

    private val moshi = Moshi.Builder()
        .build()

    val service: PoliceApiService = Retrofit.Builder()
        .baseUrl("https://data.police.uk/api/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(PoliceApiService::class.java)
}
