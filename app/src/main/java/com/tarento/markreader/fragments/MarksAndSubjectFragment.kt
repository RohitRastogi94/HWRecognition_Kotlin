package com.tarento.markreader.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tarento.markreader.R
import com.tarento.markreader.SummaryActivity
import com.tarento.markreader.data.model.*
import kotlinx.android.synthetic.main.fragment_marks_and_subject.*

/**
 * A simple [Fragment] subclass.
 */
class MarksAndSubjectFragment : Fragment() {

    private var processResult: ProcessResult? = null
    private var checkOCRResponse: CheckOCRResponse? = null
    private var totalMarks: Float = 0F
    private var totalMarksSecured: Float = 0F
    private val adapter = MarksListAdapter()
    private var updatedTableModel: MutableList<ResultTableModel>? = null
    private var responseIndex: Int = 0
    private var verifyMarksAndSubjectListener: MarksAndSubjectListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MarksAndSubjectListener) {
            verifyMarksAndSubjectListener = context
        } else {
            Log.d("TAG", "Implement Marks summary listener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            val intentData = it.getSerializable("data")
            intentData?.let {
                processResult = intentData as ProcessResult
            }
            val intentOCRResponse = it.getSerializable("dataOCRResponse")
            intentOCRResponse?.let {
                checkOCRResponse = intentOCRResponse as CheckOCRResponse
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_marks_and_subject, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (checkOCRResponse != null) {
            textStudentNameFirstLetter.text =
                checkOCRResponse!!.data.student_name.first().toString()
            textStudentName.text = checkOCRResponse!!.data.student_name
            textStudentId.text = checkOCRResponse!!.data.student_code
            textTestId.text = checkOCRResponse!!.data.exam_code
        }

        buttonCancelMark.setOnClickListener {
            verifyMarksAndSubjectListener?.backToSubjectStep1()
        }

        buttonSummary.setOnClickListener {
            var isValidMarks = true
            if (!updatedTableModel.isNullOrEmpty()) {
                try {
                    totalMarks = 0F
                    totalMarksSecured = 0F
                    val markResponse = checkOCRResponse!!.data.ocr_data.response[this.responseIndex]
                    for (i in 0 until markResponse.header.row) {
                        for (j in 0 until 5) {
                            if (i > 0 && j == 3) {
                                val processDataMax = getProcessData(markResponse.data, i, j - 1)
                                val processData = getProcessData(markResponse.data, i, j)
                                if (updatedTableModel!!.size > i && processData?.row == updatedTableModel!![i].rowId) {
                                    updatedTableModel!![i].columnValue.forEachIndexed { _, columnValues ->
                                        if (columnValues.columnId == 3 && columnValues.columnId == processData.col) {
                                            processData.title = columnValues.value.toString()

                                            if (processData.title.isNotEmpty() && !processData.title.contentEquals(
                                                    "."
                                                )
                                            ) {
                                                val pointReceived =
                                                    when {
                                                        processData.title.isEmpty() -> -1F
                                                        processData.title.contentEquals(
                                                            "る.0"
                                                        ) -> 3F
                                                        else -> processData.title.toFloat()
                                                    }
                                                val maxMarks = processDataMax?.title
                                                if (maxMarks != null) {
                                                    totalMarks += maxMarks.toFloat()
                                                    if (pointReceived >= 0) {
                                                        processData.title =
                                                            String.format("%.1f", pointReceived)
                                                        totalMarksSecured += pointReceived
                                                    }
                                                    if (pointReceived in 0.0..maxMarks.toDouble()) {
                                                        if (isValidMarks) {
                                                            isValidMarks = true
                                                        }
                                                    } else {
                                                        isValidMarks = false
                                                    }
                                                } else {
                                                    isValidMarks = false
                                                }
                                            } else {
                                                isValidMarks = false
                                            }
                                            Log.i(
                                                "TAG",
                                                "Result:Max${processDataMax?.title}:${processData.title}"
                                            )
                                        }
                                    }
                                }
                            }

                        }
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
            if (isValidMarks) {
                val intent = Intent(activity, SummaryActivity::class.java)
                val bundle = Bundle()
                bundle.putSerializable("data", processResult)
                bundle.putSerializable("dataOCRResponse", checkOCRResponse)
                bundle.putFloat("TotalMarks", totalMarks)
                bundle.putFloat("TotalMarksSecured", totalMarksSecured)
                intent.putExtras(bundle)
                startActivity(intent)
            } else {
                val summaryMessage = AlertDialog.Builder(activity, R.style.DialogTheme).create()
                summaryMessage.setTitle(getString(R.string.message))
                summaryMessage.setMessage(getString(R.string.please_enter_valid_marks))
                summaryMessage.setCancelable(false)
                summaryMessage.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ok)) { dialog, _ ->
                    dialog.dismiss()
                }
                summaryMessage.show()
            }
        }

        val layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        recyclerMarks.adapter = adapter
        recyclerMarks.layoutManager = layoutManager
        recyclerMarks.addItemDecoration(
            DividerItemDecoration(requireContext(), LinearLayoutManager.HORIZONTAL)
        )
        adapter.onItemClickListener { _, mutableList ->
            Log.d("TAG", "ResultTable:$mutableList")
            updatedTableModel = mutableList
        }
    }

    private fun getMarksHeader(response: List<CheckOCRResponse.Response>?): Int? =
        response?.indexOfFirst {
            println("header ${it.header.title}")
            it.header.title.equals("Marks Received", true)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        renderTable()
    }

    private val headerList = arrayOf(
        "પ્રટનમ ",
        "અધ્યયન નિષ્પતિ કમ ",
        "કુલ ગુણ ",
        "મેળવેલ ગુણ ",
        "ઉપચારપાત્ર અધ્યયન નિષ્પતિ "
    )

    private fun renderTable() {
        responseIndex = getMarksHeader(checkOCRResponse?.data?.ocr_data?.response) ?: return
        val markResponseItem = checkOCRResponse?.data?.ocr_data?.response?.get(responseIndex)
        var columns = markResponseItem!!.header.col
        val row = markResponseItem.header.row
        val taleDetail = mutableListOf<ResultTableModel>()

        columns = 5
        totalMarks = 0F
        totalMarksSecured = 0F

        for (i in 0 until row) {
            val columnValuesList = mutableListOf<ColumnValues>()
            for (j in 0 until columns) {
                val columnVal = ColumnValues()
                if (j > 3 && i > 0) {
                    var maxMarks = getData(markResponseItem.data, i, j - 2)
                    val obtainedMarks = getData(markResponseItem.data, i, j - 1)
                    maxMarks = if (maxMarks.isEmpty()) "0" else maxMarks
                    val pointReceived =
                        when {
                            obtainedMarks.isEmpty() -> -1F
                            obtainedMarks.contentEquals("る.0") -> 3F
                            else -> obtainedMarks.toFloat()
                        }
                    totalMarks += maxMarks.toFloat()
                    if (pointReceived >= 0)
                        totalMarksSecured += pointReceived
                    columnVal.columnId = j
                    columnVal.maxMark = maxMarks.toFloat()
                    if (pointReceived >= 0 && pointReceived <= maxMarks.toFloat()) {
                        columnVal.value = "Pass"
                    } else {
                        columnVal.value = "Fail"
                    }
                } else {
                    val text = getData(markResponseItem.data, i, j)
                    if (i == 0) {//&& text.isEmpty()) {
                        columnVal.columnId = j
                        columnVal.value = headerList[j]
                    } else {
                        columnVal.columnId = j
                        columnVal.value = text
                    }
                }
                if (j == 0 && columnVal.value.isNullOrEmpty()) {

                } else {
                    columnValuesList.add(columnVal)
                }
            }
            if (columnValuesList.size == 5) {
                val resultTableModel = ResultTableModel(i)
                resultTableModel.columnValue = columnValuesList
                taleDetail.add(resultTableModel)
            }
        }
        Log.d("TAG", "Result:$taleDetail")
        adapter.refreshListItem(taleDetail)
    }

    private fun getData(
        data: List<CheckOCRResponse.Response.Data>?,
        row: Int,
        column: Int
    ): String {
        data?.forEachIndexed { _, processResponseData ->
            if ((processResponseData.col == column) and (processResponseData.row == row))
                return processResponseData.title
        }
        return ""
    }

    private fun getProcessData(
        data: List<CheckOCRResponse.Response.Data>?,
        row: Int, column: Int
    ): CheckOCRResponse.Response.Data? {
        data?.forEachIndexed { _, processResponseData ->
            if ((processResponseData.col == column) and (processResponseData.row == row))
                return processResponseData
        }
        return null
    }

    interface MarksAndSubjectListener {
        fun backToSubjectStep1()
    }
}
