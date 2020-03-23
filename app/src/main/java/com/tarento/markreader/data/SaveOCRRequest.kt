package com.tarento.markreader.data

import com.tarento.markreader.data.model.CheckOCRResponse

data class SaveOCRRequest(
    val exam_date: String,
    val exam_code: String,
    val student_code: String,
    val teacher_code: String,
    val ocr_data: CheckOCRResponse.Ocr_data
)
