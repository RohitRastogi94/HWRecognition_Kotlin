package com.tarento.markreader.data.model

import java.io.Serializable

/**
 * Created by Muthukrishnan.R on 07,Jan,2020.
 */
data class ProcessResult(
    val response: ArrayList<ProcessResponse>,
    val status: ProcessResponseStatus
) : Serializable

data class ProcessResponseStatus(val code: Int, val message: String) : Serializable
data class ProcessResponseHeader(val col: Int, val row: Int, val title: String) : Serializable
data class ProcessResponse(
    val header: ProcessResponseHeader,
    val data: ArrayList<ProcessResponseData>
) :
    Serializable

data class ProcessResponseData(val col: Int, val row: Int, val text: String) : Serializable

