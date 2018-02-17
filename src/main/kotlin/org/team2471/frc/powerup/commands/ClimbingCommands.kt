package org.team2471.frc.powerup.commands

import edu.wpi.first.wpilibj.Timer
import kotlinx.coroutines.experimental.launch
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.control.experimental.suspendUntil
import org.team2471.frc.powerup.CoDriver
import org.team2471.frc.powerup.Driver
import org.team2471.frc.powerup.subsystems.Carriage
import org.team2471.frc.powerup.subsystems.Drivetrain
import org.team2471.frc.powerup.subsystems.Wings


val climbCommand = Command("Climb", Carriage, Drivetrain, Wings) {
    val acquireRung = launch(coroutineContext) {
        Carriage.animateToPose(Carriage.Pose.CLIMB.inches, Carriage.Pose.CLIMB.armAngle)
        println("Carriage Up")
        suspendUntil { Driver.acquireRung }
        println("Button Recieved")
        Carriage.animateToPose(Carriage.Pose.CLIMB_ACQUIRE_RUNG.inches, Carriage.Pose.CLIMB_ACQUIRE_RUNG.armAngle)
        println("Arm Move Down")
    }

    try {
        Wings.climbingGuideDeployed = true
        periodic(condition = { acquireRung.isActive }) {
            Drivetrain.drive(Driver.throttle, Driver.softTurn, Driver.hardTurn)
        }
        println("Stage 2")
        Drivetrain.drive(0.0, 0.0, 0.0)
        Carriage.Lifter.isLowGear = true
        Wings.wingsDeployed = true

        Carriage.setAnimation(Carriage.Pose.FACE_THE_BOSS.inches, Carriage.Pose.FACE_THE_BOSS.armAngle)

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
        Carriage.Lifter.heightRawSpeed = 0.0
        Wings.wingsDeployed = false
        Drivetrain.drive(0.0, 0.0, 0.0)
    }


}