package com.tarento.markreader

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_index.*

class IndexActivity : AppCompatActivity() {

    companion object {
        val TAG = IndexActivity.javaClass.canonicalName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_index)

        fabOpenCamera.setColorFilter(Color.WHITE)
        fabOpenCamera.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    JavaPreviewActivity::class.java
                )
            )
        }

        relativeLeftHolder.setOnClickListener {

        }
        relativeRightHolder.setOnClickListener {
            val intent = Intent(
                this,
                LoginActivity::class.java
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }
}
