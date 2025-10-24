package com.tachyon.transcripter.data.remote.api

import com.tachyon.transcripter.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor() : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Get API key from BuildConfig
        val apiKey = BuildConfig.GEMINI_API_KEY

        // Build new request with auth header
        val authenticatedRequest = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .build()

        return chain.proceed(authenticatedRequest)
    }
}