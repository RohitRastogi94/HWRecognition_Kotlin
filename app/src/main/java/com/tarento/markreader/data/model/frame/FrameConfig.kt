
package com.tarento.markreader.data.model.frame

import com.google.gson.annotations.SerializedName


data class FrameConfig (
    @SerializedName("camera_index") val camera_index : Int,

    @SerializedName("max_frame_width") val max_frame_width : Int,
    @SerializedName("max_frame_height") val max_frame_height : Int,
    @SerializedName("min_frame_width") val min_frame_width : Int,
    @SerializedName("min_frame_height") val min_frame_height : Int,
    @SerializedName("homographic_image_width") val homographic_image_width : Double,
    @SerializedName("homographic_image_height") val homographic_image_height : Double,
	@SerializedName("frame_small") val frame_small : FrameSmall,
	@SerializedName("frame_large") val frame_large : FrameLarge
)