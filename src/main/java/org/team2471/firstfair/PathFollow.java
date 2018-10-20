package org.team2471.firstfair;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.command.Scheduler;
import org.team2471.frc.lib.motion_profiling.Autonomi;
import org.team2471.frc.lib.motion_profiling.Autonomous;
import org.team2471.frc.lib.motion_profiling.Path2D;

public class PathFollow extends TimedRobot {
    public static DriveTrain drive;

    public void robotInit() {
        drive = new DriveTrain();
    }

    public void autonomousInit() {
//        Autonomi autonomi = new Autonomi();
//        Autonomous autonomous = autonomi.get("Tests");
//        Path2D path = autonomous.get("8 Foot");

        Autonomous autonomous = new Autonomous("Tests");
        Path2D path = new Path2D("8 foot");
        path.setAutonomous(autonomous);
        path.setRobotDirection(Path2D.RobotDirection.BACKWARD);
        path.addPointAndTangent(0.0, 0.0, 0.0, 8.0);
        path.addPointAndTangent(4.0, 8.0, 0.0, 8.0);
        path.addEasePoint(0.0, 0.0);
        path.addEasePoint(5.0, 1.0);

        FollowPathCommand command = new FollowPathCommand( path );
        command.start();
    }

    public void autonomousPeriodic() {
        Scheduler.getInstance().run();
    }
}
