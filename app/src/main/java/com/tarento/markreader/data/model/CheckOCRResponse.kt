package com.tarento.markreader.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class CheckOCRResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("http") val http: Http,
    @SerializedName("why") val why: String,
    @SerializedName("data") val data: Data
) : Serializable {
    data class Data(

        @SerializedName("exam_code") val exam_code: String,
        @SerializedName("ocr_data") val ocr_data: Ocr_data,
        @SerializedName("student_code") val student_code: String,
        @SerializedName("student_name") val student_name: String
    )

    data class Ocr_data(

        @SerializedName("response") val response: List<Response>,
        @SerializedName("status") val status: Status
    )

    data class Status(

        @SerializedName("code") val code: Int,
        @SerializedName("message") val message: String
    )

    data class Response(

        @SerializedName("data") val data: List<Data>,
        @SerializedName("header") val header: Header
    ) {
        data class Data(

            @SerializedName("col") val col: Int,
            @SerializedName("row") val row: Int,
            @SerializedName("text") val title: String
        )

    }

    data class Header(

        @SerializedName("col") val col: Int,
        @SerializedName("row") val row: Int,
        @SerializedName("title") val title: String
    )
}

