package com.tarento.markreader.data

import com.tarento.markreader.data.model.FetchExamsResponse
import com.tarento.markreader.data.model.LoginRequest
import com.tarento.markreader.data.model.LoginResponse
import com.tarento.markreader.data.model.ProcessResult
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

/**
 * Created by Muthukrishnan.R on 07,Jan,2020.
 */
interface OCRService {

    @Headers("Content-Type: application/json")
    @POST("app/v1/login")
    fun login(@Body body: LoginRequest): Call<LoginResponse>

    @Headers("Content-Type: application/octet-stream")
    @POST("upload")
    fun getData(@Body body: RequestBody): Call<OCR>

    @Headers("Content-Type: application/json")
    @POST("ocr/process")
    fun processOcr(@Body body: RequestBody): Call<ProcessResult>

    @Headers("Content-Type: application/json")
    @GET("/app/v1/fetch-exams?")
    fun fetchExams(@Query("school") school_code:String, @Query("exam_date") exam_date:String): Call<FetchExamsResponse>
}