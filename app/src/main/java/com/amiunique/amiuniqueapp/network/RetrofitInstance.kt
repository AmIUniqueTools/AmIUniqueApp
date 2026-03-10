package com.amiunique.amiuniqueapp.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    private const val FALLBACK_URL = "" // Replace by your BASE_URL

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private var baseUrl: String = FALLBACK_URL
    private var _api: FingerprintApi? = null

    val api: FingerprintApi
        get() {
            if (_api == null) {
                buildRetrofit()
            }
            return _api!!
        }

    fun init(customBaseUrl: String?) {
        baseUrl = if (customBaseUrl.isNullOrBlank()) {
            FALLBACK_URL
        } else {
            ensureTrailingSlash(customBaseUrl)
        }
        buildRetrofit()
    }

    private fun buildRetrofit() {
        _api = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FingerprintApi::class.java)
    }

    private fun ensureTrailingSlash(url: String): String {
        return if (url.endsWith("/")) url else "$url/"
    }
}