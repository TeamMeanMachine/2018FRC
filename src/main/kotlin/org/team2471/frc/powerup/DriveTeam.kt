package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.GenericHID
import edu.wpi.first.wpilibj.XboxController
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.runWhen
import org.team2471.frc.lib.math.deadband
import org.team2471.frc.lib.math.squareWithSign
import org.team2471.frc.powerup.subsystems.Carriage

object Driver {
    private val controller = XboxController(0)

    val throttle: Double
        get() = -controller.getRawAxis(1)
                .deadband(0.2)
                .squareWithSign()

    val softTurn: Double
        get() = controller.getRawAxis(4)

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

    val leftIntake: Double
        get() = controller.getTriggerAxis(GenericHID.Hand.kLeft)

    val rightIntake: Double
        get() = controller.getTriggerAxis(GenericHID.Hand.kRight)

    val invertIntake: Boolean
        get() = controller.bButton

    val brake: Boolean
        get() = controller.startButton

    val wristPivot: Double
        get() = controller.getRawAxis(5)
                .deadband(.2)


    init {
        println("Initialized")
        Carriage.Arm.toggleClampCommand.runWhen { controller.aButton }
        Carriage.dropOffToScale.runWhen { controller.yButton }
        Carriage.intakeCubeCommand.runWhen { controller.xButton }
        Carriage.climb.runWhen { controller.yButtonPressed }
        Command("Arm Preset 0", Carriage.Arm) { Carriage.Arm.moveToAngle(0.0) }.runWhen { controller.pov == 0 }
        Command("Arm Preset 45", Carriage.Arm) { Carriage.Arm.moveToAngle(45.0) }.runWhen { controller.pov == 90 }
        Command("Arm Preset 90", Carriage.Arm) { Carriage.Arm.moveToAngle(90.0) }.runWhen { controller.pov == 180 }
    }
}
