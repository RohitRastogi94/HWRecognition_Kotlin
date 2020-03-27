package com.tarento.markreader

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tarento.markreader.data.ApiClient
import com.tarento.markreader.data.OCRService
import com.tarento.markreader.data.SaveOCRRequest
import com.tarento.markreader.data.model.CheckOCRResponse
import com.tarento.markreader.data.model.ProcessResult
import com.tarento.markreader.data.model.SaveOCRResponse
import com.tarento.markreader.data.preference.AppPreferenceHelper
import com.tarento.markreader.fragments.SubjectDetailsFragment
import com.tarento.markreader.utils.ProgressBarUtil
import kotlinx.android.synthetic.main.activity_summary.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SummaryActivity : AppCompatActivity() {

    var processResult: ProcessResult? = null
    var checkOCRResponse: CheckOCRResponse? = null
    var saveOCRResponse: SaveOCRResponse? = null
    var totalMarks: Float = 0F
    var totalMarksSecured: Float = 0F

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        val bundle = intent?.extras
        bundle?.let {
            processResult = it.getSerializable("data") as ProcessResult?
            checkOCRResponse = it.getSerializable("dataOCRResponse") as CheckOCRResponse?
            totalMarks = it.getFloat("TotalMarks")
            totalMarksSecured = it.getFloat("TotalMarksSecured")
        }
        if (checkOCRResponse != null) {
            textSummaryStudentFirstLetter.text =
                checkOCRResponse!!.data.student_name.first().toString()
            textSummaryStudentName.text = checkOCRResponse!!.data.student_name
            textSummaryStudentId.text = checkOCRResponse!!.data.student_code
            textSummaryTestId.text = checkOCRResponse!!.data.exam_code
            textTotalMarks.text = String.format("%.2f", this.totalMarks)
            textTotalMarksSecured.text = String.format("%.2f", this.totalMarksSecured)
        }


        buttonEditSummary.setOnClickListener {
            this.finish()
        }

        buttonSummarySubmit.setOnClickListener {
            saveOCRData()
        }
    }

    private fun saveOCRData() {
        var appPreferenceHelper = AppPreferenceHelper(this.applicationContext)

        val apiInterface: OCRService = ApiClient.getClient()!!.create(OCRService::class.java)
        val saveOCRRequest = SaveOCRRequest(
            appPreferenceHelper.getExamDate()!!,
            appPreferenceHelper.getExamCode()!!,
            appPreferenceHelper.getStudentCode()!!,
            appPreferenceHelper.getTeacherCode()!!,
            checkOCRResponse!!.data.ocr_data
        )

        val hero = apiInterface.saveOCR(saveOCRRequest)

        hero.enqueue(object : Callback<SaveOCRResponse> {
            override fun onFailure(call: Call<SaveOCRResponse>, t: Throwable) {
                Log.e(SubjectDetailsFragment.TAG, "onResponse: Failuer", t)

                Toast.makeText(this@SummaryActivity, "Some thing went wrong", Toast.LENGTH_SHORT)
                //.show()

                ProgressBarUtil.dismissProgressDialog()
            }

            override fun onResponse(
                call: Call<SaveOCRResponse>,
                response: Response<SaveOCRResponse>
            ) {
                Log.d(SubjectDetailsFragment.TAG, "onResponse: ${response.isSuccessful}")
                ProgressBarUtil.dismissProgressDialog()
                if (response != null && response.isSuccessful && response.body() != null) {
                    Log.d(SubjectDetailsFragment.TAG, "onResponse: ${response.body()}")
                    saveOCRResponse = response.body()

                    saveOCRResponse?.let {
                        if (saveOCRResponse != null) {
                            if (saveOCRResponse!!.http.status == 200) {
                                val summaryMessage =
                                    AlertDialog.Builder(this@SummaryActivity, R.style.DialogTheme).create()
                                summaryMessage.setTitle("Data Summary")
                                summaryMessage.setMessage("Saved Successfully")
                                summaryMessage.setCancelable(false)

                                summaryMessage.setButton(
                                    AlertDialog.BUTTON_POSITIVE,
                                    "Ok"
                                ) { dialog, which ->
                                    val intent = Intent(
                                        this@SummaryActivity,
                                        IndexActivity::class.java
                                    )
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(intent)
                                    finish()
                                }
                                summaryMessage.show()
                            } else {
                                Toast.makeText(
                                    this@SummaryActivity,
                                    "Some thing went wrong",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()

                            }
                        } else {
                            Toast.makeText(
                                this@SummaryActivity,
                                "Some thing went wrong",
                                Toast.LENGTH_SHORT
                            )
                                .show()

                        }

                    }
                } else {
                    Toast.makeText(
                        this@SummaryActivity,
                        "Some thing went wrong",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }

        })
    }
}
