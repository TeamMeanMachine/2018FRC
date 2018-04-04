package org.team2471.powerupvision

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.res.Configuration
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.Gravity
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import io.apptik.widget.MultiSlider
import org.jetbrains.anko.powerManager
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat

class MainActivity : Activity(), CameraBridgeViewBase.CvCameraViewListener2 {


    private var openCvCameraView: CameraBridgeViewBase? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val TAG = "Android Vision"
    @SuppressLint("WakelockTimeout")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.camera_layout)

        val powerManqer = applicationContext.powerManager
        wakeLock = powerManqer.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG)

        openCvCameraView = findViewById(R.id.CameraView)
        openCvCameraView?.visibility = SurfaceView.VISIBLE
        openCvCameraView?.setCvCameraViewListener(this)
        openCvCameraView?.setMaxFrameSize(320, 240)
    }


    override fun onResume() {
        super.onResume()
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, loaderCallback)
    }


    override fun onPause() {
        super.onPause()
        openCvCameraView?.disableView()
    }

    override fun onDestroy() {
        super.onDestroy()
        openCvCameraView?.disableView()
    }

    override fun onCameraViewStarted(width: Int, height: Int) = Unit

    override fun onCameraViewStopped() = Unit

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
//        val image = VisionProcessing.processImage(inputFrame.rgba())
        val image = inputFrame.rgba()
        wakeLock?.acquire(2000)
        CameraStream.updateImage(image)
        return image
    }

    fun openBottomSheet(v: View) {
        val view = layoutInflater.inflate(R.layout.image_preferences, null)
        val container = view.findViewById<LinearLayout>(R.id.popup_window)
        container.background.alpha = 20

        val bottomSheetDialog = Dialog(this@MainActivity, R.style.MaterialDialogSheet)
        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.setCancelable(true)
        bottomSheetDialog.window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        bottomSheetDialog.window.setGravity(Gravity.BOTTOM)
        bottomSheetDialog.show()

        val hueSlider = view.findViewById<MultiSlider>(R.id.hueSlider)
        hueSlider.min = 0
        hueSlider.max = 255
        hueSlider.setOnThumbValueChangeListener { _, _, thumb, value ->
            if (thumb == 0) {
                ImagePreferences.hueMin = value
                Log.i("Image Preferences", "Hue Min: $value")
            } else if (thumb == 1) {
                ImagePreferences.hueMax = value
                Log.i("Image Preferences", "Hue Max: $value")
            }
        }
        hueSlider.getThumb(0).value = ImagePreferences.hueMin
        hueSlider.getThumb(1).value = ImagePreferences.hueMax

        val satSlider = view.findViewById<MultiSlider>(R.id.satSlider)
        satSlider.min = 0
        satSlider.max = 255
        satSlider.setOnThumbValueChangeListener { _, _, thumb, value ->
            if (thumb == 0) {
                ImagePreferences.satMin = value
                Log.i("Image Preferences", "Sat Min: $value")
            } else if (thumb == 1) {
                ImagePreferences.satMax = value
                Log.i("Image Preferences", "Sat Max: $value")
            }
        }
        satSlider.getThumb(0).value = ImagePreferences.satMin
        satSlider.getThumb(1).value = ImagePreferences.satMax

        val valSlider = view.findViewById<MultiSlider>(R.id.valSlider)
        valSlider.min = 0
        valSlider.max = 255
        valSlider.setOnThumbValueChangeListener { _, _, thumb, value ->
            if (thumb == 0) {
                ImagePreferences.valMin = value
                Log.i("Image Preferences", "Val Min: $value")
            } else if (thumb == 1) {
                ImagePreferences.valMax = value
                Log.i("Image Preferences", "Val Max: $value")
            }
        }
        valSlider.getThumb(0).value = ImagePreferences.valMin
        valSlider.getThumb(1).value = ImagePreferences.valMax

        val exposureSlider = view.findViewById<MultiSlider>(R.id.exposureSlider)
        exposureSlider.min = -12
        exposureSlider.max = 12
        exposureSlider.setOnThumbValueChangeListener { _, _, _, value ->
            ImagePreferences.exposure = value
            Log.i("Image Preferences", "Exposure: $value")
        }
        exposureSlider.getThumb(0).value = ImagePreferences.exposure

        val viewModeSpinner = view.findViewById<Spinner>(R.id.viewModeSpinner)
        val adapter = ArrayAdapter.createFromResource(this, R.array.visionModes,
                android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        viewModeSpinner.adapter = adapter
        viewModeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                ImagePreferences.displayMode = when(parent.getItemAtPosition(position) as String) {
                    "Thresholded" -> VisionProcessing.DisplayMode.THRESH
                    "Thresholded (Processed)" -> VisionProcessing.DisplayMode.THRESH_DEBUG
                    else -> VisionProcessing.DisplayMode.RAW
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                ImagePreferences.displayMode =  VisionProcessing.DisplayMode.RAW
            }
        }
    }

    private val loaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                Log.i("OpenCV Loader", "OpenCV loaded successfully")
                openCvCameraView?.enableView()
                CameraStream
            } else {
                super.onManagerConnected(status)
            }
        }
    }
}

