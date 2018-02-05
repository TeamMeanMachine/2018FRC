package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.IterativeRobot
import org.team2471.frc.lib.motion_profiling.Autonomi

class Robot : IterativeRobot() {

    var autonomi: Autonomi? = null

    override fun robotInit() {
    }

    override fun autonomousInit() {
        super.autonomousInit()

        autonomi = Autonomi.initFromNetworkTables()
    }
}
