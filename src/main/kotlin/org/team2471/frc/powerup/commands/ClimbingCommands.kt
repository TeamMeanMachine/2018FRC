package org.team2471.frc.powerup.commands

import edu.wpi.first.wpilibj.Timer
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import kotlinx.coroutines.experimental.launch
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.control.experimental.suspendUntil
import org.team2471.frc.powerup.*
import org.team2471.frc.powerup.subsystems.Carriage
import org.team2471.frc.powerup.subsystems.Drivetrain
import org.team2471.frc.powerup.subsystems.Wings


val climbCommand = Command("Climb", Carriage, Drivetrain, Wings, LEDController) {
    LEDController.state = ClimbStopState
    val acquireRung = launch(coroutineContext) {
        Carriage.animateToPose(Carriage.Pose.CLIMB)
        suspendUntil { Driver.acquireRung }
        launch(this@Command.coroutineContext) {
            periodic {
                val deploy = SmartDashboard.getBoolean("Deploy Wings", true) &&
                        Carriage.Lifter.height < Carriage.Pose.CLIMB.lifterHeight - 12.0 &&
                        Game.isEndGame

                if (Game.matchTime < 5.0) {
                    LEDController.state = ClimbStopState // TODO: fast pulsing
                } else if (LEDController.state != ClimbStopState &&
                        Carriage.Lifter.setpoint != Carriage.Pose.CLIMB_ACQUIRE_RUNG.lifterHeight) {
                    LEDController.state = ClimbStopState
                } else if (deploy && LEDController.state != ClimbGoState) {
                    LEDController.state = ClimbGoState
                } else if (!deploy && LEDController.state == ClimbGoState) {
                    LEDController.state = ClimbStopState
                }

                Wings.wingsDeployed = deploy
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
        LEDController.state = ClimbStopState
        Drivetrain.drive(0.2, 0.0, 0.0)
        Carriage.Lifter.isLowGear = true



        Carriage.setAnimation(Carriage.Pose.FACE_THE_BOSS)

        val timer = Timer()
        timer.start()
        var previousTime = 0.0
        periodic {
            val time = timer.get()
            val input = CoDriver.leftStickUpDown
            Carriage.adjustAnimationTime((time - previousTime) * input)

            Carriage.Lifter.isBraking = input == 0.0

            previousTime = time
        }
    } finally {
        Wings.climbingGuideDeployed = false
        Carriage.Lifter.isLowGear = false
        Carriage.Lifter.isBraking = true
        Carriage.Lifter.heightRawSpeed = 0.0
        Wings.wingsDeployed = false
        Drivetrain.drive(0.0, 0.0, 0.0)
    }
}