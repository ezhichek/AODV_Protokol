import com.fazecast.jSerialComm.SerialPort;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class TestServer extends DefaultSerialEventHandler {

    private static final String AT_OK = "AT,OK";

    private static final String ERR_PARA = "ERR:PARA";
    private static final String ERR_CMD = "ERR:CMD";

    private final Map<String, Function<String, String>> commands = new HashMap<>() {{
        put("AT", TestServer.this::replyAtOk);
        put("AT+RST", TestServer.this::replyAtOk);
        put("AT+VER", TestServer.this::ver);
        put("AT+IDLE", TestServer.this::replyAtOk);
        put("AT+SLEEP", TestServer.this::sleep);
        put("AT+RX", TestServer.this::replyAtOk);
        put("AT+RSSI?", TestServer.this::rssi);
        put("AT+ADDR", TestServer.this::setAddr);
        put("AT+ADDR?", TestServer.this::getAddr);
        put("AT+DEST", TestServer.this::setDest);
        put("AT+DEST?", TestServer.this::getDest);
        put("AT+CFG", TestServer.this::replyAtOk);
        put("AT+SAVE", TestServer.this::replyAtOk);
        put("AT+SEND", TestServer.this::send);
        put("AT+PB0", TestServer.this::setPb0);
        put("AT+PB0?", TestServer.this::getPb0);
    }};

    private SerialConnector connector = null;

    private int address;

    private int destination;

    private int pb0;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public void start() {
        final Scanner scanner = new Scanner(System.in);
        while (true) {
            if (connector == null) {
                System.out.print("Please enter port or press 0 to quit: ");
                final String s = scanner.nextLine();
                if (s.trim().equals("0")) {
                    return;
                }
                connector = new SerialConnector(SerialPort.getCommPort(s), this);
                try {
                    connector.connect();
                } catch (Exception e) {
                    System.out.println("Failed to connect to port: " + e.getMessage());
                }
            }
        }
    }

    @Override
    protected void handleText(String text) {
        System.out.println("Received text: " + text);
        try {
            connector.sendCommand("AT,SENDING");
            connector.sendCommand("AT,SENDED");
        } catch (IOException e) {
            System.out.println("Failed to send reply 'SENDING/SENDED': " + e.getMessage());
        }
    }

    protected void handleCommand(String command) {
        System.out.print("Received command: " + command);
        final String commandName = StringUtils.substringBefore(command, "=");
        final Function<String, String> handler = commands.getOrDefault(commandName, this::errCmd);
        final String reply = handler.apply(command);
        System.out.println(", Replying with: " + reply);
        try {
            connector.sendCommand(reply);
        } catch (IOException e) {
            System.out.println("Failed to send reply '" + reply + "': " + e.getMessage());
        }
    }

    private String errCmd(String command) {
        return ERR_CMD;
    }

    private String replyAtOk(String command) {
        return AT_OK;
    }

    private String ver(String command) {
        return "AT,V0.3,OK";
    }

    private String sleep(String command) {
        try {
            final int seconds = Integer.parseInt(StringUtils.substringAfter(command, "="));
            executor.schedule(this::wakeUp, seconds, TimeUnit.SECONDS);
            return AT_OK;
        } catch (Exception e) {
            return ERR_PARA;
        }
    }

    private void wakeUp() {
        try {
            connector.sendCommand("AT,WakeUp");
        } catch (IOException e) {
            System.out.println("Failed to send reply 'AT,WakeUp': " + e.getMessage());
        }
    }

    private String rssi(String command) {
        return "AT,-063,OK";
    }

    private String setAddr(String command) {
        try {
            this.address = parseAddr(StringUtils.substringAfter(command, "="));
            return AT_OK;
        } catch (RuntimeException e) {
            return ERR_PARA;
        }
    }

    private String getAddr(String command) {
        return "AT," + formatAddr(address) + ",OK";
    }

    private String setDest(String command) {
        try {
            this.destination = parseAddr(StringUtils.substringAfter(command, "="));
            return AT_OK;
        } catch (RuntimeException e) {
            return ERR_PARA;
        }
    }

    private String getDest(String command) {
        return "AT," + formatAddr(destination) + ",OK";
    }

    private String send(String command) {
        int sendBytes = 0;
        try {
            sendBytes = Integer.parseInt(StringUtils.substringAfter(command, "="));
        } catch (Exception e) {
            return ERR_PARA;
        }
        if (sendBytes < 1 || sendBytes > 250) {
            return ERR_PARA;
        }
        setSendBytes(sendBytes);
        return AT_OK;
    }

    private String setPb0(String command) {
        final String flag = StringUtils.substringAfter(command, "=");
        if ("0".equals(flag)) {
            this.pb0 = 0;
        } else if ("1".equals(flag)) {
            this.pb0 = 1;
        } else {
            return ERR_PARA;
        }
        return AT_OK;
    }

    private String getPb0(String command) {
        return "AT," + pb0 + ",OK";
    }

    private static int parseAddr(String address) {
        int addr = Integer.parseInt(address, 16);
        if (addr < 0 || addr > 0xFFFF) {
            throw new RuntimeException();
        }
        return addr;
    }

    private static String formatAddr(int i) {
        return String.format("%04X", i).toUpperCase();
    }

    public static void main(String[] args) {
        new TestServer().start();
    }

}
