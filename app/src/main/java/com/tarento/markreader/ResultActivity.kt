package com.tarento.markreader

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.tarento.markreader.data.model.ProcessResponse
import com.tarento.markreader.data.model.ProcessResult
import com.tarento.markreader.fragments.DataFragment
import kotlinx.android.synthetic.main.activity_result.*

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_result)

        val result = intent.getSerializableExtra("result") as ProcessResult

        if (result.response == null || result.response.size == 0) {
            finish()
            return
        }

        view_pager.adapter = SectionsPagerAdapter(this, result.response)

        TabLayoutMediator(tabs, view_pager) { tab, position ->
            tab.text = result.response[position].header.title ?: ""
        }.attach()

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
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
