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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.hardware.Camera
import android.hardware.display.DisplayManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.webkit.MimeTypeMap
import android.widget.ImageButton
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.CaptureMode
import androidx.camera.core.ImageCapture.Metadata
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.tarento.markreader.*
import com.tarento.markreader.R
import com.tarento.markreader.data.ApiClient
import com.tarento.markreader.data.OCR
import com.tarento.markreader.data.OCRService
import com.tarento.markreader.data.model.ProcessResult
import com.tarento.markreader.scanner.ScannerConstants
import com.tarento.markreader.utils.*
import kotlinx.android.synthetic.main.camera_ui_container.*
import kotlinx.android.synthetic.main.fragment_camera.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Callback
import retrofit2.Response
//import team.clevel.documentscanner.ImageCropActivity
//import team.clevel.documentscanner.helpers.ImageUtils
//import team.clevel.documentscanner.helpers.ScannerConstants
import java.io.*
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Helper type alias used for analysis use case callbacks */
typealias LumaListener = (luma: Double) -> Unit

/**
 * Main fragment for this app. Implements all camera operations including:
 * - Viewfinder
 * - Photo taking
 * - Image analysis
 */
class CameraFragment : Fragment() {

    var selectedImageBitmap: Bitmap? = null
    var convertedImage: Bitmap? = null
    private lateinit var container: ConstraintLayout
    private lateinit var viewFinder: TextureView
    private lateinit var outputDirectory: File
    private var gallery: File? = null
    //    private lateinit var broadcastManager: LocalBroadcastManager
    private lateinit var mainExecutor: Executor

    private var displayId = -1
    private var lensFacing = CameraX.LensFacing.BACK
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null

