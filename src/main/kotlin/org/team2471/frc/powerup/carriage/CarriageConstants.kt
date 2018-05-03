package org.team2471.frc.powerup.carriage

object CarriageConstants {
    const val STANDARD_INTAKE_SPEED = 0.85
    const val LIFTER_MAX_HEIGHT = 61.5

    const val ARM_VELOCITY_FEED_FORWARD = 0.004 / 2.0

    const val ARM_P = 13.3 * 0.5

    const val LIFTER_UPWARD_FEED_FORWARD = 0.15
    const val LIFTER_DOWNWARD_FEED_FORWARD = 0.05
    // unit conversions
    const val LIFTER_TICKS_PER_INCH = 9437 / 64.25

    const val ARM_TICKS_PER_DEGREE = 20.0 / 9.0
    const val ARM_OFFSET_NATIVE = -720.0
}