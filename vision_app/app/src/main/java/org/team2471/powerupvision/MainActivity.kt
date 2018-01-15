package org.team2471.powerupvision

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat

class MainActivity : Activity(), CameraBridgeViewBase.CvCameraViewListener2 {


    private var openCvCameraView : CameraBridgeViewBase? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("OpenCV Loader", "called onCreate")
        openCvCameraView = findViewById(R.id.CameraView)
        Log.i("ostrich", openCvCameraView?.toString()?: "null")
        super.onCreate(savedInstanceState)
        openCvCameraView?.visibility = SurfaceView.VISIBLE
        openCvCameraView?.setCvCameraViewListener(this)
        VisionProcessing(applicationContext)
        setContentView(R.layout.camera_layout)
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
        return inputFrame.rgba()
    }

    private val loaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            if (status==LoaderCallbackInterface.SUCCESS) {
                Log.i("OpenCV Loader", "OpenCV loaded successfully")
                openCvCameraView?.enableView()
            } else {
                super.onManagerConnected(status)
            }
        }
    }



}