    /** Volume down button receiver used to trigger shutter */
    private val volumeDownReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(KEY_EVENT_EXTRA, KeyEvent.KEYCODE_UNKNOWN)) {
                // When the volume down button is pressed, simulate a shutter button click
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    val shutter = container
                        .findViewById<ImageButton>(R.id.camera_capture_button)
                    shutter.simulateClick()
                }
            }
        }
    }

    /** Internal reference of the [DisplayManager] */
    private lateinit var displayManager: DisplayManager

    /**
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@CameraFragment.displayId) {
                Log.d(TAG, "Rotation changed: ${view.display.rotation}")
                preview?.setTargetRotation(view.display.rotation)
                imageCapture?.setTargetRotation(view.display.rotation)
                imageAnalyzer?.setTargetRotation(view.display.rotation)
            }
        } ?: Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainExecutor = ContextCompat.getMainExecutor(requireContext())
    }

    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since user could have removed them
        //  while the app was on paused state
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                CameraFragmentDirections.actionCameraToPermissions()
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Unregister the broadcast receivers and listeners
//        broadcastManager.unregisterReceiver(volumeDownReceiver)
        displayManager.unregisterDisplayListener(displayListener)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_camera, container, false)


    private fun setGalleryThumbnail(file: File) {
        Log.d(TAG, "setGalleryThumbnail() called with: file = [$file]")
        // Reference of the view that holds the gallery thumbnail
        val thumbnail = container.findViewById<ImageButton>(R.id.photo_view_button)

        // Run the operations in the view's thread
        thumbnail.post {

            // Remove thumbnail padding
            thumbnail.setPadding(resources.getDimension(R.dimen.stroke_small).toInt())

            // Load thumbnail into circular button using Glide
            Glide.with(thumbnail)
                .load(file)
                .apply(RequestOptions.circleCropTransform())
                .into(thumbnail)
        }


//        val newFile = file.absolutePath
//        val newFile = processFile(file) ?: return
//
//

//        CropImage.activity(Uri.fromFile(File(newFile)))
//            .start(activity!!, this)

//        val intent = InstaCropperActivity.getIntent(
//            context,
//            Uri.fromFile(File(newFile)),
//            Uri.fromFile(File(file.parent + "/" + System.currentTimeMillis() + "ffffff.jpeg")),
//            920,
//            100
//        );
//        startActivityForResult(intent, 1002);

//        val options = UCrop.Options()
//        UCrop.of(
//            Uri.fromFile(File(newFile)),
//            Uri.fromFile(File(file.parent + "/" + System.currentTimeMillis() + "ffffff.jpeg"))
//        )
//            .withOptions(options)
//            .withAspectRatio(3.0f, 4.0f)
//            .withMaxResultSize(920, ((920/3.0f) * 4).toInt())
//            .start(activity!!, this, UCrop.REQUEST_CROP)

//        val mimeType = MimeTypeMap.getSingleton()
//            .getMimeTypeFromExtension(file.extension)
//        MediaScannerConnection.scanFile(
//            context, arrayOf(newFile), arrayOf(mimeType), null
//        )
    }

    inner class someTask(val file: File, val bitmap: Bitmap, val rotation: Float) :
        AsyncTask<File, Void, String>() {
        override fun doInBackground(vararg params: File): String? {
            return processFile(file, bitmap, rotation = rotation)
        }

        override fun onPreExecute() {
            super.onPreExecute()

            ProgressBarUtil.showProgressDialog(activity)
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            result?.let {
                ProgressBarUtil.dismissProgressDialog()
                gallery = File(it)
                showResult(it)
                setGalleryThumbnail(File(it))
            }
        }
    }


    private fun showResult(result: String) {
//        convertedImage = getResizedBitmap(ScannerConstants.selectedImageBitmap, 960)

       // val intent = Intent(activity, ImageCropActivity::class.java)
//        intent.putExtra("photo", result)

//        println("showResult")
        //startActivityForResult(intent, 1234)
//        println(intent)


//        val options = UCrop.Options()
//        UCrop.of(
//            Uri.fromFile(File(result)),
//            Uri.fromFile(File(File(result).parent + "/" + System.currentTimeMillis() + "ffffff.jpg"))
//        )
//            .withOptions(options)
////            .withAspectRatio(3.0f, 4.0f)
//            .withMaxResultSize(920, ((920 / 3.0f) * 4).toInt())
//            .start(activity!!, this, UCrop.REQUEST_CROP)

//        val intent = Intent(activity, CropActivity::class.java)
//        intent.putExtra("photo", result)
//
//        startActivity(intent)
//        Log.d(TAG, "showResult() called with: result = [$result]")
//        CropImage.activity(Uri.fromFile(File(result)))
//            .start(activity!!, this)
    }

    private fun processFile(file: File, bitmap: Bitmap, rotation: Float): String? {

        var bmp: Bitmap = bitmap

//        println("Time 1 ${System.currentTimeMillis()}")
//        try {
//            bmp = BitmapFactory.decodeFile(file.absolutePath)
////            bmp = viewFinder.bitmap
//        } catch (e: java.lang.Exception) {
//            e.printStackTrace()
//        }
//        println("Time 2 ${System.currentTimeMillis()}")
//
//        if (bmp == null) {                `
//            return null
//        }
//
//        val exif: ExifInterface
        try {
//            exif = ExifInterface(file.absolutePath)
//            val orientation =
//                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)
            val matrix = Matrix()


            matrix.postRotate(rotation)
            println("bmp.width old ${bmp.width}")
            println("bmp.height old ${bmp.height}")

            bmp = Bitmap.createBitmap(
                bmp, 0, 0, bmp.width,
                bmp.height, matrix, true
            )
        } catch (e: IOException) { // e.printStackTrace();
        }

        println("Time 3 ${System.currentTimeMillis()}")
        val actualHeight = bmp?.height?.toFloat() ?: return null
        val actualWidth = bmp?.width.toFloat()

        println("bmp.actualHeight old ${actualHeight}")
        println("bmp.actualWidth old ${actualWidth}")

        val graphicOverlayLocation =
            IntArray(2).apply { graphicOverlay.getLocationOnScreen(this) }
        val cameraLocation = IntArray(2).apply { view_finder.getLocationOnScreen(this) }

        if (actualHeight == 0f) {
            return null
        }

        val cameraHeight = view_finder.height.toFloat()

        var scaleFactorWidth =
            actualWidth / (view_finder.width.toFloat() * AutoFitPreviewBuilder.xScaleValue)
        var scaleFactorHeight =
            actualHeight / (view_finder.height.toFloat() * AutoFitPreviewBuilder.yScaleValue)

        val startX =
            ((actualWidth / 2) - (((view_finder.width.toFloat() * scaleFactorWidth)) / 2))
        var startY =
            ((actualHeight / 2) - (((view_finder.height.toFloat() * scaleFactorHeight)) / 2))

        println("startY $startY")
        println("startX $startX")

        if (startY < 0f) {
            startY = 0f
        }

        val actualCropX =
            startX + ((graphicOverlayLocation[0].toFloat() * scaleFactorWidth))
        val actualCropY = startY + (graphicOverlayLocation[1].toFloat() * scaleFactorHeight)
        val actualCropWidth =
            (graphicOverlay.width * scaleFactorWidth).toInt()
        val actualCropHeight = (graphicOverlay.height.toFloat() * scaleFactorHeight)

        if (actualCropWidth == 0) {
            return null
        }

        var fileOutputStream: FileOutputStream? = null
        println("Time 4 ${System.currentTimeMillis()}")
        val finalImage = file.parent + "/" + System.currentTimeMillis() + ".jpg"
        try {
            var newScaledBitmap = Bitmap.createBitmap(
                bmp,
                actualCropX.toInt(),
                actualCropY.toInt(),
                actualCropWidth.toInt(),
                actualCropHeight.toInt()
            )

            fileOutputStream =
                FileOutputStream(finalImage)
            newScaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, fileOutputStream)
            println("newScaledBitmap $newScaledBitmap")
            //ScannerConstants.selectedImageBitmap = newScaledBitmap;
        } catch (e: FileNotFoundException) { // e.printStackTrace();
            e.printStackTrace()
        }

        println("Time 5 ${System.currentTimeMillis()}")
        BitmapUtils.copyExif(file.absolutePath, finalImage)

        println("Time 6 ${System.currentTimeMillis()} finalImage $finalImage")

        return finalImage
    }


    /** Define callback that will be triggered after a photo has been taken and saved to disk */
    private val imageSavedListener = object : ImageCapture.OnImageSavedListener {
        override fun onError(
            error: ImageCapture.ImageCaptureError, message: String, exc: Throwable?
        ) {
            Log.e(TAG, "Photo capture failed: $message")
            exc?.printStackTrace()
        }

        override fun onImageSaved(photoFile: File) {
            Log.d(TAG, "Photo capture succeeded: ${photoFile.absolutePath}")

            // We can only change the foreground Drawable using API level 23+ API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                // Update the gallery thumbnail with latest picture taken
//                setGalleryThumbnail(photoFile)
            }

//            someTask().execute(photoFile)

            // Implicit broadcasts will be ignored for devices running API
            // level >= 24, so if you only target 24+ you can remove this statement
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                requireActivity().sendBroadcast(
                    Intent(Camera.ACTION_NEW_PICTURE, Uri.fromFile(photoFile))
                )
            }

            // If the folder selected is an external media directory, this is unnecessary
            // but otherwise other apps will not be able to access our images unless we
            // scan them using [MediaScannerConnection]
            val mimeType = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(photoFile.extension)
            MediaScannerConnection.scanFile(
                context, arrayOf(photoFile.absolutePath), arrayOf(mimeType), null
            )
        }
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        container = view as ConstraintLayout
        viewFinder = container.findViewById(R.id.view_finder)
