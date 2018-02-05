package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.PowerDistributionPanel

object RobotMap {

    val pdp = PowerDistributionPanel()

    object Talons {
        const val RIGHT_DRIVE_MOTOR_1 = -1
        const val RIGHT_DRIVE_MOTOR_2 = -1
        const val LEFT_DRIVE_MOTOR_1 = -1
        const val LEFT_DRIVE_MOTOR_2 = -1

        const val ELEVATOR_MOTOR_1 = -1
        const val ELEVATOR_MOTOR_2 = -1
        const val ELEVATOR_MOTOR_3 = -1
        const val ELEVATOR_MOTOR_4 = -1

        const val ARM_MOTOR_1 = -1
        const val ARM_MOTOR_2 = -1
        const val INTAKE_MOTOR_LEFT = -1
        const val INTAKE_MOTOR_RIGHT = -1

    }

    object Solenoids {
        const val ARM_CLAMP = -1

        const val FORK_RIGHT = -1
        const val FORK_LEFT = -1

        const val CLIMBING_GUIDE = -1

        const val INTAKE_CLAW = -1
    }
}