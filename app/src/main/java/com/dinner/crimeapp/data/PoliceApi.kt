package com.dinner.crimeapp.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object PoliceApi {
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory()) // reflection-based fallback for @JsonClass models
        .build()

    val service: PoliceApiService = Retrofit.Builder()
        .baseUrl("https://data.police.uk/api/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(PoliceApiService::class.java)
}
