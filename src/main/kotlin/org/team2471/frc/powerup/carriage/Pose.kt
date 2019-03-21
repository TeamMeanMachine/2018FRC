package org.team2471.frc.powerup.carriage

enum class Pose(val lifterHeight: Double, val armAngle: Double) {
    INTAKE(1.5, 0.0),
    SCALE_LOW(24.0, 195.0), //25.5
    SCALE_MED(40.0, 195.0), //34.0
    SCALE_HIGH(53.0, 195.0), //42.0
    SCALE_FRONT(52.0, 18.0),
    CARRY(6.0, 0.0),
    SWITCH(22.0, 20.0),
    CLIMB(63.0, 0.0),
    CLIMB_ACQUIRE_RUNG(30.0, 90.0),
    FACE_THE_BOSS(0.5, 90.0),
    STARTING_POSITION(0.0, 90.0),
    INTAKE_RAISED(13.0, 0.0);

    val isScale get() = this == SCALE_LOW || this == SCALE_MED || this == SCALE_HIGH || this == SCALE_FRONT
}