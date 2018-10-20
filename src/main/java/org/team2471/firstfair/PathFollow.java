package org.team2471.firstfair;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.command.Scheduler;
import org.team2471.frc.lib.motion_profiling.Autonomi;
import org.team2471.frc.lib.motion_profiling.Autonomous;
import org.team2471.frc.lib.motion_profiling.Path2D;

class PathFollow extends TimedRobot {
    public static DriveTrain drive;

    public void robotInit() {
        drive = new DriveTrain();
    }

    public void autonomousInit() {
        Autonomi autonomi = new Autonomi();
        Autonomous autonomous = autonomi.get("Tests");
        Path2D path = autonomous.get("8 Foot");
        FollowPathCommand command = new FollowPathCommand( path );
        command.start();
    }

    public void autonomousPeriodic() {
        Scheduler.getInstance().run();
    }
}
