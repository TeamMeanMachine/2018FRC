package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.GenericHID
import edu.wpi.first.wpilibj.RobotState
import edu.wpi.first.wpilibj.XboxController
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import org.team2471.frc.lib.control.experimental.runWhen
import org.team2471.frc.lib.control.experimental.runWhile
import org.team2471.frc.lib.control.experimental.toggleWhen
import org.team2471.frc.lib.math.deadband
import org.team2471.frc.lib.math.squareRootWithSign
import org.team2471.frc.lib.math.squareWithSign
import org.team2471.frc.powerup.commands.*
import java.lang.Double.max

object Driver {
    private val controller = XboxController(0)

    var rumble = 0.0
        set(value) {
            val result = if (RobotState.isEnabled()) value else 0.0
            controller.setRumble(GenericHID.RumbleType.kLeftRumble, result)
            controller.setRumble(GenericHID.RumbleType.kRightRumble, result)
            field = value
        }

    val throttle: Double
        get() = -controller.getY(GenericHID.Hand.kLeft)
                .deadband(0.2)
                .squareWithSign()

    val softTurn: Double
        get() = controller.getX(GenericHID.Hand.kRight)
                .deadband(0.2)
                .squareWithSign()

    val hardTurn: Double
        get() = (-controller.getTriggerAxis(GenericHID.Hand.kLeft) + controller.getTriggerAxis(GenericHID.Hand.kRight))

    val intaking: Boolean
        get() = controller.getBumper(GenericHID.Hand.kRight)

    val acquireRung: Boolean
        get() = controller.backButton

    init {
        driveTuner.toggleWhen { controller.getStickButton(GenericHID.Hand.kLeft) }
        driverIntake.toggleWhen { intaking }
        driverSpit.runWhile { controller.getBumper(GenericHID.Hand.kLeft) }
        climbCommand.toggleWhen { controller.startButton }
    }
}

object CoDriver {
    private val controller = XboxController(1)

    var rumble = 0.0
        set(value) {
            val result = if (RobotState.isEnabled()) max(value, passiveRumble) else 0.0
            controller.setRumble(GenericHID.RumbleType.kLeftRumble, result)
            controller.setRumble(GenericHID.RumbleType.kRightRumble, result)
            field = value
        }

    var passiveRumble = 0.0
        set(value) {
            field = value
            rumble = rumble
        }

    val leftStickUpDown: Double
        get() = -controller.getY(GenericHID.Hand.kLeft)
                .deadband(.2)

    val microAdjust: Double
        get() = -controller.getY(GenericHID.Hand.kRight)
                .deadband(.2)

    val spitSpeed: Double
        get() = Math.max(controller.getTriggerAxis(GenericHID.Hand.kRight) * 0.8,
                controller.getTriggerAxis(GenericHID.Hand.kLeft) * 0.4)

    val release: Boolean
        get() = controller.getBumper(GenericHID.Hand.kRight)

    init {
        println("Initialized")
        SmartDashboard.putBoolean("Tune Arm PID", false)
        zero.toggleWhen { controller.backButton }
        goToSwitch.runWhen { controller.getStickButton(GenericHID.Hand.kRight) }
        goToScaleLowPreset.runWhen { controller.aButton }
        goToScaleMediumPreset.runWhen { controller.xButton }
        goToScaleHighPreset.runWhen { controller.yButton }
        goToIntakePreset.runWhen { controller.bButton }
        incrementScaleStackHeight.runWhen { controller.pov == 0 }
        decrementScaleStackHeight.runWhen { controller.pov == 180 }
        tuneArmPID.runWhen { SmartDashboard.getBoolean("Tune Arm PID", false) }
    }
}
