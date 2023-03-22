package com.example.synaera

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.viewpager2.widget.ViewPager2
import com.example.synaera.databinding.ActivityMainBinding
import com.google.common.util.concurrent.ListenableFuture
import java.io.*
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), ServerResultCallback, IVideoFrameExtractor {
    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService

//    private var url : String = "http://192.168.1.16:5000/sendImg"
//    private var url : String = "https://42bd-2001-8f8-1623-131a-c997-5075-626d-f3eb.eu.ngrok.io/sendImg"
//    private var url : String = "http://synaera-api.centralindia.cloudapp.azure.com:5000/sendImg"

    private var translationOngoing : Boolean = false
    private var cameraFacing : Int = CameraSelector.LENS_FACING_FRONT
    private var chatList = ArrayList<ChatBubble>()
    private var videoList = ArrayList<VideoItem>()
    private lateinit var chatFragment: ChatFragment
    private lateinit var filesFragment: FilesFragment

    private lateinit var mServer: ServerClient
    private lateinit var mCameraPreview: PreviewView
//    private lateinit var mCameraPreview2: PreviewView

    // Camera Use-Cases
    private var mPreview : Preview? = null
//    private var mPreview2 : Preview? = null
    private var mImageAnalysis: ImageAnalysis? = null

    private val mTargetWidth = 640
    private val mTargetHeight = 480

    private var mLastTime: Long = 0
    private val mUploadDelay: Long = 100

    private var mIsStreaming = false

    private val mStreamFromCameraPreview = true

    private lateinit var circleView: CircleView
    private lateinit var recordButton: CardView
    private lateinit var handler: Handler
    private var runnable: Runnable? = null

    private var recordAnimation: ButtonAnimator = ButtonAnimator()
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()

    private lateinit var videoThumbnail: Bitmap
    private var videoFrames = ArrayList<ByteArray>()
    lateinit var selectVideoIntent : ActivityResultLauncher<Intent>
    private var transcriptGenerated : Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        mServer = ServerClient.getInstance()
        mServer.init("user", "pass", "20.193.159.90", 5000)
//        mServer.init("user", "pass", "20.211.25.165", 5000)
        mServer.connect()

//        mCameraPreview2 = viewBinding.viewFinder2
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

        /** list for the videos*/
//        videoList.add(VideoItem("Video1", "Processing...", getDummyBitmap(100,100,123) ,"123"))
//        videoList.add(VideoItem("Video2", "View Transcript", getDummyBitmap(120,120,50) ,"123"))

        chatFragment = ChatFragment.newInstance(chatList)
        filesFragment = FilesFragment.newInstance(videoList)

        // Set up HTTP client
//        client = OkHttpClient().newBuilder()
//            .connectTimeout(30, TimeUnit.SECONDS)
//            .readTimeout(30, TimeUnit.SECONDS)
//            .writeTimeout(30, TimeUnit.SECONDS)
//            .retryOnConnectionFailure(true)
//            .build()

        // Request camera permissions
        if (allPermissionsGranted()) {
//            startCameraPreview2()
            startCameraPreview()
//            setThumbnailRecentVideo()
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
        selectVideoIntent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val dataUri = data?.data
                if (dataUri != null) {
                    val uriPathHelper = URIPathHelper()
                    val videoInputPath = uriPathHelper.getPath(this, dataUri).toString()
                    val videoInputFile = File(videoInputPath)
//                    setThumbnailRecentVideo()
                    Log.d(TAG, "videoInputPath=$videoInputPath")
                    val frameExtractor = FrameExtractor(this)
                    executorService.execute {
                        try {
                            frameExtractor.extractFrames(videoInputFile.absolutePath)
//                            this.runOnUiThread {
//                                filesFragment.changeStatus("Extraction complete")
//                            }
                        } catch (exception: Exception) {
                            exception.printStackTrace()
                            Log.d(TAG, "Failed!!!")
                            this.runOnUiThread {
                                Toast.makeText(
                                    this,
                                    "Failed to extract frames",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    viewBinding.bottomNavBar.selectedItemId = R.id.gallery_menu_id // switches to gallery fragment

                } else {
                    Toast.makeText(this, "Video input error!", Toast.LENGTH_LONG).show()
                }

            }
            //do whatever with the result, its the URI
//            var videoBytes = convertVideoToBytes(uri)

//            val frames = getFrames(uri)
//            for (frame in frames) {
//                val byteArray = ImageConverter.BitmaptoJPEG(frame)
//                val len = byteArray.size
//                Log.d(TAG, "frame len is $len")
//                mServer.sendImage(byteArray)
//                mLastTime = System.currentTimeMillis()
//            }

        }

        // Set up the listeners for record, flip camera and open gallery buttons
        viewBinding.openGalleryButton.setOnClickListener {
//            selectVideoIntent.launch("video/*")

            val intent = Intent()
            intent.type = "video/*"
            intent.action = Intent.ACTION_PICK
            selectVideoIntent.launch(intent)
        }

        viewBinding.flipCameraButton.setOnClickListener {
            if (translationOngoing) {
                if (mStreamFromCameraPreview) {
                    stopStreaming()
                } else {
                    stopCameraImageAnalysis()
                }
                circleView.animateRadius(
                    circleView.getmMinRadius(),
                    circleView.getmMinStroke()
                )
                recordAnimation.stopAnimationOfSquare(resources, recordButton)
                handler.removeCallbacks(runnable!!)
                recordAnimation.resetAnimation(circleView)
            }
            if (cameraFacing == CameraSelector.LENS_FACING_FRONT)
                cameraFacing = CameraSelector.LENS_FACING_BACK
            else
                cameraFacing = CameraSelector.LENS_FACING_FRONT
//            startCameraPreview2()
            startCameraPreview()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        circleView = viewBinding.recordCircle
        recordButton = viewBinding.recordButton

        circleView.setOnClickListener {
            viewBinding.openGalleryButton.visibility = View.VISIBLE

            if (!translationOngoing) {
                if (mStreamFromCameraPreview) {
                    startStreaming()
                } else {
                    startCameraImageAnalysis()
                }
                recordAnimation.startAnimationOfSquare(resources, circleView, recordButton)
                circleView.animateRadius(
                    circleView.getmMaxRadius(),
                    circleView.getmMinStroke()
                )
                handler.postDelayed(runnable!!, 80)
                slideView(viewBinding.bottomNavBar, viewBinding.bottomNavBar.layoutParams.height, 1)
                viewBinding.openGalleryButton.visibility = View.INVISIBLE
                viewBinding.flipCameraButton.visibility = View.INVISIBLE
                viewBinding.infoButton.visibility = View.INVISIBLE
                viewBinding.settingsButton.visibility = View.INVISIBLE
            }
            else {
                if (mStreamFromCameraPreview) {
                    stopStreaming()
                } else {
                    stopCameraImageAnalysis()
                }
                circleView.animateRadius(
                    circleView.getmMinRadius(),
                    circleView.getmMinStroke()
                )
                recordAnimation.stopAnimationOfSquare(resources, recordButton)
                handler.removeCallbacks(runnable!!)
                recordAnimation.resetAnimation(circleView)
                slideView(viewBinding.bottomNavBar, 1, dpToPx(55))
                viewBinding.openGalleryButton.visibility = View.VISIBLE
                viewBinding.flipCameraButton.visibility = View.VISIBLE
                viewBinding.infoButton.visibility = View.VISIBLE
                viewBinding.settingsButton.visibility = View.VISIBLE
            }
            translationOngoing = !translationOngoing
        }

        recordAnimation.resetAnimation(circleView)


        handler = Handler()
        runnable = Runnable {
            recordAnimation.animateStroke(circleView)
            handler.postDelayed(runnable!!, 130)
        }
    }

    override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()
        mServer.registerCallback(this)
        mServer.connect()
//        setThumbnailRecentVideo()
    }
    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
        if (mIsStreaming) stopStreaming()
//        mServer!!.unregisterCallback()
//        mServer!!.disconnect()
    }

    private fun slideView(view: View, currentHeight: Int, newHeight: Int) {
        val slideAnimator = ValueAnimator.ofInt(currentHeight, newHeight).setDuration(400)
        slideAnimator.addUpdateListener { animation1 ->
            val value = animation1.animatedValue as Int
            view.layoutParams.height = value
            view.requestLayout()
        }

        val animationSet = AnimatorSet()
        animationSet.interpolator = AccelerateDecelerateInterpolator()
        animationSet.play(slideAnimator)
        animationSet.start()
    }

    fun dpToPx(dp: Int): Int {
        val density: Float = resources.displayMetrics.density
        return (dp.toFloat() * density).roundToInt()
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
            val uriStr = uri.toString()
            Log.d(TAG, "uri = $uriStr")
            retriever.setDataSource(this, uri)
        }

        val timeInterval = 150L // extract frame every 1 second
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

    private fun setThumbnailRecentVideo() {
        /*
        val projection = arrayOf(MediaStore.Video.Media._ID)

        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val path = URIPathHelper().getPath(this, uri).toString()
        Log.d(TAG, "uripath is $path")
        val cursor = this.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            "1000000063",
            null,
            "${MediaStore.Video.Media.DATE_TAKEN} DESC"
        )

        if (cursor != null && cursor.moveToFirst()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
            val thumbnail = MediaStore.Video.Thumbnails.getThumbnail(
                this.contentResolver,
                id,
                MediaStore.Video.Thumbnails.MINI_KIND,
                null
            )
            cursor.close()
            viewBinding.openGalleryButton.setImageBitmap(thumbnail)
            Log.d(TAG, "set new image!!!")
            // use the thumbnail here
        }
        else {
            Log.d(TAG, "could not set thumbnail :(")
            if (cursor == null)
                Log.d(TAG, "could not set thumbnail, cursor is null")
            else if (!cursor.moveToFirst())
                Log.d(TAG, "could not set thumbnail, cursor cannot move to first")
        }*/

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
            when (it.itemId) {
                R.id.home_menu_id -> {
                    viewBinding.viewPager.setCurrentItem(0, false)
//                    viewBinding.viewPager.currentItem = 0
                    disableCameraButtons()
                }
                R.id.gallery_menu_id -> {
//                    supportFragmentManager.beginTransaction().replace(R.id.container, filesFragment).commit()
                    viewBinding.viewPager.setCurrentItem(1, false)
//                    viewBinding.viewPager.currentItem = 1
                    disableCameraButtons()
                }
                R.id.camera_menu_id -> {

                    viewBinding.viewPager.setCurrentItem(2, false)
//                    viewBinding.viewPager.currentItem = 2
                    enableCameraButtons()
                }
                R.id.chat_menu_id -> {
                    viewBinding.viewPager.setCurrentItem(3, false)
//                    viewBinding.viewPager.currentItem = 3
                    disableCameraButtons()
                }
                R.id.profile_menu_id -> {
                    viewBinding.viewPager.setCurrentItem(4, false)
//                    viewBinding.viewPager.currentItem = 4
                    disableCameraButtons()
                }
                else -> viewBinding.viewPager.currentItem = 2
//                else -> viewBinding.viewPager.setCurrentItem(2, false)
            }
            return@setOnItemSelectedListener true
        }
    }

    private fun disableCameraButtons() {
        circleView.visibility = View.INVISIBLE
        recordButton.visibility = View.INVISIBLE
        viewBinding.openGalleryButton.visibility = View.INVISIBLE
        viewBinding.flipCameraButton.visibility = View.INVISIBLE
        viewBinding.infoButton.visibility = View.INVISIBLE
        viewBinding.settingsButton.visibility = View.INVISIBLE
        viewBinding.textView.visibility = View.INVISIBLE
    }

    private fun enableCameraButtons() {
        circleView.visibility = View.VISIBLE
        recordButton.visibility = View.VISIBLE
        viewBinding.openGalleryButton.visibility = View.VISIBLE
        viewBinding.flipCameraButton.visibility = View.VISIBLE
        viewBinding.infoButton.visibility = View.VISIBLE
        viewBinding.settingsButton.visibility = View.VISIBLE
        viewBinding.textView.visibility = View.VISIBLE
    }

    private fun setViewPagerAdapter() {
        val curItem = viewBinding.viewPager.currentItem
        Log.d(TAG, "current item in viewpager = $curItem")
        viewBinding.viewPager.adapter = ViewPagerAdapter(this, chatFragment, filesFragment)
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
//                startCameraPreview2()
                startCameraPreview()
//                setThumbnailRecentVideo()
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

//    private fun startCameraPreview2() {
//        Log.d(TAG, "startCameraPreview")
//        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
//            ProcessCameraProvider.getInstance(this)
//        cameraProviderFuture.addListener({
//            try {
//                val cameraProvider = cameraProviderFuture.get()
//                bindPreview2(cameraProvider)
//            } catch (e: ExecutionException) {
//                // do nothing
//            } catch (_: InterruptedException) {
//            }
//        }, ContextCompat.getMainExecutor(this))
//    }

    private fun startCameraImageAnalysis() {
        Log.d(TAG, "startCameraPreview")
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> = ProcessCameraProvider.getInstance(this)
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

        mPreview!!.setSurfaceProvider(mCameraPreview.createSurfaceProvider())
        val cameraSelector: CameraSelector = if (cameraFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        cameraProvider.unbindAll()
//        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, mPreview)
    }

//    private fun bindPreview2(cameraProvider: ProcessCameraProvider) {
//        mPreview2 = Preview.Builder().build()
//        // Preview use-case will render a preview image on the screen as defined by the PreviewView
//        // element on the main's layout activity. The resolution of the layout is relative to the
//        // screen size and defined in dp, which means the final resolution in pixels will be decided
//        // at run-time when the layout is inflated to the device screen. But will always be proportional
//        // to the resolution defined on the layout.
//
//        mPreview2!!.setSurfaceProvider(mCameraPreview2.createSurfaceProvider())
//        val cameraSelector: CameraSelector = if (cameraFacing == CameraSelector.LENS_FACING_FRONT) {
//            CameraSelector.DEFAULT_FRONT_CAMERA
//        } else {
//            CameraSelector.DEFAULT_BACK_CAMERA
//        }
//        cameraProvider.unbindAll()
////        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
//        cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, mPreview2)
//    }
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
                    // This its a better camera stream but the conversion might create artifacts with
                    // some cameras. Needs more investigation
                    val byteArray: ByteArray = ImageConverter.YUV_420_800toJPEG(image)
                    mServer.sendImage(byteArray)
                    mLastTime = System.currentTimeMillis()
                }
                image.close()
            })
        val cameraSelector: CameraSelector = if (cameraFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        cameraProvider.bindToLifecycle((this as LifecycleOwner), cameraSelector, mImageAnalysis)
    }
    private fun stopCameraImageAnalysis() {
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbind(mImageAnalysis) // The stream will stop after the unbind
            } catch (e: ExecutionException) {
                // do nothing
            } catch (e: InterruptedException) {
            }
        }, ContextCompat.getMainExecutor(this))
        mServer.getPrediction()
    }

    private fun startStreaming() {
        mIsStreaming = true
        Thread {
            while (mIsStreaming) {
                val elapsedTime: Long = System.currentTimeMillis() - mLastTime
                if (elapsedTime > mUploadDelay && mUploadDelay != 0L) {   // Bound the image upload based on the user-defined frequency
                    var byteArray: ByteArray
                    val bmp = mCameraPreview.bitmap
                    val bmp2 = Bitmap.createScaledBitmap(bmp!!, mTargetWidth, mTargetHeight, false)
                    byteArray = ImageConverter.BitmaptoJPEG(bmp2)
                    mServer.sendImage(byteArray)
                    mLastTime = System.currentTimeMillis()
                }
            }
        }.start()
    }

    private fun stopStreaming() {
        mIsStreaming = false
        mServer.getPrediction()
    }

    override fun onConnected(success: Boolean) {
        Log.d(TAG, "ServerResultCallback-onConnected: $success")
    }

    override fun displayResponse(result: String) {
        Log.d(TAG, "displayResponse in main act: $result")
        runOnUiThread {
            val curLen = viewBinding.textView.text.length
            if (curLen < 40) {
                chatFragment.addItem(ChatBubble(result, true))
                viewBinding.textView.append(" $result")
            } else {
                chatFragment.addItem(ChatBubble(result, true))
                viewBinding.textView.text = result
            }
        }
    }

    override fun addNewTranscript(result: String?) {
        if (result != null) {
            Log.d(TAG, "result is $result")
            if (result.isNotEmpty() || result.compareTo("") != 0)
                transcriptGenerated = true
        }

    }

    private fun fromBufferToBitmap(buffer: ByteBuffer, width: Int, height: Int): Bitmap? {
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        buffer.rewind()
        result.copyPixelsFromBuffer(buffer)
        val transformMatrix = Matrix()
        val outputBitmap = Bitmap.createBitmap(result, 0, 0, result.width, result.height, transformMatrix, false)
        outputBitmap.density = DisplayMetrics.DENSITY_DEFAULT
        return outputBitmap
    }
    override fun onCurrentFrameExtracted(currentFrame: Frame, decodeCount: Int) {
//        Thread {
            // 1. Convert frame byte buffer to bitmap
        val imageBitmap = fromBufferToBitmap(currentFrame.byteBuffer, currentFrame.width, currentFrame.height)
        val byteArray = ImageConverter.BitmaptoJPEG(imageBitmap)

        videoFrames.add(byteArray)
//        mServer.sendVideoFrame(byteArray)
//        mLastTime = System.currentTimeMillis()
        if (decodeCount == 0) {
            videoThumbnail = imageBitmap!!
            this.runOnUiThread {
                filesFragment.addItem(VideoItem("Video3", "Processing...", videoThumbnail, "test"))
                viewBinding.openGalleryButton.setImageBitmap(videoThumbnail)
            }
        }

//        }.start()

        /*// 2. Get the frame file in app external file directory
        val allFrameFileFolder = File(this.getExternalFilesDir(null), UUID.randomUUID().toString())
        if (!allFrameFileFolder.isDirectory) {
            allFrameFileFolder.mkdirs()
        }
        val frameFile = File(allFrameFileFolder, "frame_num_${currentFrame.timestamp.toString().padStart(10, '0')}.jpeg")

        // 3. Save current frame to storage
        imageBitmap?.let {
            val savedFile = Utils.saveImageToFile(it, frameFile)
            savedFile?.let {
                imagePaths.add(savedFile.toUri())
                titles.add("${currentFrame.position} (${currentFrame.timestamp})")
            }
        }

        totalSavingTimeMS += System.currentTimeMillis() - startSavingTime

        this.runOnUiThread {
            infoTextView.text = "Extract ${currentFrame.position} frames"
        }*/
    }

    override fun onAllFrameExtracted(processedFrameCount: Int, processedTimeMs: Long) {
        this.runOnUiThread {
            filesFragment.changeStatus("Uploading...")
        }
        mIsStreaming = true
        Thread {
            var frameNo = 0
            while (mIsStreaming && frameNo < processedFrameCount) {
                val elapsedTime: Long = System.currentTimeMillis() - mLastTime
                if (elapsedTime > mUploadDelay && mUploadDelay != 0L) {
                    mServer.sendVideoFrame(videoFrames[frameNo++], processedFrameCount)
                    mLastTime = System.currentTimeMillis()
                    Log.d(TAG, "sending frame $frameNo")
                }
            }
            println("suspending execution")
            Thread.sleep(15000)
            println("resuming execution")

            mServer.startTranscriptProcessing()
            println("suspending execution")
            Thread.sleep(30000)
            println("resuming execution")

            mServer.checkTranscript()
            mLastTime = System.currentTimeMillis()
            while (!transcriptGenerated) {
                val elapsedTime: Long = System.currentTimeMillis() - mLastTime
                if (elapsedTime > 100) {
                    mServer.checkTranscript()
                    mLastTime = System.currentTimeMillis()
                    Log.d(TAG, "Checking for transcript...")
                }
            }
//            mIsStreaming = false
            this.runOnUiThread {
                filesFragment.changeStatus("View transcript")
            }
            videoFrames.clear()
            transcriptGenerated = false
        }.start()
        Log.d(TAG, "Save: $processedFrameCount frames in: $processedTimeMs ms.")
    }
}
