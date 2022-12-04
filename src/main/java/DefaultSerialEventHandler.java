import org.apache.commons.lang3.StringUtils;

public class DefaultSerialEventHandler implements SerialEventHandler {

    private final StringBuilder builder = new StringBuilder();

    private int sendBytes;

    public int getSendBytes() {
        return sendBytes;
    }

    public void setSendBytes(int sendBytes) {
        this.sendBytes = sendBytes;
    }

    @Override
    public void handleEvent(byte[] bytes) {
        if (sendBytes > 0) {
            handleSendEvent(bytes);
        } else {
            handleCommandEvent(bytes);
        }
    }

    private void handleSendEvent(byte[] bytes) {
        final String text = new String(bytes);
        builder.append(text);
        if (builder.length() >= sendBytes) {
            handleText(StringUtils.substring(builder.toString(), 0, sendBytes));
            builder.setLength(0);
            sendBytes = 0;
        }
    }

    private void handleCommandEvent(byte[] bytes) {
        final String[] parts = new String(bytes).split("(?<=\n)");
        for (String part : parts) {
            final int idx = part.indexOf('\n');
            if (idx >= 0) {
                builder.append(part, 0, idx);
                handleCommand(builder.toString().toUpperCase().trim());
                builder.setLength(0);
                builder.append(part, idx + 1, part.length());
            } else {
                builder.append(part);
            }
        }
    }

    protected void handleText(String text) {
        System.out.println(text);
    }

    protected void handleCommand(String command) {
        System.out.println(command);
    }

}
