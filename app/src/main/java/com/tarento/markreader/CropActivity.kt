package com.tarento.markreader

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.android.synthetic.main.activity_crop.*
import java.io.File
import java.lang.Exception

class CropActivity : AppCompatActivity(), CropImageView.OnSetImageUriCompleteListener,
    CropImageView.OnCropImageCompleteListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val photoUri: String = intent.getStringExtra("photo") ?: return finish()

        setContentView(R.layout.activity_crop)

        cropImageView.setImageUriAsync(Uri.fromFile(File(photoUri)))

        cropImageView.isAutoZoomEnabled = false

//        cropImageView.resetCropOverlayView()
    }

    override fun onSetImageUriComplete(view: CropImageView?, uri: Uri?, error: Exception?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCropImageComplete(view: CropImageView?, result: CropImageView.CropResult?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
