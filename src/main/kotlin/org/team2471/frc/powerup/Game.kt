package org.team2471.frc.powerup

import edu.wpi.first.wpilibj.DriverStation

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
    val isEndGame: Boolean
        get() = DriverStation.getInstance().matchTime <= 28

    var switchSide: Side? = null
        private set

    var scaleSide: Side? = null
        private set

    fun updateGameData() {
        val gameData = DriverStation.getInstance().gameSpecificMessage
        if (gameData.length < 2) {
            DriverStation.reportError("Invalid game data received ($gameData)", false)
            switchSide = null
            scaleSide = null
            return
        }

        switchSide = Side.fromChar(gameData[0])
        scaleSide = Side.fromChar(gameData[1])
    }
}