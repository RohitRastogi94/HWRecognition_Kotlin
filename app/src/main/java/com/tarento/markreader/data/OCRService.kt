package com.tarento.markreader.data

import com.tarento.markreader.data.model.ProcessResult
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Created by Muthukrishnan.R on 07,Jan,2020.
 */
interface OCRService {
    @Headers("Content-Type: application/octet-stream")
    @POST("upload")
    fun getData(@Body body: RequestBody): Call<OCR>

    @Headers("Content-Type: application/json")
    @POST("ocr/process")
    fun processOcr(@Body body: RequestBody): Call<ProcessResult>
}