package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.Compressor
import edu.wpi.first.wpilibj.PowerDistributionPanel

object RobotMap {

    val pdp = PowerDistributionPanel()
    val compressor = Compressor()

    object Talons {
        const val RIGHT_DRIVE_MOTOR_1 = 15
        const val RIGHT_DRIVE_MOTOR_2 = 14
        const val RIGHT_DRIVE_MOTOR_3 = 13
        const val LEFT_DRIVE_MOTOR_1 = 0
        const val LEFT_DRIVE_MOTOR_2 = 1
        const val LEFT_DRIVE_MOTOR_3 = 2


        const val ELEVATOR_MOTOR_1 = 7
        const val ELEVATOR_MOTOR_2 = 4
        const val ELEVATOR_MOTOR_3 = 6
        const val ELEVATOR_MOTOR_4 = 3

        const val ARM_MOTOR_1 = 11
        const val ARM_MOTOR_2 = 12
        const val INTAKE_MOTOR_LEFT = 9
        const val INTAKE_MOTOR_RIGHT = 8

    }

    object Solenoids {
        const val WINGS = 0

        const val CLIMBING_GUIDE = 7

        const val BRAKE = 3 // good

        const val CARRIAGE_SHIFT = 6 // good

        const val TENSION = 1

        const val INTAKE_CLAW = 2
    }

    object DIO {
        const val CLIMB_BUTTON = 0
    }
}