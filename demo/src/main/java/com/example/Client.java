package com.example;

import java.io.*;
import java.net.*;

public class Client {
    private Socket s;
    private DataOutputStream out;
    private DataInputStream in;
    private boolean shuttingDown = false;

    public boolean isShuttingDown() {
        return shuttingDown;
    }

    public static class InputHandler extends Thread {
        private Client client;

        public InputHandler(Client client) {
            this.client = client;
        }

        public void run() {
            String response;
            try {
                while (true) {
                    response = client.in.readUTF();
                    System.out.println(response);
                }
            } catch (IOException e) {
                if (!client.isShuttingDown()) {
                    System.out.println("Error reading from server: " + e);
                }
            }
        }
    }

    public static class OutputHandler extends Thread {
        private DataOutputStream output;
        private Client client;

        public OutputHandler(Client client) {
            this.client = client;
        }

        public void run() {
            String str;
            BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in));
            output = client.out;
            try {
                while (true) {
                    str = userIn.readLine();
                    if (str.equals(".")) {
                        output.writeUTF(".");
                        break;
                    }
                    output.writeUTF(str);
                    output.flush();
                }

            } catch (IOException e) {
                System.out.println("Error writing to server: " + e);
            } finally {
                client.shutdown();
                try {
                    output.close();
                    userIn.close();
                } catch (IOException e) {
                    System.out.println("Error closing resources: " + e);
                }
            }
        }
    }

    public void shutdown() {
        this.shuttingDown = true;
        System.out.println("Shutting down...");
        try {
            this.s.close();
            this.out.close();
            this.in.close();
        } catch (IOException e) {
            System.out.println("Error closing resources: " + e);
        }
    }

    public static void main(String[] args) {
        try {
            Client client = new Client();
            client.s = new Socket("localhost", 1111);
            System.out.println("\nConnected to server in address: " + client.s.getLocalSocketAddress());
            DataOutputStream out = new DataOutputStream(client.s.getOutputStream());
            DataInputStream in = new DataInputStream(client.s.getInputStream());

            client.out = out;
            client.in = in;

            InputHandler inputHandler = new InputHandler(client);
            inputHandler.start();

            OutputHandler outputHandler = new OutputHandler(client);
            outputHandler.start();

        } catch (Exception e) {
            System.out.print("Client error: " + e);
        }
    }
}
