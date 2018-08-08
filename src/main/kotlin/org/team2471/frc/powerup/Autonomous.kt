package org.team2471.frc.powerup

import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.NeutralMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import edu.wpi.first.networktables.EntryListenerFlags
import edu.wpi.first.networktables.NetworkTableInstance
import edu.wpi.first.wpilibj.DriverStation
import edu.wpi.first.wpilibj.Talon
import edu.wpi.first.wpilibj.Timer
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import kotlinx.coroutines.experimental.delay
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.delaySeconds
import org.team2471.frc.lib.control.experimental.parallel
import org.team2471.frc.lib.motion_profiling.Autonomi
import org.team2471.frc.lib.motion_profiling.Path2D
import org.team2471.frc.lib.util.measureTimeFPGA
import org.team2471.frc.powerup.carriage.Arm
import org.team2471.frc.powerup.carriage.Carriage
import org.team2471.frc.powerup.carriage.Lifter
import org.team2471.frc.powerup.carriage.Pose
import org.team2471.frc.powerup.drivetrain.Drivetrain
import sun.plugin.dom.exception.InvalidStateException
import java.io.File

private lateinit var autonomi: Autonomi

private var startingSide = Side.RIGHT

object AutoChooser {
    private val cacheFile = File("/home/lvuser/autonomi.json")

    private val sideChooser = SendableChooser<Side>().apply {
        addDefault("Left", Side.LEFT)
        addObject("Center", Side.CENTER)
        addObject("Right", Side.RIGHT)
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
        addObject(driveStraightAuto.name, driveStraightAuto)
        addObject(preMatchTest.name, preMatchTest)
    }

    private val nearSwitchFarScaleChooser = SendableChooser<Command>().apply {
        addDefault(farScaleAuto.name, farScaleAuto)
        addObject(allFarScaleMeanMachine.name, allFarScaleMeanMachine)
        addObject(driveStraightAuto.name, driveStraightAuto)
        addObject(preMatchTest.name, preMatchTest)
    }

    private val farSwitchNearScaleChooser = SendableChooser<Command>().apply {
        addDefault(nearScaleAuto.name, nearScaleAuto)
        addObject(driveStraightAuto.name, driveStraightAuto)
        addObject(preMatchTest.name, preMatchTest)
    }

    private val farSwitchFarScaleChooser = SendableChooser<Command>().apply {
        addDefault(farScaleAuto.name, farScaleAuto)
        addObject(allFarScaleMeanMachine.name, allFarScaleMeanMachine)
        addObject(driveStraightAuto.name, driveStraightAuto)
        addObject(preMatchTest.name, preMatchTest)
    }

