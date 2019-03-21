package org.team2471.frc.powerup

import com.ctre.phoenix.motorcontrol.can.TalonSRX
import edu.wpi.first.wpilibj.Compressor
import edu.wpi.first.wpilibj.PowerDistributionPanel
import edu.wpi.first.wpilibj.Solenoid

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

        const val ELEVATOR_MOTOR_1 = 6
        const val ELEVATOR_MOTOR_2 = 4
        const val ELEVATOR_MOTOR_3 = 5
        const val ELEVATOR_MOTOR_4 = 3


        val elevatorMotor1 = TalonSRX(ELEVATOR_MOTOR_1)
        val elevatorMotor2 = TalonSRX(ELEVATOR_MOTOR_2)
        val elevatorMotor3 = TalonSRX(ELEVATOR_MOTOR_3)
        val elevatorMotor4 = TalonSRX(ELEVATOR_MOTOR_4)

        const val ARM_MOTOR_1 = 12
        const val INTAKE_MOTOR_LEFT = 10
        const val INTAKE_MOTOR_RIGHT = 9
    }

    object Solenoids {
        const val WINGS = 0
        const val CLIMBING_GUIDE = 7
        const val BRAKE = 3
        const val CARRIAGE_SHIFT = 6
        const val INTAKE_CLAW = 2

        val discBrake = Solenoid(BRAKE)
        val shifter = Solenoid(CARRIAGE_SHIFT)
    }
}