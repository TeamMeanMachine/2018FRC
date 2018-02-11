package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.GenericHID
import edu.wpi.first.wpilibj.XboxController
import kotlinx.coroutines.experimental.delay
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.runWhen
import org.team2471.frc.lib.control.experimental.runWhile
import org.team2471.frc.lib.control.experimental.suspendUntil
import org.team2471.frc.lib.math.deadband
import org.team2471.frc.lib.math.squareWithSign
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
        Command("Spit", Carriage) {
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
    val updown: Double
        get() = -controller.getY(GenericHID.Hand.kLeft)
                .deadband(.2)

    val rightStickUpDown: Double
        get() = -controller.getY(GenericHID.Hand.kRight)
                .deadband(.2)

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

        Command("Position 1,", Carriage) {
            Carriage.moveToHeight(Carriage.Pose.IDLE.inches)
        }.runWhen { controller.aButton }

        Command("Position 2,", Carriage) {
            Carriage.moveToHeight(Carriage.Pose.SWITCH.inches)
        }.runWhen { controller.bButton }

        Command("Position 3,", Carriage) {
            Carriage.moveToHeight(Carriage.Pose.SCALE.inches)
        }.runWhen { controller.yButton }
    }
}

fun startDriveTeamLogger() {

}
