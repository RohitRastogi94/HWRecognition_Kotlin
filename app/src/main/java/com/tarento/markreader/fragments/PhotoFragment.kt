package com.tarento.markreader.fragments

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.tarento.markreader.R
import com.tarento.markreader.ResultActivity
import com.tarento.markreader.data.ApiClient
import com.tarento.markreader.data.OCR
import com.tarento.markreader.data.OCRService
import com.tarento.markreader.data.model.ProcessResult
import com.tarento.markreader.scanner.ScannerConstants
import com.tarento.markreader.utils.ProgressBarUtil
import kotlinx.android.synthetic.main.fragment_photo.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File


/** Fragment used for each individual page showing a photo */
class PhotoFragment internal constructor() : Fragment() {
    private var resourceFileToUpload: File? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_photo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = arguments ?: return
        resourceFileToUpload = args.getString(FILE_NAME_KEY)?.let { File(it) }
        Glide.with(this).load(ScannerConstants.previewImageBitmap).into(imageViewPreview)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        buttonSubmit.setOnClickListener {
            if (resourceFileToUpload != null)
                getOCRImageUploadResponse(resourceFileToUpload)
        }

        buttonCancel.setOnClickListener {
            activity?.onBackPressed()
        }
    }

    companion object {
        private const val TAG = "PhotoFragment"
        private const val FILE_NAME_KEY = "file_name"

        fun create(image: File) = PhotoFragment().apply {
            arguments = Bundle().apply {
                putString(FILE_NAME_KEY, image.absolutePath)
            }
        }

        fun createBundle(image: File) = Bundle().apply {
            putString(FILE_NAME_KEY, image.absolutePath)
        }
    }

    private fun encodeImage(file: File?): ByteArray? {
        val byteArrayOutputStream = ByteArrayOutputStream()
        var bitmap = BitmapFactory.decodeFile(file?.absolutePath)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    private fun getOCRImageUploadResponse(file: File?) {
        activity?.let {
            ProgressBarUtil.showProgressDialog(it)
        }

        val apiInterface: OCRService = ApiClient.getClient()!!.create(OCRService::class.java)

        val data = encodeImage(file) ?: return

        val requestBody = RequestBody.create("application/octet-stream".toMediaTypeOrNull(), data)

        val hero = apiInterface.getData(requestBody)
        hero.enqueue(object : Callback<OCR> {
            override fun onFailure(call: retrofit2.Call<OCR>, t: Throwable) {

                Toast.makeText(activity, "Some thing went wrong", Toast.LENGTH_SHORT)
                //.show()

                ProgressBarUtil.dismissProgressDialog()
            }

            override fun onResponse(
                call: retrofit2.Call<OCR>,
                response: Response<OCR>
            ) {
                Log.d(TAG, "onResponse: ${response.isSuccessful}")
                if (response != null && response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "onResponse: ${response.body()}")

                    if (response.body() == null || response.body()?.filepath == null) {

                        Toast.makeText(activity, "Some thing went wrong", Toast.LENGTH_SHORT)
                        //.show()

                        ProgressBarUtil.dismissProgressDialog()
                        return
                    }

                    getGetProcessData(response.body()?.filepath!!)
                } else {
                    ProgressBarUtil.dismissProgressDialog()
                }
            }

        })
    }

    private fun getGetProcessData(data: String) {
        val apiInterface: OCRService = ApiClient.getClient()!!.create(OCRService::class.java)
        val json = JSONObject()
        json.put("filename", data)

        val requestBody =
            RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())

        Log.d(TAG, "getGetProcessData() called with: data = [$requestBody]")
        val hero = apiInterface.processOcr(requestBody)

        hero.enqueue(object : Callback<ProcessResult> {
            override fun onFailure(call: retrofit2.Call<ProcessResult>, t: Throwable) {
                Log.e(TAG, "onResponse: Failuer", t)

                Toast.makeText(activity, "Some thing went wrong", Toast.LENGTH_SHORT)
                //.show()

                ProgressBarUtil.dismissProgressDialog()
            }

            override fun onResponse(
                call: retrofit2.Call<ProcessResult>,
                response: Response<ProcessResult>
            ) {
                Log.d(TAG, "onResponse: ${response.isSuccessful}")
                if (response != null && response.isSuccessful && response.body() != null) {
                    ProgressBarUtil.dismissProgressDialog()
                    Log.d(TAG, "onResponse: ${response.body()}")

                    val processResult = response.body()

                    processResult?.let {
                        if (processResult.status.code == 200) {
                            val intent = Intent(activity, ResultActivity::class.java)
                            intent.putExtra("result", processResult)
                            startActivity(intent)
                            activity?.finish()
                        } else {

                            Toast.makeText(activity, "Some thing went wrong", Toast.LENGTH_SHORT)
                                .show()

                        }

                    }
                }
            }

        })

    }
}