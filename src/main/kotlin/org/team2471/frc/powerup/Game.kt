package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.DriverStation
import org.team2471.frc.lib.util.Alliance
import org.team2471.frc.lib.util.measureTimeFPGA

enum class Side {
    LEFT,
    RIGHT,
    CENTER;

    operator fun not(): Side = when (this) {
        LEFT -> RIGHT
        RIGHT -> LEFT
        CENTER -> CENTER
    }

    companion object {
        fun fromChar(char: Char): Side? = when (char) {
            'L' -> LEFT
            'R' -> RIGHT
            'C' -> CENTER
            else -> null
        }
    }
}

object Game {
    private val ds = DriverStation.getInstance()

    val matchTime: Double
        get() = ds.matchTime

    val isEndGame: Boolean
        get() = matchTime <= 28

    var switchSide: Side? = null
        private set

    var scaleSide: Side? = null
        private set

    var alliance: Alliance? = null

    fun updateGameData() {
        val findDsTime = measureTimeFPGA {
            while (!ds.isDSAttached) {
                Thread.sleep(100)
            }
        }
        println("Found driverstation in $findDsTime seconds")

        val gameData = ds.gameSpecificMessage

        if (gameData.length < 2) {
            DriverStation.reportError("Invalid game data received ($gameData)", false)
            switchSide = null
            scaleSide = null
            return
        }

        switchSide = Side.fromChar(gameData[0])
        scaleSide = Side.fromChar(gameData[1])

        val dsAlliance = ds.alliance
        println("Game data alliance: $dsAlliance")
        alliance = when (dsAlliance) {
            DriverStation.Alliance.Red -> Alliance.RED
            DriverStation.Alliance.Blue -> Alliance.BLUE
            else -> null
        }
        println("Set alliance: $alliance")
    }
}