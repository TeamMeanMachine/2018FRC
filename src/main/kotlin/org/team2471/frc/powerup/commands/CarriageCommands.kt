package org.team2471.frc.powerup.commands

import org.team2471.frc.lib.control.experimental.Command
import org.team2471.frc.powerup.subsystems.Carriage

val testLifter = Command("Test Lifter", Carriage) {
    try {
        //Carriage.playAnimation(Carriage.Animation.INTAKE_TO_SCALE)
        Carriage.playAnimation(Carriage.Animation.INITIAL_TEST)
    }
    finally {
    }
}