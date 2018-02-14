package org.team2471.frc.powerup.commands

import kotlinx.coroutines.experimental.delay
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.powerup.CoDriver
import org.team2471.frc.powerup.Driver
import org.team2471.frc.powerup.subsystems.Carriage

val testLifter = Command("Test Lifter", Carriage) {
    try {
        Carriage.playAnimation(Carriage.Animation.INTAKE_TO_SCALE)
//        Carriage.playAnimation(Carriage.Animation.INITIAL_TEST)
    }
    finally {
    }
}

val spit = Command("Spit", Carriage) {
    try {
        periodic {
            Carriage.Arm.intake = -CoDriver.spitSpeed
        }
    } finally {
        Carriage.Arm.intake = 0.0
    }
}

val softRelease = Command("Soft Release", Carriage) {
    try {
        Carriage.Arm.clamp = false
        delay(Long.MAX_VALUE)
    } finally {
        Carriage.Arm.clamp = true
    }
}

val zero = Command("Carriage Zero", Carriage) {
    try {
        Carriage.Arm.setpoint = 45.0
        periodic {
            Carriage.heightRawSpeed = CoDriver.leftStickUpDown * 0.4
            println("Height: ${Carriage.height}")
        }
    } finally {
        Carriage.zero()
    }
}