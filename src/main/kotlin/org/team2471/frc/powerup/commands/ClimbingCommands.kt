package org.team2471.frc.powerup.commands

import kotlinx.coroutines.experimental.launch
import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.powerup.Driver
import org.team2471.frc.powerup.subsystems.Carriage
import org.team2471.frc.powerup.subsystems.Drivetrain
import org.team2471.frc.powerup.subsystems.Wings


val climbCommand = Command("Climb", Carriage, Drivetrain, Wings) {
    Wings.climbingGuideDeployed = true
    val elevatorToTop = launch(coroutineContext) {
        //Move elevator to top
    }


    periodic(condition = { elevatorToTop.isCompleted && Carriage.Lifter.meanAmperage > 9.5 }) {
        Drivetrain.drive(Driver.throttle, Driver.softTurn, Driver.hardTurn)
    }
}