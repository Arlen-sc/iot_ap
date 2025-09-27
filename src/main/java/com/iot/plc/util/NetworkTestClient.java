package com.iot.plc.util;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class NetworkTestClient {
    
    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 8888; // From the code, we see the server is actually using port 8888
        
        try {
            System.out.println("Attempting to connect to server: " + host + ":" + port);
            Socket socket = new Socket(host, port);
            System.out.println("Connection successful! Server has responded to connection request.");
            
            // Send some test data
            OutputStream out = socket.getOutputStream();
            String testData = "Test connection data";
            out.write(testData.getBytes());
            out.flush();
            System.out.println("Sent test data: " + testData);
            
            // Keep connection open for a few seconds
            Thread.sleep(3000);
            
            // Close connection
            socket.close();
            System.out.println("Connection closed");
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}