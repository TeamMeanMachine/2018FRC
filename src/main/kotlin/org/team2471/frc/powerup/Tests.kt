import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import edu.wpi.first.wpilibj.Timer
import kotlinx.coroutines.experimental.delay
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.powerup.RobotMap
import org.team2471.frc.powerup.carriage.Arm
import org.team2471.frc.powerup.carriage.CarriageConstants
import org.team2471.frc.powerup.drivetrain.Drivetrain

suspend fun testMotorTime(motor: TalonSRX, encoderMotor: TalonSRX, time: Double, power: Double) {
    var sampleCount = 0
    var velocityAcc = 0.0
    var currentAcc = 0.0
    try {
        motor.set(ControlMode.PercentOutput, power)
        val timer = Timer()
        timer.start()
        while (timer.get() < time) {
            velocityAcc += encoderMotor.getSelectedSensorVelocity(0)
            currentAcc += motor.outputCurrent
            sampleCount++
            delay(20)
        }
    } finally {
        val velocityFinal = velocityAcc / sampleCount
        val currentFinal = currentAcc / sampleCount
        println("Motor ${motor.deviceID} Velocity: $velocityFinal")
        println("Motor ${motor.deviceID} Current: $currentFinal")
        motor.set(ControlMode.PercentOutput, 0.0)
        if (velocityFinal < 580.0 || currentFinal > 23.0) {
            println("Potentiall" +
                    "y Bad Motor ****************************************************************************")
        }
    }
}

suspend fun testMotorDistance(motor: TalonSRX, encoderMotor: TalonSRX, distance: Double, power: Double) {
    var sampleCount = 0
    var velocityAcc = 0.0
    var currentAcc = 0.0
    try {
        motor.set(ControlMode.PercentOutput, power)
        encoderMotor.setSelectedSensorPosition(0, 0, 5)
        while (encoderMotor.getSelectedSensorPosition(0) / CarriageConstants.LIFTER_TICKS_PER_INCH < distance) {
            RobotMap.Solenoids.discBrake.set(true)
            RobotMap.Solenoids.shifter.set(true) // low gear
            velocityAcc += encoderMotor.getSelectedSensorVelocity(0)
            currentAcc += motor.outputCurrent
            sampleCount++
            delay(20)
        }
        motor.set(ControlMode.PercentOutput, 0.0)
        delay(250)

        motor.set(ControlMode.PercentOutput, -power)
        while (encoderMotor.getSelectedSensorPosition(0) / CarriageConstants.LIFTER_TICKS_PER_INCH > 2.0) {
            RobotMap.Solenoids.discBrake.set(true)
            RobotMap.Solenoids.shifter.set(true) // low gear
            velocityAcc += -encoderMotor.getSelectedSensorVelocity(0)
            currentAcc += motor.outputCurrent
            sampleCount++
            delay(20)
        }
    } finally {
        val velocityFinal = velocityAcc / sampleCount
        val currentFinal = currentAcc / sampleCount
        println("Motor ${motor.deviceID} Velocity: $velocityFinal")
        println("Motor ${motor.deviceID} Current: $currentFinal")
        motor.set(ControlMode.PercentOutput, 0.0)
        if (velocityFinal < 180.0 || currentFinal > 20.0) {
            println("Potentially Bad Motor ****************************************************************************")
        }
        delay(250)
    }
}

val preMatchTest = Command("Pre Match Test", Drivetrain, Arm) {
    val motor0 = TalonSRX(0)
    val motor1 = TalonSRX(1)
    val motor2 = TalonSRX(2)
    val motor13 = TalonSRX(13)
    val motor14 = TalonSRX(14)
    val motor15 = TalonSRX(15)

    val elevatorMotor1 = RobotMap.Talons.elevatorMotor1
    val elevatorMotor2 = RobotMap.Talons.elevatorMotor2
    val elevatorMotor3 = RobotMap.Talons.elevatorMotor3
    val elevatorMotor4 = RobotMap.Talons.elevatorMotor4
    try {
        println(1)
        println(2)
        testMotorDistance(elevatorMotor1, elevatorMotor1, 18.0, 1.0)
        println(3)
        testMotorDistance(elevatorMotor2, elevatorMotor1, 18.0, 1.0)
        println(4)
        testMotorDistance(elevatorMotor3, elevatorMotor1, 18.0, 1.0)
        println(5)
        testMotorDistance(elevatorMotor4, elevatorMotor1, 18.0, 1.0)
        println(6)
    } finally {
        motor1.set(ControlMode.Follower, 0.0)
        motor2.set(ControlMode.Follower, 0.0)
        motor0.neutralOutput()
        motor13.set(ControlMode.Follower, 15.0)
        motor14.set(ControlMode.Follower, 15.0)
        motor15.neutralOutput()

        elevatorMotor4.set(ControlMode.Follower, 6.0)
        elevatorMotor2.set(ControlMode.Follower, 6.0)
        elevatorMotor3.set(ControlMode.Follower, 6.0)
        elevatorMotor1.neutralOutput()
    }
}