    private val scaleSideChooser = SendableChooser<Side?>().apply {
        addDefault("Get from FMS", null)
        addObject("Force Left", Side.LEFT)
        addObject("Force Right", Side.RIGHT)
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
        val testPath = if (!Game.isFMSAttached) testAutoChooser.selected else null
        if (testPath != null) {
            val testAutonomous = autonomi["Tests"]
            Drivetrain.driveAlongPath(testAutonomous[testPath])
            delay(Long.MAX_VALUE)
            return@Command
        }

        val scaleSide = scaleSideChooser.selected ?: Game.scaleSide

        val chosenCommand = when {
            nearSide == Side.CENTER -> centerAuto
            Game.switchSide == nearSide && scaleSide == nearSide -> nearSwitchNearScaleChooser.selected
            Game.switchSide == farSide && scaleSide == farSide -> farSwitchFarScaleChooser.selected
            Game.switchSide == farSide && scaleSide == nearSide -> farSwitchNearScaleChooser.selected
            Game.switchSide == nearSide && scaleSide == farSide -> nearSwitchFarScaleChooser.selected
            else -> null
        }
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
        SmartDashboard.putData("Test Path Chooser", testAutoChooser)
        SmartDashboard.putData("Scale Side Chooser", scaleSideChooser)
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

const val RELEASE_DELAY = 500L

val nearScaleAuto = Command("Near Scale", Drivetrain, Carriage) {
    val auto = autonomi["All Near Scale"]
    auto.isMirrored = startingSide == Side.LEFT

    try {
        Arm.intakeSpeed = 0.4
        var path = auto["Start To Near Scale"]

        parallel({
            Drivetrain.driveAlongPath(path)
        }, {
            delaySeconds(path.durationWithSpeed - 1.5)
            Carriage.animateToPose(Pose.SCALE_MED, -1.0, -30.0)
            Arm.intakeSpeed = -0.525
        })

        path = auto["Near Scale To Cube1"]
        parallel({
            Drivetrain.driveAlongPath(path)
        }, {
            Carriage.animateToPose(Pose.INTAKE)
        }, {
            delay(RELEASE_DELAY)
            Arm.isClamping = false
            Arm.intakeSpeed = 0.8
            delaySeconds(path.durationWithSpeed - 0.1 - (RELEASE_DELAY / 1000.0))
            Arm.isClamping = true
        })

        path = auto["Cube1 To Near Scale"]
        parallel({
            Drivetrain.driveAlongPath(path)
        }, {
            delaySeconds(path.durationWithSpeed - 1.5)
            Carriage.animateToPose(Pose.SCALE_LOW, -3.0, -30.0)
            Arm.intakeSpeed = -0.55
        }, {
            delay(400)
            Arm.intakeSpeed = 0.4
        })

        path = auto["Near Scale To Cube2"]
        parallel({
            Drivetrain.driveAlongPath(path)
        }, {
            Carriage.animateToPose(Pose.INTAKE)
        }, {
            delay(RELEASE_DELAY)
            Arm.isClamping = false
            Arm.intakeSpeed = 0.68
            delaySeconds(path.durationWithSpeed - 0.1 - (RELEASE_DELAY / 1000.0))
            Arm.isClamping = true
        })

        path = auto["Cube2 To Near Scale"]
        parallel({
            Drivetrain.driveAlongPath(path)
        }, {
            delaySeconds(path.durationWithSpeed - 1.5)
            Carriage.animateToPose(Pose.SCALE_LOW, -6.0, angleOffset = -30.0)
            Arm.intakeSpeed = -0.45
        }, {
            delay(400)
            Arm.intakeSpeed = 0.4
        })
        delay(250)
        path = auto["Near Scale To Cube3"]
        parallel({
            Drivetrain.driveAlongPath(path)
        },  {
            Carriage.animateToPose(Pose.INTAKE)
        }, {
            delay(RELEASE_DELAY)
            Arm.isClamping = false
            Arm.intakeSpeed = 0.5
            delaySeconds(path.durationWithSpeed - 0.1 - (RELEASE_DELAY / 1000.0))
            Arm.isClamping = true
        })

        path = auto["Cube3 To Near Scale"]
        parallel({
            Drivetrain.driveAlongPath(path)
        }, {
            delaySeconds(path.durationWithSpeed - 1.5)
            Carriage.animateToPose(Pose.SCALE_LOW)
            Arm.intakeSpeed = -0.4
        }, {
            delay(400)
            Arm.intakeSpeed = 0.4
        })
       delay(250)
    } finally {
        Arm.intakeSpeed = 0.0
        Arm.isClamping = true
    }
}

val farScaleAuto = Command("Far Scale", Drivetrain, Carriage) {
    val auto = autonomi["All Far Scale"]
    auto.isMirrored = startingSide == Side.LEFT

    try {
        var path = auto["Start To Far Scale"]
        Arm.intakeSpeed = 0.2
        parallel({
            Drivetrain.driveAlongPath(path)
        }, {
            delaySeconds(path.durationWithSpeed - 1.5)
            Carriage.animateToPose(Pose.SCALE_HIGH, -6.0, -30.0)
            Arm.intakeSpeed = -0.5
        })

        parallel({
            Drivetrain.driveAlongPath(auto["Far Scale To Cube1"])
        }, {
            Carriage.animateToPose(Pose.INTAKE)
        },  {
            delay(300)
            Arm.isClamping = false
            Arm.intakeSpeed = 0.6
        })

        Arm.isClamping = true

        path = auto["Cube1 To Far Scale"]

        parallel({
            Drivetrain.driveAlongPath(path)
        }, {
            delaySeconds(path.durationWithSpeed - 1.6)
            Carriage.animateToPose(Pose.SCALE_MED, -6.0, -30.0)
            Arm.intakeSpeed = -0.5
        }, {
            delay(400)
            Arm.intakeSpeed = 0.3
        })

        parallel({
            Drivetrain.driveAlongPath(auto["Far Scale To Cube2"])
        }, {
            Carriage.animateToPose(Pose.INTAKE)
        }, {
            delay(300)
            Arm.isClamping = false
            Arm.intakeSpeed = 0.6
        })

        Arm.isClamping = true

        path = auto["Cube2 To Far Scale"]
        parallel({
            Drivetrain.driveAlongPath(path)
        }, {
            delaySeconds(path.durationWithSpeed - 1.6)
            Carriage.animateToPose(Pose.SCALE_MED, -6.0, -15.0)
            Arm.intakeSpeed = -0.4
        }, {
            delay(400)
            Arm.intakeSpeed = 0.3
        })
    } finally {
        Arm.intakeSpeed = 0.0
        Arm.isClamping = true
    }
}

val centerAuto = Command("Robonauts Auto", Drivetrain, Carriage) {
    val auto = autonomi["Center Switch"]
    auto.isMirrored = false
    val switchSide = when (Game.switchSide) {
        Side.LEFT -> "Left"
        Side.RIGHT -> "Right"
        else -> throw InvalidStateException("Invalid Switch Side")
    }
    var path = auto["$switchSide Switch"]

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

        Drivetrain.driveAlongPath(auto["Back From Right Switch"])
        Arm.intakeSpeed = 0.0

        parallel({
            Carriage.animateToPose(Pose.INTAKE)
            Arm.intakeSpeed = 0.6
        }, {
            Drivetrain.driveAlongPath(auto["Back To Cube"])
        })
        Arm.isClamping = true
        delay(350)
        Arm.intakeSpeed = 0.2

        if (!SmartDashboard.getBoolean("Safe Center Auto", false)) {
            auto.isMirrored = Game.scaleSide == Side.LEFT
            path = auto["Cube To Scale"]
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
                Drivetrain.driveAlongPath(auto["Cube1 Backup"])
            }, {
                Carriage.animateToPose(Pose.SWITCH)
            })
            Drivetrain.driveAlongPath(auto["Cube1 To Switch"])
            Arm.intakeSpeed = -0.6

            Drivetrain.driveAlongPath(auto["Switch To Cube2"])

            Arm.intakeSpeed = 0.0
            parallel({
                Drivetrain.driveAlongPath(auto["To Cube2"])
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

val allFarScaleMeanMachine = Command("All Far Scale Platform", Drivetrain, Carriage, Arm) {
    val auto = autonomi["All Far Scale Mean Machine"]
    auto.isMirrored = false
    try {
        var path = auto["Start To Far Platform"]
        parallel({
            Drivetrain.driveAlongPath(path)
        }, {
            delaySeconds(path.durationWithSpeed - 1.5)
            Carriage.animateToPose(Pose.SCALE_HIGH, 6.0)
            Arm.isClamping = false
        })
        parallel({
            Drivetrain.driveAlongPath(auto["Far Platform To Cube1"])
        }, {
            Carriage.animateToPose(Pose.INTAKE)
            Arm.isClamping = false
            Arm.intakeSpeed = 0.6
        }, {
            delay(300)
            Arm.isClamping = true
        })
        Arm.isClamping = true
        path = auto["Cube1 To Far Platform"]
        parallel({
            Drivetrain.driveAlongPath(auto["Cube1 To Far Platform"])
        }, {
            delaySeconds(path.durationWithSpeed - 1.0)
            Carriage.animateToPose(Pose.SCALE_HIGH, 6.0)
        }, {
            delay(400)
            Arm.intakeSpeed = 0.3
        })
        delay(200)
        Arm.isClamping = false
        delay(300)
    } finally {
        Arm.intakeSpeed = 0.0
        Arm.isClamping = true
    }
}

val driveStraightAuto = Command("Drive Straight",  Drivetrain) {
    val auto = autonomi["Tests"]
    auto.isMirrored = false
    try {
        Drivetrain.driveAlongPath(auto["8 Foot Straight"])
    } finally {
        Arm.intakeSpeed = 0.0
        Arm.isClamping = true
    }
}

suspend fun testMotor(motor: TalonSRX, encoderMotor: TalonSRX) {
    var sampleCount = 0
    var velocityAcc = 0.0
    var currentAcc = 0.0
    try {
        motor.set(ControlMode.PercentOutput, 1.0)
        val timer = Timer()
        timer.start()
        while (timer.get() < 3.0) {
            velocityAcc += encoderMotor.getSelectedSensorVelocity(0)
            currentAcc += motor.outputCurrent
            sampleCount++
            delay(20)
        }
    } finally {
        val velocityFinal = velocityAcc/sampleCount
        val currentFinal = currentAcc/sampleCount
        println("Motor ${motor.deviceID} Velocity: $velocityFinal")
        println("Motor ${motor.deviceID} Current: $currentFinal")
        motor.set(ControlMode.PercentOutput, 0.0)
        if (velocityFinal<700.0 || currentFinal>15.0) {
            println("Potentialy Bad Motor ****************************************************************************")
        }
    }
}

val preMatchTest = Command("Pre Match Test", Drivetrain, Arm) {
    val motor0 = TalonSRX(0)
    val motor1 = TalonSRX(1)
    val motor2 = TalonSRX(2)
    val motor13 = TalonSRX(13)
    val motor14 = TalonSRX(14)
    val motor15 = TalonSRX(15)
    motor0.setNeutralMode(NeutralMode.Coast)
    motor1.setNeutralMode(NeutralMode.Coast)
    motor2.setNeutralMode(NeutralMode.Coast)
    motor13.setNeutralMode(NeutralMode.Coast)
    motor14.setNeutralMode(NeutralMode.Coast)
    motor15.setNeutralMode(NeutralMode.Coast)
    testMotor(motor0, motor0)
    testMotor(motor14,motor15)
    testMotor(motor1,motor0)
    testMotor(motor15,motor15)
    testMotor(motor2,motor0)
    testMotor(motor13,motor15)
}

val backUpOneCube = Path2D().apply {
    setPathDefaults()
    robotDirection = Path2D.RobotDirection.BACKWARD
    isMirrored = false
    addPointAndTangent(0.0, 0.0, 0.0, 0.5)
    addPointAndTangent(0.0, 1.0, 0.0, 0.5)
    addEasePoint(0.0, 0.0)
    addEasePoint(1.0, 1.0)
}

val backUpOneCubeAndRollCubeOut = Command("Drive Straight",  Drivetrain) {
    try {
        Drivetrain.driveAlongPath(backUpOneCube)
    } finally {
        Arm.intakeSpeed = 0.0
        Arm.isClamping = true
    }
}
