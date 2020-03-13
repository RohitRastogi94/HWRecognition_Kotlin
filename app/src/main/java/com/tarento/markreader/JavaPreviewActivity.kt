package com.tarento.markreader

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.media.MediaActionSound
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.tarento.markreader.data.model.frame.FrameConfig
import com.tarento.markreader.fragments.*
import com.tarento.markreader.scanner.ImageUtils
import com.tarento.markreader.scanner.ScannerConstants
import com.tarento.markreader.utils.BitmapUtils
import com.tarento.markreader.utils.FLAGS_FULLSCREEN
import kotlinx.android.synthetic.main.activity_javapreview.*
import org.opencv.android.*
import org.opencv.calib3d.Calib3d
import org.opencv.core.*
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.imgproc.Imgproc
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

private const val PERMISSIONS_REQUEST_CODE = 10
private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)

private const val IMMERSIVE_FLAG_TIMEOUT = 500L

private var AREA_THRESHOLD_LOWER = 80000.00
private var AREA_THRESHOLD_START = 100000.00
private var AREA_THRESHOLD_END = 130000.00
private var AREA_THRESHOLD_UPPER = 150000.00

class JavaPreviewActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {
    private var mOpenCvCameraView: CameraBridgeViewBase? = null
    private var javaCameraView: JavaCameraView? = null
    private lateinit var container: FrameLayout
    var mRgba: Mat? = null
    private lateinit var outputDirectory: File
    private var gallery: File? = null
    lateinit var frameConfig: FrameConfig

    private val mLoaderCallback: BaseLoaderCallback =
        object : BaseLoaderCallback(this) {
            override fun onManagerConnected(status: Int) {
                when (status) {
                    LoaderCallbackInterface.SUCCESS -> {
                        Log.i(TAG, "OpenCV loaded successfully")
                        mOpenCvCameraView!!.enableView()
                    }
                    else -> {
                        super.onManagerConnected(status)
                    }
                }
            }
        }

