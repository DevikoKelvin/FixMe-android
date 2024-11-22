package com.erela.fixme.helpers

import com.erela.fixme.BuildConfig
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object InitAPI {
    // Local IP
    const val MAIN_URL = "http://192.168.3.109:88/fixme"
    const val SOCKET_URL = "wss://ntfy.sh/erela_pengaduan001/ws"
    const val BASE_URL = "$MAIN_URL/apimobile/"
    const val IMAGE_URL = "$MAIN_URL/assets/upload/"

    private val client = OkHttpClient().newBuilder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        })
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun getInstance(): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
        .build()

    val getAPI: GetAPI = getInstance().create(GetAPI::class.java)
}