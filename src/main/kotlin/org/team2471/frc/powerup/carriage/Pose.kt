package org.team2471.frc.powerup.carriage

import org.team2471.frc.powerup.IS_COMP_BOT

enum class Pose(val lifterHeight: Double, val armAngle: Double) {
    INTAKE(6.0, 0.0),
    CRITICAL_JUNCTION(24.0, 110.0),
    SCALE_LOW(22.5, if (IS_COMP_BOT) 195.0 else 180.0),
    SCALE_MED(26.0, if (IS_COMP_BOT) 195.0 else 180.0),
    SCALE_HIGH(35.5, if (IS_COMP_BOT) 195.0 else 180.0),
    /*    SCALE_LOW(25.5, 190.0),
        SCALE_MED(36.0, 185.0),
        SCALE_HIGH(45.0, 185.0),*/
    SCALE_FRONT(56.0, 18.0),
    CARRY(10.0, 0.0),
    SWITCH(26.0, 20.0),
    CLIMB(60.0, 0.0),
    CLIMB_ACQUIRE_RUNG(30.0, 90.0),
    FACE_THE_BOSS(0.5, 90.0),
    STARTING_POSITION(6.0, 110.0),
    INTAKE_RAISED(17.0, 0.0);

    val isScale get() = this == SCALE_LOW || this == SCALE_MED || this == SCALE_HIGH || this == SCALE_FRONT
    val isFlipped get() = this == SCALE_LOW || this == SCALE_MED || this == SCALE_HIGH
}