package org.team2471.frc.powerup.carriage

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.parallel
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.control.experimental.suspendUntil
import org.team2471.frc.powerup.CoDriver
import org.team2471.frc.powerup.Driver
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard

val zero = Command("Carriage Zero", Carriage) {
    try {
        Arm.setpoint = 90.0
        Lifter.isBraking = false
        Lifter.isLowGear = false
        periodic {
            Lifter.heightRawSpeed = CoDriver.leftStickUpDown * 0.4
        }
    } finally {
        Carriage.setAnimation(Pose.STARTING_POSITION)
        Arm.hold()
        Lifter.stop()
        Lifter.zero()
    }
}

var scaleOffset = 0.0
    set(value) {
        field = value.coerceIn(0.0, 66.0) //33.0
    }

var switchOffset = 0.0
    set(value) {
        field = value.coerceIn(-12.0, 12.0)
    }

val goToSwitch = Command("Switch Preset", Carriage) {
    Carriage.animateToPose(Pose.SWITCH, heightOffset = switchOffset)
}

val goToScaleLowPreset = Command("Scale Low Preset", Carriage) {
    Carriage.animateToPose(Pose.SCALE_LOW, heightOffset = scaleOffset)
}

val goToScaleMediumPreset = Command("Scale Medium Preset", Carriage) {
    Carriage.animateToPose(Pose.SCALE_MED, heightOffset = scaleOffset)
}

val goToScaleHighPreset = Command("Scale High Preset", Carriage) {
    Carriage.animateToPose(Pose.SCALE_HIGH, heightOffset = scaleOffset)
}

val goToFrontScalePreset = Command("Scale Front Preset", Carriage) {
    Carriage.animateToPose(Pose.SCALE_FRONT, heightOffset = scaleOffset)
}

val goToIntakePreset = Command("Intake Preset", Carriage) {
    Carriage.animateToPose(Pose.INTAKE)
}

val returnToIntakePosition = Command("Return to Intake Position", Carriage) {
    parallel({
        try {
            Arm.isClamping = false
            delay(1000)
        } finally {
            Arm.isClamping = true
        }
    }, {
        Carriage.animateToPose(Pose.INTAKE)
    })
}

val driverIntake = Command("Intake", Carriage) {
    val rumbleJob = launch(coroutineContext) {
        try {
            periodic {
                val detectingCube = Arm.detectingCube
                Driver.rumble = if (detectingCube) 1.0 else 0.12
                CoDriver.rumble = if (detectingCube) 1.0 else 0.0
            }
        } finally {
            Driver.rumble = 0.0
            CoDriver.rumble = 0.0
        }
    }

    try {
        Arm.isClamping = false
        Arm.intakeSpeed = CarriageConstants.STANDARD_INTAKE_SPEED
        Driver.rumble = 0.12
        Carriage.animateToPose(Pose.INTAKE)
        var prevIntaking = Driver.intaking
        suspendUntil {
            val intaking = Driver.intaking
            val finished = Arm.hasCube || (intaking && !prevIntaking)
            prevIntaking = intaking
            finished
        }

        Arm.isClamping = true
        delay(800)
    } finally {
        rumbleJob.cancel()
        Arm.isClamping = true
        Arm.intakeSpeed = 0.0
        Driver.rumble = 0.0
    }
}

val driverSpit = Command("Driver Spit", Carriage) {
    try {
        Arm.intakeSpeed = -0.8
        delay(Long.MAX_VALUE)
    } finally {
        Arm.intakeSpeed = 0.0
    }
}

val incrementScaleStackHeight = Command("Increment Cube Stack Count") {
    scaleOffset += 11
    @Suppress("NON_EXHAUSTIVE_WHEN")
    when (Carriage.targetPose) {
        Pose.SCALE_LOW -> goToScaleLowPreset(coroutineContext)
        Pose.SCALE_MED -> goToScaleMediumPreset(coroutineContext)
        Pose.SCALE_HIGH -> goToScaleHighPreset(coroutineContext)
    }
}

val decrementScaleStackHeight = Command("Increment Cube Stack Count") {
    scaleOffset -= 11
    @Suppress("NON_EXHAUSTIVE_WHEN")
    when (Carriage.targetPose) {
        Pose.SCALE_LOW -> goToScaleLowPreset(coroutineContext)
        Pose.SCALE_MED -> goToScaleMediumPreset(coroutineContext)
        Pose.SCALE_HIGH -> goToScaleHighPreset(coroutineContext)
    }

}

val tuneArmPID = Command("Tune Arm Pid", Carriage) {
    periodic {
        val rightStick = CoDriver.microAdjust
        Arm.setpoint = rightStick * 45 + 90
    }
}

val toggleCubeSensor = Command("Toggle Cube Sensor") {
    //val entry = Arm.table.getEntry("Use Cube Sensor")
    val useIntake = SmartDashboard.getBoolean("Using Intake Sensor", true)
//    entry.setBoolean(!entry.getBoolean(true))
    SmartDashboard.putBoolean("Using Intake Sensor", !useIntake)

}