    /** Called when the activity is first created.  */
    public override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "called onCreate")
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_javapreview)
        container = findViewById(R.id.frameLayout)
        javaCameraView = findViewById<View>(R.id.tutorial1_activity_java_surface_view) as JavaCameraView
        //Read config asset file
        readFrameConfigAsset()
        mOpenCvCameraView = javaCameraView as CameraBridgeViewBase
        mOpenCvCameraView?.setMinMaxFrameSize(
            frameConfig.min_frame_width,
            frameConfig.min_frame_height,
            frameConfig.max_frame_width,
            frameConfig.max_frame_height
        )
        mOpenCvCameraView?.setCameraIndex(frameConfig.camera_index)
        mOpenCvCameraView!!.visibility = SurfaceView.VISIBLE
        mOpenCvCameraView!!.setCvCameraViewListener(this)
        // Determine the output directory to save the captured image
        outputDirectory = getOutputDirectory(this)

    }

    private fun readFrameConfigAsset() {
        val data = getJsonDataFromAsset(applicationContext, "frame_config_json.json");
        frameConfig = Gson().fromJson(data, FrameConfig::class.java);
    }

    @RequiresApi(Build.VERSION_CODES.M)
    public override fun onResume() {
        super.onResume()
        // Before setting full screen flags, we must wait a bit to let UI settle; otherwise, we may
        // be trying to set app to immersive mode before it's ready and the flags do not stick
        container.postDelayed({
            container.systemUiVisibility = FLAGS_FULLSCREEN
        }, IMMERSIVE_FLAG_TIMEOUT)
        // Make sure that all permissions are still present, since user could have removed them
        //  while the app was on paused state
        if (!hasPermissions(this)) {
            // Request camera-related permissions
            requestPermissions(PERMISSIONS_REQUIRED, PERMISSIONS_REQUEST_CODE)
        } else {
            initializeOpenCVLibrary()
        }
        //To rest the flag to start uploading the image
        fileUploading = false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (PackageManager.PERMISSION_GRANTED == grantResults.firstOrNull()) {
                // Take the user to the success fragment when permission is granted
                //Toast.makeText(this, "Permission request granted", Toast.LENGTH_LONG).show()
                initializeOpenCVLibrary()
            } else {
                Toast.makeText(this, "Permission request denied", Toast.LENGTH_LONG).show()
            }
        }
    }

    /** Convenience method used to check if all permissions required by this app are granted */
    private fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun initializeOpenCVLibrary() {
        if (!OpenCVLoader.initDebug()) {
            Log.d(
                TAG,
                "Internal OpenCV library not found. Using OpenCV Manager for initialization"
            )
            OpenCVLoader.initAsync(
                OpenCVLoader.OPENCV_VERSION_3_0_0,
                this,
                mLoaderCallback
            )

        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    public override fun onPause() {
        super.onPause()
        if (mOpenCvCameraView != null) mOpenCvCameraView!!.disableView()
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (mOpenCvCameraView != null) mOpenCvCameraView!!.disableView()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        mRgba = Mat(height, width, CvType.CV_8UC4)
    }

    override fun onCameraViewStopped() {
        mRgba?.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
        mRgba = inputFrame.rgba();
        val imgCloned = mRgba!!.clone()
        val gray = Mat()
        Imgproc.cvtColor(imgCloned, gray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.medianBlur(gray, gray, 5)
        val circles = Mat()
        if (inputFrame.rgba().width() < 480) {
            Imgproc.HoughCircles(
                gray,
                circles,
                Imgproc.CV_HOUGH_GRADIENT,
                frameConfig.frame_small.circle.dp,
                frameConfig.frame_small.circle.minDist,
                frameConfig.frame_small.circle.param1,
                frameConfig.frame_small.circle.param2,
                frameConfig.frame_small.circle.minRadius,
                frameConfig.frame_small.circle.maxRadius
            )
        } else {
            Imgproc.HoughCircles(
                gray,
                circles,
                Imgproc.CV_HOUGH_GRADIENT,
                frameConfig.frame_large.circle.dp,
                frameConfig.frame_large.circle.minDist,
                frameConfig.frame_large.circle.param1,
                frameConfig.frame_large.circle.param2,
                frameConfig.frame_large.circle.minRadius,
                frameConfig.frame_large.circle.maxRadius
            )

        }

        runOnUiThread {
            textViewArea.text = ""
            textViewArea.visibility = View.GONE
        }

        if (circles.cols() > 0) {
            for (x in 0 until Math.min(circles.cols(), 5)) {
                val circleVec = circles[0, x] ?: break
                val center =
                    Point(circleVec[0], circleVec[1])
                val radius = circleVec[2].toInt()
                //Imgproc.circle(imgCloned, center, 3, Scalar(255.0, 0.0, 0.0), 5)
                Imgproc.circle(imgCloned, center, radius, Scalar(0.0,	206.0,	209.0), 1)
            }
            if (circles.cols() == 4) {
                Log.d(TAG, "Circles detected: ${circles.cols()}")
                var x1 = circles[0, 0][0]
                var y1 = circles[0, 0][1]
                var x2 = circles[0, 1][0]
                var y2 = circles[0, 1][1]
                var x3 = circles[0, 2][0]
                var y3 = circles[0, 2][1]
                var x4 = circles[0, 3][0]
                var y4 = circles[0, 3][1]

                val pointsArray = mutableListOf<Point>()
                pointsArray.add(0, Point(x1, y1))
                pointsArray.add(1, Point(x2, y2))
                pointsArray.add(2, Point(x3, y3))
                pointsArray.add(3, Point(x4, y4))

                //Log.d(TAG, "pointsArray:$pointsArray ");
                Collections.sort(pointsArray, kotlin.Comparator { o1, o2 ->
                    if (o1.x > o2.x) {
                        return@Comparator 1;
                    } else if (o1.x < o2.x) {
                        return@Comparator -1;
                    } else {
                        return@Comparator 0;
                    }
                })

                val leftPoints = mutableListOf<Point>()
                val rightPoints = mutableListOf<Point>()

                leftPoints.add(0, pointsArray[0])
                leftPoints.add(1, pointsArray[1])

                rightPoints.add(0, pointsArray[2])
                rightPoints.add(1, pointsArray[3])

                Collections.sort(leftPoints, kotlin.Comparator { o1, o2 ->
                    if (o1.y > o2.y) {
                        return@Comparator 1;
                    } else if (o1.y < o2.y) {
                        return@Comparator -1;
                    } else {
                        return@Comparator 0;
                    }
                })

                Collections.sort(rightPoints, kotlin.Comparator { o1, o2 ->
                    if (o1.y > o2.y) {
                        return@Comparator 1;
                    } else if (o1.y < o2.y) {
                        return@Comparator -1;
                    } else {
                        return@Comparator 0;
                    }
                })

                val topLeftOriginal = leftPoints[0]
                val topRightOriginal = rightPoints[0]
                val bottomLeftOriginal = leftPoints[1]
                val bottomRightOriginal = rightPoints[1]

                //Log.d(TAG, "Sorted pointsArray:$pointsArray ");
                //System.out.println("x1::"+x1+"Y1::"+y1+"circl2::"+circles[0,1]+"circl4::"+circles[0,2])
                /* Imgproc.rectangle(mRgba, topLeftOriginal,
                    bottomRightOriginal,
                    Scalar
                        (0.0,0.0,255.0), 3)*/

                Imgproc.line(
                    imgCloned, topLeftOriginal,
                    bottomLeftOriginal,
                    Scalar
                        (0.0,	206.0,	209.0), 1
                )
                Imgproc.line(
                    imgCloned, bottomLeftOriginal,
                    bottomRightOriginal,
                    Scalar
                        (0.0,	206.0,	209.0), 1
                )
                Imgproc.line(
                    imgCloned, topLeftOriginal,
                    topRightOriginal,
                    Scalar
                        (0.0,	206.0,	209.0), 1
                )
                Imgproc.line(
                    imgCloned, topRightOriginal,
                    bottomRightOriginal,
                    Scalar
                        (0.0,	206.0,	209.0), 1
                )

                val area =
                    abs((bottomRightOriginal.x - topLeftOriginal.x) * (bottomRightOriginal.y - topRightOriginal.y))

                Log.d(TAG, "Area calculated:$area")
                if (inputFrame.rgba().width() < 480) {
                    AREA_THRESHOLD_LOWER = frameConfig.frame_small.aREA_THRESHOLD_LOWER
                    AREA_THRESHOLD_START = frameConfig.frame_small.aREA_THRESHOLD_START
                    AREA_THRESHOLD_END = frameConfig.frame_small.aREA_THRESHOLD_END
                    AREA_THRESHOLD_UPPER = frameConfig.frame_small.aREA_THRESHOLD_UPPER
                } else {
                    AREA_THRESHOLD_LOWER = frameConfig.frame_large.aREA_THRESHOLD_LOWER
                    AREA_THRESHOLD_START = frameConfig.frame_large.aREA_THRESHOLD_START
                    AREA_THRESHOLD_END = frameConfig.frame_large.aREA_THRESHOLD_END
                    AREA_THRESHOLD_UPPER = frameConfig.frame_large.aREA_THRESHOLD_UPPER
                }

                if (area >= AREA_THRESHOLD_LOWER && area < AREA_THRESHOLD_START) {
                    Log.d(TAG, "Please bring your phone near to the paper")
                    runOnUiThread {
                        textViewArea.text = "Please bring your phone near to the paper"
                        textViewArea.visibility = View.VISIBLE
                    }
                    //Toast.makeText(this@JavaPreviewActivity, "Please bring your phone near to the paper", Toast.LENGTH_SHORT).show()
                    //return;
                }

                if (area > AREA_THRESHOLD_END && area <= AREA_THRESHOLD_UPPER) {
                    Log.d(TAG, "Please move your phone away from the paper")
                    runOnUiThread {
                        textViewArea.text = "Please move your phone away from the paper"
                        textViewArea.visibility = View.VISIBLE
                    }
                    //Toast.makeText(this@JavaPreviewActivity, "Please move your phone away from the paper", Toast.LENGTH_SHORT).show()
                    //return ;
                }

                if (area >= AREA_THRESHOLD_START && area <= AREA_THRESHOLD_END) {
                    Log.d(TAG, "Optimal image found, detecting boxes")
                    var maxHeight = Math.max(
                        (bottomRightOriginal.y.toInt() - topRightOriginal.y.toInt()),
                        (bottomLeftOriginal.y.toInt() - topLeftOriginal.y.toInt())
                    )

                    var maxWidth = Math.max(
                        (bottomRightOriginal.x.toInt() - bottomLeftOriginal.x.toInt()),
                        (topRightOriginal.y.toInt() - topLeftOriginal.y.toInt())
                    )
                    var croppedMat = Mat()
                    val rectCrop = Rect(
                        topLeftOriginal.x.toInt(), topLeftOriginal.y.toInt(),
                        maxWidth,
                        maxHeight
                    )
                    //croppedMat = imgCloned.submat(rectCrop)

                    croppedMat = this.homographicTransformation(
                        mRgba,
                        topLeftOriginal,
                        bottomLeftOriginal,
                        topRightOriginal,
                        bottomRightOriginal
                    )
                    val sharped = Mat()
                    Imgproc.cvtColor(croppedMat, sharped, Imgproc.COLOR_BGR2GRAY);
                    Imgproc.adaptiveThreshold(sharped, sharped,
                        255.0, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 5, 4.0
                    );

                    ScannerConstants.previewImageBitmap = rotateImage(ImageUtils.matToBitmap(sharped))
                    moveBitmapToPhotoMap(rotateImage(ImageUtils.matToBitmap(croppedMat)))
                }
            }
        }
        return imgCloned!! // This function must return
    }

    private fun rotateImage(resultBitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.setRotate(90F)
        return Bitmap.createBitmap(
            resultBitmap,
            0,
            0,
            resultBitmap.getWidth(),
            resultBitmap.getHeight(),
            matrix,
            true
        )
    }

    //Method to generate homographic image from the given Mat
    private fun homographicTransformation(
        imgCloned: Mat?,
        topLeftOriginal: Point,
        bottomLeftOriginal: Point,
        topRightOriginal: Point,
        bottomRightOriginal: Point
    ): Mat {
        val pointSource = arrayListOf<Point>()
        pointSource.add(Point(topLeftOriginal.x, topLeftOriginal.y))
        pointSource.add(Point(topRightOriginal.x, topRightOriginal.y))
        pointSource.add(Point(bottomRightOriginal.x, bottomRightOriginal.y))
        pointSource.add(Point(bottomLeftOriginal.x, bottomLeftOriginal.y))

        val size = Size(frameConfig.homographic_image_height, frameConfig.homographic_image_width)

        val destinationMat = Mat(size, CvType.CV_8UC3)
        var pointDestination = arrayListOf<Point>()
        pointDestination.add(Point(0.0, 0.0))
        pointDestination.add(Point(size.width - 1, 0.0))
        pointDestination.add(Point(size.width - 1, size.height - 1))
        pointDestination.add(Point(0.0, size.height - 1))


        val sourcePoint2f = MatOfPoint2f()
        sourcePoint2f.fromList(pointSource)

        val destinationPoint2f = MatOfPoint2f()
        destinationPoint2f.fromList(pointDestination)

        val he = Calib3d.findHomography(sourcePoint2f, destinationPoint2f)
        Imgproc.warpPerspective(imgCloned, destinationMat, he, size)
        return destinationMat
    }

    var fileUploading = false
    private fun moveBitmapToPhotoMap(bmRotated: Bitmap) {
        if (fileUploading) return
        fileUploading = true
        //To play camera shutter sound
        val sound = MediaActionSound()
        sound.play(MediaActionSound.SHUTTER_CLICK);
        // Create output file to hold the image
        val photoFile = createFile(outputDirectory, FILENAME, PHOTO_EXTENSION)
        someTask(photoFile, bmRotated!!).execute()
    }

    inner class someTask(val file: File, val bitmap: Bitmap) :
        AsyncTask<File, Void, String>() {
        override fun doInBackground(vararg params: File): String? {
            return processFile(file, bitmap)
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            result?.let {
                gallery = File(it)
                val arag = PhotoFragment.createBundle(File(""))
                val intent = Intent(this@JavaPreviewActivity, PhotoActivity::class.java)
                intent.putExtras(arag)
                startActivity(intent)
            }
        }
    }

    private fun processFile(file: File, bitmap: Bitmap): String? {
        var bmp: Bitmap = bitmap
        var fileOutputStream: FileOutputStream? = null
        val finalImage = file.parent + "/" + System.currentTimeMillis() + ".jpg"
        try {
            var newScaledBitmap = bmp
            fileOutputStream =
                FileOutputStream(finalImage)

            newScaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
            ScannerConstants.selectedImageBitmap = newScaledBitmap;
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        BitmapUtils.copyExif(file.absolutePath, finalImage)
        return finalImage
    }


    companion object {
        private const val TAG = "OCVSample::Activity"

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

        /** Use external media if it is available, our app's file directory otherwise */
        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
                File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() }
            }
            return if (mediaDir != null && mediaDir.exists())
                mediaDir else appContext.filesDir
        }
    }

    private fun getJsonDataFromAsset(context: Context, fileName: String): String? {
        val jsonString: String
        try {
            jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            return null
        }
        return jsonString
    }

}