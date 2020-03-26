package com.tarento.markreader

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_launcher.*

class LauncherActivity : AppCompatActivity() {

    companion object {
        val TAG = LauncherActivity.javaClass.canonicalName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)


        buttonProceed.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }


    }


}
