package lora;

import com.fazecast.jSerialComm.SerialPort;

import java.util.Scanner;

public class Main {

    private void start() {

        final SerialPort[] availablePorts = SerialPort.getCommPorts();

        for (int i = 0; i < availablePorts.length; i++) {
            System.out.println("(" + (i + 1) + ") " + availablePorts[i].getDescriptivePortName());
        }

        LoraNode node = null;

        final Scanner scanner = new Scanner(System.in);

        while (true) {
            if (node == null) {
                System.out.print("Please enter port or press 0 to quit: ");
                final String s = scanner.nextLine();
                if (s.trim().equals("0")) {
                    return;
                }
                node = new LoraNode(SerialPort.getCommPort(s));
                try {
                    node.connect();
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
                final String response = node.sendMessage(command);
                System.out.println(response);
            } catch (Exception e) {
                System.out.println("Failed to send command: " + e.getMessage());
                node = null;
            }
        }
    }

    public static void main(String[] args) {
        new Main().start();
    }
}