//        broadcastManager = LocalBroadcastManager.getInstance(view.context)

        // Set up the intent filter that will receive events from our main activity
        val filter = IntentFilter().apply { addAction(KEY_EVENT_ACTION) }
//        broadcastManager.registerReceiver(volumeDownReceiver, filter)

        // Every time the orientation of device changes, recompute layout
        displayManager = viewFinder.context
            .getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(displayListener, null)

        // Determine the output directory
        outputDirectory = MainActivity.getOutputDirectory(requireContext())

        // Wait for the views to be properly laid out
        viewFinder.post {
            // Keep track of the display in which this view is attached
            displayId = viewFinder.display.displayId

            // Build UI controls and bind all camera use cases
            updateCameraUi()
            bindCameraUseCases()

            // In the background, load latest photo taken (if any) for gallery thumbnail
            lifecycleScope.launch(Dispatchers.IO) {
                outputDirectory.listFiles { file ->
                    EXTENSION_WHITELIST.contains(file.extension.toUpperCase())
                }?.max()?.let { setGalleryThumbnail(it) }
            }
        }

//        getGetProcessData("0061018280")
    }

    /**
     * Inflate camera controls and update the UI manually upon config changes to avoid removing
     * and re-adding the view finder from the view hierarchy; this provides a seamless rotation
     * transition on devices that support it.
     *
     * NOTE: The flag is supported starting in Android 8 but there still is a small flash on the
     * screen for devices that run Android 9 or below.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateCameraUi()
    }

    /** Declare and bind preview, capture and analysis use cases */
    private fun bindCameraUseCases() {

        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        Log.d(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")
        Log.d(TAG, "Screen metrics: ${metrics.density} x ${metrics.densityDpi}")
        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

        // Set up the view finder use case to display camera preview
        val viewFinderConfig = PreviewConfig.Builder().apply {
            setLensFacing(lensFacing)

            // We request aspect ratio but no resolution to let CameraX optimize our use cases
//            setTargetAspectRatio(screenAspectRatio)
            Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
//            setTargetRotation(Surface.ROTATION_0)
            setTargetRotation(viewFinder.display.rotation)
        }.build()

        // Use the auto-fit preview builder to automatically handle size and orientation changes
        preview = AutoFitPreviewBuilder.build(viewFinderConfig, viewFinder)


        // Set up the capture use case to allow users to take photos
        val imageCaptureConfig = ImageCaptureConfig.Builder().apply {
            setLensFacing(lensFacing)
            setCaptureMode(CaptureMode.MIN_LATENCY)
            // We request aspect ratio but no resolution to match preview config but letting
            // CameraX optimize for whatever specific resolution best fits requested capture mode
//            setTargetAspectRatio(screenAspectRatio)
            Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
//            setTargetRotation(Surface.ROTATION_0)
            setTargetRotation(viewFinder.display.rotation)
        }.build()

        imageCapture = ImageCapture(imageCaptureConfig)

        // Setup image analysis pipeline that computes average pixel luminance in real time
        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
            setLensFacing(lensFacing)
            // In our analysis, we care more about the latest image than analyzing *every* image
            setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            setTargetRotation(viewFinder.display.rotation)
        }.build()

        imageAnalyzer = ImageAnalysis(analyzerConfig).apply {
            setAnalyzer(mainExecutor,
                LuminosityAnalyzer { luma ->
                    // Values returned from our analyzer are passed to the attached listener
                    // We log image analysis results here --
                    // you should do something useful instead!

                    val fps = (analyzer as LuminosityAnalyzer).framesPerSecond

                    Log.d(
                        TAG, "Average luminosity: $luma. " +
                                "Frames per second: ${"%.01f".format(fps)}"
                    )
                })
        }

        // Apply declared configs to CameraX using the same lifecycle owner
        CameraX.bindToLifecycle(
            viewLifecycleOwner, preview, imageCapture, imageAnalyzer
        )
    }

    /**
     *  [androidx.camera.core.ImageAnalysisConfig] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): AspectRatio {
        val previewRatio = max(width, height).toDouble() / min(width, height)

        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    /** Method used to re-draw the camera UI controls, called every time configuration changes */
    @SuppressLint("RestrictedApi")
    private fun updateCameraUi() {

        // Remove previous UI if any
        container.findViewById<ConstraintLayout>(R.id.camera_ui_container)?.let {
            container.removeView(it)
        }

        // Inflate a new view containing all UI for controlling the camera
        val controls = View.inflate(requireContext(), R.layout.camera_ui_container, container)

        // Listener for button used to capture photo
        controls.findViewById<ImageButton>(R.id.camera_capture_button).setOnClickListener {
            // Get a stable reference of the modifiable image capture use case
            imageCapture?.let { imageCapture ->

                // Create output file to hold the image
                val photoFile = createFile(outputDirectory, FILENAME, PHOTO_EXTENSION)

                // Setup image capture metadata
                val metadata = Metadata().apply {
                    // Mirror image when using the front camera
                    isReversedHorizontal = lensFacing == CameraX.LensFacing.FRONT
                }

                // Setup image capture listener which is triggered after photo has been taken
//                imageCapture.takePicture(photoFile, metadata, mainExecutor, imageSavedListener)

                imageCapture.takePicture(mainExecutor,
                    object : ImageCapture.OnImageCapturedListener() {
                        override fun onCaptureSuccess(image: ImageProxy?, rotationDegrees: Int) {
                            image.use { image ->
                                var bitmap: Bitmap? = image?.let {
                                    imageProxyToBitmap(it)
                                } ?: return

                                someTask(photoFile, bitmap!!, rotationDegrees.toFloat()).execute()

                            }
                        }
                    })

                // We can only change the foreground Drawable using API level 23+ API
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    // Display flash animation to indicate that photo was captured
                    container.postDelayed({
                        container.foreground = ColorDrawable(Color.WHITE)
                        container.postDelayed(
                            { container.foreground = null }, ANIMATION_FAST_MILLIS
                        )
                    }, ANIMATION_SLOW_MILLIS)
                }
            }
        }

        // Listener for button used to switch cameras
        controls.findViewById<ImageButton>(R.id.camera_switch_button).setOnClickListener {
            lensFacing = if (CameraX.LensFacing.FRONT == lensFacing) {
                CameraX.LensFacing.BACK
            } else {
                CameraX.LensFacing.FRONT
            }
            try {
                // Only bind use cases if we can query a camera with this orientation
                CameraX.getCameraWithLensFacing(lensFacing)

                // Unbind all use cases and bind them again with the new lens facing configuration
                CameraX.unbindAll()
                bindCameraUseCases()
            } catch (exc: Exception) {
                // Do nothing
            }
        }

        // Listener for button used to view last photo
        controls.findViewById<ImageButton>(R.id.photo_view_button).setOnClickListener {
            /*if (ScannerConstants.selectedImageBitmap != null) {
                gallery?.let {
                    val arag = PhotoFragment.createBundle(it)
                    val intent = Intent(activity, PhotoActivity::class.java)
                    intent.putExtras(arag)
                    startActivity(intent)
                }
            }*/
//            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
//                CameraFragmentDirections.actionCameraToGallery(outputDirectory.absolutePath)
//            )
        }
    }

    public fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }


    /**
     * Our custom image analysis class.
     *
     * <p>All we need to do is override the function `analyze` with our desired operations. Here,
     * we compute the average luminosity of the image by looking at the Y plane of the YUV frame.
     */
    private class LuminosityAnalyzer(listener: LumaListener? = null) : ImageAnalysis.Analyzer {
        private val frameRateWindow = 8
        private val frameTimestamps = ArrayDeque<Long>(5)
        private val listeners = ArrayList<LumaListener>().apply { listener?.let { add(it) } }
        private var lastAnalyzedTimestamp = 0L
        var framesPerSecond: Double = -1.0
            private set

        /**
         * Used to add listeners that will be called with each luma computed
         */
        fun onFrameAnalyzed(listener: LumaListener) = listeners.add(listener)

        /**
         * Helper extension function used to extract a byte array from an image plane buffer
         */
        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        /**
         * Analyzes an image to produce a result.
         *
         * <p>The caller is responsible for ensuring this analysis method can be executed quickly
         * enough to prevent stalls in the image acquisition pipeline. Otherwise, newly available
         * images will not be acquired and analyzed.
         *
         * <p>The image passed to this method becomes invalid after this method returns. The caller
         * should not store external references to this image, as these references will become
         * invalid.
         *
         * @param image image being analyzed VERY IMPORTANT: do not close the image, it will be
         * automatically closed after this method returns
         * @return the image analysis result
         */
        override fun analyze(image: ImageProxy, rotationDegrees: Int) {
            // If there are no listeners attached, we don't need to perform analysis
            if (listeners.isEmpty()) return

            // Keep track of frames analyzed
            val currentTime = System.currentTimeMillis()
            frameTimestamps.push(currentTime)
            //val m:Mat


            // Compute the FPS using a moving average
            while (frameTimestamps.size >= frameRateWindow) frameTimestamps.removeLast()
            val timestampFirst = frameTimestamps.peekFirst() ?: currentTime
            val timestampLast = frameTimestamps.peekLast() ?: currentTime
            framesPerSecond = 1.0 / ((timestampFirst - timestampLast) /
                    frameTimestamps.size.coerceAtLeast(1).toDouble()) * 1000.0

            // Calculate the average luma no more often than every second
            if (frameTimestamps.first - lastAnalyzedTimestamp >= TimeUnit.SECONDS.toMillis(1)) {
                lastAnalyzedTimestamp = frameTimestamps.first

                // Since format in ImageAnalysis is YUV, image.planes[0] contains the luminance
                //  plane
                val buffer = image.planes[0].buffer

                // Extract image data from callback object
                val data = buffer.toByteArray()

                // Convert the data into an array of pixel values ranging 0-255
                val pixels = data.map { it.toInt() and 0xFF }

                // Compute average luminance for the image
                val luma = pixels.average()
                // Call all listeners with new value
                listeners.forEach { it(luma) }
            }
        }
    }



    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

        /** Helper function used to create a timestamped file */
        private fun createFile(baseFolder: File, format: String, extension: String) =
            File(
                baseFolder, SimpleDateFormat(format, Locale.US)
                    .format(System.currentTimeMillis()) + extension
            )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        println("*******")
