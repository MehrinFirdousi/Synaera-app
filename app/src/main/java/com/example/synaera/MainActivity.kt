package com.example.synaera

//import android.media.Image

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.synaera.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

typealias ServListener = (serv: Byte) -> Unit

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var client : OkHttpClient
    private lateinit var cameraExecutor: ExecutorService
    private var url : String = "http://192.168.1.16:80/sendImg"
    private var translationOngoing : Boolean = false
    private var cameraFacing : Int = CameraSelector.LENS_FACING_FRONT
    private var imgNo : Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Set up HTTP client
        client = OkHttpClient().newBuilder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Set up the listeners for record, flip camera and open gallery buttons
        viewBinding.startCaptureButton.setOnClickListener {
            if (translationOngoing)
                viewBinding.startCaptureButton.setText(R.string.start_capture)
            else
                viewBinding.startCaptureButton.setText(R.string.stop_capture)
            translationOngoing = !translationOngoing
            viewBinding.textView.text = "" // this should run only once the imageanalyzer stops running and the last packets are done sending, right now it doesnt work properly
        }
        viewBinding.flipCameraButton.setOnClickListener {
            if (cameraFacing == CameraSelector.LENS_FACING_FRONT)
                cameraFacing = CameraSelector.LENS_FACING_BACK
            else
                cameraFacing = CameraSelector.LENS_FACING_FRONT
            startCamera()
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ServerConnection { serv ->
                        Log.d(TAG, "byte is: $serv")
                    })
                }
            // Select back camera as a default
            val cameraSelector: CameraSelector = if (cameraFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private inner class ServerConnection(private val listener: ServListener) : ImageAnalysis.Analyzer {

        private fun sendPostRequest(sUrl : String, array : ByteArray) {

            val body: RequestBody

            val json: String = array.contentToString()
            body = json.toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(sUrl)
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }
                override fun onResponse(call: Call, response: Response) {

                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    for ((name, value) in response.headers) {
                        println("$name: $value")
                    }

                    //this@MainActivity.runOnUiThread {
                        val responseData = response.body!!.string()
                        //viewBinding.textView.text = responseData
                        Log.d(TAG, "response: $responseData")
                        //}
                }
            })
        }

        private fun ImageProxy.toBitmap(): Bitmap {
            val yBuffer = planes[0].buffer // Y
            val vuBuffer = planes[2].buffer // VU

            val ySize = yBuffer.remaining()
            val vuSize = vuBuffer.remaining()

            val nv21 = ByteArray(ySize + vuSize)

            yBuffer.get(nv21, 0, ySize)
            vuBuffer.get(nv21, ySize, vuSize)

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
            val imageBytes = out.toByteArray()
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        }

        private fun sendPost(array: ByteArray): String? {
            var responseData: String? = null
            try {
//                val body: RequestBody
//
//                val json: String = array.contentToString()
//                body = json.toRequestBody("application/json".toMediaTypeOrNull())

                val postBodyImage: RequestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "image",
                        "frame$imgNo.jpg",
                        array.toRequestBody("image/*jpg".toMediaTypeOrNull(), 0, array.size)
                    )
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .post(postBodyImage)
                    .build()

                val response = client.newCall(request).execute()
                responseData = response.body!!.string()
            } catch (err: Error) {
                println("Error when executing postt request: "+err.localizedMessage)
            }
            return (responseData)
        }

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        override fun analyze(image: ImageProxy) {
            if (translationOngoing) {

    //            val buffer = image.planes[0].buffer
    //            val data = buffer.toByteArray()
    //            val tempData = byteArrayOf(data[0], data[1], data[2], data[3])
    //            val len = data.size

                val stream = ByteArrayOutputStream()
                val options = BitmapFactory.Options()
                options.inPreferredConfig = Bitmap.Config.RGB_565
                val imgBitmap = image.toBitmap()
                imgBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                val byteArray = stream.toByteArray()

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val result = sendPost(byteArray)
                        imgNo++
                        if (result != null) {
                            withContext(Dispatchers.Main) {
                                val curLen = viewBinding.textView.text.length
                                if (curLen < 30) {
                                    viewBinding.textView.append(" $result")
                                }
                                else
                                    viewBinding.textView.text = result
                            }
                            Log.d(TAG, "responsee: $result")
                        }
                    } catch(exc: Exception) {
                        Log.e(TAG, "Cannot connect to Flask server", exc)
                    }
                }
                listener(byteArray[0])
            }
            else
                listener(0)
            image.close()
        }
    }

    /*private fun sendRequest(sUrl : String, array : ByteArray) {

        val body: RequestBody

        val json: String = array.contentToString()
        body = json.toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(sUrl)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
            override fun onResponse(call: Call, response: Response) {

                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                for ((name, value) in response.headers) {
                    println("$name: $value")
                }

//                this@MainActivity.runOnUiThread {
                    val responseData = response.body!!.string()
                    viewBinding.textView.text = responseData
//                }
            }
        })
    }*/
}
