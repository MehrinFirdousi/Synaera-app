package com.example.synaera

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import androidx.viewpager2.widget.ViewPager2
import com.example.synaera.databinding.ActivityMainBinding
import com.google.common.util.concurrent.ListenableFuture
import okhttp3.OkHttpClient
import java.io.*
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


typealias ServListener = (serv: Int) -> Unit

class MainActivity : AppCompatActivity(), ServerResultCallback, IVideoFrameExtractor {
    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService

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

    private lateinit var circleView: CircleView
    private lateinit var recordButton: CardView
    private lateinit var handler: Handler
    private var runnable: Runnable? = null

    private var i = 0
    private val al: ArrayList<Int> = ArrayList()
    private val al2: ArrayList<Int> = ArrayList()
    private var currentAnimator: AnimatorSet? = null
    private var settingPopupVisibilityDuration = 0

    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()

    //    var filesFragment = FilesFragment()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        mServer = ServerClient.getInstance()
        mServer.init("user", "pass", "20.193.159.90", 5000)
//        mServer.init("user", "pass", "20.211.25.165", 5000)
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
//        client = OkHttpClient().newBuilder()
//            .connectTimeout(30, TimeUnit.SECONDS)
//            .readTimeout(30, TimeUnit.SECONDS)
//            .writeTimeout(30, TimeUnit.SECONDS)
//            .retryOnConnectionFailure(true)
//            .build()

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
        val selectVideoIntent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val dataUri = data?.data
                if (dataUri != null) {
                    val uriPathHelper = URIPathHelper()
                    val videoInputPath = uriPathHelper.getPath(this, dataUri).toString()
                    val videoInputFile = File(videoInputPath)
                    Log.d(TAG, "videoInputPath=$videoInputPath")
                    val frameExtractor = FrameExtractor(this)
                    executorService.execute {
                        try {

                            frameExtractor.extractFrames(videoInputFile.absolutePath)
                        } catch (exception: Exception) {
                            exception.printStackTrace()
                            this.runOnUiThread {
                                Log.d(TAG, "failed!!!!!")
                                Toast.makeText(
                                    this,
                                    "Failed to extract frames",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }


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
                stopAnimationOfSquare()
                handler.removeCallbacks(runnable!!)
                resetAnimation()
            }
            if (cameraFacing == CameraSelector.LENS_FACING_FRONT)
                cameraFacing = CameraSelector.LENS_FACING_BACK
            else
                cameraFacing = CameraSelector.LENS_FACING_FRONT
            startCameraPreview()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        // ------------------------------- animate record button -------------------------------
        // ------------------------------- animate record button -------------------------------
        // ------------------------------- animate record button -------------------------------
        // ------------------------------- animate record button -------------------------------
        // ------------------------------- animate record button -------------------------------

        circleView = viewBinding.recordCircle
        recordButton = viewBinding.recordButton

        circleView.setOnClickListener {
            viewBinding.openGalleryButton.visibility = View.VISIBLE;

            if (!translationOngoing) {
                if (mStreamFromCameraPreview) {
                    startStreaming()
                } else {
                    startCameraImageAnalysis()
                }
                startAnimationOfSquare()
                circleView.animateRadius(
                    circleView.getmMaxRadius(),
                    circleView.getmMinStroke()
                )
                handler.postDelayed(runnable!!, 80)
//                viewBinding.bottomNavBar.visibility = View.INVISIBLE
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
                stopAnimationOfSquare()
                handler.removeCallbacks(runnable!!)
                resetAnimation()
//                viewBinding.bottomNavBar.visibility = View.VISIBLE
                viewBinding.openGalleryButton.visibility = View.VISIBLE
                viewBinding.flipCameraButton.visibility = View.VISIBLE
                viewBinding.infoButton.visibility = View.VISIBLE
                viewBinding.settingsButton.visibility = View.VISIBLE
            }
            translationOngoing = !translationOngoing
        }

        resetAnimation()

        handler = Handler()
        runnable = Runnable {

            //to make smooth stroke width animation I increase and decrease value step by step
            val random: Int
            if (al.isNotEmpty()) {
                random = al[i++]
                if (i >= al.size) {
                    for (j in al.indices.reversed()) {
                        al2.add(al[j])
                    }
                    al.clear()
                    i = 0
                }
            } else {
                random = al2[i++]
                if (i >= al2.size) {
                    for (j in al2.indices.reversed()) {
                        al.add(al2[j])
                    }
                    al2.clear()
                    i = 0
                }
            }
            circleView.animateRadius(circleView.getmMaxRadius(), random.toFloat())
            handler.postDelayed(runnable!!, 130)
        }
    }
    override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()
        mServer.registerCallback(this)
        mServer.connect()
    }
    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
        if (mIsStreaming) stopStreaming()
//        mServer!!.unregisterCallback()
//        mServer!!.disconnect()
    }

