package com.tarento.markreader

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.tarento.markreader.data.model.ProcessResponse
import com.tarento.markreader.data.model.ProcessResult
import com.tarento.markreader.fragments.DataFragment
import com.tarento.markreader.fragments.MarksAndSubjectFragment
import com.tarento.markreader.fragments.SubjectDetailsFragment
import kotlinx.android.synthetic.main.activity_data_result.*

class DataResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_result)

        val result = intent.getSerializableExtra("result") as ProcessResult

        if (result.response == null || result.response.size == 0) {
            finish()
            return
        }

        //Default select Subject Fragment
        linearVerifySubject.setBackgroundResource(R.color.white)
        linearVerifyMark.setBackgroundResource(R.color.colorGray_40)
        txtVerifySubject.setTextColor(resources.getColor(R.color.black))
        txtVerifyMark.setTextColor(resources.getColor(R.color.text_hint))
        val fragment = SubjectDetailsFragment()
        val bundle = Bundle()
        bundle.putSerializable("data", result.response[0])
        fragment.arguments = bundle
        val fragmentTransaction =
            supportFragmentManager.beginTransaction().replace(
                R.id.verify_scan_data_fragment_container,
                fragment
            )
        fragmentTransaction.commit()

        linearVerifySubject.setOnClickListener {
            linearVerifySubject.setBackgroundResource(R.color.white)
            linearVerifyMark.setBackgroundResource(R.color.colorGray_40)
            txtVerifySubject.setTextColor(resources.getColor(R.color.black))
            txtVerifyMark.setTextColor(resources.getColor(R.color.text_hint))
            val fragment = SubjectDetailsFragment()
            val bundle = Bundle()
            bundle.putSerializable("data", result.response[0])
            fragment.arguments = bundle
            val fragmentTransaction =
                supportFragmentManager.beginTransaction().replace(
                    R.id.verify_scan_data_fragment_container,
                    fragment
                )
            fragmentTransaction.commit()

        }
        linearVerifyMark.setOnClickListener {
            linearVerifySubject.setBackgroundResource(R.color.colorGray_40)
            linearVerifyMark.setBackgroundResource(R.color.white)
            txtVerifySubject.setTextColor(resources.getColor(R.color.text_hint))
            txtVerifyMark.setTextColor(resources.getColor(R.color.black))

            val fragment = MarksAndSubjectFragment()
            val bundle = Bundle()
            bundle.putSerializable("data", result.response[1])
            fragment.arguments = bundle

            val fragmentTransaction =
                supportFragmentManager.beginTransaction().replace(
                    R.id.verify_scan_data_fragment_container,
                    fragment
                )
            fragmentTransaction.commit()
        }


       /* view_pager.adapter = SectionsPagerAdapter(this, result.response)

        TabLayoutMediator(tabs, view_pager) { tab, position ->
            tab.text = result.response[position].header.title ?: ""
        }.attach()

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)*/
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)

        if (item.itemId == android.R.id.home) {
            onBackPressed()

            return true
        }
        return false
    }
}

/*
class SectionsPagerAdapter(context: FragmentActivity, val response: ArrayList<ProcessResponse>) :
    FragmentStateAdapter(context) {

    override fun getItemCount() = response.size

    override fun createFragment(position: Int): Fragment {
        val fragment = DataFragment()
        val bundle = Bundle()
        bundle.putSerializable("data", response[position])
        fragment.arguments = bundle
        return fragment
    }
}
*/
