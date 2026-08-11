package com.erela.fixme.helpers.api

import android.annotation.SuppressLint
import com.erela.fixme.BuildConfig
import com.google.gson.GsonBuilder
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
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
    /** Host of BASE_URL. The bearer token is only ever sent here. */
    private val apiHost: String? = BuildConfig.BASE_URL.toHttpUrlOrNull()?.host

    /**
     * Every OkHttp client in the app, so SseService and Glide are authenticated too —
     * they previously used a bare builder and sent no token, which would have started
     * failing the moment auth:sanctum lands on the apimobile routes.
     */
    fun okHttpClientBuilder(): OkHttpClient.Builder =
        OkHttpClient.Builder().addInterceptor(authInterceptor)

    /**
     * Supplies the current bearer token. A lambda rather than a UserDataHelper because this
     * is a Context-less object; FixMeApplication installs it so services that start without
     * any activity (FCMService, SseService) are authenticated too.
     */
    @Volatile
    var tokenProvider: () -> String? = { null }

    /**
     * Invoked when the server rejects the token (401) — expired, revoked by the 4h idle
     * window, or superseded because the account signed in on another device.
     */
    @Volatile
    var onUnauthorized: () -> Unit = {}

    private val authInterceptor = Interceptor { chain ->
        val token = tokenProvider()

        // Host check, not just "is there a token": SSE_URL is a different server
        // (103.96.147.242) from BASE_URL (182.23.21.202), so attaching the header
        // unconditionally would hand our Sanctum token to a third party.
        val sendToken = !token.isNullOrBlank() && chain.request().url.host == apiHost

        val request = if (sendToken) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        chain.proceed(request).also { response ->
            // Only meaningful if we actually presented a token: a 401 from a host we did
            // not authenticate against says nothing about session validity.
            if (response.code == 401 && sendToken) {
                onUnauthorized()
            }
        }
    }

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