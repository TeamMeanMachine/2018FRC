package org.team2471.frc.powerup.vision

import edu.wpi.first.wpilibj.DriverStation

object ADB {
    private fun runCommand(command: String) {
        try {
            Runtime.getRuntime().exec("adb $command").waitFor()
        } catch (e: Exception) {
            DriverStation.reportError("ADB command $command failed to run.", true)
        }
    }

    fun start() = runCommand("start")

    fun stop() = runCommand("kill-server")

    fun restart() {
        stop()
        start()
    }

    fun reversePortForward(remotePort: Int, localPort: Int) = runCommand("reverse tcp: $remotePort tcp: $localPort")
}
