package org.team2471.frc.powerup.carriage

import edu.wpi.first.networktables.NetworkTableInstance
import edu.wpi.first.wpilibj.RobotState
import edu.wpi.first.wpilibj.Timer
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import org.team2471.frc.lib.control.experimental.CommandSystem
import org.team2471.frc.lib.control.experimental.periodic
import org.team2471.frc.lib.motion_profiling.MotionCurve
import org.team2471.frc.powerup.RobotMap
import kotlin.math.max
import kotlin.math.min

object Carriage {
    val table = NetworkTableInstance.getDefault().getTable("Carriage")

    @Suppress("LiftReturnOrAssignment")
    private var animationTime = 0.0
        set(value) = when {
            value <= 0.0 -> field = 0.0
            value >= max(Arm.curve.length, Lifter.curve.length) -> field = max(Arm.curve.length, Lifter.curve.length)
            else -> field = value
        }

    var targetPose = Pose.STARTING_POSITION
        private set

    fun adjustAnimationTime(dt: Double, heightOffset: Double = 0.0) {
        animationTime += dt

        val lifterSetpoint = Lifter.curve.getValue(animationTime) + heightOffset
        val armSetpoint = Arm.curve.getValue(animationTime)
        val armVelocity = if (dt != 0.0) {
            (Arm.curve.getValue(animationTime + dt) - armSetpoint) / dt
        } else {
            0.0
        }

        Lifter.set(lifterSetpoint, 0.0)
        Arm.set(armSetpoint, armVelocity)
    }

    val isAnimationCompleted: Boolean
        get() = animationTime >= max(Lifter.curve.length, Arm.curve.length)

    fun setAnimation(pose: Pose, lifterTime: Double = 1.5, armTime: Double = 1.5,
                     lifterTimeOffset: Double = 0.0, armTimeOffset: Double = 0.0,
                     heightOffset: Double = 0.0, angleOffset: Double = 0.0) {
        targetPose = pose
        Lifter.curve = MotionCurve()
        Arm.curve = MotionCurve()
        Lifter.curve.storeValue(lifterTimeOffset, Lifter.height)
        Lifter.curve.storeValue(lifterTime + lifterTimeOffset,
                min(pose.lifterHeight + heightOffset, CarriageConstants.LIFTER_MAX_HEIGHT))

        val armEndTime = armTime + armTimeOffset
        val armStartPosition = Arm.angle
        val armEndPosition = pose.armAngle
        Arm.curve.storeValue(armTimeOffset, armStartPosition) //0.5
        Arm.curve.storeValue(armEndTime, armEndPosition + angleOffset) //2.0

        animationTime = 0.0
    }

    suspend fun animateToPose(pose: Pose, heightOffset: Double = 0.0, angleOffset: Double = 0.0) {
        val lifterDelta = pose.lifterHeight + heightOffset - Lifter.height
        val armDelta = pose.armAngle + angleOffset - Arm.angle
        val lifterTime = (1.25 / 58.0) * (Math.abs(lifterDelta)) + 0.25
        val armSpeed = 1.1 //if (Arm.hasCube) 1.4 else 1.0
        val armTime = (armSpeed / 180.0) * Math.abs(armDelta) + 0.25
        var lifterTimeOffset = 0.0
        var armTimeOffset = 0.0

        if (lifterDelta > 0.0 && armTime < lifterTime) { // going up and arm time is shorter
            armTimeOffset = lifterTime - armTime
        }
        if (lifterDelta < 0.0 && armTime > lifterTime) { // going down and arm time is longer
            lifterTimeOffset = armTime - lifterTime
        }

        if (pose != targetPose || isAnimationCompleted) {
            setAnimation(pose, lifterTime, armTime, lifterTimeOffset, armTimeOffset, heightOffset, angleOffset)
        }

        val timer = Timer()
        timer.start()
        var previousTime = 0.0
        Lifter.isBraking = false
        periodic(condition = { !isAnimationCompleted }) {
            val t = timer.get()
            adjustAnimationTime(t - previousTime)
            previousTime = t
            SmartDashboard.putNumber("Arm Amperage", RobotMap.pdp.getCurrent(RobotMap.Talons.ARM_MOTOR_1))
        }
        println("Animation took ${timer.get()} seconds")
    }

    init {
        Lifter
        Arm
        CommandSystem.registerDefaultCommand(this, carriageDefaultCommand)
    }
}