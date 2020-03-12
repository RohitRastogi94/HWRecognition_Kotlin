
package com.tarento.markreader.data.model.frame

import com.google.gson.annotations.SerializedName

data class Circle (

	@SerializedName("dp") val dp : Double,
	@SerializedName("minDist") val minDist : Double,
	@SerializedName("param1") val param1 : Double,
	@SerializedName("param2") val param2 : Double,
	@SerializedName("minRadius") val minRadius : Int,
	@SerializedName("maxRadius") val maxRadius : Int
)