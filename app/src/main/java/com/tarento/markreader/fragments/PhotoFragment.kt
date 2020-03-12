/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tarento.markreader.fragments

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.tarento.markreader.JavaPreviewActivity
import com.tarento.markreader.R
import com.tarento.markreader.ResultActivity
import com.tarento.markreader.data.ApiClient
import com.tarento.markreader.data.OCR
import com.tarento.markreader.data.OCRService
import com.tarento.markreader.data.model.ProcessResult
import com.tarento.markreader.utils.BitmapUtils
import com.tarento.markreader.utils.ProgressBarUtil
import kotlinx.android.synthetic.main.fragment_photo.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Callback
import retrofit2.Response
import team.clevel.documentscanner.helpers.ScannerConstants
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File


/** Fragment used for each individual page showing a photo inside of [GalleryFragment] */
class PhotoFragment internal constructor() : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?):View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_photo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = arguments ?: return
        val resource = args.getString(FILE_NAME_KEY)?.let { File(it) } ?: R.drawable.ic_photo
//        Glide.with(view).load(resource).into(view as ImageView)
        Glide.with(this).load(ScannerConstants.selectedImageBitmap).into(imageViewPreview)

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        buttonSubmit.setOnClickListener {
            getOCRImageUploadResponse(File(""))
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
        var bitmap = ScannerConstants.selectedImageBitmap
//        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        println("bitmap height ${bitmap.height}")
        println("bitmap width   ${bitmap.width}")

        //bitmap = bitmap.resizeByWidth(960)

        println("bitmap height ${bitmap.height}")
        println("bitmap width   ${bitmap.width}")
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    private fun getOCRImageUploadResponse(file: File?) {
        activity?.let {
            ProgressBarUtil.showProgressDialog(it)
        }

        val apiInterface: OCRService = ApiClient.getClient()!!.create(OCRService::class.java)

        val data = encodeImage(file) ?: return

        //BitmapUtils.getCameraPhotoOrientation(activity, ByteArrayInputStream(data))

//        val requestBody = data.toRequestBody("application/octet-stream".toMediaTypeOrNull())
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
//                    ProgressBarUtil.dismissProgressDialog()

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

//        val requestBody = data.toRequestBody("application/octet-stream".toMediaTypeOrNull())
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