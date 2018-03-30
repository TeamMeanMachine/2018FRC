package org.team2471.frc.powerup.carriage

enum class Pose(val lifterHeight: Double, val armAngle: Double) {
    INTAKE(6.0, 0.0),
    CRITICAL_JUNCTION(24.0, 110.0),
    SCALE_LOW(22.0, 195.0),
    SCALE_MED(26.0, 195.0),
    SCALE_HIGH(35.5, 195.0),
    /*    SCALE_LOW(25.5, 190.0),
        SCALE_MED(36.0, 185.0),
        SCALE_HIGH(45.0, 185.0),*/
    SCALE_FRONT(56.0, 18.0),
    CARRY(10.0, 0.0),
    SWITCH(26.0, 20.0),
    CLIMB(58.0, 0.0),
    CLIMB_ACQUIRE_RUNG(26.0, 0.0),
    FACE_THE_BOSS(3.0, 0.0),
    STARTING_POSITION(6.0, 110.0),
    INTAKE_RAISED(17.0, 0.0);

    val isScale get() = this == SCALE_LOW || this == SCALE_MED || this == SCALE_HIGH || this == SCALE_FRONT
}