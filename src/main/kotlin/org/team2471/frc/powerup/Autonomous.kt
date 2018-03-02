package org.team2471.frc.powerup

import edu.wpi.first.networktables.EntryListenerFlags
import edu.wpi.first.networktables.NetworkTableInstance
import edu.wpi.first.wpilibj.DriverStation
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.cancelAndJoin
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.delaySeconds
import org.team2471.frc.lib.control.experimental.parallel
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.motion_profiling.Autonomi
import org.team2471.frc.lib.motion_profiling.Autonomous
import org.team2471.frc.lib.util.measureTimeFPGA
import org.team2471.frc.powerup.subsystems.Carriage
import org.team2471.frc.powerup.subsystems.Drivetrain
import kotlin.math.roundToInt
import kotlin.math.roundToLong


lateinit var autonomi: Autonomi

private fun Autonomi.getAutoOrCancel(autoName: String) =
        this[autoName] ?: run {
            val message = "Failed to find auto $autoName"
            DriverStation.reportError(message, false)
            throw CancellationException(message)
        }

private fun Autonomous.getPathOrCancel(pathName: String) =
        this[pathName] ?: run {
            val message = "Failed to find path $pathName"
            DriverStation.reportError(message, false)
            throw CancellationException(message)
        }

object AutoChooser {
    private val sideChooser = SendableChooser<Side>().apply {
        addDefault("Left", Side.LEFT)
        addObject("Right", Side.RIGHT)
    }

    private val nearSwitchNearScaleChooser = SendableChooser<Command>().apply {
        addDefault(nearScaleNearSwitchScale.name, nearScaleNearSwitchScale)
    }

    private val nearSwitchFarScaleChooser = SendableChooser<Command>().apply {

    }

    private val farSwitchNearScaleChooser = SendableChooser<Command>().apply {

    }

    private val farSwitchFarScaleChooser = SendableChooser<Command>().apply {

    }

    val auto = Command("Autonomous", Drivetrain, Carriage) {
        Carriage.Arm.stop()
        Carriage.Lifter.stop()

        val nearSide = sideChooser.selected
        val farSide = !nearSide
        val chosenCommand = when {
            Game.switchSide == nearSide && Game.scaleSide == nearSide -> nearSwitchNearScaleChooser
            Game.switchSide == nearSide && Game.scaleSide == farSide -> nearSwitchFarScaleChooser
            Game.switchSide == farSide && Game.scaleSide == nearSide -> farSwitchNearScaleChooser
            Game.switchSide == farSide && Game.scaleSide == farSide -> farSwitchFarScaleChooser
            else -> null
        }?.selected
        if (chosenCommand == null) {
            DriverStation.reportError("Autonomous could not be chosen", false)
            return@Command
        }
        chosenCommand(coroutineContext)
    }

    init {
        SmartDashboard.putData("Near Switch Near Scale Auto", nearSwitchNearScaleChooser)
        SmartDashboard.putData("Near Switch Far Scale Auto", nearSwitchFarScaleChooser)
        SmartDashboard.putData("Far Switch Near Scale Auto", farSwitchNearScaleChooser)
        SmartDashboard.putData("Far Switch Far Scale Auto", farSwitchFarScaleChooser)
        SmartDashboard.putData("Side Chooser", sideChooser)

        loadAutonomi()
        NetworkTableInstance.getDefault()
                .getTable("PathVisualizer")
                .getEntry("Autonomi").addListener({ _ ->
            loadAutonomi()
        }, EntryListenerFlags.kNew or EntryListenerFlags.kUpdate)
    }

    private fun loadAutonomi() {
        val t = measureTimeFPGA {
            autonomi = Autonomi.initFromNetworkTables()
        }
        println("Loaded Autonomi in $t seconds")
    }
}

private val nearScaleNearSwitchScale = Command("Near Scale Near Switch Scale Auto", Carriage, Drivetrain) {
    val auto = autonomi.getAutoOrCancel("Near Scale Near Switch Scale")

    try {
        var path = auto.getPathOrCancel("Start To Near Scale")
        parallel(coroutineContext, {
            Drivetrain.driveAlongPath(path)
        }, {
            delaySeconds(path.durationWithSpeed - 2.0)
            Carriage.animateToPose(Carriage.Pose.SCALE_MED)
            Carriage.Arm.intake = -0.4
            delay(350)
            Carriage.Arm.intake = 0.0
        })

        parallel(coroutineContext, {
            Carriage.Arm.isClamping = false
            Carriage.Arm.intake = 0.6
            Carriage.animateToPose(Carriage.Pose.INTAKE)
        }, {
            Drivetrain.driveAlongPath(auto.getPathOrCancel("Near Scale To Cube1"))
        })

        Carriage.Arm.isClamping = true
        delay(100)
        Carriage.Arm.intake = 0.2

        parallel(coroutineContext, {
            Carriage.animateToPose(Carriage.Pose.SWITCH)
            Carriage.Arm.intake = -0.4
            delay(350)
            Carriage.Arm.intake = 0.0
        }, {
            delay(300)
            Drivetrain.driveAlongPath(auto.getPathOrCancel("Cube1 To Near Switch"))
        })

        Drivetrain.driveAlongPath(auto.getPathOrCancel("Back From Near Switch"))
        Carriage.Arm.isClamping = false
        Carriage.Arm.intake = 0.6

        parallel(coroutineContext, {
            Carriage.animateToPose(Carriage.Pose.INTAKE)
        }, {
            Drivetrain.driveAlongPath(auto.getPathOrCancel("Onward To Cube2"))
        })

        Carriage.Arm.isClamping = true
        Carriage.Arm.intake = 0.2

        path = auto.getPathOrCancel("Cube2 To Near Scale")
        parallel(coroutineContext, {
            delaySeconds(path.durationWithSpeed - 2.0)
            Carriage.animateToPose(Carriage.Pose.SCALE_MED)
        }, {
            Drivetrain.driveAlongPath(path)
        })
        Carriage.Arm.intake = -0.2
        delay(500)
    } finally {
        Carriage.Arm.isClamping = true
        Carriage.Arm.intake = 0.0
    }
}

val driveTuner = Command("Drive Train Tuner", Drivetrain) {
    Drivetrain.zeroDistance()
    periodic {
        Drivetrain.setDistance(Driver.throttle * 2.0)
    }
}
