package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.Timer
import kotlinx.coroutines.experimental.delay
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.parallel
import org.team2471.frc.powerup.carriage.Arm
import org.team2471.frc.powerup.carriage.Carriage
import org.team2471.frc.powerup.drivetrain.Drivetrain


val smartSpit = Command("Back up and Roll Cube", Drivetrain, Carriage) {
    val timer = Timer()
    try {
        val scalingFactor = -0.14
        if (Arm.angle > 90) {
            parallel({
                Drivetrain.driveDistance(1.2, 1.0)
            }, {
                timer.start()
                while (timer.get() < 1.0) {
                    println("Time: ${timer.get()}")
                    val driveVoltage = Drivetrain.rightMaster.motorOutputVoltage
                    println("Drive Voltage: $driveVoltage")
                    Arm.intakeSpeed = driveVoltage * scalingFactor
                    delay(20)
                }
            })
        } else {
            parallel({
                Drivetrain.driveDistance(-1.2, 1.0)
            }, {
                timer.start()
                while (timer.get() < 1.0) {
                    println("Time: ${timer.get()}")
                    val driveVoltage = Drivetrain.rightMaster.motorOutputVoltage
                    println("Drive Voltage: $driveVoltage")
                    Arm.intakeSpeed = driveVoltage * -scalingFactor
                    delay(20)
                }
            })
        }
    } finally {
        timer.stop()
        Arm.intakeSpeed = 0.0
        Arm.isClamping = true
    }
}