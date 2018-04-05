import edu.wpi.first.networktables.EntryListenerFlags
import edu.wpi.first.networktables.NetworkTableInstance
import edu.wpi.first.wpilibj.SampleRobot
import edu.wpi.first.wpilibj.SerialPort

class LEDTest : SampleRobot() {
    override fun robotInit() {
        val serial = SerialPort(9600, SerialPort.Port.kUSB1)


        val dataEntry = NetworkTableInstance.getDefault()
                .getTable("LEDController")
                .getEntry("Test Input")
        dataEntry.forceSetString("")

        dataEntry.addListener({ data ->
            println(data.value.string)
            serial.writeString("${data.value.string}\n")
        }, EntryListenerFlags.kUpdate)
    }
}