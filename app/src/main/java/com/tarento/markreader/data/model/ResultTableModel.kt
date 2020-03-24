package com.tarento.markreader.data.model

data class ResultTableModel(var rowId: Int) {
    var columnValue: List<ColumnValues> = listOf<ColumnValues>()
}

class ColumnValues() {
    var columnId: Int = 0
    var maxMark: Float = 0F
    var value: String? = null
}
