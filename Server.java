/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package networks.project;
import java.net.*; 
import java.io.*; 
import java.util.*;
import java.text.*;

/**
 *
 * @author Grant Posner
 */
public class Server {
    private int port;
    private Set<String> usernames = new HashSet<>();
    private Set<ChatThread> chatThreads = new HashSet<>();
    
    public Server(int p) {
        this.port = p;
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Error: No port given. Quitting.");
            System.exit(1);
        }
        Server server = new Server(Integer.parseInt(args[0]));
        server.workIt();
    }
    
    public void workIt() {
        try {
            ServerSocket servSock = new ServerSocket(port);
            System.out.println("Port " + port + " ready and waiting.");
            while(true) {
                Socket socket = servSock.accept();
                ChatThread newClient = new ChatThread(socket, this);
                chatThreads.add(newClient);
                newClient.start();
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
    
    public synchronized boolean addUsername(String username) {
        return (usernames.add(username));
    }
    
    public synchronized void removeUser(String username, ChatThread clientThread) {
        if (usernames.remove(username)) {
            chatThreads.remove(clientThread);
        }
    }
    
    public synchronized void sendMessage(String message) {
        ChatThread[] chatThreads = (ChatThread[])this.chatThreads.toArray();
        for(int i = 0; i < chatThreads.length; i++) {
            chatThreads[i].toClient(message);
        }
    }
    
    private class ChatThread extends Thread {
        private Socket socket;
        private Server server;
        private PrintWriter outToClient;
        
        ChatThread(Socket sock, Server serv) {
            socket = sock;
            server = serv;
        }
        
        public void run() {
            try {
                InputStream in = socket.getInputStream();
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(in));
                OutputStream out = socket.getOutputStream();
                outToClient = new PrintWriter(out, true);
                String username = inFromClient.readLine();
                while (!server.addUsername(username)) {
                    outToClient.println("REJECT");
                    username = inFromClient.readLine();
                }
                outToClient.println("ACCEPT");
                String message = username + "has joined.";
                server.sendMessage(message);
                while(!message.equals("!exit")) {
                    message = username + ": " + inFromClient.readLine();
                    server.sendMessage(message);
                }
                server.removeUser(username, this);
                socket.close();
                message = username + " has logged off.";
                server.sendMessage(message);
                
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
        
        public void toClient(String message) {
            outToClient.println(message);
        }
    }
}
