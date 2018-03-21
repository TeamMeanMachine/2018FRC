package org.team2471.frc.powerup.carriage

import edu.wpi.first.wpilibj.Timer
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.powerup.CoDriver
import org.team2471.frc.powerup.RobotMap

const val MICRO_ADJUST_RATE = 18.0

val carriageDefaultCommand = Command("Carriage Default", Carriage) {
    var previousTime = Timer.getFPGATimestamp()
    var prevReleasing = false
    try {
        periodic {
            val releaseClamp = CoDriver.release
            Arm.isClamping = !releaseClamp
            val spit = CoDriver.spitSpeed

            Arm.intake = if (spit == 0.0 && Arm.hasCube) 0.2 else -spit

            val releasing = releaseClamp || spit != 0.0
            if (!releasing && prevReleasing && Arm.angle > 150.0 && Carriage.targetPose != Pose.SCALE_FRONT) returnToIntakePosition.launch()
            prevReleasing = releasing

            val leftStick = CoDriver.leftStickUpDown

            val currentTime = Timer.getFPGATimestamp()
            val deltaTime = currentTime - previousTime
            previousTime = currentTime


            if (Carriage.targetPose == Pose.SWITCH) {
                val switchOffsetDiff = (CoDriver.microAdjust * MICRO_ADJUST_RATE) * deltaTime
                switchOffset += switchOffsetDiff
                Lifter.curve.tailKey.value = Carriage.targetPose.lifterHeight + switchOffset
            }

            if (Carriage.targetPose.isScale) {
                val scaleOffsetDiff = (CoDriver.microAdjust * MICRO_ADJUST_RATE) * deltaTime
                scaleOffset += scaleOffsetDiff

                Lifter.curve.tailKey.value = Carriage.targetPose.lifterHeight + scaleOffset

                SmartDashboard.putNumber("Scale Offset Diff", scaleOffsetDiff)
                SmartDashboard.putNumber("Scale Offset", scaleOffset)
            }

            Carriage.adjustAnimationTime(deltaTime * leftStick)

            SmartDashboard.putNumberArray("Lifter Amperages", Lifter.amperages)
            Lifter.isLowGear = false
            Lifter.isBraking = false

            SmartDashboard.putNumber("Arm Amperage", RobotMap.pdp.getCurrent(RobotMap.Talons.ARM_MOTOR_1))
            SmartDashboard.putBoolean("Low Gear", Lifter.isLowGear)
            SmartDashboard.putBoolean("Braking", Lifter.isBraking)
        }
    } finally {
        Arm.isClamping = true
    }
}