package com.tarento.markreader.data.model.login

import com.google.gson.annotations.SerializedName
import com.tarento.markreader.data.model.Http
import java.io.Serializable

data class LoginResponse(

    @SerializedName("ok") val ok: Boolean,
    @SerializedName("http") val http: Http,
    @SerializedName("why") val why: String,
    @SerializedName("data") val data : Data,
    @SerializedName("component") val component: String
) : Serializable

data class Data(

    @SerializedName("school") val school: School,
    @SerializedName("teacher") val teacher: Teacher
) : Serializable

data class School(

    @SerializedName("_id") val _id: String,
    @SerializedName("school_name") val school_name: String,
    @SerializedName("school_code") val school_code: String,
    @SerializedName("cluster_code") val cluster_code: String,
    @SerializedName("status") val status: String,
    @SerializedName("created_on") val created_on: String
) : Serializable

data class Teacher(

    @SerializedName("_id") val _id: String,
    @SerializedName("teacher_code") val teacher_code: String,
    @SerializedName("teacher_name") val teacher_name: String,
    @SerializedName("school_code") val school_code: String,
    @SerializedName("status") val status: String,
    @SerializedName("created_on") val created_on: String,
    @SerializedName("data") val data: Data,
    @SerializedName("statue") val statue: String
) : Serializable
