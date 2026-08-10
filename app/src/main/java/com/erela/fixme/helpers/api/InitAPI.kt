package com.erela.fixme.helpers.api

import android.annotation.SuppressLint
import com.erela.fixme.BuildConfig
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object InitAPI {
    private const val API_BASE_URL = "${BuildConfig.BASE_URL}apimobile/"
    const val IMAGE_URL = "${BuildConfig.BASE_URL}public/assets/upload/"

    /**
     * Plain OkHttp with the platform trust store.
     *
     * This used to install a trust-all X509TrustManager and a hostnameVerifier that
     * returned true for everything. Every BASE_URL/SSE_URL is http://, so that code never
     * protected a real connection — but it would have silently accepted any certificate,
     * including an attacker's, the moment the server moved to https. Removing it means a
     * future TLS migration actually validates.
     */
    fun okHttpClientBuilder(): OkHttpClient.Builder = OkHttpClient.Builder()

    private val client =
        okHttpClientBuilder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .protocols(listOf(Protocol.HTTP_1_1))
            .build()

    @Suppress("DEPRECATION")
    private fun getInstance(): Retrofit {
        val gson = GsonBuilder()
            .registerTypeAdapter(Int::class.java, IntegerTypeAdapter())
            .registerTypeAdapter(Int::class.javaObjectType, IntegerTypeAdapter())
            .setLenient()
            .create()

        return Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val getEndpoint: GetEndpoint = getInstance().create(GetEndpoint::class.java)
}