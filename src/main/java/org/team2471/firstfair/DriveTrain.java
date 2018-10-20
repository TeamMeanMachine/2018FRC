package org.team2471.firstfair;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import org.team2471.frc.powerup.RobotMap;

public class DriveTrain extends Subsystem {
    private TalonSRX driveMotorL1;
    private TalonSRX driveMotorR1;

    public DriveTrain() {
        driveMotorL1 = new TalonSRX(RobotMap.Talons.LEFT_DRIVE_MOTOR_1);
        driveMotorR1 = new TalonSRX(RobotMap.Talons.RIGHT_DRIVE_MOTOR_1);

        TalonSRX driveMotorL2 = new TalonSRX(RobotMap.Talons.LEFT_DRIVE_MOTOR_2);
        TalonSRX driveMotorL3 = new TalonSRX(RobotMap.Talons.LEFT_DRIVE_MOTOR_3);
        TalonSRX driveMotorR2 = new TalonSRX(RobotMap.Talons.RIGHT_DRIVE_MOTOR_2);
        TalonSRX driveMotorR3 = new TalonSRX(RobotMap.Talons.RIGHT_DRIVE_MOTOR_3);

        driveMotorL2.set(ControlMode.Follower, RobotMap.Talons.LEFT_DRIVE_MOTOR_1);
        driveMotorL3.set(ControlMode.Follower, RobotMap.Talons.LEFT_DRIVE_MOTOR_1);
        driveMotorR2.set(ControlMode.Follower, RobotMap.Talons.RIGHT_DRIVE_MOTOR_1);
        driveMotorR3.set(ControlMode.Follower, RobotMap.Talons.RIGHT_DRIVE_MOTOR_1);

        driveMotorL1.configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 1, 10);
//        driveMotorL1.setSensorPhase(true);

        driveMotorL1.setInverted(true);
        driveMotorL2.setInverted(true);
        driveMotorL3.setInverted(true);

        driveMotorR1.configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 1, 10);

        if (!SmartDashboard.containsKey("Drive Position KP")) {
            SmartDashboard.putNumber("Drive Position KP", 1.0);
            SmartDashboard.putNumber("Drive Position KI", 0.0);
            SmartDashboard.putNumber("Drive Position KD", 0.0);
        }
        SmartDashboard.setPersistent("Drive Position KP");
        SmartDashboard.setPersistent("Drive Position KI");
        SmartDashboard.setPersistent("Drive Position KD");

    }

    public void resetEncoderDistances() {
        driveMotorL1.setSelectedSensorPosition(0, 0, 10);
        driveMotorR1.setSelectedSensorPosition(0, 0, 10);
    }

    public void drivePosition(double leftPosition, double rightPosition) {
        driveMotorL1.config_kP(0, SmartDashboard.getNumber("Drive Position KP", 1.0), 0);
        driveMotorL1.config_kI(0, SmartDashboard.getNumber("Drive Position KI", 0), 0);
        driveMotorL1.config_kD(0, SmartDashboard.getNumber("Drive Position KD", 0), 0);

        driveMotorR1.config_kP(0, SmartDashboard.getNumber("Drive Position KP", 1.0), 0);
        driveMotorR1.config_kI(0, SmartDashboard.getNumber("Drive Position KI", 0), 0);
        driveMotorR1.config_kD(0, SmartDashboard.getNumber("Drive Position KD", 0), 0);

        driveMotorL1.set(ControlMode.Position, leftPosition);
        driveMotorR1.set(ControlMode.Position, rightPosition);

        System.out.println("Power: " + driveMotorL1.getMotorOutputPercent() + " " + driveMotorR1.getMotorOutputPercent());
    }

    @Override
    protected void initDefaultCommand() {
    }
}
