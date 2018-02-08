package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.PowerDistributionPanel

object RobotMap {

    val pdp = PowerDistributionPanel()

    object Talons {
        const val RIGHT_DRIVE_MOTOR_1 = 15
        const val RIGHT_DRIVE_MOTOR_2 = 14
        const val LEFT_DRIVE_MOTOR_1 = 0
        const val LEFT_DRIVE_MOTOR_2 = 1

        const val ELEVATOR_MOTOR_1 = 3
        const val ELEVATOR_MOTOR_2 = 4
        const val ELEVATOR_MOTOR_3 = 6
        const val ELEVATOR_MOTOR_4 = 7

        const val ARM_MOTOR_1 = 12
        const val ARM_MOTOR_2 = 11
        const val INTAKE_MOTOR_LEFT = 9
        const val INTAKE_MOTOR_RIGHT = 8

        const val SPARE_1 = 13
        const val SPARE_2 = 2
    }

    object Solenoids {
        const val WINGS = 1

        const val CLIMBING_GUIDE = 5

        const val BRAKE = 6

        const val CARRIAGE_SHIFT = 4

        const val TENSION = 2

        const val INTAKE_CLAW = 3
    }
}