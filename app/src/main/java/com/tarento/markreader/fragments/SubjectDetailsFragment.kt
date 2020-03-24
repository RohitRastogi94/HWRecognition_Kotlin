package com.tarento.markreader.fragments

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tarento.markreader.R
import com.tarento.markreader.data.ApiClient
import com.tarento.markreader.data.OCRService
import com.tarento.markreader.data.model.*
import com.tarento.markreader.data.preference.AppPreferenceHelper
import com.tarento.markreader.data.preference.PreferenceHelper
import com.tarento.markreader.utils.ProgressBarUtil
import kotlinx.android.synthetic.main.fragment_subject_details.*
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Call
import java.util.*


class SubjectDetailsFragment : Fragment() {

    companion object {
        val TAG = SubjectDetailsFragment.javaClass.canonicalName
    }

    var processResult:ProcessResult? = null
    var data: ProcessResponse? = null
    var fetchExamsResponse: FetchExamsResponse? = null
    var checkOCRResponse:CheckOCRResponse?= null
    private var schoolCode: String? = null
    private var examDate: String? = null
    private var studentCode:String? = null
    private var examId:String? = null
    var subjectSummaryListener:SubjectSummaryListener? = null
    var preferenceHelper: PreferenceHelper? = null
    var dataIndex:Int = 0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is SubjectSummaryListener){
            subjectSummaryListener = context
        }else{
            Log.d(TAG,"Implement Subject summary listener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            processResult = it.getSerializable("data") as ProcessResult
            dataIndex = getStudentSummary(processResult!!.response)!!
            data = processResult!!.response[dataIndex]
        }


    }

    fun getStudentSummary(response: List<ProcessResponse>?): Int? =
        response?.indexOfFirst {
            println("header ${it.header.title}")
            it.header.title.equals("Student summary", true)
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_subject_details, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
         preferenceHelper = activity?.applicationContext?.let { it ->
            AppPreferenceHelper(
                it
            )
        }
        examDate =  preferenceHelper?.getExamDate()?: data?.data?.get(3)?.text
        studentCode = preferenceHelper?.getStudentCode()?:data?.data?.get(2)?.text
        editStudentId.setText(studentCode)
        editExamDate.setText(examDate)
        if(preferenceHelper?.getExamCodeList() != null){
            fetchExamsResponse = preferenceHelper?.getExamCodeList()
            updateExamCodeList()
        }else {
            fetchExamList(examDate)
        }

        editTestId.setOnClickListener {
            showPopupDialog(editTestId)
        }

        editExamDate.setOnClickListener {
            //To show current date in the datepicker
            val mcurrentDate = Calendar.getInstance();
            val mYear = mcurrentDate.get(Calendar.YEAR);
            val mMonth = mcurrentDate.get(Calendar.MONTH);
            val mDay = mcurrentDate.get(Calendar.DAY_OF_MONTH);

            val mDatePicker = activity?.let { it1 ->
                DatePickerDialog(it1,
                    DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
                        var monthText = (month + 1).toString()
                        var dayText = dayOfMonth.toString()
                        if (monthText.length == 1) {
                            monthText = "0$monthText"
                        }
                        if (dayText.length == 1) {
                            dayText = "0$dayText"
                        }
                        examDate = "$dayText/$monthText/$year"
                        editExamDate.setText(examDate)
                        fetchExamList(examDate)
                    }, mYear, mMonth, mDay
                )
            }
            mDatePicker?.setTitle("Select Exam date");
            mDatePicker?.show();
        }

        buttonMoveNext.setOnClickListener {
            checkOCR(editTestId.text.toString(), editStudentId.text.toString(), true)
        }

        buttonCancel.setOnClickListener {
            activity?.finish()
        }

    }

    private fun fetchExamList(examDate: String?) {
        val apiInterface: OCRService = ApiClient.getClient()!!.create(OCRService::class.java)

        activity?.applicationContext?.let {
            schoolCode = AppPreferenceHelper(it).getSchoolCode()
        }
        val hero = apiInterface.fetchExams(schoolCode!!, examDate!!)

        hero.enqueue(object : Callback<FetchExamsResponse> {
            override fun onFailure(call: Call<FetchExamsResponse>, t: Throwable) {
                Log.e(TAG, "onResponse: Failuer", t)

                Toast.makeText(activity, "Some thing went wrong", Toast.LENGTH_SHORT)
                //.show()

                ProgressBarUtil.dismissProgressDialog()
            }

            override fun onResponse(
                call: Call<FetchExamsResponse>,
                response: Response<FetchExamsResponse>
            ) {
                Log.d(TAG, "onResponse: ${response.isSuccessful}")
                ProgressBarUtil.dismissProgressDialog()
                if (response != null && response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "onResponse: ${response.body()}")

                    fetchExamsResponse = response.body()

                    fetchExamsResponse?.let {
                        if (fetchExamsResponse!!.http.status == 200) {
                            if (fetchExamsResponse!!.data.isNotEmpty()) {
                                preferenceHelper?.setExamCodeList(fetchExamsResponse!!)
                                updateExamCodeList()

                            }else{
                                textNoTestId?.visibility = View.VISIBLE
                                examId = ""
                                editTestId?.setText("")
                                Toast.makeText(activity, "No tests for given date", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        } else {
                            Toast.makeText(activity, "Some thing went wrong", Toast.LENGTH_SHORT)
                                .show()

                        }

                    }
                }else{
                    Toast.makeText(activity, "Some thing went wrong", Toast.LENGTH_SHORT)
                        .show()
                }
            }

        })

    }

    fun updateExamCodeList(){
        textNoTestId?.visibility = View.GONE
        examId = preferenceHelper?.getExamCode()?:fetchExamsResponse!!.data[0].exam_code
        editTestId?.setText(examId)
        if(editStudentId?.text?.isNotBlank()!!) {
            checkOCR(
                editTestId.text.toString(),
                editStudentId.text.toString(),
                false
            )
        }
    }


    private fun showPopupDialog(view: View) {
        val layoutInflater =
            LayoutInflater.from(requireContext()).inflate(R.layout.popup_testid, null)
        val popupWindow = PopupWindow(requireContext())
        popupWindow.contentView = layoutInflater
        popupWindow.width = view.width
        popupWindow.height = ViewGroup.LayoutParams.WRAP_CONTENT
        popupWindow.isFocusable = true
        popupWindow.setBackgroundDrawable(ColorDrawable())
        val recyclerView = layoutInflater.findViewById<RecyclerView>(R.id.recyclerViewUsers)

        val adapter = TestIdAdapter()

        val layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)


        recyclerView.adapter = adapter
        recyclerView.layoutManager = layoutManager
        recyclerView.addItemDecoration(DividerItemDecoration(requireContext(),LinearLayoutManager.HORIZONTAL))
        adapter.onItemClickListener {
            editTestId.setText(it.exam_code)
            if (popupWindow.isShowing)
                popupWindow.dismiss()
            checkOCR(editTestId.text.toString(), editStudentId.text.toString(),false)

        }

        adapter.refreshListItem(fetchExamsResponse?.data!!)
        if (!popupWindow.isShowing)
            popupWindow.showAsDropDown(view, 0, 0)

    }

    private fun checkOCR(examCode:String, studentCode:String, moveToMarks: Boolean ) {
        val apiInterface: OCRService = ApiClient.getClient()!!.create(OCRService::class.java)
        data?.data?.get(3)?.text = examDate.toString()
        data?.data?.get(2)?.text = studentCode
        val checkOCRRequest = CheckOCRRequest(examCode,studentCode, processResult!!)
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
                ProgressBarUtil.dismissProgressDialog()
                if (response != null && response.isSuccessful && response.body() != null) {
                    Log.d(SubjectDetailsFragment.TAG, "onResponse: ${response.body()}")
                    checkOCRResponse = response.body()

                    checkOCRResponse?.let {
                        if (checkOCRResponse != null){
                            if(checkOCRResponse!!.http.status == 200) {
                                subjectSummaryListener?.getCheckOCRResponse(checkOCRResponse!!)
                                if(moveToMarks) {
                                    preferenceHelper?.setStudentCode(editStudentId.text.toString())
                                    preferenceHelper?.setExamCode(editTestId.text.toString())
                                    preferenceHelper?.setExamDate(editExamDate.text.toString())
                                    subjectSummaryListener?.moveToMarksReceived()
                                }
                            }else{
                                Toast.makeText(activity, "Some thing went wrong", Toast.LENGTH_SHORT)
                                    .show()

                            }
                        } else {
                            Toast.makeText(activity, "Some thing went wrong", Toast.LENGTH_SHORT)
                                .show()

                        }

                    }
                }else{
                    Toast.makeText(activity, "Some thing went wrong", Toast.LENGTH_SHORT)
                        .show()
                }

            }

        })

    }

    interface SubjectSummaryListener{
        fun getCheckOCRResponse(ocrResponse: CheckOCRResponse)
        fun moveToMarksReceived()
    }

}
