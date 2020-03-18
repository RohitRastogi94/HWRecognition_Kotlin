package com.tarento.markreader.fragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TableRow
import androidx.fragment.app.Fragment
import com.tarento.markreader.R
import com.tarento.markreader.data.model.ProcessResponse
import com.tarento.markreader.data.model.ProcessResponseData
import com.tarento.markreader.data.model.ProcessResult
import kotlinx.android.synthetic.main.fragment_marks_and_subject.*

/**
 * A simple [Fragment] subclass.
 */
class MarksAndSubjectFragment : Fragment() {

    var data: ProcessResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            data = it.getSerializable("data") as ProcessResponse
        }

        if (data == null) {
            activity?.finish()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment


        return inflater.inflate(R.layout.fragment_marks_and_subject, container, false)

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
                    if(j == 0 || (columns > 2 && i == 0)) {
                        view.setBackgroundResource(R.drawable.cell_shape_gray)
                        editText.setEnabled(false)
                        editText.isClickable = false
                        editText.setTextColor(Color.BLACK)
                        editText.setTypeface(null, Typeface.BOLD)
                    }else{
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
