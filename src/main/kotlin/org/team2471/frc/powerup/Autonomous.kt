package org.team2471.frc.powerup

import edu.wpi.first.networktables.EntryListenerFlags
import edu.wpi.first.networktables.NetworkTableInstance
import edu.wpi.first.wpilibj.DriverStation
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.delay
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.delaySeconds
import org.team2471.frc.lib.control.experimental.parallel
import org.team2471.frc.lib.motion_profiling.Autonomi
import org.team2471.frc.lib.motion_profiling.Autonomous
import org.team2471.frc.lib.util.measureTimeFPGA
import org.team2471.frc.powerup.carriage.Arm
import org.team2471.frc.powerup.carriage.Carriage
import org.team2471.frc.powerup.carriage.Lifter
import org.team2471.frc.powerup.carriage.Pose
import org.team2471.frc.powerup.drivetrain.Drivetrain
import sun.plugin.dom.exception.InvalidStateException
import java.io.File
import java.io.FileNotFoundException

private lateinit var autonomi: Autonomi

fun Autonomi.getAutoOrCancel(autoName: String) =
        this[autoName] ?: run {
            val message = "Failed to find auto $autoName"
            DriverStation.reportError(message, false)
            throw CancellationException(message)
        }

fun Autonomous.getPathOrCancel(pathName: String) =
        this[pathName] ?: run {
            val message = "Failed to find path $pathName"
            DriverStation.reportError(message, false)
            throw CancellationException(message)
        }

private var startingSide = Side.RIGHT

object AutoChooser {
    private val cacheFile = File("/home/lvuser/autonomi.json")

    private val sideChooser = SendableChooser<Side>().apply {
        addObject("Left", Side.LEFT)
        addObject("Center", Side.CENTER)
        addDefault("Right", Side.RIGHT)
    }

    private val testAutoChooser = SendableChooser<String?>().apply {
        addDefault("None", null)
        addObject("Drive Straight", "8 Foot Straight")
        addObject("2 Foot Circle", "2 Foot Circle")
        addObject("4 Foot Circle", "4 Foot Circle")
        addObject("8 Foot Circle", "8 Foot Circle")
        addObject("S Curve", "S Curve")
    }

    private val nearSwitchNearScaleChooser = SendableChooser<Command>().apply {
        addDefault(nearScaleAuto.name, nearScaleAuto)
    }

    private val nearSwitchFarScaleChooser = SendableChooser<Command>().apply {
        addDefault(farScaleAuto.name, farScaleAuto)
        addObject(allFarScaleMeanMachine.name, allFarScaleMeanMachine)
    }

    private val farSwitchNearScaleChooser = SendableChooser<Command>().apply {
        addDefault(nearScaleAuto.name, nearScaleAuto)
    }

    private val farSwitchFarScaleChooser = SendableChooser<Command>().apply {
        addDefault(farScaleAuto.name, farScaleAuto)
        addObject(allFarScaleMeanMachine.name, allFarScaleMeanMachine)
    }


    val auto = Command("Autonomous", Drivetrain, Carriage) {
        Arm.hold()
        Lifter.stop()
        Arm.isClamping = true
        val nearSide = sideChooser.selected
        startingSide = nearSide
        val farSide = !nearSide

        // adjust gyro for starting position
        Drivetrain.gyroAngleOffset = if (nearSide == Side.CENTER) 0.0 else 180.0
        Lifter.zero()
        val testPath = if(!Game.isFMSAttached) testAutoChooser.selected else null
        if (testPath != null) {
            val testAutonomous = autonomi.getAutoOrCancel("Tests")
            Drivetrain.driveAlongPath(testAutonomous.getPathOrCancel(testPath))
            delay(Long.MAX_VALUE)
            return@Command
        }

        val chosenCommand = when {

            nearSide == Side.CENTER -> centerAuto
            Game.switchSide == nearSide && Game.scaleSide == nearSide -> nearSwitchNearScaleChooser.selected
            Game.switchSide == farSide && Game.scaleSide == farSide -> farSwitchFarScaleChooser.selected
            Game.switchSide == farSide && Game.scaleSide == nearSide -> farSwitchNearScaleChooser.selected
            Game.switchSide == nearSide && Game.scaleSide == farSide -> nearSwitchFarScaleChooser.selected
//            Game.scaleSide == nearSide -> nearScaleAuto
//            Game.scaleSide == farSide -> farScaleAuto
            else -> null
        }
        if (chosenCommand == null) {
            DriverStation.reportError("Autonomous could not be chosen", false)
            return@Command
        }
        chosenCommand(coroutineContext)
//        farScaleAuto(coroutineContext)
    }

