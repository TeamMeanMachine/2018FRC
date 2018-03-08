package org.team2471.frc.powerup.commands

import edu.wpi.first.wpilibj.Timer
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import kotlinx.coroutines.experimental.launch
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.control.experimental.suspendUntil
import org.team2471.frc.powerup.CoDriver
import org.team2471.frc.powerup.Driver
import org.team2471.frc.powerup.Game
import org.team2471.frc.powerup.subsystems.Carriage
import org.team2471.frc.powerup.subsystems.Drivetrain
import org.team2471.frc.powerup.subsystems.Wings


val climbCommand = Command("Climb", Carriage, Drivetrain, Wings) {
    val acquireRung = launch(coroutineContext) {
        Carriage.animateToPose(Carriage.Pose.CLIMB)
        suspendUntil { Driver.acquireRung }
        launch(this@Command.coroutineContext) {
            periodic {
                Wings.wingsDeployed = SmartDashboard.getBoolean("Deploy Wings", true) &&
                        Carriage.Lifter.height < Carriage.Pose.CLIMB.lifterHeight - 12.0 &&
                        Game.isEndGame
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