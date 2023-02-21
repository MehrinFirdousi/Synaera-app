package com.example.synaera

//import android.media.Image

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
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
    private val pickImage = 100
    private var url : String = "http://192.168.1.16:80/sendImg"
    private var translationOngoing : Boolean = false
    private var cameraFacing : Int = CameraSelector.LENS_FACING_FRONT
    private var imgNo : Int = 0
    private var chatList = ArrayList<ChatBubble>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        /** sender = true for system, false for user */

        chatList.add(ChatBubble("Hello", true))
        chatList.add(ChatBubble("hi", false))
        chatList.add(ChatBubble("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum", false))
        chatList.add(ChatBubble("hi", false))
        chatList.add(ChatBubble("hi", false))
        chatList.add(ChatBubble("hi", false))
        chatList.add(ChatBubble("bye", true))
        chatList.add(ChatBubble("Lorem ipsum dolor sit amet, et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum", true))
        chatList.add(ChatBubble("hi", false))
        chatList.add(ChatBubble("hi", false))
        chatList.add(ChatBubble("hi", false))
        chatList.add(ChatBubble("hi", false))

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

        setViewPagerAdapter()
        setBottomNavigation()
        setViewPagerListener()
        viewBinding.bottomNavBar.selectedItemId = R.id.camera_menu_id
        val selectImageIntent = registerForActivityResult(ActivityResultContracts.GetContent())
        { uri ->
            //do whatever with the result, its the URI
        }

        viewBinding.openGalleryButton.setOnClickListener {
            selectImageIntent.launch("image/*")
        }

        // Set up the listeners for record, flip camera and open gallery buttons

//         Set up the listeners for record, flip camera and open gallery buttons
        viewBinding.startCaptureButton.setOnClickListener {
            if (translationOngoing)
                viewBinding.startCaptureButton.setBackgroundResource(R.drawable.outline_circle_24)
            else
                viewBinding.startCaptureButton.setBackgroundResource(R.drawable.outline_stop_circle_24)

            translationOngoing = !translationOngoing
//            viewBinding.textView.text = "" // this should run only once the imageanalyzer stops running and the last packets are done sending, right now it doesnt work properly
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

    private fun setViewPagerListener() {
        viewBinding.viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewBinding.bottomNavBar.selectedItemId = when(position) {
                    0 ->  R.id.gallery_menu_id
                    1 ->  R.id.camera_menu_id
                    2 ->  R.id.chat_menu_id
                    else -> R.id.camera_menu_id
                }
                super.onPageSelected(position)
            }
        })
    }

    private fun setBottomNavigation() {
        viewBinding.bottomNavBar.setOnItemSelectedListener {
//            viewBinding.viewPager.currentItem = when(it.itemId) {
//                R.id.gallery_menu_id -> 0
//                R.id.camera_menu_id -> 1
//                R.id.chat_menu_id -> 2
//                else -> 1
//            }
            if (it.itemId == R.id.gallery_menu_id) {
                viewBinding.viewPager.currentItem = 0
                disableCameraButtons()
            }
            else if (it.itemId == R.id.camera_menu_id) {
                viewBinding.viewPager.currentItem = 1
                enableCameraButtons()
            }
            else if (it.itemId == R.id.chat_menu_id) {
                viewBinding.viewPager.currentItem = 2
                disableCameraButtons()
            }
            else
                viewBinding.viewPager.currentItem = 1
            return@setOnItemSelectedListener true
        }
    }

    private fun disableCameraButtons() {
        viewBinding.openGalleryButton.visibility = View.INVISIBLE;
        viewBinding.startCaptureButton.visibility = View.INVISIBLE;
        viewBinding.flipCameraButton.visibility = View.INVISIBLE;
        viewBinding.infoButton.visibility = View.INVISIBLE;
        viewBinding.settingsButton.visibility = View.INVISIBLE;
        viewBinding.textView.visibility = View.INVISIBLE;
    }
    private fun enableCameraButtons() {
        viewBinding.openGalleryButton.visibility = View.VISIBLE;
        viewBinding.startCaptureButton.visibility = View.VISIBLE;
        viewBinding.flipCameraButton.visibility = View.VISIBLE;
        viewBinding.infoButton.visibility = View.VISIBLE;
        viewBinding.settingsButton.visibility = View.VISIBLE;
        viewBinding.textView.visibility = View.VISIBLE;
    }

    private fun setViewPagerAdapter() {
        val curitem = viewBinding.viewPager.currentItem
        Log.d(TAG, "current item in viewpager = $curitem")
        viewBinding.viewPager.adapter = ViewPagerAdapter(this, chatList)
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
        private const val TAG = "SynaeraApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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
//                Log.d(TAG, "responsee: $responseData")

            } catch (err: Error) {
                println("Error when executing postt request: "+err.localizedMessage)
            }
            if (responseData != null) {
                if (responseData.contains("nothing"))
                    return null
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
                                if (curLen < 100) {
                                    chatList.add(ChatBubble(result, false))
                                    viewBinding.textView.append(" $result")
                                }
                                else
                                    chatList.add(ChatBubble(result, false))
                                    viewBinding.textView.text = result
                            }
                        }
                    } catch(exc: Exception) {
                        Log.e(TAG, "Cannot connect to Flask server", exc)
                    }
                }
//                listener(byteArray[0])
            }
//            else
//                listener(0)
            image.close()
        }
    }
}
