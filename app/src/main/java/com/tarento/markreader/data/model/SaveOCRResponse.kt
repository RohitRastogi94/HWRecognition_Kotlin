package com.tarento.markreader.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class SaveOCRResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("http") val http: Http,
    @SerializedName("why") val why: String,
    @SerializedName("data") val data: String
) : Serializable
