package com.tarento.markreader.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Created by Muthukrishnan.R on 07,Jan,2020.
 */


class ApiClient {
    companion object {
        //val BASE_URL = "http://52.11.90.50/"
        val BASE_URL = "http://ocr.anuvaad.org"
        var retrofit: Retrofit? = null
        fun getClient(): Retrofit? {
            if (retrofit == null) {
                val interceptor = HttpLoggingInterceptor()
                interceptor.level = HttpLoggingInterceptor.Level.BODY

                val client = OkHttpClient.Builder().apply {
                    readTimeout(60, TimeUnit.SECONDS)
                    writeTimeout(60, TimeUnit.SECONDS)
                    connectTimeout(60, TimeUnit.SECONDS)
                    addInterceptor(interceptor)
//                    addInterceptor { chain ->
//                        var request = chain.request()
//                        request = request.newBuilder()
//                            .build()
//                        val response = chain.proceed(request)
//                        response
//                    }
                }
                retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client.build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            }
            return retrofit
        }
    }
}