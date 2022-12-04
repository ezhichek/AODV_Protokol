import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import java.io.IOException;

public class SerialConnector implements SerialPortDataListener {

    private final SerialPort port;

    private final SerialEventHandler eventHandler;

    public SerialConnector(SerialPort port) {
        this(port, new DefaultSerialEventHandler());
    }

    public SerialConnector(SerialPort port, SerialEventHandler eventHandler) {
        this.port = port;
        this.eventHandler = eventHandler;
    }

    public void connect() {
        if (!port.isOpen() && !port.openPort(2000)) {
            throw new RuntimeException();
        }
        port.addDataListener(this);
        System.out.println("Opened port: " + port.getDescriptivePortName());
    }

    public void sendCommand(String command) throws IOException {
        port.getOutputStream().write((command + "\r\n").getBytes());
        port.getOutputStream().flush();
    }

    @Override
    public int getListeningEvents() {
        return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        final SerialPort sp = event.getSerialPort();
        final byte[] bytes = new byte[sp.bytesAvailable()];
        sp.readBytes(bytes, bytes.length);
        eventHandler.handleEvent(bytes);
    }
 }
