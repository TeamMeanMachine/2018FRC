package org.team2471.firstfair;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.command.Command;
import org.team2471.frc.lib.motion_profiling.Path2D;

public class FollowPathCommand extends Command {
    private Path2D path;
    private Timer timer = new Timer();

    public FollowPathCommand(Path2D path) {
        requires(PathFollow.drive);
        this.path = path;
    }

    @Override
    protected void initialize() {
        timer.start();
        PathFollow.drive.resetEncoderDistances();
    }

    @Override
    protected void execute() {
        double time = timer.get();
        PathFollow.drive.drivePosition(path.getLeftDistance(time), path.getRightDistance(time));
        System.out.println("Time: " + time);
    }

    @Override
    protected boolean isFinished() {
        return timer.get() >= path.getDuration();
    }

    @Override
    protected void end() {
        timer.stop();
    }
}
