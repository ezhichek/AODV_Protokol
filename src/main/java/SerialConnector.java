import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import java.io.IOException;

public class SerialConnector implements SerialPortDataListener {

    private static final char DELIMITER = '\n';

    private final SerialPort port;

    private final StringBuilder builder = new StringBuilder();

    public SerialConnector(SerialPort port) {
        this.port = port;
    }

    public void connect() {
        if (!port.isOpen()) {
            port.openPort(3000);
            port.addDataListener(this);
            System.out.println("Opened port: " + port.getDescriptivePortName());
        }
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
        handleMessage(new String(bytes));
    }

    private void handleMessage(String message) {
        final int idx = message.indexOf(DELIMITER);
        if (idx >= 0) {
            builder.append(message, 0, idx);
            System.out.println(builder);
            builder.setLength(0);
            builder.append(message, idx + 1, message.length());
        } else {
            builder.append(message);
        }
    }

 }
