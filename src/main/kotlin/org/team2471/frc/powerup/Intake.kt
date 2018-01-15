package org.team2471.frc.powerup

import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import edu.wpi.first.wpilibj.Solenoid
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.CommandSystem
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.powerup.RobotMap.Solenoids.ARM_CLAMP
import org.team2471.frc.powerup.RobotMap.Talons.INTAKE_MOTOR

object Intake {
    private val armClamp = Solenoid(ARM_CLAMP)
    private val intake = TalonSRX(INTAKE_MOTOR).apply {
        configContinuousCurrentLimit(10, 10)
        configPeakCurrentLimit(15, 10)
        configPeakCurrentDuration(100, 10)
        enableCurrentLimit(true)
    }

    private var spin:Double
        get() = intake.motorOutputVoltage
        set(value) = intake.set(ControlMode.PercentOutput, value)

    private var clamp: Boolean = false
        set(value) {
            field = value
            armClamp.set(clamp)
        }


    val toggleClampCommand = Command("Toggle Clamp") {
        clamp = !clamp

    }


    init {
        CommandSystem.registerDefaultCommand(this, Command("Intake Default", this) {
            periodic {
                spin = when {
                    CoDriver.spit -> -1.0
                    CoDriver.spin -> 1.0
                    else -> 0.0
                }
            }
        })

    }


}



