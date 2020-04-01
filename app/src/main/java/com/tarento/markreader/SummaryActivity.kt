package com.tarento.markreader

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
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
import java.lang.Exception

class SummaryActivity : AppCompatActivity() {

    private var processResult: ProcessResult? = null
    private var checkOCRResponse: CheckOCRResponse? = null
    var saveOCRResponse: SaveOCRResponse? = null
    private var totalMarks: Float = 0F
    private var totalMarksSecured: Float = 0F

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
        checkOCRResponse?.let {
            textSummaryStudentFirstLetter.text =
                it.data.student_name.first().toString()
            textSummaryStudentName.text = it.data.student_name
            textSummaryStudentId.text = it.data.student_code
            textSummaryTestId.text = it.data.exam_code
        }
        textTotalMarks.text = String.format("%.2f", this.totalMarks)
        textTotalMarksSecured.text = String.format("%.2f", this.totalMarksSecured)

        buttonEditSummary.setOnClickListener {
            this.finish()
        }

        buttonSummarySubmit.setOnClickListener {
            saveOCRData()
        }
    }

    private fun saveOCRData() {
        val appPreferenceHelper = AppPreferenceHelper(this.applicationContext)
        val apiInterface: OCRService? = ApiClient.createAPIService()
        val saveOCRRequest = SaveOCRRequest(
            appPreferenceHelper.getExamDate()?:"",
            appPreferenceHelper.getExamCode()?:"",
            appPreferenceHelper.getStudentCode()?:"",
            appPreferenceHelper.getTeacherCode()?:"",
            checkOCRResponse!!.data.ocr_data
        )
        apiInterface?.saveOCR(saveOCRRequest)?.enqueue(object : Callback<SaveOCRResponse> {
            override fun onFailure(call: Call<SaveOCRResponse>, t: Throwable) {
                Log.e(SubjectDetailsFragment.TAG, "onResponse: Failure", t)
                ProgressBarUtil.dismissProgressDialog()
            }

            override fun onResponse(
                call: Call<SaveOCRResponse>,
                response: Response<SaveOCRResponse>
            ) {
                Log.d(SubjectDetailsFragment.TAG, "onResponse: ${response.isSuccessful}")
                ProgressBarUtil.dismissProgressDialog()
                if (response.isSuccessful && response.body() != null) {
                    Log.d(SubjectDetailsFragment.TAG, "onResponse: ${response.body()}")
                    saveOCRResponse = response.body()
                    saveOCRResponse?.let {
                        if (it.http.status == 200) {
                            val summaryMessage =
                                AlertDialog.Builder(this@SummaryActivity, R.style.DialogTheme)
                                    .create()
                            summaryMessage.setTitle(getString(R.string.data_summary))
                            summaryMessage.setMessage(getString(R.string.saved_successfully))
                            summaryMessage.setCancelable(false)
                            summaryMessage.setButton(
                                AlertDialog.BUTTON_POSITIVE,
                                getString(R.string.ok)
                            ) { _, _ ->
                                val intent = Intent(this@SummaryActivity, IndexActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                                finish()
                            }
                            summaryMessage.show()
                        } else {
                            Toast.makeText(
                                this@SummaryActivity, getString(R.string.something_went_wrong),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    var errorMessage = getString(R.string.something_went_wrong)
                    try {
                        val saveOCRErrorResponseOj = Gson().fromJson<SaveOCRResponse>(
                            response.errorBody()?.string() ?: "",
                            SaveOCRResponse::class.java
                        )
                        saveOCRErrorResponseOj?.let {
                            errorMessage = it.why
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                    Toast.makeText(
                        this@SummaryActivity,
                        errorMessage,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }
}