//        if (requestCode == UCrop.REQUEST_CROP)
////        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
////            val result = CropImage.getActivityResult(data)
//            data?.let {
//                val result = UCrop.getOutput(data)
//                result?.let {
//                    if (resultCode == Activity.RESULT_OK) {
////                    val resultUri = result.getUri()
//
//
//                        gallery = result.toFile()
//
////                    println("resultUri ${resultUri}")
//                        setGalleryThumbnail(result.toFile())
//
//                        getOCRImageUploadResponse(result.toFile())
//                    }
//                }
//            }
//
//        }

        println("Resultcode ${Activity.RESULT_OK}")

        if (requestCode == 1234 && resultCode == Activity.RESULT_OK) {
            println("onActivityResult")
            /*if (ScannerConstants.selectedImageBitmap != null) {
                println("selectedImageBitmap ${ScannerConstants.selectedImageBitmap}")
//                imgBitmap.setImageBitmap(ScannerConstants.selectedImageBitmap)
//                imgBitmap.visibility=View.VISIBLE
//                btnPick.visibility=View.GONE

                getOCRImageUploadResponse(null)

            }*/
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


//        val requestBody = data.toRequestBody("application/octet-stream".toMediaTypeOrNull())
        val requestBody = RequestBody.create("application/octet-stream".toMediaTypeOrNull(), data)

        val hero = apiInterface.getData(requestBody)
        hero.enqueue(object : Callback<OCR> {
            override fun onFailure(call: retrofit2.Call<OCR>, t: Throwable) {
                activity?.let {
                    Toast.makeText(it, "Some thing went wrong", Toast.LENGTH_SHORT)
                        .show()
                }
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
                        activity?.let {
                            Toast.makeText(it, "Some thing went wrong", Toast.LENGTH_SHORT)
                                .show()
                        }
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

        activity?.let {
            ProgressBarUtil.showProgressDialog(it)
        }

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
                activity?.let {
                    Toast.makeText(it, "Some thing went wrong", Toast.LENGTH_SHORT)
                        .show()
                }
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
                        } else {
                            activity?.let {
                                Toast.makeText(it, "Some thing went wrong", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }

                    }
                }
            }

        })

    }

    fun Bitmap.getGetProcessDataresizeByWidth(width: Int): Bitmap {
        val ratio: Float = this.width.toFloat() / this.height.toFloat()
        val height: Int = Math.round(width / ratio)
        return Bitmap.createScaledBitmap(
            this,
            width,
            height,
            false
        )
    }
}
