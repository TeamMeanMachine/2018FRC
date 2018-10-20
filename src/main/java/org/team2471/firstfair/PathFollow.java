package org.team2471.firstfair;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.command.Scheduler;
import org.team2471.frc.lib.motion_profiling.Autonomi;
import org.team2471.frc.lib.motion_profiling.Autonomous;
import org.team2471.frc.lib.motion_profiling.Path2D;

public class PathFollow extends TimedRobot {
    public static DriveTrain drive;

    private NetworkTableEntry autoEntry = NetworkTableInstance.getDefault()
            .getTable("PathVisualizer")
            .getEntry("Autonomi");

    public void robotInit() {
        drive = new DriveTrain();
    }

    public void autonomousInit() {
        Autonomi autonomi = Autonomi.fromJsonString(autoEntry.getString(""));

        // get autonomous from autonomi
        Autonomous testsAutonomous = autonomi.get("Tests");
        // get path from autonomous
        Path2D path = testsAutonomous.get("8 Foot Straight");

        // create follow path command
        Command command = new FollowPathCommand(path);
        // start command
        command.start();

        /*
        Autonomous autonomous = new Autonomous("My Tests");
        Path2D path = new Path2D("8 Foot Straight");
        path.setAutonomous(autonomous);
        path.setRobotDirection(Path2D.RobotDirection.BACKWARD);

        path.addPointAndTangent(0.0, 0.0, 0.0, 8.0);
        path.addPointAndTangent(4.0, 8.0, 0.0, 8.0);
        path.addEasePoint(0.0, 0.0);
        path.addEasePoint(5.0, 1.0);
        */
    }

    public void autonomousPeriodic() {
        Scheduler.getInstance().run();
    }
}