    init {
        SmartDashboard.putData("Near Switch Near Scale Auto", nearSwitchNearScaleChooser)
        SmartDashboard.putData("Near Switch Far Scale Auto", nearSwitchFarScaleChooser)
        SmartDashboard.putData("Far Switch Near Scale Auto", farSwitchNearScaleChooser)
        SmartDashboard.putData("Far Switch Far Scale Auto", farSwitchFarScaleChooser)

        SmartDashboard.putData("Side Chooser", sideChooser)
        SmartDashboard.putData("Test Path Chooser", testAutoChooser)
        if (!SmartDashboard.containsKey("Safe Center Auto")) {
            SmartDashboard.putBoolean("Safe Center Auto", false)
        }
        SmartDashboard.setPersistent("Safe Center Auto")

        // load cached autonomi
        try {
            autonomi = Autonomi.fromJsonString(cacheFile.readText())
            println("Autonomi cache loaded.")
        } catch (_: Exception) {
            DriverStation.reportError("Autonomi cache could not be found", false)
            autonomi = Autonomi()
        }

        NetworkTableInstance.getDefault()
                .getTable("PathVisualizer")
                .getEntry("Autonomi").addListener({ event ->
                    val json = event.value.string
                    if (!json.isEmpty()) {
                        val t = measureTimeFPGA {
                            autonomi = Autonomi.fromJsonString(json)
                        }
                        println("Loaded autonomi in $t seconds")

                        cacheFile.writeText(json)
                        println("New autonomi written to cache")
                    } else {
                        autonomi = Autonomi()
                        DriverStation.reportWarning("Empty autonomi received from network tables", false)
                    }
                }, EntryListenerFlags.kImmediate or EntryListenerFlags.kNew or EntryListenerFlags.kUpdate)
    }

}

val nearScaleAuto = Command("Near Scale", Drivetrain, Carriage) {
    val auto = autonomi.getAutoOrCancel("All Near Scale")
    auto.isMirrored = startingSide == Side.LEFT

    try {
        Arm.intakeSpeed = 0.2
        var path = auto.getPathOrCancel("Start To Near Scale")
        parallel({
            Drivetrain.driveAlongPath(path)
        }, {
            delaySeconds(path.durationWithSpeed - 1.5)
            Carriage.animateToPose(Pose.SCALE_HIGH)
            Arm.intakeSpeed = -0.5
            delay(250)
        })
        Arm.isClamping = false

        parallel({
            Arm.intakeSpeed = 0.8
            Drivetrain.driveAlongPath(auto.getPathOrCancel("Near Scale To Cube1"))
            Arm.isClamping = true
        }, {
            Carriage.animateToPose(Pose.INTAKE)
        })


        path = auto.getPathOrCancel("Cube1 To Near Scale")
        parallel({
            Drivetrain.driveAlongPath(path)
        }, {
            delay(300)
            Arm.intakeSpeed = 0.2
            delaySeconds(path.durationWithSpeed - 1.5)
            Carriage.animateToPose(Pose.SCALE_MED)
            Arm.intakeSpeed = -0.4
            delay(350)
        })
        Arm.intakeSpeed = 0.0

        parallel({
            Drivetrain.driveAlongPath(auto.getPathOrCancel("Near Scale To Cube2"))
        }, {
            Arm.isClamping = false
            Carriage.animateToPose(Pose.INTAKE)
            Arm.intakeSpeed = 0.8
        })

        Arm.isClamping = true

        path = auto.getPathOrCancel("Cube2 To Near Scale")
        parallel({
            Drivetrain.driveAlongPath(path)
        }, {
            delay(300)
            Arm.intakeSpeed = 0.2
            delaySeconds(path.durationWithSpeed - 2.0)
            Carriage.animateToPose(Pose.SCALE_LOW)
            delay(100)
            Arm.isClamping = false
            delay(500)
        })
//        Carriage.animateToPose(Pose.INTAKE)
    } finally {
        Arm.intakeSpeed = 0.0
        Arm.isClamping = true
    }
}

val farScaleAuto = Command("Far Scale", Drivetrain, Carriage) {
    val auto = autonomi.getAutoOrCancel("All Far Scale")
    auto.isMirrored = startingSide == Side.LEFT

    try {
        var path = auto.getPathOrCancel("Start To Far Scale")
        Arm.intakeSpeed = 0.2
        parallel({
            Drivetrain.driveAlongPath(path)
        }, {
            delaySeconds(path.durationWithSpeed - 1.5)
            Carriage.animateToPose(Pose.SCALE_HIGH)
            Arm.intakeSpeed = -0.5
            delay(200)
        })
        parallel({
            Arm.isClamping = false
            Arm.intakeSpeed = 0.6
            Drivetrain.driveAlongPath(auto.getPathOrCancel("Far Scale To Cube1"))
            Arm.isClamping = true
            delay(250)
            Arm.intakeSpeed = 0.2
        }, {
            Carriage.animateToPose(Pose.INTAKE)
        })

        path = auto.getPathOrCancel("Cube1 To Far Scale")

        parallel({
            Drivetrain.driveAlongPath(path)
        }, {
            delaySeconds(path.durationWithSpeed - 1.5)
            Carriage.animateToPose(Pose.SCALE_HIGH)
            Arm.intakeSpeed = 0.0
            Arm.isClamping = false
            delay(200)
        })
        parallel({
            Drivetrain.driveAlongPath(auto.getPathOrCancel("Far Scale To Cube2"))
        }, {
            Arm.intakeSpeed = 0.6
            Carriage.animateToPose(Pose.INTAKE)
        })
        Arm.isClamping = true
        delay(250)
    } finally {
        Arm.intakeSpeed = 0.0
        Arm.isClamping = true
    }
}

