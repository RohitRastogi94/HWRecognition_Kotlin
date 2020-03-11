package com.tarento.markreader.fragments


import android.annotation.SuppressLint
import android.graphics.Color
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
import kotlinx.android.synthetic.main.fragment_data.*

/**
 * A simple [Fragment] subclass.
 */
class DataFragment : Fragment() {

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
        return inflater.inflate(R.layout.fragment_data, container, false)
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
}
