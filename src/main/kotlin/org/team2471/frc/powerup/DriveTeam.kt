package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.GenericHID
import edu.wpi.first.wpilibj.XboxController
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.runWhen
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
}

object CoDriver {
    private val controller = XboxController(1)

    val updown: Double
        get() = -controller.getRawAxis(5)
                .deadband(.2) * 0.5

    val grab: Boolean
        get() = controller.aButtonPressed

    val spin: Boolean
        get() = controller.getTriggerAxis(GenericHID.Hand.kRight) > 0.15

    val wristPivot: Double
        get() = controller.getRawAxis(5)
                .deadband(.2)

    val spit: Boolean
        get() = controller.getTriggerAxis(GenericHID.Hand.kLeft) > 0.15

    init {
        Intake.toggleClampCommand.runWhen { controller.aButton }
        Command("Arm Preset 0", Arm){ Arm.moveToAngle(0.0) }.runWhen { controller.bButton }
        Command("Arm Preset 45", Arm){ Arm.moveToAngle(45.0) }.runWhen { controller.xButton }
        Command("Arm Preset 90", Arm){ Arm.moveToAngle(90.0) }.runWhen { controller.yButton }
    }
}
