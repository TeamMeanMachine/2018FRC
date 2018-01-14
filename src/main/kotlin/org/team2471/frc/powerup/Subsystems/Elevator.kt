package org.team2471.frc.powerup.Subsystems

import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.FeedbackDevice
import com.ctre.phoenix.motorcontrol.NeutralMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import org.team2471.frc.lib.comm.ClockServer.init
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.CommandSystem
import org.team2471.frc.lib.control.plus

import org.team2471.frc.powerup.RobotMap

object Elevator {
    private val bottomElevatorMotors = TalonSRX(RobotMap.Talons.ELEVATOR_LEFT_BOTTOM_MOTOR).apply {
        setNeutralMode(NeutralMode.Brake)
        configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 0, 10)

    } + TalonSRX(RobotMap.Talons.ELEVATOR_RIGHT_BOTTOM_MOTOR).apply {
        setNeutralMode(NeutralMode.Brake)
        //inverted = true

    }
    private val topElevatorMotors = TalonSRX(RobotMap.Talons.ELEVATOR_LEFT_TOP_MOTOR).apply {
        setNeutralMode(NeutralMode.Brake)
        configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 1, 10)
    } + TalonSRX(RobotMap.Talons.ELEVATOR_RIGHT_TOP_MOTOR).apply {
        setNeutralMode(NeutralMode.Brake)
        //inverted = true
    }
    private val wristMotors = TalonSRX(RobotMap.Talons.ELEVATOR_WRIST_LEFT_MOTOR).apply {
        setNeutralMode(NeutralMode.Brake)
        configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Relative, 2, 10)
    } + TalonSRX(RobotMap.Talons.ELEVATOR_WRIST_RIGHT_MOTOR).apply {
        setNeutralMode(NeutralMode.Brake)
        //inverted = true
    }

    private var wristAngle: Double
        get() = (wristMotors.getSelectedSensorPosition(2)).toDouble()
        set(value) = wristMotors.set(ControlMode.Position, value)

    private var bottomElevator: Double
        get() = bottomElevatorMotors.getSelectedSensorPosition(1).toDouble()
        set(value) = bottomElevatorMotors.set(ControlMode.Position,value)

    private var topElevator: Double
        get() = topElevatorMotors.getSelectedSensorPosition(0).toDouble()
        set(value) = topElevatorMotors.set(ControlMode.Position, value)


    init {
        CommandSystem.registerDefaultCommand(this, Command("Elevator Default",this) {
            var wristAngle = wristAngle
        })
    }



}