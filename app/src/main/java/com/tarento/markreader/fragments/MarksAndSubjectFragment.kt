package com.tarento.markreader.fragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TableRow
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.tarento.markreader.R
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

/**
 * A simple [Fragment] subclass.
 */
class MarksAndSubjectFragment : Fragment() {

    var processResult: ProcessResult? = null
    var data: ProcessResponse? = null
    var checkOCRResponse: CheckOCRResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            processResult = it.getSerializable("data") as ProcessResult
            data = processResult!!.response[1]
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

/*
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        renderTable()

    }

    fun getMarksHeader(response: ProcessResponse?) =

        response?.apply {
            println("header ${header.title}")
            header.title.equals("Marks Received", true)
        }


    fun renderTable() {
        var result: ProcessResult? = data

        var markResponseItem = getMarksHeader(result?.response?.first()) ?: return
        var columns = markResponseItem.header.col
        val row = markResponseItem.header.row

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
                editText.setText(getData(markResponseItem.data, i, j))
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

    private fun getData(data: ArrayList<ProcessResponseData>, row: Int, colum: Int): String {
        data?.forEachIndexed { index, processResponseData ->
            if ((processResponseData.col == colum) and (processResponseData.row == row)) return processResponseData.text
        }

        return ""
    }
*/

}
