package com.tarento.markreader.data.model

data class CheckOCRRequest(val exam_code:String, val student_code:String,val ocr_data:ProcessResult)