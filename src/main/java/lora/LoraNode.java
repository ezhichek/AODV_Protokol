package lora;

import aodv.*;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoraNode implements SerialPortDataListener, RoutingCallback {

    private final SerialPort port;

    private final AodvRouter router = new AodvRouterImpl(0, this);

    private final StringBuilder messageBuilder = new StringBuilder();

    private final ExecutorService commandExecutor = Executors.newSingleThreadExecutor();

    private final Object lock = new Object();

    private String lastResponse;

    public LoraNode(SerialPort port) {
        this.port = port;
    }

    public void connect() {
        if (!port.isOpen() && !port.openPort(2000)) {
            throw new RuntimeException();
        }
        port.addDataListener(this);
        System.out.println("Opened port: " + port.getDescriptivePortName());
    }

    public String sendMessage(String message) {
        try {
            final String response = commandExecutor.submit(() -> sendAndAwaitResponse(message)).get();
            if (message.startsWith("AT+ADDR?")) {
                final int address = Integer.parseInt(StringUtils.substringAfter(response, "="), 16);
                router.setAddress(address);
            }
            return response;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private String sendAndAwaitResponse(String message) {
        try {
            synchronized (lock) {
                lastResponse = null;
                port.getOutputStream().write((message + "\r\n").getBytes());
                port.getOutputStream().flush();
                while (lastResponse == null) {
                    wait();
                }
                if (lastResponse.startsWith("AT,SENDING")) {
                    System.out.println(lastResponse);
                    lastResponse = null;
                    while (lastResponse == null) {
                        wait();
                    }
                }
                return lastResponse;
            }
        } catch (IOException e) {
            return "Failed to send message: " + e.getMessage();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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
        final String[] parts = new String(bytes).split("(?<=\n)");
        for (String part : parts) {
            final int idx = part.indexOf('\n');
            if (idx >= 0) {
                messageBuilder.append(part, 0, idx);
                receiveMessage(messageBuilder.toString().toUpperCase().trim());
                messageBuilder.setLength(0);
                messageBuilder.append(part, idx + 1, part.length());
            } else {
                messageBuilder.append(part);
            }
        }
    }

    private void receiveMessage(String message) {
        if (message.startsWith("LR")) {
            handleAsyncMessage(message);
        } else if (message.startsWith("AT")) {
            handleResponse(message);
        } else {
            System.out.println("Unexpected message: " + message);
        }
    }

    private void handleAsyncMessage(String message) {

        if (router.getAddress() == 0) {
            System.out.println("Router is not initialized. Execute AT+ADDR? once!");
            return;
        }

        final String[] parts = StringUtils.split(message, ',');

        final int address = Integer.parseInt(parts[1].trim());

        final byte[] bytes = Base64.getDecoder().decode(parts[3]);

        try {

            if (UserData.isUserData(bytes)) {

                System.out.println("Received user data");
                final UserData data = UserData.parse(bytes);
                if (data.getDestinationAddress() == router.getAddress()) {
                    System.out.println(new String(data.getData()));
                } else {
                    router.processUserData(data, address);
                }

            } else if (RouteRequest.isRouteRequest(bytes)) {

                System.out.println("Received route request");
                final RouteRequest request = RouteRequest.parse(bytes);
                router.processRouteRequest(request, address);

            } else if (RouteReply.isRouteReply(bytes)) {

                System.out.println("Received route reply");
                final RouteReply reply = RouteReply.parse(bytes);
                router.processRouteReply(reply, address);

            } else {

                System.out.println("Received unsupported message: " + parts[3]);
            }

        } catch (IOException e) {
            System.out.println("Failed to parse message");
        }
    }

    private void handleResponse(String message) {
        synchronized (lock) {
            lastResponse = message;
            lock.notifyAll();
        }
    }

    @Override
    public void send(Message message, int destination) {

        try {

            final byte[] bytes = message.serialize();
            final String messageStr = Base64.getEncoder().encodeToString(bytes);

            String response = sendMessage("AT+DEST=" + String.format("%04X", destination));
            System.out.println(response);
            if (!response.startsWith("AT,OK")) {
                return;
            }

            response = sendMessage("AT+SEND=" + String.format("%02X", messageStr.length()));
            System.out.println(response);
            if (!response.startsWith("AT,OK")) {
                return;
            }

            response = sendMessage(messageStr);
            System.out.println(response);

        } catch (IOException e) {
            System.out.println("Failed to send message: " + e.getMessage());
        }
    }

    @Override
    public void onError(String msg) {
        System.out.println(msg);
    }
}