    private fun resetAnimation() {
        i = 0
        al.clear()
        al2.clear()
        al.add(25)
        al.add(30)
        al.add(35)
        al.add(40)
        al.add(45)
//        al.add(50);
//        al.add(55);
//        al.add(60);
        circleView.endAnimation()
    }

    private fun dpToPx(valueInDp: Float): Int {
        val metrics = resources.displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics).toInt()
    }

    private fun startAnimationOfSquare() {
        settingPopupVisibilityDuration =
            resources.getInteger(android.R.integer.config_shortAnimTime)
        currentAnimator?.cancel()
        val finalBounds = Rect()
        val globalOffset = Point()
        circleView.getGlobalVisibleRect(finalBounds, globalOffset)
        recordButton.let {
            TransitionManager.beginDelayedTransition(
                it, TransitionSet()
                    .addTransition(ChangeBounds()).setDuration(settingPopupVisibilityDuration.toLong())
            )
        }
        val params = recordButton.layoutParams
        params.height = dpToPx(40F)
        params.width = dpToPx(40F)
        recordButton.layoutParams = params
        val set = AnimatorSet()
        set.play(ObjectAnimator.ofFloat(recordButton, "radius", dpToPx(8F).toFloat()))
        set.duration = settingPopupVisibilityDuration.toLong()
        set.interpolator = DecelerateInterpolator()
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                finishAnimation()
            }

            override fun onAnimationCancel(animation: Animator) {
                finishAnimation()
            }

            private fun finishAnimation() {
                currentAnimator = null
            }
        })
        set.start()
        currentAnimator = set
    }

    private fun stopAnimationOfSquare() {
        if (currentAnimator != null) {
            currentAnimator!!.cancel()
        }
        recordButton.let {
            TransitionManager.beginDelayedTransition(
                it, TransitionSet()
                    .addTransition(ChangeBounds()).setDuration(settingPopupVisibilityDuration.toLong())
            )
        }
        val params = recordButton.layoutParams
        params.width = dpToPx(80F)
        params.height = dpToPx(80F)
        recordButton.layoutParams = params
        val set1 = AnimatorSet()
        set1.play(
            ObjectAnimator.ofFloat(
                recordButton,
                "radius",
                dpToPx(40F).toFloat()
            )
        ) //radius = height/2 to make it round
        set1.duration = settingPopupVisibilityDuration.toLong()
        set1.interpolator = DecelerateInterpolator()
        set1.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                finishAnimation()
            }

            override fun onAnimationCancel(animation: Animator) {
                finishAnimation()
            }

            private fun finishAnimation() {
                currentAnimator = null
            }
        })
        set1.start()
        currentAnimator = set1
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
//        viewBinding.startCaptureButton.visibility = View.INVISIBLE;
        circleView.visibility = View.INVISIBLE
        recordButton.visibility = View.INVISIBLE
        viewBinding.openGalleryButton.visibility = View.INVISIBLE;
        viewBinding.flipCameraButton.visibility = View.INVISIBLE;
        viewBinding.infoButton.visibility = View.INVISIBLE;
        viewBinding.settingsButton.visibility = View.INVISIBLE;
        viewBinding.textView.visibility = View.INVISIBLE;
    }

    private fun enableCameraButtons() {
//        viewBinding.startCaptureButton.visibility = View.VISIBLE;
        circleView.visibility = View.VISIBLE;
        recordButton.visibility = View.VISIBLE;
        viewBinding.openGalleryButton.visibility = View.VISIBLE;
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
                    mServer?.sendImage(byteArray)
                    mLastTime = System.currentTimeMillis()
                }
                image.close()
            })
        val cameraSelector: CameraSelector = if (cameraFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
//        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
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

    fun fromBufferToBitmap(buffer: ByteBuffer, width: Int, height: Int): Bitmap? {
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        buffer.rewind()
        result.copyPixelsFromBuffer(buffer)
        val transformMatrix = Matrix()
        val outputBitmap = Bitmap.createBitmap(result, 0, 0, result.width, result.height, transformMatrix, false)
        outputBitmap.density = DisplayMetrics.DENSITY_DEFAULT
        return outputBitmap
    }
    override fun onCurrentFrameExtracted(currentFrame: Frame) {
        val startSavingTime = System.currentTimeMillis()
        // 1. Convert frame byte buffer to bitmap
        val imageBitmap = fromBufferToBitmap(currentFrame.byteBuffer, currentFrame.width, currentFrame.height)
//        val imageBitmap = fromBufferToBitmap(currentFrame.byteBuffer, 640, 480)
        Log.d(TAG, "len of current frame "+currentFrame.byteBuffer.position().toString())
        val byteArray = ImageConverter.BitmaptoJPEG(imageBitmap)
        val len = byteArray.size
        Log.d(TAG, "frame len is $len")
        mServer.sendImage(byteArray)
        mLastTime = System.currentTimeMillis()

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
        Log.d(TAG, "Save: $processedFrameCount frames in: $processedTimeMs ms.")
    }
}