val centerAuto = Command("Robonauts Auto", Drivetrain, Carriage) {
    val auto = autonomi.getAutoOrCancel("Center Switch")
    auto.isMirrored = false
    val switchSide = when (Game.switchSide) {
        Side.LEFT -> "Left"
        Side.RIGHT -> "Right"
        else -> throw InvalidStateException("Invalid Switch Side")
    }
    var path = auto.getPathOrCancel("$switchSide Switch")

    try {
        parallel({
            Drivetrain.driveAlongPath(path)
        }, {
            Carriage.animateToPose(Pose.SWITCH)
            delaySeconds(path.durationWithSpeed - 1.25)
            Arm.intakeSpeed = 0.0
            Arm.isClamping = false
        })

        auto.isMirrored = Game.switchSide == Side.LEFT

        Drivetrain.driveAlongPath(auto.getPathOrCancel("Back From Right Switch"))
        Arm.intakeSpeed = 0.0

        parallel({
            Carriage.animateToPose(Pose.INTAKE)
            Arm.intakeSpeed = 0.6
        }, {
            Drivetrain.driveAlongPath(auto.getPathOrCancel("Back To Cube"))
        })
        Arm.isClamping = true
        delay(350)
        Arm.intakeSpeed = 0.2

        if (!SmartDashboard.getBoolean("Safe Center Auto", false)) {
            auto.isMirrored = Game.scaleSide == Side.LEFT
            path = auto.getPathOrCancel("Cube To Scale")
            parallel({
                Drivetrain.driveAlongPath(path)
            }, {
                delaySeconds(path.durationWithSpeed - 1.5)
                Carriage.animateToPose(Pose.SCALE_HIGH)
                Arm.intakeSpeed = -0.4
            })
            delay(300)
            Carriage.animateToPose(Pose.INTAKE)
        } else {
            parallel({
                Drivetrain.driveAlongPath(auto.getPathOrCancel("Cube1 Backup"))
            }, {
                Carriage.animateToPose(Pose.SWITCH)
            })
            Drivetrain.driveAlongPath(auto.getPathOrCancel("Cube1 To Switch"))
            Arm.intakeSpeed = -0.6

            Drivetrain.driveAlongPath(auto.getPathOrCancel("Switch To Cube2"))

            Arm.intakeSpeed = 0.0
            parallel({
                Drivetrain.driveAlongPath(auto.getPathOrCancel("To Cube2"))
            }, {
                Arm.isClamping = false
                Carriage.animateToPose(Pose.INTAKE_RAISED)
                Arm.intakeSpeed = 0.5
            })
            Arm.isClamping = true
            delay(300)

        }
    } finally {
        Arm.intakeSpeed = 0.0
    }

}

val allFarScaleMeanMachine = Command("All Far Scale Mean Machine", Drivetrain, Carriage, Arm) {
    val auto = autonomi.getAutoOrCancel("All Far Scale Mean Machine")
    auto.isMirrored = false
    try {
        var path = auto.getPathOrCancel("Start To Far Platform")
        parallel({
            Drivetrain.driveAlongPath(path)
        }, {
            delaySeconds(path.durationWithSpeed - 1.5)
            Carriage.animateToPose(Pose.SCALE_HIGH, 6.0)
        })
        Arm.isClamping = false
        delay(200)
        parallel({
            Drivetrain.driveAlongPath(auto.getPathOrCancel("Far Platform To Cube1"))
        }, {
            Carriage.animateToPose(Pose.INTAKE)
            Arm.intakeSpeed = 0.5
        })
        Arm.isClamping = true
        delay(300)
        path = auto.getPathOrCancel("Cube1 To Far Platform")
        parallel({
            Drivetrain.driveAlongPath(auto.getPathOrCancel("Cube1 To Far Platform"))
        }, {
            delaySeconds(path.durationWithSpeed - 1.0)
            Carriage.animateToPose(Pose.SCALE_HIGH, 6.0)
        })
        delay(200)
        Arm.isClamping = false
        delay(200)

        parallel({
            Drivetrain.driveAlongPath(auto.getPathOrCancel("Far Platform To Cube2"))
        }, {
            Carriage.animateToPose(Pose.INTAKE)
            Arm.isClamping = false
            Arm.intakeSpeed = 0.5
        })
        delay(200)
        Arm.isClamping = true
        Arm.intakeSpeed = 0.2
        delay(200)
        parallel({
            Drivetrain.driveAlongPath(auto.getPathOrCancel("Cube2 To Far Platform"))
        }, {
            delaySeconds(path.durationWithSpeed - 1.0)
            Carriage.animateToPose(Pose.SCALE_HIGH, 6.0)
        })
        Arm.isClamping = false
        Carriage.animateToPose(Pose.INTAKE)
    } finally {
        Arm.intakeSpeed = 0.0
        Arm.isClamping = true
    }
}
