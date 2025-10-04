package banana.re;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

// Server จริง ที่รับ Command object จาก client และส่ง Message กลับ
public class Server_Real {
    private static final int PORT = 5000;
    private static Map<String, Set<ClientHandler>> roomRegistry = new ConcurrentHashMap<>();
    private static Map<String, ClientHandler> clientRegistry = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server started on port " + PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(new ClientHandler(clientSocket)).start();
        }
    }

    // ---- class ClientHandler ----
    static class ClientHandler implements Runnable {
        private Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private String clientId;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                // 1. รับ clientId ตอนแรก
                clientId = (String) in.readObject();
                clientRegistry.put(clientId, this);
                System.out.println(clientId + " connected.");

                // 2. รับ Command loop
                Object obj;
                while ((obj = in.readObject()) != null) {
                    Command cmd = (Command) obj;
                    handleCommand(cmd);
                }

            } catch (Exception e) {
                System.out.println("Client " + clientId + " disconnected.");
                leaveAllRooms();
                clientRegistry.remove(clientId);
            }
        }

        private void handleCommand(Command cmd) throws IOException {
            String type = cmd.getType();
            switch (type) {
                case "JOIN":
                    joinRoom(cmd.getArgs().get("room"));
                    break;
                case "SAY":
                    sayMessage(cmd.getArgs().get("room"), cmd.getArgs().get("text"));
                    break;
                case "DM":
                    directMessage(cmd.getArgs().get("target"), cmd.getArgs().get("text"));
                    break;
                case "WHO":
                    listMembers(cmd.getArgs().get("room"));
                    break;
                case "LEAVE":
                    leaveRoom(cmd.getArgs().get("room"));
                    break;
                case "QUIT":
                    leaveAllRooms();
                    sendSystem("Goodbye!");
                    socket.close();
                    break;
                default:
                    sendSystem("Unknown command.");
            }
        }

        private void joinRoom(String room) {
            roomRegistry.putIfAbsent(room, ConcurrentHashMap.newKeySet());
            roomRegistry.get(room).add(this);
            broadcast(room, new Message("System", clientId + " joined", room, "system"));
        }

        private void sayMessage(String room, String text) {
            broadcast(room, new Message(clientId, text, room, "chat"));
        }

        private void directMessage(String target, String text) {
            ClientHandler t = clientRegistry.get(target);
            if (t != null) {
                t.send(new Message(clientId, text, "(DM)", "chat"));
            } else {
                sendSystem("User not found: " + target);
            }
        }

        private void listMembers(String room) {
            Set<ClientHandler> members = roomRegistry.get(room);
            if (members == null) {
                sendSystem("Room not found.");
                return;
            }
            String names = String.join(", ",
                    members.stream().map(m -> m.clientId).toList());
            send(new Message("System", "Members in " + room + ": " + names, room, "system"));
        }

        private void leaveRoom(String room) {
            Set<ClientHandler> members = roomRegistry.get(room);
            if (members != null) {
                members.remove(this);
                broadcast(room, new Message("System", clientId + " left the room.", room, "system"));
                if (members.isEmpty()) {
                    roomRegistry.remove(room);
                    System.out.println("Room removed: " + room);
                }
            }
        }

        private void leaveAllRooms() {
            for (String room : roomRegistry.keySet()) {
                leaveRoom(room);
            }
        }

        private void send(Message msg) {
            try {
                out.writeObject(msg);
                out.flush();
            } catch (IOException e) {
                System.out.println("Error sending to " + clientId);
            }
        }

        private void sendSystem(String text) {
            send(new Message("System", text, "-", "system"));
        }

        private void broadcast(String room, Message msg) {
            Set<ClientHandler> members = roomRegistry.get(room);
            if (members != null) {
                for (ClientHandler c : members) {
                    c.send(msg);
                }
            }
        }
    }
}
