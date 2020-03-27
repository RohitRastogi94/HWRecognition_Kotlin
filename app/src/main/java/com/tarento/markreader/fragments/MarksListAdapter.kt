package com.tarento.markreader.fragments

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.tarento.markreader.R
import com.tarento.markreader.data.model.CheckOCRResponse
import com.tarento.markreader.data.model.FetchExamsResponse
import com.tarento.markreader.data.model.ResultTableModel
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_testid.view.*
import kotlinx.android.synthetic.main.marks_row_item.view.*
import java.lang.Exception

class MarksListAdapter : RecyclerView.Adapter<MarksListAdapter.ViewHolder>() {
    val userList: MutableList<ResultTableModel> = mutableListOf()
    private lateinit var itemClickListener: (ResultTableModel, MutableList<ResultTableModel>) -> Unit

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.marks_row_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(position, itemClickListener)
    }

    fun onItemClickListener(listener: (ResultTableModel, MutableList<ResultTableModel>) -> Unit) {
        itemClickListener = listener
    }

    fun refreshListItem(itemList: List<ResultTableModel>) {
        userList.clear()
        userList.addAll(itemList)
        notifyDataSetChanged()
    }

    inner class ViewHolder(override val containerView: View) :
        RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bindView(
            position: Int,
            listener: (ResultTableModel, MutableList<ResultTableModel>) -> Unit
        ) {
            containerView.textMarkSecured.imeOptions = EditorInfo.IME_ACTION_DONE
            with(userList[position]) {

                containerView.textRollNo.setText(columnValue[0].value)
                containerView.txtSubjectName.setText(columnValue[1].value)
                containerView.textMaxMark.setText(columnValue[2].value)
                containerView.textMarkSecured.setText(columnValue[3].value)
                if (this.rowId == 0) {
                    containerView.firstElement.setBackgroundResource(R.drawable.cell_shape_gray)
                    containerView.secondElement.setBackgroundResource(R.drawable.cell_shape_gray)
                    containerView.thirdElement.setBackgroundResource(R.drawable.cell_shape_gray)
                    containerView.fourthElement.setBackgroundResource(R.drawable.cell_shape_gray)
                    containerView.cell_layout.setBackgroundResource(R.drawable.cell_shape_gray)
                    setDisabledEditTextProperties(containerView.textRollNo)
                    setDisabledEditTextProperties(containerView.txtSubjectName)
                    setDisabledEditTextProperties(containerView.textMaxMark)
                    setDisabledEditTextProperties(containerView.textMarkSecured)
                    setDisabledEditTextProperties(containerView.textMarkResult)
                    containerView.textRollNo.setLines(2)
                    containerView.txtSubjectName.setLines(2)
                    containerView.textMaxMark.setLines(2)
                    containerView.textMarkSecured.setLines(2)
                    containerView.textMarkResult.setLines(2)

                    containerView.textMarkResult.setText(columnValue[4].value)
                    containerView.imgResult.visibility = View.GONE
                    containerView.textMarkResult.visibility = View.VISIBLE
                } else {
                    setDisabledEditTextNormal(containerView.textRollNo)
                    setDisabledEditTextNormal(containerView.textMaxMark)
                    setDisabledEditTextNormal(containerView.txtSubjectName)
                    containerView.textMarkSecured.setTextColor(Color.BLACK)
                    containerView.textMarkSecured.setTypeface(null, Typeface.NORMAL)
                    containerView.textRollNo.setLines(1)
                    containerView.txtSubjectName.setLines(1)
                    containerView.textMaxMark.setLines(1)
                    containerView.textMarkSecured.setLines(1)
                    try {
                        if (columnValue[4].value?.contentEquals("Pass")!!) {
                            containerView.imgResult.setImageResource(R.drawable.ic_pass)
                        } else {
                            containerView.imgResult.setImageResource(R.drawable.ic_failed)
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                    containerView.imgResult.visibility = View.VISIBLE
                    containerView.textMarkResult.visibility = View.GONE
                }
            }

            containerView.textMarkSecured.doAfterTextChanged {
                val resultModel = userList[position]
                resultModel.columnValue[3].value = containerView.textMarkSecured.text.toString()
                if (!resultModel.columnValue[3].value.isNullOrEmpty() && !resultModel.columnValue[3].value!!.contentEquals(
                        "."
                    )
                ) {
                    val pointReceived =
                        if (resultModel.columnValue[3].value!!.contentEquals("る.0")) 3F else resultModel.columnValue[3].value!!.toFloat()
                    val maxMarks = resultModel.columnValue[4].maxMark
                    if (pointReceived in 0.0..maxMarks.toDouble()) {
                        containerView.imgResult.setImageResource(R.drawable.ic_pass)
                        resultModel.columnValue[4].value = "Pass"
                    } else {
                        containerView.imgResult.setImageResource(R.drawable.ic_failed)
                        resultModel.columnValue[4].value = "Fail"
                    }
                } else {
                    containerView.imgResult.setImageResource(R.drawable.ic_failed)
                    resultModel.columnValue[4].value = "Fail"
                }
                listener(resultModel, userList)
            }

            /*containerView.textMarkSecured.apply {
                setOnEditorActionListener { _, actionId, _ ->
                    when (actionId) {
                        EditorInfo.IME_ACTION_DONE -> {
                            val resultModel = userList[position]
                            resultModel.columnValue[3].value = containerView.textMarkSecured.text.toString()
                            listener(resultModel, userList)
                        }
                    }
                    false
                }
            }*/

            itemView.setOnClickListener {

            }

        }
    }

    private fun setDisabledEditTextNormal(editText: EditText?) {
        editText?.setEnabled(false)
        editText?.isClickable = false
        editText?.setTextColor(Color.BLACK)
        editText?.setTypeface(null, Typeface.NORMAL)
    }

    private fun setDisabledEditTextProperties(editText: EditText?) {
        editText?.setEnabled(false)
        editText?.isClickable = false
        editText?.setTextColor(Color.BLACK)
        editText?.setTypeface(null, Typeface.BOLD)
    }

}
