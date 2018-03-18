package org.team2471.frc.powerup.endgame

import edu.wpi.first.wpilibj.Timer
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import kotlinx.coroutines.experimental.launch
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.control.experimental.suspendUntil
import org.team2471.frc.powerup.*
import org.team2471.frc.powerup.carriage.Carriage
import org.team2471.frc.powerup.drivetrain.Drivetrain


val climbCommand = Command("Climb", Carriage, Drivetrain, Wings, LEDController) {
    LEDController.state = ClimbStopState
    val acquireRung = launch(coroutineContext) {
        Carriage.animateToPose(Carriage.Pose.CLIMB)
        suspendUntil { Driver.acquireRung }
        launch(this@Command.coroutineContext) {
            LEDController.state = ClimbGoState
            periodic {
                val isAcquiringRung = // Carriage.Lifter.setpoint < Carriage.Pose.CLIMB_ACQUIRE_RUNG.lifterHeight + 2.0 &&
                        (Game.matchTime > 5.0 || Game.matchTime == -1.0)

                val deploy = SmartDashboard.getBoolean("Deploy Wings", true) &&
                        isAcquiringRung &&
                        Game.isEndGame

                Wings.wingsDeployed = deploy

                if (!isAcquiringRung) {
                    Drivetrain.drive(0.0, 0.0, 0.0)
                    LEDController.state = ClimbStopState
                } else if (deploy) {
                    Drivetrain.drive(-0.1, 0.0, 0.0)
                    LEDController.state = ClimbGoState
                } else if (!deploy) {
                    LEDController.state = ClimbStopState
                }
            }
        }
        Carriage.animateToPose(Carriage.Pose.CLIMB_ACQUIRE_RUNG)
    }


    try {
        Wings.climbingGuideDeployed = true
        periodic(condition = { acquireRung.isActive }) {
            Drivetrain.drive(Driver.throttle, Driver.softTurn, Driver.hardTurn)
        }
        println("Stage 2")
        Drivetrain.drive(-0.1, 0.0, 0.0)
        LEDController.state = ClimbStopState
        Carriage.Lifter.isLowGear = true

        Carriage.setAnimation(Carriage.Pose.FACE_THE_BOSS)

        val timer = Timer()
        timer.start()
        var previousTime = 0.0
        periodic {
            val time = timer.get()
            val input = CoDriver.leftStickUpDown
            if(input == 0.0) {
                Carriage.Lifter.isBraking = true
                Carriage.Lifter.stop()
            } else {
                Carriage.Lifter.isBraking = false
                Carriage.adjustAnimationTime((time - previousTime) * input)
            }

            previousTime = time
        }
    } finally {
        Wings.climbingGuideDeployed = false
        Carriage.Lifter.isLowGear = false
        Carriage.Lifter.isBraking = false
        Carriage.Lifter.heightRawSpeed = 0.0
        Wings.wingsDeployed = false
        Drivetrain.drive(0.0, 0.0, 0.0)
        resetClimbCommand.launch()
    }
}

val resetClimbCommand = Command("Reset Climb", Drivetrain, Carriage, Wings) {
    Carriage.animateToPose(Carriage.Pose.CLIMB)
}