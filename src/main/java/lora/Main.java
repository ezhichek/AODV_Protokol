package lora;

import com.fazecast.jSerialComm.SerialPort;

import java.util.Scanner;

public class Main {

    private void start() {

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
            System.out.print("> ");
            final String command = scanner.nextLine();
            if (command.equals("0")) {
                return;
            }
            try {
                node.sendMessage(command);
            } catch (Exception e) {
                System.out.println("Failed to send command: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        new Main().start();
    }
}
