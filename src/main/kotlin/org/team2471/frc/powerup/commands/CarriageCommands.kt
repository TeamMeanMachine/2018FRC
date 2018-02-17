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
        Carriage.Arm.setpoint = 45.0
        periodic {
            Carriage.Lifter.heightRawSpeed = CoDriver.leftStickUpDown * 0.4
            println("Height: ${Carriage.Lifter.height}")
        }
    } finally {
        Carriage.Lifter.zero()
    }
}

private val scaleStackHeight get() = SmartDashboard.getNumber("Scale Stack Height", 0.0)

val goToSwitch = Command("Switch Preset", Carriage) {
    Carriage.animateToPose(Carriage.Pose.SWITCH.inches, Carriage.Pose.SWITCH.armAngle)
}

val goToScaleLowPreset = Command("Scale Low Preset", Carriage) {
    Carriage.animateToPose(Carriage.Pose.SCALE_LOW.inches + 11 * scaleStackHeight, Carriage.Pose.SCALE_LOW.armAngle)
}

val goToScaleMediumPreset = Command("Scale Medium Preset", Carriage) {
    Carriage.animateToPose(Carriage.Pose.SCALE_MED.inches + 11 * scaleStackHeight, Carriage.Pose.SCALE_MED.armAngle)
}

val goToScaleHighPreset = Command("Scale High Preset", Carriage) {
    Carriage.animateToPose(Carriage.Pose.SCALE_HIGH.inches + 11 * scaleStackHeight, Carriage.Pose.SCALE_HIGH.armAngle)
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
        suspendUntil { Carriage.Arm.hasCube || !Driver.intaking }
        Carriage.Arm.clamp = true

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
    } finally {
        Carriage.Arm.clamp = true
        Carriage.Arm.intake = 0.0
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

val incrementScaleStackHeight = Command("Increment Cube Stack Count") {
    SmartDashboard.putNumber("Scale Stack Height", min(scaleStackHeight + 1, 3.0))
}

val decrementScaleStackHeight = Command("Increment Cube Stack Count") {
    SmartDashboard.putNumber("Scale Stack Height", max(scaleStackHeight - 1, 0.0))
}
