package org.team2471.frc.powerup.carriage

import edu.wpi.first.wpilibj.Timer
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.control.experimental.suspendUntil
import org.team2471.frc.powerup.CoDriver
import org.team2471.frc.powerup.Driver

val zero = Command("Carriage Zero", Carriage) {
    try {
        Arm.setpoint = 100.0
        Lifter.isBraking = false
        Lifter.isLowGear = false
        periodic {
            Lifter.heightRawSpeed = CoDriver.leftStickUpDown * 0.4
        }
    } finally {
        Carriage.setAnimation(Pose.INTAKE)
        Arm.hold()
        Lifter.stop()
        Lifter.zero()
        Lifter.isBraking = true
    }
}

var scaleOffset = 0.0
    set(value) {
        field = value.coerceIn(0.0, 33.0)
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
    launch(coroutineContext) {
        try {
            Arm.isClamping = false
            delay(1000)
        } finally {
            Arm.isClamping = true
        }
    }
    Carriage.animateToPose(Pose.INTAKE)
}

val driverIntake = Command("Intake", Carriage) {
    try {
        Arm.isClamping = false
        Arm.intake = 0.55

        val timer = Timer()
        timer.start()
        var previousTime = 0.0
        periodic(condition = { !Carriage.isAnimationCompleted }) {
            val t = timer.get()
            Carriage.adjustAnimationTime(t - previousTime)
            previousTime = t
        }

        var prevIntaking = Driver.intaking
        suspendUntil {
            val intaking = Driver.intaking
            val finished = Arm.hasCube || (intaking && !prevIntaking)
            prevIntaking = intaking
            finished
        }
        Arm.intake = 0.8
        Arm.isClamping = true

        if (Arm.hasCube) {
            launch(coroutineContext) {
                Driver.rumble = 1.0
                CoDriver.rumble = 1.0
                try {
                    delay(700)
                } finally {
                    Driver.rumble = 0.0
                    CoDriver.rumble = 0.0
                }
            }
        }

        delay(800)
        if (Arm.hasCube) Carriage.animateToPose(Pose.CARRY)
    } finally {
        Arm.isClamping = true
        Arm.intake = 0.0
    }
}

val driverSpit = Command("Driver Spit", Carriage) {
    try {
        Arm.intake = -0.8
        delay(Long.MAX_VALUE)
    } finally {
        Arm.intake = 0.0
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
    val entry = Arm.table.getEntry("Use Cube Sensor")
    entry.setBoolean(!entry.getBoolean(true))

}
