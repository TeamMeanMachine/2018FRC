package org.team2471.frc.powerup

import com.team254.frc2017.vision.AdbBridge

object AndroidPhone {
    private val adb = AdbBridge()

    init {
        adb.start()
        setupAdb()
    }

    private fun setupAdb() {
        adb.portForward(2471, 2471)
    }
}