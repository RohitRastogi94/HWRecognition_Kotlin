package com.tarento.markreader.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Http(

    @SerializedName("status") val status: Int
) : Serializable