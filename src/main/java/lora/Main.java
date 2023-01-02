package lora;

import com.fazecast.jSerialComm.SerialPort;

import java.util.Scanner;

public class Main {

    private void start() {

        final SerialPort[] availablePorts = SerialPort.getCommPorts();

        for (int i = 0; i < availablePorts.length; i++) {
            System.out.println("(" + (i + 1) + ") " + availablePorts[i].getDescriptivePortName());
        }

        SerialConnector connector = null;

        final Scanner scanner = new Scanner(System.in);

        while (true) {
            if (connector == null) {
                System.out.print("Please select a port or press 0 to quit: ");
                final int i = scanner.nextInt();
                scanner.nextLine();
                if (i == 0) {
                    return;
                }
                if (i < 1 || i > availablePorts.length) {
                    System.out.println("Invalid port");
                    continue;
                }
                connector = new SerialConnector(availablePorts[i-1]);
                try {
                    connector.connect();
                } catch (Exception e) {
                    System.out.println("Failed to connect to port: " + e.getMessage());
                    continue;
                }
            }
            System.out.print("Enter your command or press 0 to quit: ");
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
