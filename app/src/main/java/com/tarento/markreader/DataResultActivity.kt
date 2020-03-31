package com.tarento.markreader

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.core.content.ContextCompat
import com.tarento.markreader.data.model.CheckOCRResponse
import com.tarento.markreader.data.model.ProcessResult
import com.tarento.markreader.fragments.MarksAndSubjectFragment
import com.tarento.markreader.fragments.SubjectDetailsFragment
import kotlinx.android.synthetic.main.activity_data_result.*

class DataResultActivity : AppCompatActivity() , SubjectDetailsFragment.SubjectSummaryListener,
    MarksAndSubjectFragment.MarksAndSubjectListener {

    var result:ProcessResult? = null
    var checkOCRResponse: CheckOCRResponse? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_result)

        result = intent.getSerializableExtra("result") as ProcessResult

        if (result != null && (result!!.response == null || result!!.response.size == 0)) {
            finish()
            return
        }

        //Default select Subject Fragment
        verifySubjectStep1()

        linearVerifySubject.setOnClickListener {
            verifySubjectStep1()

        }
        linearVerifyMark.setOnClickListener {
            //verifyMarksReceivedStep2()
        }

    }

    private fun verifyMarksReceivedStep2() {
        linearVerifySubject.setBackgroundResource(R.color.colorGray_40)
        linearVerifyMark.setBackgroundResource(R.color.white)
        txtVerifySubject.setTextColor(ContextCompat.getColor(this, R.color.dark_green))
        txtVerifyMark.setTextColor(ContextCompat.getColor(this, R.color.black))
        textStep1.setBackgroundResource(R.drawable.ic_tick)
        textStep1.text = ""
        textStep2.setBackgroundResource(R.drawable.round_small_shape)
        val fragment = MarksAndSubjectFragment()
        val bundle = Bundle()
        bundle.putSerializable("data", result)
        bundle.putSerializable("dataOCRResponse", checkOCRResponse)
        fragment.arguments = bundle

        val fragmentTransaction =
            supportFragmentManager.beginTransaction().replace(
                R.id.verify_scan_data_fragment_container,
                fragment
            )
        fragmentTransaction.commit()
    }

    private fun verifySubjectStep1() {
        linearVerifySubject.setBackgroundResource(R.color.white)
        linearVerifyMark.setBackgroundResource(R.color.colorGray_40)
        txtVerifySubject.setTextColor(ContextCompat.getColor(this, R.color.black))
        txtVerifyMark.setTextColor(ContextCompat.getColor(this, R.color.text_hint))
        textStep1.setBackgroundResource(R.drawable.round_small_shape)
        textStep1.text = "1"
        textStep2.setBackgroundResource(R.drawable.round_small_disabled_shape)
        val fragment = SubjectDetailsFragment()
        val bundle = Bundle()
        bundle.putSerializable("data", result)
        fragment.arguments = bundle
        val fragmentTransaction =
            supportFragmentManager.beginTransaction().replace(
                R.id.verify_scan_data_fragment_container,
                fragment
            )
        fragmentTransaction.commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)

        if (item.itemId == android.R.id.home) {
            onBackPressed()

            return true
        }
        return false
    }

    override fun getCheckOCRResponse(ocrResponse: CheckOCRResponse) {
        checkOCRResponse = ocrResponse
    }

    override fun moveToMarksReceived() {
        verifyMarksReceivedStep2()
    }

    override fun backToSubjectStep1() {
        verifySubjectStep1()
    }
}

