package org.team2471.powerupvision

import android.app.Activity
import android.app.Dialog
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import io.apptik.widget.MultiSlider
import kotlinx.android.synthetic.main.image_preferences.*
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat

class MainActivity : Activity(), CameraBridgeViewBase.CvCameraViewListener2 {


    private var openCvCameraView: CameraBridgeViewBase? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("OpenCV Loader", "called onCreate")
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.camera_layout)

        openCvCameraView = findViewById(R.id.CameraView)

        openCvCameraView?.visibility = SurfaceView.VISIBLE
        openCvCameraView?.setCvCameraViewListener(this)
        openCvCameraView?.setMaxFrameSize(640, 480)

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

    override fun onCameraViewStarted(width: Int, height: Int) {
    }

    override fun onCameraViewStopped() {
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
        return VisionProcessing.processImage(inputFrame.rgba(), VisionProcessing.DisplayMode.RAW)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
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
        hueSlider.getThumb(0).value = ImagePreferences.hueMin
        hueSlider.getThumb(1).value = ImagePreferences.hueMax
        hueSlider.setOnThumbValueChangeListener { _, _, thumb, value ->
            if (thumb == 0) {
                ImagePreferences.hueMin = value
                Log.i("Image Preferences", "Hue Min: $value")
            } else if (thumb == 1) {
                ImagePreferences.hueMax = value
                Log.i("Image Preferences", "Hue Max: $value")
            }
        }

        val satSlider = view.findViewById<MultiSlider>(R.id.satSlider)
        satSlider.min = 0
        satSlider.max = 255
        satSlider.getThumb(0).value = ImagePreferences.satMin
        satSlider.getThumb(1).value = ImagePreferences.satMax
        satSlider.setOnThumbValueChangeListener { _, _, thumb, value ->
            if (thumb == 0) {
                ImagePreferences.satMin = value
                Log.i("Image Preferences", "Sat Min: $value")
            } else if (thumb == 1) {
                ImagePreferences.satMax = value
                Log.i("Image Preferences", "Sat Max: $value")
            }
        }

        val valSlider = view.findViewById<MultiSlider>(R.id.valSlider)
        valSlider.min = 0
        valSlider.max = 255
        valSlider.getThumb(0).value = ImagePreferences.valMin
        valSlider.getThumb(1).value = ImagePreferences.valMax
        valSlider.setOnThumbValueChangeListener { _, _, thumb, value ->
            if (thumb == 0) {
                ImagePreferences.valMin = value
                Log.i("Image Preferences", "Val Min: $value")
            } else if (thumb == 1) {
                ImagePreferences.valMax = value
                Log.i("Image Preferences", "Val Max: $value")
            }
        }
    }

    private val loaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                Log.i("OpenCV Loader", "OpenCV loaded successfully")
                openCvCameraView?.enableView()
            } else {
                super.onManagerConnected(status)
            }
        }
    }


}

