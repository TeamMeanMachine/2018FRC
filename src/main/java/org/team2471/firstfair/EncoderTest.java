package org.team2471.firstfair;

import com.ctre.phoenix.motorcontrol.can.TalonSRX;
import edu.wpi.first.wpilibj.TimedRobot;
import org.team2471.frc.powerup.RobotMap.Talons;

public class EncoderTest extends TimedRobot {
    private TalonSRX driveMotorL1;
    private TalonSRX driveMotorR1;

    public void robotInit() {
        driveMotorL1 = new TalonSRX(Talons.LEFT_DRIVE_MOTOR_1);
        driveMotorR1 = new TalonSRX(Talons.RIGHT_DRIVE_MOTOR_1);

        driveMotorL1.setSensorPhase(true);

        driveMotorL1.setSelectedSensorPosition(0, 0, 10);
        driveMotorR1.setSelectedSensorPosition(0, 0, 10);
    }

    public void disabledPeriodic() {
        System.out.println("ticks: " + driveMotorL1.getSelectedSensorPosition(0) + " " + driveMotorR1.getSelectedSensorPosition(0));
    }
}
