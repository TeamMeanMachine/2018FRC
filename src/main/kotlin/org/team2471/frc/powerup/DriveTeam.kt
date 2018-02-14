package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.GenericHID
import edu.wpi.first.wpilibj.XboxController
import kotlinx.coroutines.experimental.delay
import org.team2471.frc.lib.control.experimental.*
import org.team2471.frc.lib.math.deadband
import org.team2471.frc.lib.math.squareWithSign
import org.team2471.frc.powerup.commands.zero
import org.team2471.frc.powerup.commands.softRelease
import org.team2471.frc.powerup.commands.spit
import org.team2471.frc.powerup.subsystems.Carriage
import org.team2471.frc.powerup.subsystems.Wings

object Driver {
    private val controller = XboxController(0)

    val throttle: Double
        get() = -controller.getY(GenericHID.Hand.kLeft)
                .deadband(0.2)
                .squareWithSign()

    val softTurn: Double
        get() = controller.getX(GenericHID.Hand.kRight)

    val hardTurn: Double
        get() = -controller.getTriggerAxis(GenericHID.Hand.kLeft) + controller.getTriggerAxis(GenericHID.Hand.kRight)


    init {

        Command("Intake", Carriage) {
            try {
                Carriage.Arm.clamp = false
                Carriage.Arm.intake = .75
                suspendUntil { Carriage.Arm.hasCube }
                Carriage.Arm.clamp = true
                delay(300)
            } finally {
                Carriage.Arm.clamp = true
                Carriage.Arm.intake = 0.0
            }
        }.runWhile { controller.getBumper(GenericHID.Hand.kRight) }
        Command("Driver Spit", Carriage) {
            try {
                Carriage.Arm.clamp = false
                Carriage.Arm.intake = -1.0
                delay(Long.MAX_VALUE)
            } finally {
                Carriage.Arm.clamp = true
                Carriage.Arm.intake = 0.0
            }
        }.runWhile { controller.getBumper(GenericHID.Hand.kLeft) }
    }

}


object CoDriver {
    private val controller = XboxController(1)
    val leftStickUpDown: Double
        get() = -controller.getY(GenericHID.Hand.kLeft)
                .deadband(.2)

    val rightStickUpDown: Double
        get() = -controller.getY(GenericHID.Hand.kRight)
                .deadband(.2)


    val spitSpeed: Double
        get() = controller.getTriggerAxis(GenericHID.Hand.kRight)

    val grab: Boolean
        get() = controller.aButtonPressed

//    val leftIntake: Double
//        get() = controller.getTriggerAxis(GenericHID.Hand.kLeft)
//
//    val rightIntake: Double
//        get() = controller.getTriggerAxis(GenericHID.Hand.kRight)

    val invertIntake: Boolean
        get() = controller.bButton

    val brake: Boolean
        get() = controller.getBumper(GenericHID.Hand.kRight)

    val shift: Boolean
        get() = controller.getBumper(GenericHID.Hand.kLeft)

    val wristPivot: Double
        get() = controller.getRawAxis(5)
                .deadband(.2)
    val climbGuide: Boolean
        get() = controller.xButtonPressed

    init {
        println("Initialized")
        Command("DeployClimbGuide", Wings) {
            Wings.climbingGuideDeployed = !Wings.climbingGuideDeployed
        }.runWhen { controller.xButton }

        spit.runWhile { controller.getTriggerAxis(GenericHID.Hand.kRight) > 0.1 }

        softRelease.runWhile { controller.getBumper(GenericHID.Hand.kRight) }

        zero.toggleWhen { controller.backButton }
    }
}
