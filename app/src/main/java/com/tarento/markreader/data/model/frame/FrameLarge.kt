
package com.tarento.markreader.data.model.frame

import com.google.gson.annotations.SerializedName


data class FrameLarge (
	@SerializedName("AREA_THRESHOLD_LOWER") val aREA_THRESHOLD_LOWER : Double,
	@SerializedName("AREA_THRESHOLD_START") val aREA_THRESHOLD_START : Double,
	@SerializedName("AREA_THRESHOLD_END") val aREA_THRESHOLD_END : Double,
	@SerializedName("AREA_THRESHOLD_UPPER") val aREA_THRESHOLD_UPPER : Double,
	@SerializedName("Circle") val circle : Circle
)