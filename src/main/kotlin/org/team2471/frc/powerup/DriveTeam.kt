package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.XboxController
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
                .deadband(.2)

    val grab: Boolean
        get() = controller.aButtonPressed

    val wristPivot: Double
        get() = -controller.getRawAxis(5)
                .deadband(.2)
}
