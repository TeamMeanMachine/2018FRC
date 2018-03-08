package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.DriverStation
import org.team2471.frc.lib.util.Alliance

enum class Side {
    LEFT,
    RIGHT;

    operator fun not(): Side = when (this) {
        LEFT -> RIGHT
        RIGHT -> LEFT
    }

    companion object {
        fun fromChar(char: Char): Side? = when (char) {
            'L' -> LEFT
            'R' -> RIGHT
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
        val gameData = ds.gameSpecificMessage

        if (gameData.length < 2) {
            DriverStation.reportError("Invalid game data received ($gameData)", false)
            switchSide = null
            scaleSide = null
            return
        }

        switchSide = Side.fromChar(gameData[0])
        scaleSide = Side.fromChar(gameData[1])

        alliance = when (ds.alliance) {
            DriverStation.Alliance.Red -> Alliance.RED
            DriverStation.Alliance.Blue -> Alliance.BLUE
            else -> null
        }
    }
}