package com.tarento.markreader

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.tarento.markreader.data.model.CheckOCRResponse
import com.tarento.markreader.data.model.ProcessResult
import kotlinx.android.synthetic.main.activity_summary.*

class SummaryActivity : AppCompatActivity() {

    var processResult: ProcessResult? = null
    var checkOCRResponse: CheckOCRResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        val bundle = intent?.extras
        bundle?.let {
            processResult = it.getSerializable("data") as ProcessResult?
            checkOCRResponse = it.getSerializable("dataOCRResponse") as CheckOCRResponse?
        }
        if (checkOCRResponse != null) {
            textSummaryStudentFirstLetter.text =
                checkOCRResponse!!.data.student_name.first().toString()
            textSummaryStudentName.text = checkOCRResponse!!.data.student_name
            textSummaryStudentId.text = checkOCRResponse!!.data.student_code
            textSummaryTestId.text = checkOCRResponse!!.data.exam_code
            textTotalMarks.text = ""
            textTotalMarksSecured.text = ""
        }


        buttonEditSummary.setOnClickListener {
            this.finish()
        }

        buttonSummarySubmit.setOnClickListener {

        }
    }
}
