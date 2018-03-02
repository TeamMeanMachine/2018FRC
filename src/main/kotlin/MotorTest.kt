import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import edu.wpi.first.wpilibj.GenericHID
import edu.wpi.first.wpilibj.IterativeRobot
import edu.wpi.first.wpilibj.XboxController
import org.team2471.frc.lib.math.deadband
import org.team2471.frc.powerup.RobotMap.Talons

class MotorTest : IterativeRobot() {
    private lateinit var motors: Array<TalonSRX>
    private lateinit var controller: XboxController
    private var selectedMotorIndex = 0
    private var prevPOV = -1

    override fun robotInit() {
        motors = arrayOf(
                TalonSRX(Talons.RIGHT_DRIVE_MOTOR_1),
                TalonSRX(Talons.RIGHT_DRIVE_MOTOR_2),
                TalonSRX(Talons.RIGHT_DRIVE_MOTOR_3),
                TalonSRX(Talons.LEFT_DRIVE_MOTOR_1),
                TalonSRX(Talons.LEFT_DRIVE_MOTOR_2),
                TalonSRX(Talons.LEFT_DRIVE_MOTOR_3),
                TalonSRX(Talons.ELEVATOR_MOTOR_1),
                TalonSRX(Talons.ELEVATOR_MOTOR_2),
                TalonSRX(Talons.ELEVATOR_MOTOR_3),
                TalonSRX(Talons.ELEVATOR_MOTOR_4),
                TalonSRX(Talons.ARM_MOTOR_1),
                TalonSRX(Talons.INTAKE_MOTOR_LEFT),
                TalonSRX(Talons.INTAKE_MOTOR_RIGHT)
        )

        motors[10].setSensorPhase(true)

        controller = XboxController(0)
    }

    override fun teleopPeriodic() {
        val pov = controller.pov
        if (pov != prevPOV) {
            if (pov == 0) {
                selectedMotorIndex++
                if (selectedMotorIndex > motors.lastIndex) selectedMotorIndex = 0
            } else if (pov == 180) {
                selectedMotorIndex--
                if (selectedMotorIndex < 0) selectedMotorIndex = motors.lastIndex
            }
        }
        prevPOV = pov

        val output = -controller.getY(GenericHID.Hand.kRight).deadband(0.2)
        println("Selected motor: ${motors[selectedMotorIndex].deviceID}, output: $output, Arm Encoder: ${motors[10].getSelectedSensorPosition(0)}")
        println("")

        motors.forEachIndexed { index, motor ->
            motor.set(ControlMode.PercentOutput, if (index == selectedMotorIndex) output else 0.0)
        }
    }
}