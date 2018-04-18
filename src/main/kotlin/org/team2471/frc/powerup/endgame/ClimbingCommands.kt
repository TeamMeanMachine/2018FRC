package org.team2471.frc.powerup.endgame

import edu.wpi.first.wpilibj.Timer
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import kotlinx.coroutines.experimental.cancel
import kotlinx.coroutines.experimental.cancelAndJoin
import kotlinx.coroutines.experimental.launch
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.control.experimental.suspendUntil
import org.team2471.frc.powerup.*
import org.team2471.frc.powerup.carriage.Carriage
import org.team2471.frc.powerup.carriage.Lifter
import org.team2471.frc.powerup.carriage.Pose
import org.team2471.frc.powerup.drivetrain.Drivetrain
import kotlin.math.max


val climbCommand = Command("Climb", Carriage, Drivetrain, Wings, LEDController) {
    LEDController.state = ClimbStopState
    val acquireRung = launch(coroutineContext) {
        Carriage.animateToPose(Pose.CLIMB)
        suspendUntil { Driver.acquireRung }
        launch(this@Command.coroutineContext) {
            LEDController.state = ClimbGoState
            periodic {
                val deploy = SmartDashboard.getBoolean("Deploy Wings", true) &&
                        Game.isEndGame && Lifter.height > Pose.CLIMB_ACQUIRE_RUNG.lifterHeight - 6.0

                Wings.wingsDeployed = deploy

                if (deploy) {
                    Drivetrain.driveRaw(-max(0.2, Driver.leftTrigger), -max(0.2, Driver.rightTrigger))
                    LEDController.state = ClimbGoState
                } else {
                    LEDController.state = ClimbStopState
                }
            }
        }
        Carriage.animateToPose(Pose.CLIMB_ACQUIRE_RUNG)
    }


    try {
//        Wings.climbingGuideDeployed = true
        periodic(condition = { acquireRung.isActive }) {
            Drivetrain.drive(Driver.throttle, Driver.softTurn, Driver.hardTurn)
        }
        println("Stage 2")
        Drivetrain.drive(-0.1, 0.0, 0.0)
        LEDController.state = ClimbStopState
        Lifter.isLowGear = true

        Carriage.setAnimation(Pose.FACE_THE_BOSS)

        val timer = Timer()
        timer.start()
        var previousTime = 0.0
        periodic {
            val time = timer.get()
            val input = CoDriver.leftStickUpDown
            if (input == 0.0) {
                Lifter.isBraking = true
                Lifter.stop()
            } else {
                Lifter.isBraking = false
                Carriage.adjustAnimationTime((time - previousTime) * input)
            }

            previousTime = time
        }
    } finally {
        Wings.climbingGuideDeployed = false
        Lifter.isLowGear = false
        Lifter.isBraking = false
        Lifter.heightRawSpeed = 0.0
        Wings.wingsDeployed = false
        Drivetrain.drive(0.0, 0.0, 0.0)
        resetClimbCommand.launch()
    }
}

val newClimbCommand = Command("New Climb", Drivetrain, Carriage, Wings) {
    try {
        val soloClimb = SmartDashboard.getBoolean("Solo Climb", false)
        Wings.climbingGuideDeployed = !soloClimb
        val driveJob = launch(coroutineContext) {
            periodic {
                Drivetrain.drive(Driver.throttle, Driver.softTurn, Driver.hardTurn * 0.5)
            }
        }
        LEDController.state = ClimbStopState

        if(!soloClimb) {
            suspendUntil { !Driver.climb }
            suspendUntil { Driver.climb }
        }


        // extra press cancels the climb
        launch(coroutineContext) {
            suspendUntil { !Driver.climb }
            suspendUntil { Driver.climb }
            resetClimbCommand.launch()
        }

        Carriage.animateToPose(Pose.CLIMB)
        if (!soloClimb) {
            driveJob.cancelAndJoin()
            Drivetrain.driveRaw(-0.2, -0.2)
        }
        suspendUntil { Driver.acquireRung }

        Carriage.animateToPose(Pose.CLIMB_ACQUIRE_RUNG)
        Lifter.isLowGear = true
        Carriage.setAnimation(Pose.FACE_THE_BOSS)
        driveJob.cancelAndJoin()

        val timer = Timer()
        timer.start()
        var previousTime = 0.0
        periodic {
            val deploy = SmartDashboard.getBoolean("Deploy Wings", true) &&
                    Game.isEndGame && Lifter.height > Pose.CLIMB_ACQUIRE_RUNG.lifterHeight - 6.0 && !soloClimb

            Wings.wingsDeployed = deploy

            val time = timer.get()
            val input = CoDriver.leftStickUpDown
            if (input == 0.0) {
                Lifter.isBraking = true
                Lifter.stop()
            } else {
                Lifter.isBraking = false
                Carriage.adjustAnimationTime((time - previousTime) * input)
            }

            Drivetrain.driveRaw(-max(0.2, max(Driver.throttle, Driver.leftTrigger)), -max(0.2, max(Driver.throttle, Driver.rightTrigger)))

            previousTime = time

            LEDController.state = if (deploy) ClimbGoState else ClimbStopState
        }
    } finally {
        Wings.climbingGuideDeployed = false
        Lifter.isLowGear = false
        Lifter.isBraking = false
        Lifter.stop()
        Wings.wingsDeployed = false
        Drivetrain.drive(0.0, 0.0, 0.0)
        LEDController.state = FireState
    }
}

val resetClimbCommand = Command("Reset Climb", Drivetrain, Carriage, Wings) {
    Carriage.animateToPose(Pose.CLIMB)
}