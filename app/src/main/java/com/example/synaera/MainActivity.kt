package com.example.synaera

//import android.media.Image

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.viewpager2.widget.ViewPager2
import com.example.synaera.databinding.ActivityMainBinding
import com.google.common.util.concurrent.ListenableFuture
import okhttp3.OkHttpClient
import java.io.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


typealias ServListener = (serv: Int) -> Unit

class MainActivity : AppCompatActivity(), ServerResultCallback {
    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var client : OkHttpClient
    private lateinit var cameraExecutor: ExecutorService
    private val pickImage = 100
//    private var url : String = "http://192.168.1.16:5000/sendImg"
//    private var url : String = "https://42bd-2001-8f8-1623-131a-c997-5075-626d-f3eb.eu.ngrok.io/sendImg"
//    private var url : String = "http://synaera-api.centralindia.cloudapp.azure.com:5000/sendImg"
    private var translationOngoing : Boolean = false
    private var cameraFacing : Int = CameraSelector.LENS_FACING_FRONT
    private var chatList = ArrayList<ChatBubble>()
    private lateinit var chatFragment: ChatFragment

    private lateinit var mServer: ServerClient
    private lateinit var mCameraPreview: PreviewView

    // Camera Use-Cases
    private var mPreview : Preview? = null
    private var mImageAnalysis: ImageAnalysis? = null

    private val mTargetWidth = 640
    private val mTargetHeight = 480

    private var mLastTime: Long = 0
    private val mUploadDelay: Long = 100

    private var mIsStreaming = false

    private val mStreamFromCameraPreview = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        mServer = ServerClient.getInstance()
//        mServer.init("user", "pass", "20.193.159.90", 5000)
        mServer.init("user", "pass", "192.168.1.39", 5000)
        mServer.connect()

        mCameraPreview = viewBinding.viewFinder

        /** sender = true for system, false for user */
        chatList.add(ChatBubble("Hello", true))
        chatList.add(ChatBubble("hi", false))
        chatList.add(ChatBubble("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum", false))
        chatList.add(ChatBubble("hi", false))
        chatList.add(ChatBubble("hi", false))
        chatList.add(ChatBubble("bye", true))
        //chatList.add(ChatBubble("Lorem ipsum dolor sit amet, et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum", true))
        chatList.add(ChatBubble("hi10", false))

        chatFragment = ChatFragment.newInstance(chatList)

        // Set up HTTP client
        client = OkHttpClient().newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCameraPreview()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Set up adapters and bottom navigation
        setViewPagerAdapter()
        setBottomNavigation()
        setViewPagerListener()
//        viewBinding.bottomNavBar.selectedItemId = R.id.camera_menu_id

        // allow video selection from gallery
        val selectVideoIntent = registerForActivityResult(ActivityResultContracts.GetContent())
        { uri ->
            //do whatever with the result, its the URI
//            var videoBytes = convertVideoToBytes(uri)

            val frames = getFrames(uri)
            for (frame in frames) {
                val byteArray = ImageConverter.BitmaptoJPEG(frame)
                mServer.sendImage(byteArray)
                mLastTime = System.currentTimeMillis()
            }
        }

        // Set up the listeners for record, flip camera and open gallery buttons
        viewBinding.openGalleryButton.setOnClickListener {
            selectVideoIntent.launch("video/*")
        }

