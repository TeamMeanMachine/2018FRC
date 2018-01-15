package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.PowerDistributionPanel

object RobotMap {
    val pdp = PowerDistributionPanel()

    object Talons {
        val DRIVE_LEFT_MOTOR_1 = 0
        val DRIVE_LEFT_MOTOR_2 = 1
        val DRIVE_LEFT_MOTOR_3 = 2
        val DRIVE_LEFT_MOTOR_4 = 3
        val DRIVE_RIGHT_MOTOR_1 = 15
        val DRIVE_RIGHT_MOTOR_2 = 14
        val DRIVE_RIGHT_MOTOR_3 = 13
        val DRIVE_RIGHT_MOTOR_4 = 12

        val ELEVATOR_MOTOR = 9

        val INTAKE_MOTOR = 6

        val ARM_SHOULDER_MOTOR_1 = 7
        val ARM_SHOULDER_MOTOR_2 = 8
    }

    object Solenoids {
        val ARM_CLAMP = 1
    }
}