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

val climbCommand = Command("Climb", Drivetrain, Carriage, Wings) {
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
            val input = Driver.rightStickUpDown
            if (input == 0.0) {
                Lifter.isBraking = true
                Lifter.stop()
            } else {
                Lifter.isBraking = false
                println(input)
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