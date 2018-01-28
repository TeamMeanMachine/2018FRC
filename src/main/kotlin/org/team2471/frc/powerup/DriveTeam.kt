package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.GenericHID
import edu.wpi.first.wpilibj.XboxController
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.control.experimental.runWhen
import org.team2471.frc.lib.control.experimental.toggleWhen
import org.team2471.frc.lib.math.deadband
import org.team2471.frc.lib.math.squareWithSign

object Driver {
    private val controller = XboxController(0)

    val throttle: Double
        get() = -controller.getRawAxis(1)
                .deadband(0.2)
                .squareWithSign()

    val softTurn: Double
        get() = controller.getRawAxis(4)
                .deadband(0.2)

    val hardTurn: Double
        get() = -controller.getRawAxis(2) + controller.getRawAxis(3)

    init {
        Command("Experimental Drive", Drive) {
            periodic {
                Drive.drive(throttle, softTurn, hardTurn, true)
            }
        }.toggleWhen { controller.xButton }

        Command("Find Offsets", Drive) {
            SmartDashboard.putNumber("Drive Left Offset", 0.0)
            SmartDashboard.putNumber("Drive Right Offset", 0.0)
            periodic {
                Drive.driveRaw(SmartDashboard.getNumber("Drive Left Offset", 0.0),
                        SmartDashboard.getNumber("Drive Right Offset", 0.0))
            }
        }.toggleWhen { controller.yButton }
    }
}

object CoDriver {
    private val controller = XboxController(1)

    val updown: Double
        get() = -controller.getRawAxis(5)
                .deadband(.2) * 0.5

    val grab: Boolean
        get() = controller.aButtonPressed

    val leftIntake: Double
        get() = controller.getTriggerAxis(GenericHID.Hand.kLeft)

    val rightIntake: Double
        get() = controller.getTriggerAxis(GenericHID.Hand.kRight)

    val invertIntake: Boolean
        get() = controller.bButton

    val wristPivot: Double
        get() = controller.getRawAxis(5)
                .deadband(.2)


    init {
        println("Initialized")
        Intake.toggleClampCommand.runWhen { controller.aButton }
        Command("Arm Preset 0", Arm) { Arm.moveToAngle(0.0) }.runWhen { controller.pov == 0 }
        Command("Arm Preset 45", Arm) { Arm.moveToAngle(45.0) }.runWhen { controller.pov == 90 }
        Command("Arm Preset 90", Arm) { Arm.moveToAngle(90.0) }.runWhen { controller.pov == 180 }
    }
}
