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
import com.tarento.markreader.DataResultActivity
import com.tarento.markreader.R
import com.tarento.markreader.data.ApiClient
import com.tarento.markreader.data.OCR
import com.tarento.markreader.data.OCRService
import com.tarento.markreader.data.model.ProcessResult
import com.tarento.markreader.data.preference.AppPreferenceHelper
import com.tarento.markreader.scanner.ScannerConstants
import kotlinx.android.synthetic.main.custom_progress_dialog.*
import kotlinx.android.synthetic.main.fragment_photo.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File

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
        Glide.with(this).load(ScannerConstants.previewImageBitmap).placeholder(R.drawable.ic_photo)
            .into(imageViewPreview)
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
        private val TAG = PhotoFragment::class.java.simpleName
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
        val bitmap = BitmapFactory.decodeFile(file?.absolutePath)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    private fun getOCRImageUploadResponse(file: File?) {
        activity?.let {
            loadingView.visibility = View.VISIBLE
        }

        val apiInterface: OCRService? = ApiClient.createAPIService()
        val data = encodeImage(file) ?: return
        val requestBody = data.toRequestBody("application/octet-stream".toMediaTypeOrNull(), 0, data.size)
        apiInterface?.getData(requestBody)?.enqueue(object : Callback<OCR> {
            override fun onFailure(call: retrofit2.Call<OCR>, t: Throwable) {
                Log.e(TAG, "onResponse: Failure", t)
                loadingView.visibility = View.GONE
            }

            override fun onResponse(call: retrofit2.Call<OCR>, response: Response<OCR>) {
                Log.d(TAG, "onResponse: ${response.isSuccessful}")
                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "onResponse: ${response.body()}")
                    if (response.body() == null || response.body()?.filepath == null) {
                        loadingView.visibility = View.GONE
                        Toast.makeText(activity,  getString(R.string.something_went_wrong), Toast.LENGTH_SHORT)
                            .show()
                        return
                    }
                    textMessage.text =
                        resources.getString(R.string.please_wait_while_we_scan_the_picture)
                    getGetProcessData(response.body()?.filepath!!)
                } else {
                    loadingView.visibility = View.GONE
                    Toast.makeText(activity,  getString(R.string.something_went_wrong), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        })
    }

    private fun getGetProcessData(data: String) {
        val apiInterface: OCRService? = ApiClient.createAPIService()
        val json = JSONObject().put("filename", data)
        val requestBody = json.toString()
            .toRequestBody("application/json".toMediaTypeOrNull())
        Log.d(TAG, "getGetProcessData() called with: data = [$requestBody]")
        apiInterface?.processOcr(requestBody)?.enqueue(object : Callback<ProcessResult> {
            override fun onFailure(call: retrofit2.Call<ProcessResult>, t: Throwable) {
                Log.e(TAG, "onResponse: Failure", t)
                loadingView.visibility = View.GONE
            }

            override fun onResponse(call: retrofit2.Call<ProcessResult>,
                response: Response<ProcessResult>
            ) {
                Log.d(TAG, "onResponse: ${response.isSuccessful}")
                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "onResponse: ${response.body()}")
                    vocaProgressBar.visibility = View.GONE
                    finalResult.visibility = View.VISIBLE
                    textMessage.text = resources.getString(R.string.scanning_complete)
                    val processResult = response.body()

                    if (processResult != null && processResult.status.code == 200) {
                        activity?.applicationContext?.let { it1 -> AppPreferenceHelper(it1).clearStudentDetails() }
                        val intent = Intent(activity, DataResultActivity::class.java)
                        intent.putExtra("result", processResult)
                        startActivity(intent)
                        activity?.finish()
                    } else {
                        loadingView.visibility = View.GONE
                        Toast.makeText(activity, getString(R.string.something_went_wrong), Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    loadingView.visibility = View.GONE
                    Toast.makeText(activity,  getString(R.string.something_went_wrong), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        })
    }
}