        viewBinding.startCaptureButton.setOnClickListener {
            if (translationOngoing) { // stop translation here
                viewBinding.startCaptureButton.setBackgroundResource(R.drawable.outline_circle_24)
                if (mStreamFromCameraPreview) {
                    stopStreaming()
                } else {
                    stopCameraImageAnalysis()
                }
            }
            else { // start translation here
                viewBinding.startCaptureButton.setBackgroundResource(R.drawable.outline_stop_circle_24)
                if (mStreamFromCameraPreview) {
                    startStreaming()
                } else {
                    startCameraImageAnalysis()
                }
            }
            translationOngoing = !translationOngoing
//            viewBinding.textView.text = "" // this should run only once the imageanalyzer stops running and the last packets are done sending, right now it doesnt work properly
        }
        viewBinding.flipCameraButton.setOnClickListener {
            if (cameraFacing == CameraSelector.LENS_FACING_FRONT)
                cameraFacing = CameraSelector.LENS_FACING_BACK
            else
                cameraFacing = CameraSelector.LENS_FACING_FRONT
            startCameraPreview()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()
        mServer!!.registerCallback(this)
        mServer!!.connect()
    }
    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
        if (mIsStreaming) stopStreaming()
        mServer!!.unregisterCallback()
        mServer!!.disconnect()
    }

    fun convertVideoToBytes(uri: Uri?): ByteArray? {
        var videoBytes: ByteArray? = null
        try {
            val baos = ByteArrayOutputStream()
            val fis = FileInputStream(File(uri.toString()))
            val buf = ByteArray(1024)
            var n: Int
            while (-1 != fis.read(buf).also { n = it }) baos.write(buf, 0, n)
            videoBytes = baos.toByteArray()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return videoBytes
    }

    private fun getFrames(uri: Uri?) : ArrayList<Bitmap> {
//        val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

        val retriever = MediaMetadataRetriever()
        if (uri != null) {
            val uriStr = uri.path
            Log.d(TAG, "uri = $uriStr")
            retriever.setDataSource(uri.path)
        }

        val timeInterval = 1000L // extract frame every 1 second
//        val timeUs = 1000000L // Time in microseconds
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
        val frames = ArrayList<Bitmap>()

        for (time in 0L..duration step timeInterval) {
            val frame = retriever.getFrameAtTime(time * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            val bitmap = frame?.let { Bitmap.createBitmap(it) }
            if (bitmap != null) {
                frames.add(bitmap)
            }
        }
        return frames
    }
    private fun setViewPagerListener() {
        viewBinding.viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewBinding.bottomNavBar.selectedItemId = when(position) {
                    0 ->  R.id.home_menu_id
                    1 ->  R.id.gallery_menu_id
                    2 ->  R.id.camera_menu_id
                    3 ->  R.id.chat_menu_id
                    4 ->  R.id.profile_menu_id
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
            when (it.itemId) {
                R.id.home_menu_id -> {
                    viewBinding.viewPager.currentItem = 0
                    disableCameraButtons()
                }
                R.id.gallery_menu_id -> {
                    viewBinding.viewPager.currentItem = 1
                    disableCameraButtons()
                }
                R.id.camera_menu_id -> {
                    viewBinding.viewPager.currentItem = 2
                    enableCameraButtons()
                }
                R.id.chat_menu_id -> {
                    viewBinding.viewPager.currentItem = 3
                    disableCameraButtons()
                }
                R.id.profile_menu_id -> {
                    viewBinding.viewPager.currentItem = 4
                    disableCameraButtons()
                }
                else -> viewBinding.viewPager.currentItem = 2
            }
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
        val curItem = viewBinding.viewPager.currentItem
        Log.d(TAG, "current item in viewpager = $curItem")
        viewBinding.viewPager.adapter = ViewPagerAdapter(this, chatFragment)
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
                startCameraPreview()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startCameraPreview() {
        Log.d(TAG, "startCameraPreview")
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
            ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                bindPreview(cameraProvider)
            } catch (e: ExecutionException) {
                // do nothing
            } catch (_: InterruptedException) {
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startCameraImageAnalysis() {
        Log.d(TAG, "startCameraPreview")
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                bindImageAnalysis(cameraProvider) // The stream will start after the bind
            } catch (e: ExecutionException) {
            } catch (e: InterruptedException) {
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        mPreview = Preview.Builder().build()
        // Preview use-case will render a preview image on the screen as defined by the PreviewView
        // element on the main's layout activity. The resolution of the layout is relative to the
        // screen size and defined in dp, which means the final resolution in pixels will be decided
        // at run-time when the layout is inflated to the device screen. But will always be proportional
        // to the resolution defined on the layout.

        mPreview!!.setSurfaceProvider(mCameraPreview?.createSurfaceProvider())
        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, mPreview)
    }
    private fun bindImageAnalysis(cameraProvider: ProcessCameraProvider) {
        mImageAnalysis = ImageAnalysis.Builder()
//            .setTargetResolution(Size(mTargetWidth, mTargetHeight))
//            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // non blocking
            .build()

        // Image Analysis use-case will not render the image on the screen, but will
        // deliver a frame by frame image (stream) directly from the camera buffer to the analyser.
        // In this case we can set an actual target resolution as defined by the user. CameraX will
        // try to match the captured resolution to the target resolution. If it cannot match, will
        // capture the frame with the resolution immediately above.
        mImageAnalysis!!.setAnalyzer(
            Executors.newFixedThreadPool(3),
            ImageAnalysis.Analyzer { image: ImageProxy ->
                val elapsedTime: Long = System.currentTimeMillis() - mLastTime
                if (elapsedTime > mUploadDelay && mUploadDelay != 0L) {   // Bound the image upload based on the user-defined frequency
                    val byteArray: ByteArray
                    // This its a better camera stream but the conversion might create artifacts with
                    // some cameras. Needs more investigation
                    byteArray = ImageConverter.YUV_420_800toJPEG(image)
                    mServer?.sendImage(byteArray)
                    mLastTime = System.currentTimeMillis()
                }
                image.close()
            })
        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        cameraProvider.bindToLifecycle((this as LifecycleOwner), cameraSelector, mImageAnalysis)
    }
    private fun stopCameraImageAnalysis() {
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbind(mImageAnalysis) // The stream will stop after the unbind
            } catch (e: ExecutionException) {
                // do nothing
            } catch (e: InterruptedException) {
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startStreaming() {
        mIsStreaming = true
        Thread {
            while (mIsStreaming) {
                val elapsedTime: Long = System.currentTimeMillis() - mLastTime
                if (elapsedTime > mUploadDelay && mUploadDelay != 0L) {   // Bound the image upload based on the user-defined frequency
                    var byteArray: ByteArray
                    val bmp = mCameraPreview!!.bitmap
                    val bmp2 = Bitmap.createScaledBitmap(bmp!!, mTargetWidth, mTargetHeight, false)
                    byteArray = ImageConverter.BitmaptoJPEG(bmp2)
                    mServer?.sendImage(byteArray)
                    mLastTime = System.currentTimeMillis()
                }
            }
        }.start()
    }

    private fun stopStreaming() {
        mIsStreaming = false
    }

    override fun onConnected(success: Boolean) {
        Log.d(TAG, "ServerResultCallback-onConnected: $success")
    }

    override fun displayResponse(result: String) {
        Log.d(TAG, "displayResponse in main act: $result")
        runOnUiThread {
            val curLen = viewBinding.textView.text.length
            if (curLen < 100) {
                chatFragment.addItem(ChatBubble(result, true))
                viewBinding.textView.append(" $result")
            } else {
                chatFragment.addItem(ChatBubble(result, true))
                viewBinding.textView.text = result
            }
        }
    }
}
