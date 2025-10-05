import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import Other.Server_Os;

class ClientCommand {
    public final String clientId;
    public final String command;

    public ClientCommand(String clientId, String command) {
        this.clientId = clientId;
        this.command = command;
    }
}

public class Server_Os {
    private static final int PORT = 5000;
    private final BlockingQueue<ClientCommand> controlQueue = new LinkedBlockingQueue<>();
    private final RoomRegistry roomRegistry = new RoomRegistry();
    private final ClientRegistry clientRegistry = new ClientRegistry();
    private final BroadcasterPool broadcasterPool = new BroadcasterPool(3, roomRegistry, clientRegistry);

    public static void main(String[] args) {
        new Server_Os().startServer();
    }

    public void startServer() {
        new Thread(this::routerLoop, "RouterThread").start();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[System] Server started on port " + PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new ClientHandler(socket), "ClientHandler").start();
                System.out.println("[System] New client connected: " + socket.getInetAddress());
            }
        } catch (IOException e) {
            System.out.println("[System] Server error!");
            e.printStackTrace();
        }
    }

    private void routerLoop() {
        while (true) {
            try {
                ClientCommand cmd = controlQueue.take();
                processCommand(cmd);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void processCommand(ClientCommand cmd) {
        if (cmd.command.isEmpty())
            return;

        String[] parts = cmd.command.split(" ", 4);
        if (parts.length > 3) {
            return;
        }
        String command = parts[0].toUpperCase();
        String param1 = (parts.length > 1) ? parts[1] : "";
        String param2 = (parts.length > 2) ? parts[2] : "";

        if (command.equals("JOIN")) {
            clientRegistry.sendDirectMessage(cmd.clientId, ">>> Join room: " + param1);
            roomRegistry.joinRoom(param1, cmd.clientId);
            broadcasterPool.submitTask(new BroadcastTask(param1, ": " + cmd.clientId + " joined the room."));

        } else if (command.equals("SAY")) {
            if (roomRegistry.isMember(param1, cmd.clientId)) {
                clientRegistry.sendDirectMessage(cmd.clientId, ">>> Say to " + param1 + ": " + param2);
                BroadcastTask task = new BroadcastTask(param1, cmd.clientId + ": " + param2);
                broadcasterPool.submitTask(task);
            } else {
                clientRegistry.sendDirectMessage(cmd.clientId, "[Server]: You are not in room " + param1 + " Can't send message");
            }
        } else if (command.equals("DM")) {
            if (clientRegistry.hasClient(param1)) {
                clientRegistry.sendDirectMessage(cmd.clientId, ">>> Direct message to " + param1 + ": " + param2);
                String formatted = "[DM]" + cmd.clientId + ": " + param2;
                clientRegistry.sendDirectMessage(param1, formatted);
            } else {
                clientRegistry.sendDirectMessage(cmd.clientId, "[System]: Receiver not found " + param1);
            }
        } else if (command.equals("WHO")) {
            if (roomRegistry.isMember(param1, cmd.clientId)) {
                Set<String> members = roomRegistry.getMembers(param1);
                clientRegistry.sendDirectMessage(cmd.clientId, ">>> Members in " + param1 + ": " + members);
            } else {
                clientRegistry.sendDirectMessage(cmd.clientId, "[System]: You are not in room " + param1);
            }
        } else if (command.equals("LEAVE")) {
            clientRegistry.sendDirectMessage(cmd.clientId, ">>> Leave room " + param1);
            roomRegistry.leaveRoom(param1, cmd.clientId);
            broadcasterPool.submitTask(new BroadcastTask(param1, "[System]: " + cmd.clientId + " left the room."));
        } else if (command.equals("QUIT")) {
            clientRegistry.sendDirectMessage(cmd.clientId, ">>> Goodbye " + cmd.clientId);
            clientRegistry.unregisterClient(cmd.clientId);
        } else {
            clientRegistry.sendDirectMessage(cmd.clientId, "[System]: Unknown command " + command);
        }
    }

    // -------------------- ClientHandler --------------------
    private class ClientHandler implements Runnable {
        private final Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String clientId;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String firstLine = in.readLine();
                if (firstLine == null || !firstLine.startsWith("HELLO ")) {
                    out.println("[System]: Invalid handshake. Closing connection.");
                    socket.close();
                    return;
                }

                clientId = firstLine.substring(6).trim();
                clientRegistry.registerClient(clientId, out);
                clientRegistry.sendDirectMessage(clientId, "[System]: Welcome " + clientId + "!");

                System.out.println("[System] " + clientId + " connected.");
                String input;
                while ((input = in.readLine()) != null) {
                    controlQueue.offer(new ClientCommand(clientId, input.trim()));
                }

            } catch (IOException e) {
                System.out.println("[System] " + clientId + " disconnected.");
            } finally {
                if (clientId != null)
                    clientRegistry.unregisterClient(clientId);
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
