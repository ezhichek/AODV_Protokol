package lora;

import aodv.RouteRequest;
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

public class LoraSimulator extends DefaultSerialEventHandler {

    private static final String AT_OK = "AT,OK";

    private static final String ERR_PARA = "ERR:PARA";
    private static final String ERR_CMD = "ERR:CMD";

    private final Map<String, Function<String, String>> commands = new HashMap<>() {{
        put("AT", LoraSimulator.this::onAt);
        put("AT+RST", LoraSimulator.this::onRst);
        put("AT+VER", LoraSimulator.this::onVer);
        put("AT+IDLE", LoraSimulator.this::onIdle);
        put("AT+SLEEP", LoraSimulator.this::onSleep);
        put("AT+RX", LoraSimulator.this::onRx);
        put("AT+RSSI?", LoraSimulator.this::onRssi);
        put("AT+ADDR", LoraSimulator.this::onSetAddr);
        put("AT+ADDR?", LoraSimulator.this::onGetAddr);
        put("AT+DEST", LoraSimulator.this::onSetDest);
        put("AT+DEST?", LoraSimulator.this::onGetDest);
        put("AT+CFG", LoraSimulator.this::onCfg);
        put("AT+SAVE", LoraSimulator.this::onSafe);
        put("AT+SEND", LoraSimulator.this::onSend);
        put("AT+PB0", LoraSimulator.this::onSetPb0);
        put("AT+PB0?", LoraSimulator.this::onGetPb0);
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
                connector = new SerialConnector(SerialPort.getCommPort(s), new DefaultSerialEventHandler());
                try {
                    connector.connect();
                } catch (Exception e) {
                    System.out.println("Failed to connect to port: " + e.getMessage());
                }
            }
        }
    }

    @Override
    protected void handleSendBytes(byte[] bytes) {
        System.out.println("Received " + bytes.length + " bytes");
        try {
            connector.sendCommand("AT,SENDING");
            if (RouteRequest.isRouteRequest(bytes)) {

            }



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

    private String onAt(String command) {
        return AT_OK;
    }

    private String onRst(String command) {
        return AT_OK;
    }

    private String onVer(String command) {
        return "AT,V0.3,OK";
    }

    private String onIdle(String command) {
        return AT_OK;
    }

    private String onSleep(String command) {
        try {
            final int seconds = Integer.parseInt(StringUtils.substringAfter(command, "="));
            executor.schedule(() -> {
                try {
                    connector.sendCommand("AT,WakeUp");
                } catch (IOException e) {
                    System.out.println("Failed to send reply 'AT,WakeUp': " + e.getMessage());
                }
            }, seconds, TimeUnit.SECONDS);
            return AT_OK;
        } catch (Exception e) {
            return ERR_PARA;
        }
    }

    private String onRx(String command) {
        return AT_OK;
    }

    private String onRssi(String command) {
        return "AT,-063,OK";
    }

    private String onSetAddr(String command) {
        try {
            this.address = parseAddr(StringUtils.substringAfter(command, "="));
            return AT_OK;
        } catch (RuntimeException e) {
            return ERR_PARA;
        }
    }

    private String onGetAddr(String command) {
        return "AT," + formatAddr(address) + ",OK";
    }

    private String onSetDest(String command) {
        try {
            this.destination = parseAddr(StringUtils.substringAfter(command, "="));
            return AT_OK;
        } catch (RuntimeException e) {
            return ERR_PARA;
        }
    }

    private String onGetDest(String command) {
        return "AT," + formatAddr(destination) + ",OK";
    }

    private String onCfg(String command) {
        return AT_OK;
    }

    private String onSafe(String command) {
        return AT_OK;
    }

    private String onSend(String command) {
        int sendBytes;
        try {
            sendBytes = Integer.parseInt(StringUtils.substringAfter(command, "="));
        } catch (Exception e) {
            return ERR_PARA;
        }
        if (sendBytes < 1 || sendBytes > 250) {
            return ERR_PARA;
        }
        switchToSendMode(sendBytes);
        return AT_OK;
    }

    private String onSetPb0(String command) {
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

    private String onGetPb0(String command) {
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
        new LoraSimulator().start();
    }

}
