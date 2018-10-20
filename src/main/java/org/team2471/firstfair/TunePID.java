package org.team2471.firstfair;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;
import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import org.team2471.frc.powerup.RobotMap.Talons;

public class TunePID extends TimedRobot {
    private TalonSRX driveMotorL1;
    private TalonSRX driveMotorR1;
    private XboxController xboxController;
    private final int TICKS_PER_FOOT = 600;

    @Override
    public void robotInit() {
        driveMotorL1 = new TalonSRX(Talons.LEFT_DRIVE_MOTOR_1);
        driveMotorR1 = new TalonSRX(Talons.RIGHT_DRIVE_MOTOR_1);

        TalonSRX driveMotorL2 = new TalonSRX(Talons.LEFT_DRIVE_MOTOR_2);
        TalonSRX driveMotorL3 = new TalonSRX(Talons.LEFT_DRIVE_MOTOR_3);
        TalonSRX driveMotorR2 = new TalonSRX(Talons.RIGHT_DRIVE_MOTOR_2);
        TalonSRX driveMotorR3 = new TalonSRX(Talons.RIGHT_DRIVE_MOTOR_3);

        driveMotorL2.set(ControlMode.Follower, Talons.LEFT_DRIVE_MOTOR_1);
        driveMotorL3.set(ControlMode.Follower, Talons.LEFT_DRIVE_MOTOR_1);
        driveMotorR2.set(ControlMode.Follower, Talons.RIGHT_DRIVE_MOTOR_1);
        driveMotorR3.set(ControlMode.Follower, Talons.RIGHT_DRIVE_MOTOR_1);

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

        xboxController = new XboxController(0);
    }

    @Override
    public void teleopInit() {
        // reset encoder positions
        driveMotorL1.setSelectedSensorPosition(0, 0, 10);
        driveMotorR1.setSelectedSensorPosition(0, 0, 10);
    }

    @Override
    public void teleopPeriodic() {
        driveMotorL1.config_kP(0, SmartDashboard.getNumber("Drive Position KP", 1.0), 0);
        driveMotorL1.config_kI(0, SmartDashboard.getNumber("Drive Position KI", 0), 0);
        driveMotorL1.config_kD(0, SmartDashboard.getNumber("Drive Position KD", 0), 0);

        driveMotorR1.config_kP(0, SmartDashboard.getNumber("Drive Position KP", 1.0), 0);
        driveMotorR1.config_kI(0, SmartDashboard.getNumber("Drive Position KI", 0), 0);
        driveMotorR1.config_kD(0, SmartDashboard.getNumber("Drive Position KD", 0), 0);

        double drivePosition = -xboxController.getY(GenericHID.Hand.kLeft) * TICKS_PER_FOOT;
        driveMotorL1.set(ControlMode.Position, drivePosition);
        driveMotorR1.set(ControlMode.Position, drivePosition);

        System.out.println("Power: " + driveMotorL1.getMotorOutputPercent() + " " + driveMotorR1.getMotorOutputPercent());
    }
}
