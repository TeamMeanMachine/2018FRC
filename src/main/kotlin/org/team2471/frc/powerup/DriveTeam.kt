package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.GenericHID
import edu.wpi.first.wpilibj.XboxController
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.runWhen
import org.team2471.frc.lib.control.experimental.runWhile
import org.team2471.frc.lib.control.experimental.toggleWhen
import org.team2471.frc.lib.math.deadband
import org.team2471.frc.lib.math.squareWithSign
import org.team2471.frc.powerup.commands.*
import org.team2471.frc.powerup.subsystems.Wings
import java.lang.Double.max

object Driver {
    private val controller = XboxController(0)

    var rumble = 0.0
        set(value) {
            controller.setRumble(GenericHID.RumbleType.kLeftRumble, value)
            controller.setRumble(GenericHID.RumbleType.kRightRumble, value)
            field = value
        }

    val throttle: Double
        get() = -controller.getY(GenericHID.Hand.kLeft)
                .deadband(0.2)
                .squareWithSign()

    val softTurn: Double
        get() = controller.getX(GenericHID.Hand.kRight)
                .deadband(0.2)

    val hardTurn: Double
        get() = -controller.getTriggerAxis(GenericHID.Hand.kLeft) + controller.getTriggerAxis(GenericHID.Hand.kRight)

    val intaking: Boolean
        get() = controller.getBumper(GenericHID.Hand.kRight)

    val acquireRung: Boolean
        get() = controller.backButton

    init {
        driverIntake.runWhen { intaking }
        driverSpit.runWhile { controller.getBumper(GenericHID.Hand.kLeft) }
        climbCommand.toggleWhen { controller.startButton }
    }
}

object CoDriver {
    private val controller = XboxController(1)

    var rumble = 0.0
        set(value) {
            controller.setRumble(GenericHID.RumbleType.kLeftRumble, max(value, passiveRumble))
            controller.setRumble(GenericHID.RumbleType.kRightRumble, max(value, passiveRumble))
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

    val rightStickUpDown: Double
        get() = -controller.getY(GenericHID.Hand.kRight)
                .deadband(.2)

    val spitSpeed: Double
        get() = (controller.getTriggerAxis(GenericHID.Hand.kRight) * 0.8)
                .squareWithSign()

    val release: Boolean
        get() = controller.getBumper(GenericHID.Hand.kRight)

    init {
        println("Initialized")
        Command("DeployClimbGuide", Wings) {
            Wings.climbingGuideDeployed = !Wings.climbingGuideDeployed
        }.runWhen { controller.getBumper(GenericHID.Hand.kLeft) }


        zero.toggleWhen { controller.backButton }
        goToSwitch.runWhen { controller.getStickButton(GenericHID.Hand.kRight) }
        goToScaleLowPreset.runWhen { controller.aButton }
        goToScaleMediumPreset.runWhen { controller.xButton }
        goToScaleHighPreset.runWhen { controller.yButton }
        returnToIntakePosition.runWhen { controller.bButton }
        incrementScaleStackHeight.runWhen { controller.pov == 0 }
        decrementScaleStackHeight.runWhen { controller.pov == 180 }
    }
}
