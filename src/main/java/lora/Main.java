package lora;

import com.fazecast.jSerialComm.SerialPort;

import java.util.Scanner;

public class Main {

    private void start() {

        SerialConnector connector = null;

        final Scanner scanner = new Scanner(System.in);

        while (true) {
            if (connector == null) {
                System.out.print("Please enter port or press 0 to quit: ");
                final String s = scanner.nextLine();
                if (s.trim().equals("0")) {
                    return;
                }
                connector = new SerialConnector(SerialPort.getCommPort(s));
                try {
                    connector.connect();
                } catch (Exception e) {
                    System.out.println("Failed to connect to port: " + e.getMessage());
                    continue;
                }
            }
            final String command = scanner.nextLine();
            if (command.equals("0")) {
                return;
            }
            try {
                connector.sendCommand(command);
            } catch (Exception e) {
                System.out.println("Failed to send command: " + e.getMessage());
                connector = null;
            }
        }
    }

    public static void main(String[] args) {
        new Main().start();
    }
}
