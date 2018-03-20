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
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.motion_profiling.Autonomi
import org.team2471.frc.lib.motion_profiling.Autonomous
import org.team2471.frc.lib.util.measureTimeFPGA
import org.team2471.frc.powerup.carriage.Carriage
import org.team2471.frc.powerup.drivetrain.Drivetrain
import java.io.File
import java.io.FileNotFoundException

private lateinit var autonomi: Autonomi

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
    private val cacheFile = File("/home/lvuser/autonomi.json")

    private val sideChooser = SendableChooser<Side>().apply {
        addObject("Left", Side.LEFT)
        addObject("Center", Side.CENTER)
        addDefault("Right", Side.RIGHT)
    }

/*
    private val nearSwitchNearScaleChooser = SendableChooser<Command>().apply {
        addDefault(nearScaleNearSwitchScale.name, nearScaleNearSwitchScale)
    }

    private val nearSwitchFarScaleChooser = SendableChooser<Command>().apply {

    }

    private val farSwitchNearScaleChooser = SendableChooser<Command>().apply {

    }

    private val farSwitchFarScaleChooser = SendableChooser<Command>().apply {
        addDefault(farScaleFarSwitch.name, farScaleFarSwitch)
    }
*/

    val auto = Command("Autonomous", Drivetrain, Carriage) {
        Carriage.Arm.hold()
        Carriage.Lifter.stop()
        Carriage.Arm.isClamping = true
        val nearSide = sideChooser.selected
        val farSide = !nearSide
        val chosenCommand = when {
            nearSide == Side.CENTER -> centerLineAuto
//            Game.scaleSide == nearSide && Game.switchSide == nearSide -> nearScaleNearSwitchScale
//            Game.scaleSide == farSide && Game.switchSide == farSide -> farScaleFarSwitch
//            Game.scaleSide == farSide && Game.switchSide == nearSide -> farScaleNearSwitch
//            Game.scaleSide == nearSide && Game.switchSide == farSide -> nearScaleFarSwitch
            Game.scaleSide == nearSide -> nearScaleAuto
            Game.scaleSide == farSide -> farScaleAuto
            else -> null
        }
        if (chosenCommand == null) {
            DriverStation.reportError("Autonomous could not be chosen", false)
            return@Command
        }
        chosenCommand(coroutineContext)
    }

    init {
/*
        SmartDashboard.putData("Near Switch Near Scale Auto", nearSwitchNearScaleChooser)
        SmartDashboard.putData("Near Switch Far Scale Auto", nearSwitchFarScaleChooser)
        SmartDashboard.putData("Far Switch Near Scale Auto", farSwitchNearScaleChooser)
        SmartDashboard.putData("Far Switch Far Scale Auto", farSwitchFarScaleChooser)
*/
        SmartDashboard.putData("Side Chooser", sideChooser)

        // load cached autonomi
        try {
            autonomi = Autonomi.fromJsonString(cacheFile.readText())
            println("Autonomi cache loaded.")
        } catch (_: FileNotFoundException) {
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

private val nearScaleNearSwitchScale = Command("Near Scale Near Switch Scale Auto", Carriage, Drivetrain) {
    val auto = autonomi.getAutoOrCancel("Near Scale Near Switch Scale")

    try {
        Carriage.Arm.intake = 0.2
        var path = auto.getPathOrCancel("Start To Near Scale")
        parallel({
            Drivetrain.driveAlongPath(path)
            Carriage.Arm.intake = -0.2
            delay(350)
            Carriage.Arm.intake = 0.0
        }, {
            delaySeconds(path.durationWithSpeed - 1.5)
            Carriage.animateToPose(Carriage.Pose.SCALE_MED)
        })

        parallel({
            Drivetrain.driveAlongPath(auto.getPathOrCancel("Near Scale To Cube1"))
        }, {
            Carriage.Arm.isClamping = false
            Carriage.Arm.intake = 0.7
            Carriage.animateToPose(Carriage.Pose.INTAKE)
        })

        Carriage.Arm.isClamping = true
        delay(250)

        parallel({
            Carriage.animateToPose(Carriage.Pose.SWITCH)
        }, {
            delay(300)
            Carriage.Arm.intake = 0.2
            Drivetrain.driveAlongPath(auto.getPathOrCancel("Cube1 To Near Switch"))
            Carriage.Arm.intake = -0.4
            delay(350)
            Carriage.Arm.intake = 0.0
        })

        Drivetrain.driveAlongPath(auto.getPathOrCancel("Back From Near Switch"))
        Carriage.Arm.isClamping = false
        Carriage.Arm.intake = 0.6

        parallel({
            Carriage.animateToPose(Carriage.Pose.INTAKE)
        }, {
            Drivetrain.driveAlongPath(auto.getPathOrCancel("Onward To Cube2"))
        })


        Carriage.Arm.isClamping = true
        delay(200)
        Carriage.Arm.intake = 0.2

        path = auto.getPathOrCancel("Cube2 To Near Scale")
        parallel({
            Carriage.animateToPose(Carriage.Pose.SCALE_MED)
        }, {
            Drivetrain.driveAlongPath(path)
        })
        Carriage.Arm.intake = -0.2
        delay(250)
        Carriage.Arm.isClamping = true
        Carriage.Arm.intake = 0.0
        Carriage.animateToPose(Carriage.Pose.INTAKE)
    } finally {
        Carriage.Arm.isClamping = true
        Carriage.Arm.intake = 0.0
    }
}

private val farScaleFarSwitch = Command("Far Scale Far Switch Auto", Drivetrain, Carriage) {
    val auto = autonomi.getAutoOrCancel("Far Scale Far Switch")

    try {
        val path = auto.getPathOrCancel("Start To Far Scale")
        parallel({
            Drivetrain.driveAlongPath(path)
            Carriage.Arm.intake = -0.4
            delay(350)
        }, {
            delaySeconds(path.durationWithSpeed - 1.5)
            Carriage.animateToPose(Carriage.Pose.SCALE_MED)
        })

        parallel({
            Carriage.Arm.isClamping = false
            Carriage.Arm.intake = 0.6
            Carriage.animateToPose(Carriage.Pose.INTAKE)
        }, {
            Drivetrain.driveAlongPath(auto.getPathOrCancel("Far Scale To Cube1"))
        })

        Carriage.Arm.isClamping = true
        delay(150)
        Carriage.animateToPose(Carriage.Pose.SWITCH)
        Drivetrain.driveDistance(0.5, 0.5, false)
        Carriage.Arm.intake = -0.3
        delay(400)
    } finally {
        Carriage.Arm.intake = 0.0
        Carriage.Arm.isClamping = false
    }
}

private val nearScaleFarSwitch = Command("Near Scale Far Switch Auto", Drivetrain, Carriage) {
    val auto = autonomi.getAutoOrCancel("Near Scale Far Switch")

    try {
        val path = auto.getPathOrCancel("Start To Near Scale")

        parallel({
            Drivetrain.driveAlongPath(path)
            Carriage.Arm.intake = -0.2
            delay(350)
        }, {
            delaySeconds(path.durationWithSpeed - 1.5)
            Carriage.animateToPose(Carriage.Pose.SCALE_MED)
        })

        parallel({
            Drivetrain.driveAlongPath(auto.getPathOrCancel("Near Scale To Cube6"))
        }, {
            Carriage.Arm.isClamping = false
            Carriage.animateToPose(Carriage.Pose.INTAKE)
        })
        Drivetrain.turnInPlace(85.0, 1.0)
        Carriage.Arm.intake = 0.6
        Drivetrain.driveDistance(1.4, 0.9, false)
        Carriage.Arm.isClamping = true
        delay(200)
        Carriage.Arm.intake = 0.2

        Carriage.animateToPose(Carriage.Pose.SWITCH)

        Drivetrain.driveDistance(0.5, 0.5, false)
        Carriage.Arm.intake = -0.3
        delay(450)
    } finally {
        Carriage.Arm.intake = 0.0
        Carriage.Arm.isClamping = true
    }
}

private val farScaleNearSwitch = Command("Far Scale Near Switch Auto", Drivetrain, Carriage) {
    val auto = autonomi.getAutoOrCancel("Far Scale Near Switch")
    try {
        val path = auto.getPathOrCancel("Start To Near Switch")
        parallel({
            Drivetrain.driveAlongPath(path)
        }, {
            delaySeconds(path.durationWithSpeed - 1.85)
            Carriage.animateToPose(Carriage.Pose.SWITCH)
            Carriage.Arm.intake = -0.6
            delay(250)
        })

        Carriage.animateToPose(Carriage.Pose.INTAKE)
        Carriage.Arm.intake = 0.6
        Carriage.Arm.isClamping = false

        parallel({
            Drivetrain.driveAlongPath(auto.getPathOrCancel("Near Switch To Cube1"))
        }, {
            delay(2000)
            Carriage.Arm.isClamping = true
            delay(350)
            Carriage.Arm.intake = 0.2
        })

        parallel({
            Drivetrain.driveAlongPath(auto.getPathOrCancel("Cube1 To Far Scale"))
            Carriage.Arm.intake = -0.4
            delay(200)
        }, {
            delay(750)
            Carriage.animateToPose(Carriage.Pose.SCALE_MED)
        })

        parallel({
            Drivetrain.driveAlongPath(auto.getPathOrCancel("Far Scale To Cube6"))
        }, {
            Carriage.Arm.intake = 0.8
            Carriage.Arm.isClamping = false
            Carriage.animateToPose(Carriage.Pose.INTAKE)
        })

        Carriage.Arm.isClamping = true
        delay(250)
        Carriage.Arm.intake = 0.2

        parallel({
            Drivetrain.driveAlongPath(auto.getPathOrCancel("Cube6 To Far Scale"))
            Carriage.Arm.intake = -0.3
            delay(500)
        }, {
            Carriage.animateToPose(Carriage.Pose.SCALE_MED)
        })
        Carriage.Arm.isClamping = true
        Carriage.Arm.intake = 0.0
        Carriage.animateToPose(Carriage.Pose.INTAKE)
    } finally {
        Carriage.Arm.intake = 0.0
        Carriage.Arm.isClamping = true
    }
}

val nearScaleAuto = Command("Near Scale", Drivetrain, Carriage) {
    val auto = autonomi.getAutoOrCancel("All Near Scale")

    try {
        Carriage.Arm.intake = 0.2
        var path = auto.getPathOrCancel("Start To Near Scale")
        parallel({
            Drivetrain.driveAlongPath(path)
        }, {
            delaySeconds(path.durationWithSpeed - 1.5)
            Carriage.animateToPose(Carriage.Pose.SCALE_MED)
            Carriage.Arm.intake = -0.35
            delay(350)
        })
        Carriage.Arm.isClamping = false

        parallel({
            Carriage.Arm.intake = 0.6
            Drivetrain.driveAlongPath(auto.getPathOrCancel("Near Scale To Cube1"))
            Carriage.Arm.isClamping = true
        }, {
            Carriage.animateToPose(Carriage.Pose.INTAKE)
        })

        delay(450)
        Carriage.Arm.intake = 0.2

        path = auto.getPathOrCancel("Cube1 To Near Scale")
        parallel({
            Drivetrain.driveAlongPath(path)
            Carriage.Arm.intake = -0.15
            delay(400)
        }, {
            delaySeconds(path.durationWithSpeed - 1.5)
            Carriage.animateToPose(Carriage.Pose.SCALE_MED)
        })
        Carriage.animateToPose(Carriage.Pose.INTAKE)
        delay(1000)
    } finally {
        Carriage.Arm.intake = 0.0
        Carriage.Arm.isClamping = true
    }
}

val farScaleAuto = Command("Far Scale", Drivetrain, Carriage) {
    val auto = autonomi.getAutoOrCancel("All Far Scale")

    try {
        val path = auto.getPathOrCancel("Start To Far Scale")
        Carriage.Arm.intake = 0.2
        parallel({
            Drivetrain.driveAlongPath(path)
            Carriage.Arm.intake = -0.4
            delay(350)
        }, {
            delaySeconds(path.durationWithSpeed - 1.5)
            Carriage.animateToPose(Carriage.Pose.SCALE_MED)
        })
        parallel({
            Carriage.Arm.isClamping = false
            Carriage.Arm.intake = 0.6
            Drivetrain.driveAlongPath(auto.getPathOrCancel("Far Scale To Cube1"))
            Carriage.Arm.isClamping = true
            delay(250)
            Carriage.Arm.intake = 0.2
        }, {
            Carriage.animateToPose(Carriage.Pose.INTAKE)
        })

        parallel({
            Drivetrain.driveAlongPath(auto.getPathOrCancel("Cube1 To Far Scale"))
            Carriage.Arm.intake = -0.2
            delay(400)
        }, {
            Carriage.animateToPose(Carriage.Pose.SCALE_MED)
        })
//        Carriage.animateToPose(Carriage.Pose.INTAKE)
    } finally {
        Carriage.Arm.intake = 0.0
        Carriage.Arm.isClamping = true
    }
}

val centerLineAuto = Command("Center Line", Drivetrain, Carriage){
    val auto = autonomi.getAutoOrCancel("Tests")
    Drivetrain.driveAlongPath(auto.getPathOrCancel("Straight"))
//    Drivetrain.driveAlongPath(Path2D().apply {
//        robotDirection = Path2D.RobotDirection.BACKWARD
//        addPoint(0.0,0.0)
//        addPoint(0.0,8.0)
//        addEasePoint(0.0,0.0)
//        addEasePoint(5.0, 1.0)
//    })
}

val circleTestAuto = Command("Circle Auto", Drivetrain, Carriage) {
    val auto = autonomi.getAutoOrCancel("Tests")
    Drivetrain.driveAlongPath(auto.getPathOrCancel("Circle"))
//    Drivetrain.loadTrajectory("RightToNearScale")
//    Drivetrain.runLoadedTrajectory()
}


val driveTuner = Command("Drive Train Tuner", Drivetrain) {
    Drivetrain.zeroDistance()
    periodic {
        Drivetrain.setDistance(Driver.throttle * 2.0)
    }
}

