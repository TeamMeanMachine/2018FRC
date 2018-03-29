package org.team2471.frc.powerup.drivetrain

object DrivetrainConstants {
    // motor configuration
    const val PEAK_CURRENT_LIMIT = 20
    const val CONTINUOUS_CURRENT_LIMIT = 15
    const val PEAK_CURRENT_DURATION = 100


    // path following
    const val DISTANCE_P = 2.0 * 0.75
    const val DISTANCE_D = 0.5

//    const val LEFT_FEED_FORWARD_COEFFICIENT = 0.0863274081239196
//    const val LEFT_FEED_FORWARD_OFFSET = 0.010218260839712

//    const val RIGHT_FEED_FORWARD_COEFFICIENT = 0.078175224207892
//    const val RIGHT_FEED_FORWARD_OFFSET = 0.00081904993955539

    const val LEFT_FEED_FORWARD_COEFFICIENT = 0.070541988198899
    const val LEFT_FEED_FORWARD_OFFSET = 0.021428882425651

    const val RIGHT_FEED_FORWARD_COEFFICIENT = 0.071704891069425
    const val RIGHT_FEED_FORWARD_OFFSET = 0.020459379452296

    const val ACCELERATION_COEFFICIENT = 0.1454439814814 / 2.0 // currently unused
    const val TURNING_FEED_FORWARD = 0.03237277804646985

    const val GYRO_CORRECTION_P = 0.025 * 0.75
    const val GYRO_CORRECTION_I = 0.002 * 0.5
    const val GYRO_CORRECTION_I_DECAY = 1.0 - 0.0

    const val MAX_PATH_ERROR = 6.0


    // unit conversions
    const val TICKS_PER_REV = 795
    const val WHEEL_DIAMETER_INCHES = 5.0

    // driver ergonomics
    const val MINIMUM_OUTPUT = 0.04
}


