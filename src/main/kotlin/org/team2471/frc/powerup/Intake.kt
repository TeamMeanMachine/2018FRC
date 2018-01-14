package org.team2471.frc.powerup

import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import edu.wpi.first.wpilibj.Solenoid
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.CommandSystem
import org.team2471.frc.powerup.RobotMap.Solenoids.ARM_CLAMP
import org.team2471.frc.powerup.RobotMap.Talons.INTAKE_MOTOR

object Intake {
    private val armClamp = Solenoid(ARM_CLAMP)
    private val intake = TalonSRX(INTAKE_MOTOR)

    private var spin:Double
        get() = intake.motorOutputVoltage
        set(value) = intake.set(ControlMode.PercentOutput, value)

    private var clamp: Boolean
        get() = armClamp.get()
        set(value) = armClamp.set(clamp)




    init {
        CommandSystem.registerDefaultCommand(this, Command("Intake Default", this) {
            spin = if (CoDriver.grab) 0.75 else 0.0
            if (CoDriver.grab) clamp = !clamp
        })
    }


}



