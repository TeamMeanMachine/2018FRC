import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.FeedbackDevice
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import edu.wpi.first.wpilibj.GenericHID
import edu.wpi.first.wpilibj.IterativeRobot
import edu.wpi.first.wpilibj.XboxController
import org.team2471.frc.lib.control.plus
import org.team2471.frc.powerup.CoDriver

class TestRobot : IterativeRobot() {
    val p = 0.003

    private lateinit var motors: TalonSRX
    private lateinit var controller: XboxController

    override fun robotInit() {
        motors = TalonSRX(org.team2471.frc.powerup.RobotMap.Talons.ARM_MOTOR_1).apply {
            configSelectedFeedbackSensor(FeedbackDevice.Analog, 0, 10)
            configContinuousCurrentLimit(10, 10)
            configPeakCurrentLimit(0, 10)
            enableCurrentLimit(true)
            configNominalOutputForward(0.0, 10)
            configNominalOutputReverse(0.0, 10)
            configNeutralDeadband(0.04, 10)
            setSensorPhase(false)
            inverted = true
        } + com.ctre.phoenix.motorcontrol.can.TalonSRX(org.team2471.frc.powerup.RobotMap.Talons.ARM_MOTOR_2).apply {
            configContinuousCurrentLimit(10, 10)
            configPeakCurrentLimit(0, 10)
            enableCurrentLimit(true)
            inverted = true
        }
        controller = XboxController(1)
    }

    override fun teleopPeriodic() {
        val setpoint = controller.getY(GenericHID.Hand.kRight) * 45 + 45
        val pos = ticksToDegrees(motors.getSelectedSensorPosition(0).toDouble())
        val error = setpoint - pos
        motors.set(ControlMode.PercentOutput, error * p)
    }

    private fun ticksToDegrees(nativeUnits: Double): Double = (nativeUnits - -730) / (20.0 / 90.0)
}