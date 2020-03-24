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

    var processResult: ProcessResult? = null
    var checkOCRResponse: CheckOCRResponse? = null
    var totalMarks: Float = 0F
    var totalMarksSecured: Float = 0F
    val adapter = MarksListAdapter()
    var updatedTableModel = mutableListOf<ResultTableModel>()
    var responseIndex: Int = 0
    var verifyMarksAndSubjectListener:MarksAndSubjectListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is MarksAndSubjectListener){
            verifyMarksAndSubjectListener = context
        }else{
            Log.d("TAG","Implement Marks summary listener")
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            processResult = it.getSerializable("data") as ProcessResult
            checkOCRResponse = it.getSerializable("dataOCRResponse") as CheckOCRResponse
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
        textStudentNameFirstLetter.text =
            checkOCRResponse!!.data.student_name.first().toString()
        textStudentName.text = checkOCRResponse!!.data.student_name
        textStudentId.text = checkOCRResponse!!.data.student_code
        textTestId.text = checkOCRResponse!!.data.exam_code

        buttonCancelMark.setOnClickListener {
            verifyMarksAndSubjectListener?.backToSubjectStep1()
        }

        buttonSummary.setOnClickListener {
            var isValidMarks = true
            if (!updatedTableModel.isNullOrEmpty()) {
                totalMarks = 0F
                totalMarksSecured = 0F
                val markResponse = checkOCRResponse!!.data.ocr_data.response[this.responseIndex]
                for (i in 0 until markResponse.header.row) {
                    for (j in 0 until 5) {
                        if (i > 0 && j == 3) {
                            val processDataMax = getProcessData(markResponse.data, i, j - 1)
                            val processData = getProcessData(markResponse.data, i, j)
                            if (processData?.row == updatedTableModel[i].rowId) {
                                updatedTableModel[i].columnValue.forEachIndexed { index, columnValues ->
                                    if (columnValues.columnId == 3 && columnValues.columnId == processData.col) {
                                        processData.title = columnValues.value.toString()

                                        if (processData.title.isNotEmpty() && !processData.title.contentEquals(
                                                "."
                                            )
                                        ) {
                                            val pointReceived =
                                                if (processData.title.contentEquals("る.0")) 3F else processData.title.toFloat()
                                            val maxMarks = processDataMax?.title
                                            if (maxMarks != null) {
                                                totalMarks += maxMarks.toFloat()
                                                totalMarksSecured += pointReceived
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
            }
            if(isValidMarks) {
                val intent = Intent(activity, SummaryActivity::class.java)
                val bundle = Bundle()
                bundle.putSerializable("data", processResult)
                bundle.putSerializable("dataOCRResponse", checkOCRResponse)
                bundle.putFloat("TotalMarks", totalMarks)
                bundle.putFloat("TotalMarksSecured", totalMarksSecured)
                intent.putExtras(bundle)
                startActivity(intent)
            }else{
                val summaryMessage =
                    AlertDialog.Builder(activity).create()
                summaryMessage.setTitle("Message")
                summaryMessage.setMessage("Please enter valid marks")
                summaryMessage.setCancelable(false)

                summaryMessage.setButton(
                    AlertDialog.BUTTON_POSITIVE,
                    "Ok"
                ) { dialog, which ->
                    dialog.dismiss()
                }
                summaryMessage.show()
            }
        }


        val layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)

        recyclerMarks.adapter = adapter
        recyclerMarks.layoutManager = layoutManager
        recyclerMarks.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                LinearLayoutManager.HORIZONTAL
            )
        )
        adapter.onItemClickListener { resultTableModel, mutableList ->
            Log.d("TAG", "ResultTable:$mutableList")
            updatedTableModel = mutableList
        }

    }

    fun getMarksHeader(response: List<CheckOCRResponse.Response>?): Int? =
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

    fun renderTable() {
        responseIndex = getMarksHeader(checkOCRResponse?.data?.ocr_data?.response) ?: return
        var markResponseItem = checkOCRResponse?.data?.ocr_data?.response?.get(responseIndex)
        var columns = markResponseItem!!.header.col
        val row = markResponseItem.header.row
        var taleDetail = mutableListOf<ResultTableModel>()

        columns = 5
        totalMarks = 0F
        totalMarksSecured = 0F

        for (i in 0 until row) {
            val columnValuesList = mutableListOf<ColumnValues>()
            for (j in 0 until columns) {
                var columnVal = ColumnValues()
                if (j > 3 && i > 0) {
                    val maxMarks = getData(markResponseItem.data, i, j - 2)
                    val obtainedMarks = getData(markResponseItem.data, i, j - 1)
                    if (maxMarks.isNotEmpty() && obtainedMarks.isNotEmpty()) {
                        val pointReceived =
                            if (obtainedMarks.contentEquals("る.0")) 3F else obtainedMarks.toFloat()
                        totalMarks += maxMarks.toFloat()
                        totalMarksSecured += pointReceived
                        columnVal.columnId = j
                        columnVal.maxMark = maxMarks.toFloat()
                        if (pointReceived >= 0 && pointReceived <= maxMarks.toFloat()) {
                            columnVal.value = "Pass"
                        } else {
                            columnVal.value = "Fail"
                        }
                    }

                } else {

                    var text = getData(markResponseItem.data, i, j)
                    if (i == 0) {//&& text.isEmpty()) {
                        columnVal.columnId = j
                        columnVal.value = headerList[j]
                    } else {
                        columnVal.columnId = j
                        columnVal.value = text
                    }
                }
                columnValuesList.add(columnVal)
            }
            var resultTableModel = ResultTableModel(i)
            resultTableModel.columnValue = columnValuesList
            taleDetail.add(resultTableModel)
        }
        Log.d("TAG", "Result:" + taleDetail.toString())

        adapter.refreshListItem(taleDetail)

    }

    private fun getData(data: List<CheckOCRResponse.Response.Data>?, row: Int, colum: Int): String {
        data?.forEachIndexed { index, processResponseData ->
            if ((processResponseData.col == colum) and (processResponseData.row == row))
                return processResponseData.title
        }

        return ""
    }

    private fun getProcessData(
        data: List<CheckOCRResponse.Response.Data>?,
        row: Int,
        colum: Int
    ): CheckOCRResponse.Response.Data? {
        data?.forEachIndexed { index, processResponseData ->
            if ((processResponseData.col == colum) and (processResponseData.row == row))
                return processResponseData
        }

        return null
    }


    interface MarksAndSubjectListener{
        fun backToSubjectStep1()
    }

}
