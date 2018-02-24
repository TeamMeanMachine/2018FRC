package org.team2471.frc.powerup.commands

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.control.experimental.suspendUntil
import org.team2471.frc.powerup.CoDriver
import org.team2471.frc.powerup.Driver
import org.team2471.frc.powerup.subsystems.Carriage
import java.lang.Math.max
import java.lang.Math.min

val zero = Command("Carriage Zero", Carriage) {
    try {
        Carriage.Arm.setpoint = 100.0
        Carriage.Lifter.isBraking = false
        Carriage.Lifter.isLowGear = false
        periodic {
            Carriage.Lifter.heightRawSpeed = CoDriver.leftStickUpDown * 0.4
        }
    } finally {
        Carriage.Lifter.zero()
        Carriage.Lifter.isBraking = true
        Carriage.Lifter.isLowGear = true
    }
}

private val scaleStackHeight get() = SmartDashboard.getNumber("Scale Stack Height", 0.0)

val goToSwitch = Command("Switch Preset", Carriage) {
    Carriage.animateToPose(Carriage.Pose.SWITCH)
}

val goToScaleLowPreset = Command("Scale Low Preset", Carriage) {
    Carriage.animateToPose(Carriage.Pose.SCALE_LOW, heightOffset = scaleStackHeight * 11)
}

val goToScaleMediumPreset = Command("Scale Medium Preset", Carriage) {
    Carriage.animateToPose(Carriage.Pose.SCALE_MED, heightOffset = scaleStackHeight * 11)
}

val goToScaleHighPreset = Command("Scale High Preset", Carriage) {
    Carriage.animateToPose(Carriage.Pose.SCALE_HIGH, heightOffset = scaleStackHeight * 11)
}

val goToIntakePreset = Command("Intake Preset", Carriage) {
    Carriage.animateToPose(Carriage.Pose.INTAKE)
}

val returnToIntakePosition = Command("Return to Intake Position", Carriage) {
    launch(coroutineContext) {
        try {
            Carriage.Arm.isClamping = false
            delay(500)
        } finally {
            Carriage.Arm.isClamping = true
        }
    }
    Carriage.animateToPose(Carriage.Pose.INTAKE)
}

val driverIntake = Command("Intake", Carriage) {
    try {
        Carriage.Arm.isClamping = false
        Carriage.Arm.intake = 0.6
        suspendUntil { Carriage.Arm.hasCube || !Driver.intaking }
        Carriage.Arm.isClamping = true

        if (Carriage.Arm.hasCube) {
            launch {
                Driver.rumble = 1.0
                Driver.rumble = 1.0
                try {
                    delay(700)
                } finally {
                    Driver.rumble = 0.0
                    Driver.rumble = 0.0
                }
            }
        }

        delay(800)
        if (Carriage.Arm.hasCube) Carriage.animateToPose(Carriage.Pose.CARRY)
    } finally {
        Carriage.Arm.isClamping = true
        Carriage.Arm.intake = 0.0
    }
}

val driverSpit = Command("Driver Spit", Carriage) {
    try {
        Carriage.Arm.intake = -0.8
        delay(Long.MAX_VALUE)
    } finally {
        Carriage.Arm.intake = 0.0
    }
}

val incrementScaleStackHeight = Command("Increment Cube Stack Count") {
    SmartDashboard.putNumber("Scale Stack Height", min(scaleStackHeight + 1, 3.0))
    @Suppress("NON_EXHAUSTIVE_WHEN")
    when (Carriage.targetPose) {
        Carriage.Pose.SCALE_LOW -> goToScaleLowPreset(coroutineContext)
        Carriage.Pose.SCALE_MED -> goToScaleMediumPreset(coroutineContext)
        Carriage.Pose.SCALE_HIGH -> goToScaleHighPreset(coroutineContext)
    }
}

val decrementScaleStackHeight = Command("Increment Cube Stack Count") {
    SmartDashboard.putNumber("Scale Stack Height", max(scaleStackHeight - 1, 0.0))
    @Suppress("NON_EXHAUSTIVE_WHEN")
    when (Carriage.targetPose) {
        Carriage.Pose.SCALE_LOW -> goToScaleLowPreset(coroutineContext)
        Carriage.Pose.SCALE_MED -> goToScaleMediumPreset(coroutineContext)
        Carriage.Pose.SCALE_HIGH -> goToScaleHighPreset(coroutineContext)
    }
}
