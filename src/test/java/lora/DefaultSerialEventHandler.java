package lora;

import java.nio.ByteBuffer;

public class DefaultSerialEventHandler implements SerialEventHandler {

    private final StringBuilder commandBuilder = new StringBuilder();

    private boolean sendMode;

    private ByteBuffer sendBuffer;

    public void switchToSendMode(int expectedBytes) {
        sendMode = true;
        sendBuffer = ByteBuffer.allocate(expectedBytes);
    }

    public void switchToCommandMode() {
        sendMode = false;
        sendBuffer = null;
    }

    @Override
    public void handleEvent(byte[] bytes) {
        if (sendMode) {
            handleSendEvent(bytes);
        } else {
            handleCommandEvent(bytes);
        }
    }

    protected void handleSendEvent(byte[] bytes) {
        for (int i = 0; i < bytes.length && sendBuffer.hasRemaining(); i++) {
            sendBuffer.put(bytes[i]);
        }
        if (!sendBuffer.hasRemaining()) {
            handleSendBytes(sendBuffer.array());
            switchToCommandMode();
        }
    }

    protected void handleCommandEvent(byte[] bytes) {
        final String[] parts = new String(bytes).split("(?<=\n)");
        for (String part : parts) {
            final int idx = part.indexOf('\n');
            if (idx >= 0) {
                commandBuilder.append(part, 0, idx);
                handleCommand(commandBuilder.toString().toUpperCase().trim());
                commandBuilder.setLength(0);
                commandBuilder.append(part, idx + 1, part.length());
            } else {
                commandBuilder.append(part);
            }
        }
    }

    protected void handleSendBytes(byte[] bytes) {
        System.out.println(new String(bytes));
    }

    protected void handleCommand(String command) {
        System.out.println(command);
    }

}
