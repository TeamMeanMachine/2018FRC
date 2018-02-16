package org.team2471.frc.powerup.commands

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.control.experimental.suspendUntil
import org.team2471.frc.powerup.CoDriver
import org.team2471.frc.powerup.Driver
import org.team2471.frc.powerup.subsystems.Carriage

val zero = Command("Carriage Zero", Carriage) {
    try {
        Carriage.Arm.setpoint = 45.0
        periodic {
            Carriage.Lifter.heightRawSpeed = CoDriver.leftStickUpDown * 0.4
            println("Height: ${Carriage.Lifter.height}")
        }
    } finally {
        Carriage.Lifter.zero()
    }
}

val goToScaleLowPreset = Command("Scale Low Preset", Carriage) {
    Carriage.animateToPose(Carriage.Pose.SCALE_LOW.inches, Carriage.Pose.SCALE_LOW.armAngle)
}

val goToScaleMediumPreset = Command("Scale Medium Preset", Carriage) {
    Carriage.animateToPose(Carriage.Pose.SCALE_MED.inches, Carriage.Pose.SCALE_MED.armAngle)
}

val goToScaleHighPreset = Command("Scale High Preset", Carriage) {
    Carriage.animateToPose(Carriage.Pose.SCALE_HIGH.inches, Carriage.Pose.SCALE_HIGH.armAngle)
}

val returnToIntakePosition = Command("Return to Intake Position", Carriage) {
    launch(coroutineContext) {
        try {
            Carriage.Arm.clamp = false
            delay(500)
        } finally {
            Carriage.Arm.clamp = true
        }
    }
    Carriage.animateToPose(Carriage.Pose.INTAKE.inches, Carriage.Pose.INTAKE.armAngle)
}

val driverIntake = Command("Intake", Carriage) {
    try {
        Carriage.Arm.clamp = false
        Carriage.Arm.intake = 0.6
        suspendUntil { Carriage.Arm.hasCube }
        Carriage.Arm.clamp = true
        delay(300)
    } finally {
        Carriage.Arm.clamp = true
        Carriage.Arm.intake = 0.0
        launch {
            Driver.rumble = 1.0
            CoDriver.rumble = 1.0
            delay(700)
            Driver.rumble = 0.0
            CoDriver.rumble = 0.0
        }
    }
}

val driverSpit = Command("Driver Spit", Carriage) {
    try {
        Carriage.Arm.clamp = false
        Carriage.Arm.intake = -0.5
        delay(Long.MAX_VALUE)
    } finally {
        Carriage.Arm.clamp = true
        Carriage.Arm.intake = 0.0
    }
}
