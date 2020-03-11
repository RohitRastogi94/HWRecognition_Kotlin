package com.tarento.markreader.fragments

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.tarento.markreader.R

class PhotoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo)

        val fragment = PhotoFragment()
        fragment.arguments = intent.extras
        supportFragmentManager.beginTransaction().add(R.id.container, fragment).commit()
    }
}
