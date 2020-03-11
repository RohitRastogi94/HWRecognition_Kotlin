package com.tarento.markreader

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.Camera
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tarento.markreader.data.ApiClient
import com.tarento.markreader.data.OCR
import com.tarento.markreader.data.OCRService
import com.tarento.markreader.data.model.ProcessResult
import com.tarento.markreader.fragments.CameraFragment
import com.tarento.markreader.utils.BitmapUtils
import com.tarento.markreader.utils.FLAGS_FULLSCREEN
import com.tarento.markreader.utils.ProgressBarUtil
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.json.JSONObject
import org.opencv.android.*
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import retrofit2.Callback
import retrofit2.Response
import team.clevel.documentscanner.helpers.ImageUtils
import team.clevel.documentscanner.helpers.ScannerConstants
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs


private const val IMMERSIVE_FLAG_TIMEOUT = 500L

private const val AREA_THRESHOLD_LOWER = 150000.00
private const val AREA_THRESHOLD_START = 180000.00
private const val AREA_THRESHOLD_END = 200000.00
private const val AREA_THRESHOLD_UPPER = 220000.00

class JavaPreviewActivity : AppCompatActivity(),
    CameraBridgeViewBase.CvCameraViewListener2 {
    private var mOpenCvCameraView: CameraBridgeViewBase? = null
    private var javaCameraView: JavaCameraView? = null
    private val mIsJavaCamera = true
    private val mItemSwitchCamera: MenuItem? = null
    private lateinit var container: FrameLayout
    var mRgba: Mat? = null
    var mRgbaF: Mat? = null
    var mRgbaT: Mat? = null
    private lateinit var outputDirectory: File
    private var gallery: File? = null
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
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_javapreview)
        container = findViewById(R.id.frameLayout)
        javaCameraView =
            findViewById<View>(R.id.tutorial1_activity_java_surface_view) as JavaCameraView
        mOpenCvCameraView = javaCameraView as CameraBridgeViewBase
        mOpenCvCameraView?.setMaxFrameSize(640, 480)
        mOpenCvCameraView?.setCameraIndex(0)
        mOpenCvCameraView!!.visibility = SurfaceView.VISIBLE
        mOpenCvCameraView!!.setCvCameraViewListener(this)


        // Determine the output directory
        outputDirectory = getOutputDirectory(this)
    }

    public override fun onPause() {
        super.onPause()
        if (mOpenCvCameraView != null) mOpenCvCameraView!!.disableView()
    }

    public override fun onResume() {
        super.onResume()
        // Before setting full screen flags, we must wait a bit to let UI settle; otherwise, we may
        // be trying to set app to immersive mode before it's ready and the flags do not stick
        container.postDelayed({
            container.systemUiVisibility = FLAGS_FULLSCREEN
        }, IMMERSIVE_FLAG_TIMEOUT)
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

//        javaCameraView?.camera?.let {
//            setCameraDisplayOrientation(0, it)
//
//        }
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

        Imgproc.HoughCircles(
            gray,
            circles,
            Imgproc.CV_HOUGH_GRADIENT,
            1.5,
            100.0,
            100.0,
            30.0,
            10,
            15
        )

        if (circles.cols() == 4) {
            for (x in 0 until Math.min(circles.cols(), 5)) {
                val circleVec = circles[0, x] ?: break
                val center =
                    Point(circleVec[0], circleVec[1])
                val radius = circleVec[2].toInt()
                //Imgproc.circle(imgCloned, center, 3, Scalar(255.0, 0.0, 0.0), 5)
                //Imgproc.circle(imgCloned, center, radius, Scalar(255.0, 0.0, 0.0), 2)
            }
            if (circles.cols() == 4) {

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

/*
             Imgproc.line(imgCloned, topLeftOriginal,
                 bottomLeftOriginal,
                Scalar
                    (0.0,0.0,255.0), 3)
            Imgproc.line(imgCloned, bottomLeftOriginal,
                bottomRightOriginal,
                Scalar
                    (0.0,0.0,255.0), 3)
            Imgproc.line(imgCloned, topLeftOriginal,
                topRightOriginal,
                Scalar
                    (0.0,0.0,255.0), 3)
            Imgproc.line(imgCloned, topRightOriginal,
                bottomRightOriginal,
                Scalar
                    (0.0,0.0,255.0), 3)*/

                val area =
                    abs((bottomRightOriginal.x - topLeftOriginal.x) * (bottomRightOriginal.y - topRightOriginal.y))
                if (area >= AREA_THRESHOLD_LOWER && area < AREA_THRESHOLD_START) {
                    Log.d(TAG, "Please bring your phone near to the paper")
                    //Toast.makeText(this@JavaPreviewActivity, "Please bring your phone near to the paper", Toast.LENGTH_SHORT).show()
                    //return;
                }

                if (area > AREA_THRESHOLD_END && area <= AREA_THRESHOLD_UPPER) {
                    Log.d(TAG, "Please move your phone away from the paper")
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
                    croppedMat = imgCloned.submat(rectCrop)

                    //m_imgMat(cv::Rect((int)rect.topLeftX, (int)rect.topLeftY, (int)rect.bottomRightX - (int)rect.topLeftX,
                    // (int)rect.bottomRightY - (int)rect.topLeftY)).copyTo(croppedImg);

                    val resultBitmap = ImageUtils.matToBitmap(croppedMat)

//                var bitmap = resultBitmap as Bitmap?
//                bitmap = Bitmap.createScaledBitmap(bitmap!!, 640, 480, false)

                    val matrix = Matrix()
                    matrix.setRotate(90F)
                    val bmRotated: Bitmap = Bitmap.createBitmap(
                        resultBitmap,
                        0,
                        0,
                        resultBitmap.getWidth(),
                        resultBitmap.getHeight(),
                        matrix,
                        true
                    )

                    /* val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
                    val currentDateandTime: String = sdf.format(Date())
                    val fileName: String = Environment.getExternalStorageDirectory().getPath().toString() +
                            "/sample_picture_" + currentDateandTime + ".jpg"

                    val byteArrayOutputStream = ByteArrayOutputStream()
                    bmRotated.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                    val byteArray: ByteArray = byteArrayOutputStream.toByteArray()

                    val fos = FileOutputStream(fileName)

                    fos.write(byteArray)
                    fos.close()*/

                    moveBitmapToPhotoMap(bmRotated)
                    //mOpenCvCameraView?.disableView()

                    /*mRgba?.release()
                    circles.release()
                    imgCloned.release()*/

                    /* val arag = PhotoFragment.createBundle(File(""))
                    ScannerConstants.selectedImageBitmap = bmRotated
                    val intent = Intent(this, PhotoActivity::class.java)
                    intent.putExtras(arag)
                    startActivity(intent)*/

                    //m_imgMat(cv::Rect((int)rect.topLeftX, (int)rect.topLeftY, (int)rect.bottomRightX - (int)rect.topLeftX,
                    // (int)rect.bottomRightY - (int)rect.topLeftY)).copyTo(croppedImg);


                    /*dispatch_async(dispatch_get_main_queue(), ^{
                        [self performSegueWithIdentifier:@"SEQUE_CAPTURED_IMAGE" sender:self];
                    });*/
                }


            }
        }
        circles.release()
        return imgCloned!! // This function must return
    }
    var fileUploading = false;
    private fun moveBitmapToPhotoMap(bmRotated: Bitmap) {
        if(fileUploading) return
        fileUploading = true
        // Create output file to hold the image
        val photoFile = createFile(outputDirectory, FILENAME, PHOTO_EXTENSION)


        someTask(photoFile, bmRotated!!).execute()

    }


    inner class someTask(val file: File, val bitmap: Bitmap) :
        AsyncTask<File, Void, String>() {
        override fun doInBackground(vararg params: File): String? {
            return processFile(file, bitmap)
        }

        override fun onPreExecute() {
            super.onPreExecute()

            //ProgressBarUtil.showProgressDialog(this@JavaPreviewActivity)
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            result?.let {
                //ProgressBarUtil.dismissProgressDialog()
                gallery = File(it)
                getOCRImageUploadResponse(gallery)
                //val intent = Intent(this@JavaPreviewActivity, ImageCropActivity::class.java)
                //startActivityForResult(intent, 1234)
            }
        }
    }

    private fun processFile(file: File, bitmap: Bitmap): String? {

        var bmp: Bitmap = bitmap
        var fileOutputStream: FileOutputStream? = null
        println("Time 4 ${System.currentTimeMillis()}")
        val finalImage = file.parent + "/" + System.currentTimeMillis() + ".jpg"
        try {
            var newScaledBitmap = bmp
            fileOutputStream =
                FileOutputStream(finalImage)
            newScaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
            println("newScaledBitmap $newScaledBitmap")
            ScannerConstants.selectedImageBitmap = newScaledBitmap;
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

        println("Time 5 ${System.currentTimeMillis()}")
        BitmapUtils.copyExif(file.absolutePath, finalImage)

        println("Time 6 ${System.currentTimeMillis()} finalImage $finalImage")

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

    init {
        Log.i(TAG, "Instantiated new " + this.javaClass)
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


        val apiInterface: OCRService = ApiClient.getClient()!!.create(OCRService::class.java)

        val data = encodeImage(file) ?: return


//        val requestBody = data.toRequestBody("application/octet-stream".toMediaTypeOrNull())
        val requestBody = RequestBody.create("application/octet-stream".toMediaTypeOrNull(), data)

        val hero = apiInterface.getData(requestBody)
        hero.enqueue(object : Callback<OCR> {
            override fun onFailure(call: retrofit2.Call<OCR>, t: Throwable) {

                Toast.makeText(this@JavaPreviewActivity, "Some thing went wrong", Toast.LENGTH_SHORT)
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

                        Toast.makeText(this@JavaPreviewActivity, "Some thing went wrong", Toast.LENGTH_SHORT)
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

                    Toast.makeText(this@JavaPreviewActivity, "Some thing went wrong", Toast.LENGTH_SHORT)
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
                            val intent = Intent(this@JavaPreviewActivity, ResultActivity::class.java)
                            intent.putExtra("result", processResult)
                            startActivity(intent)
                        } else {

                                Toast.makeText(this@JavaPreviewActivity, "Some thing went wrong", Toast.LENGTH_SHORT)
                                    //.show()

                        }

                    }
                }
            }

        })

    }


}