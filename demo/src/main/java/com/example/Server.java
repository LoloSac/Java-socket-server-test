package com.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {

    private final AtomicInteger clientID = new AtomicInteger(1);
    private ServerSocket ss;
    private ConcurrentHashMap<Integer, ClientHandler> clients = new ConcurrentHashMap<>();

    public void start(int port) {
        try {

            System.out.println("\nStarting server...");
            ss = new ServerSocket(port);
            System.out.println("Started server in address: " + ss.getLocalSocketAddress());
            System.out.println("Waiting for clients...");
            while (true) {
                Socket socket = ss.accept();
                int id = clientID.getAndIncrement();
                ClientHandler handler = new ClientHandler(socket, id);
                clients.put(id, handler);
                handler.start();
            }

        } catch (IOException e) {
            System.out.println("Server error: " + e);
        }
    }

    public void stop() {
        try {
            ss.close();
        } catch (Exception e) {
            System.out.println("Error closing server: " + e);
        }
    }

    public void broadcast(String msg, int clientID) {
        for (ClientHandler handler : clients.values()) {
            handler.send("Client" + clientID + " says: " + msg);
        }
    }

    public void sysBroadcast(int clientID, String type) {
        String msg = new String();
        if (type.equals("CONNECTED")) {
            msg = "Client" + clientID + " connected.";
        } else {
            msg = "ERROR";
        }
        for (ClientHandler handler : clients.values()) {
            handler.send(msg);
        }
    }

    private class ClientHandler extends Thread {
        private Socket clientSocket;
        private int clientID;
        private DataOutputStream clientOut;
        private DataInputStream clientIn;

        public ClientHandler(Socket socket, int clientID) {
            this.clientSocket = socket;
            this.clientID = clientID;
        }

        public synchronized void send(String str) {
            try {
                this.clientOut.writeUTF(str);
                this.clientOut.flush();
            } catch (IOException e) {
                System.out.println("Error sending message to client" + clientID + ": " + e);
            }
        }

        public void run() {
            try {
                // IO client server
                this.clientIn = new DataInputStream(clientSocket.getInputStream());
                this.clientOut = new DataOutputStream(clientSocket.getOutputStream());
                sysBroadcast(clientID, "CONNECTED");
                while (true) {
                    String str = clientIn.readUTF();
                    if (str.equals(".")) {
                        broadcast(str, clientID);
                        break;
                    }
                    // Input
                    broadcast(str, clientID);

                }
                System.out.println("Shutting down thread corresponding to client" + clientID + "...");
                clientSocket.close();

            } catch (IOException e) {
                System.out.println("Thread error: " + e);
            } finally {
                clients.remove(clientID);
                try {
                    this.clientSocket.close();
                } catch (IOException e) {
                    System.out.println("Error closing client socket: " + e);
                }
            }
        }

    }
}