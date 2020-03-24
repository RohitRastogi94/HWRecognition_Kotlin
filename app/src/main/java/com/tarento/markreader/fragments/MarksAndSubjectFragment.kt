package com.tarento.markreader.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tarento.markreader.R
import com.tarento.markreader.SummaryActivity
import com.tarento.markreader.data.ApiClient
import com.tarento.markreader.data.OCRService
import com.tarento.markreader.data.model.*
import com.tarento.markreader.data.preference.AppPreferenceHelper
import com.tarento.markreader.utils.ProgressBarUtil
import kotlinx.android.synthetic.main.fragment_marks_and_subject.*
import kotlinx.android.synthetic.main.fragment_subject_details.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.max

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
        //checkOCR(checkOCRResponse!!.data.exam_code, checkOCRResponse!!.data.student_code)

        buttonEditMark.setOnClickListener {
            linearEditSummaryHolder.visibility = View.GONE
            buttonCancelMark.visibility = View.VISIBLE
        }

        buttonCancelMark.setOnClickListener {
            buttonCancelMark.visibility = View.GONE
            linearEditSummaryHolder.visibility = View.VISIBLE

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
                var intent = Intent(activity, SummaryActivity::class.java)
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

    private fun checkOCR(examCode: String, studentCode: String) {
        val apiInterface: OCRService = ApiClient.getClient()!!.create(OCRService::class.java)
        val checkOCRRequest = CheckOCRRequest(examCode, studentCode, processResult!!)
        //Log.d(TAG, "getGetProcessData() called with: data = [$requestBody]")
        val hero = apiInterface.checkOCR(checkOCRRequest)

        hero.enqueue(object : Callback<CheckOCRResponse> {
            override fun onFailure(call: Call<CheckOCRResponse>, t: Throwable) {
                Log.e(SubjectDetailsFragment.TAG, "onResponse: Failuer", t)

                Toast.makeText(activity, "Some thing went wrong", Toast.LENGTH_SHORT)
                //.show()

                ProgressBarUtil.dismissProgressDialog()
            }

            override fun onResponse(
                call: Call<CheckOCRResponse>,
                response: Response<CheckOCRResponse>
            ) {
                Log.d(SubjectDetailsFragment.TAG, "onResponse: ${response.isSuccessful}")
                if (response != null && response.isSuccessful && response.body() != null) {
                    ProgressBarUtil.dismissProgressDialog()
                    Log.d(SubjectDetailsFragment.TAG, "onResponse: ${response.body()}")
                    checkOCRResponse = response.body()

                    checkOCRResponse?.let {
                        if (checkOCRResponse != null) {
                            if (checkOCRResponse!!.http.status == 200) {
                                textStudentNameFirstLetter.text =
                                    checkOCRResponse!!.data.student_name.first().toString()
                                textStudentName.text = checkOCRResponse!!.data.student_name
                                textStudentId.text = checkOCRResponse!!.data.student_code
                                textTestId.text = checkOCRResponse!!.data.exam_code
                            } else {
                                Toast.makeText(
                                    activity,
                                    "Some thing went wrong",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()

                            }
                        } else {
                            Toast.makeText(activity, "Some thing went wrong", Toast.LENGTH_SHORT)
                                .show()

                        }

                    }
                }
            }

        })

    }


/*
    @SuppressLint("ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.let { activity ->
            var columns = data?.header?.col ?: 0
            val row = data?.header?.row ?: 0

            for (i in 0 until row) {

                val tableRow =
                    LayoutInflater.from(activity).inflate(R.layout.table_row, null) as TableRow

                tableRow.weightSum = columns.toFloat()

                for (j in 0 until columns) {
                    val view = LayoutInflater.from(activity).inflate(
                        R.layout.table_row_item,
                        null
                    ) as RelativeLayout

                    tableRow.addView(view)

                    val editText = view.findViewById<EditText>(R.id.dataField)
                    editText.setText(getData(i, j))
                    if (j == 0 || (columns > 2 && i == 0)) {
                        view.setBackgroundResource(R.drawable.cell_shape_gray)
                        editText.setEnabled(false)
                        editText.isClickable = false
                        editText.setTextColor(Color.BLACK)
                        editText.setTypeface(null, Typeface.BOLD)
                    } else {
                        editText.setEnabled(false)
                        editText.isClickable = false
                        editText.setTextColor(Color.BLACK)
                        editText.setTypeface(null, Typeface.NORMAL)
                    }
                }
                table_layout.addView(tableRow)
            }
        }
    }

    private fun getData(row: Int, colum: Int): String {
        data?.data?.forEachIndexed { index, processResponseData ->
            if ((processResponseData.col == colum) and (processResponseData.row == row)) return processResponseData.text
        }

        return ""
    }
*/

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

            /*val tableRow = TableRow(activity)
            tableRow.layoutParams = ViewGroup.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )*/
            //LayoutInflater.from(activity).inflate(R.layout.table_row, null) as TableRow
            var columnValuesList = mutableListOf<ColumnValues>()
            // tableRow.weightSum = columns.toFloat()
            for (j in 0 until columns) {
                var columnVal = ColumnValues()
                if (j > 3 && i > 0) {
                    /*val view = LayoutInflater.from(activity).inflate(
                        R.layout.table_row_item_result,
                        null
                    ) as RelativeLayout
                    var viewParams =
                        TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
                    tableRow.addView(view, viewParams)
                    val imgResult = view.findViewById<ImageView>(R.id.imgResult)*/
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
                            //imgResult.setImageResource(R.drawable.ic_pass)
                            columnVal.value = "Pass"
                        } else {
                            //imgResult.setImageResource(R.drawable.ic_failed)
                            columnVal.value = "Fail"
                        }
                    }

                } else {
                    /*val view = LayoutInflater.from(activity).inflate(
                        R.layout.table_row_item,
                        null
                    ) as RelativeLayout


                    var viewParams =
                        TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
                    tableRow.addView(view, viewParams)

                    val editText = view.findViewById<EditText>(R.id.dataField)*/
                    var text = getData(markResponseItem.data, i, j)
                    if (i == 0) {//&& text.isEmpty()) {
                        //editText.setLines(2)
                        //editText.setText(headerList[j])
                        columnVal.columnId = j
                        columnVal.value = headerList[j]
                    } else {
                        //editText.setText(text)
                        columnVal.columnId = j
                        columnVal.value = text
                    }
                    /*if (j == 0 || (columns > 2 && i == 0)) {
                        view.setBackgroundResource(R.drawable.cell_shape_gray)
                        editText.setEnabled(false)
                        editText.isClickable = false
                        editText.setTextColor(Color.BLACK)
                        editText.setTypeface(null, Typeface.BOLD)
                    } else {
                        if (j == 3) {
                            editText.setTextColor(Color.BLACK)
                            editText.setTypeface(null, Typeface.NORMAL)
                        } else {
                            editText.setEnabled(false)
                            editText.isClickable = false
                            editText.setTextColor(Color.BLACK)
                            editText.setTypeface(null, Typeface.NORMAL)
                        }
                    }*/

                }
                columnValuesList.add(columnVal)
            }
            var resultTableModel = ResultTableModel(i)
            resultTableModel.columnValue = columnValuesList
            taleDetail.add(resultTableModel)
            //table_layout.addView(tableRow)
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

}
