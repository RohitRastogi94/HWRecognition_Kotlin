package com.tarento.markreader.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class FetchExamsResponse(

    @SerializedName("ok") val ok: Boolean,
    @SerializedName("http") val http: Http,
    @SerializedName("why") val why: String,
    @SerializedName("data") val data: List<Data>
) : Serializable {
    data class Data(

        @SerializedName("_id") val _id: String,
        @SerializedName("exam_code") val exam_code: String,
        @SerializedName("exam_name") val exam_name: String,
        @SerializedName("exam_date") val exam_date: String,
        @SerializedName("school_code") val school_code: String,
        @SerializedName("status") val status: String,
        @SerializedName("created_on") val created_on: String
    )
}

