import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

class ClientCommand {
    public final User user;
    public final String command;

    public ClientCommand(User user, String command) {
        this.user = user;
        this.command = command;
    }
}

public class Server_Os {
    private final int MAX_CONTROLQUEUE_SIZE = 5000;
    private ServerConnection Cn = new ServerConnection();
    private final BlockingQueue<ClientCommand> controlQueue = new LinkedBlockingQueue<>(MAX_CONTROLQUEUE_SIZE);
    private final BlockingQueue<ClientCommand> heartbeatQueue = new LinkedBlockingQueue<>();
    private final RoomRegistry roomRegistry = new RoomRegistry();
    private final ClientRegistry clientRegistry = new ClientRegistry();
    public static BroadcasterPool broadcasterPool;

    public static void initServer() {
        RoomRegistry roomRegistry = new RoomRegistry();
        ClientRegistry clientRegistry = new ClientRegistry();
        broadcasterPool = new BroadcasterPool(3, roomRegistry, clientRegistry);
    }

    public static void main(String[] args) {
        initServer();
        new Server_Os().startServer();
    }

    public void startServer() {
        new Thread(this::routerLoop, "RouterThread").start();
        new Thread(this::heartbeatWorker, "HeartbeatWorker").start();

        try (ServerSocket serverSocket = new ServerSocket(Cn.getPort())) {
            System.out.println("[System] Server started on port " + Cn.getPort());
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

    // ---------------- Router Loop ----------------
    private void routerLoop() {
        long lastTime = System.currentTimeMillis();
        int processedCount = 0;

        while (true) {
            try {
                ClientCommand cmd = controlQueue.take();
                long enqueueTime = System.currentTimeMillis(); // สำหรับ latency
                processCommand(cmd);
                processedCount++;

                long latency = System.currentTimeMillis() - enqueueTime;
                LatencyTracker.recordLatency(latency); // เก็บ latency

                long now = System.currentTimeMillis();
                if (now - lastTime >= 1000) { // ทุก 1 วินาที
                    System.out.println("[Throughput] Messages/sec: " + processedCount);
                    processedCount = 0;
                    lastTime = now;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // ---------------- Heartbeat ----------------
    private final Map<String, Long> heartbeatMap = new ConcurrentHashMap<>(); // <clientId, lastPingTime>

    private void heartbeatWorker() {
        final long CHECK_INTERVAL = 5000; // 5 วิ เช็คครั้ง
        final long TIMEOUT = 30000; // 30 วิ หมดอายุ

        while (true) {
            try {
                // รอ ping จาก client (สูงสุด 5 วิ)
                ClientCommand cmd = heartbeatQueue.poll(CHECK_INTERVAL, java.util.concurrent.TimeUnit.MILLISECONDS);
                long now = System.currentTimeMillis();

                if (cmd != null) {
                    heartbeatMap.put(cmd.user.getClientId(), now);
                }

                // ตรวจ timeout
                for (var entry : heartbeatMap.entrySet()) {
                    String id = entry.getKey();
                    long lastPing = entry.getValue();

                    if (now - lastPing > TIMEOUT) {
                        System.out.println("[Heartbeat] Client " + id + " timed out (zombie). Removing...");

                        User u = clientRegistry.getUserById(id);
                        if (u != null) {
                            clientRegistry.unregisterClient(u);
                            roomRegistry.removeUserFromAllRooms(u, broadcasterPool);
                        }

                        heartbeatMap.remove(id);
                        System.out.println("[Heartbeat] Client " + id + " removed.");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // ---------------- Command Processor ----------------
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
            clientRegistry.sendDirectMessage(cmd.user, ">>> Join room: " + param1);
            roomRegistry.joinRoom(param1, cmd.user);
            broadcasterPool.submitTask(new BroadcastTask(param1, ": " + cmd.user.getClientId() + " joined the room."));

        } else if (command.equals("SAY")) {
            if (roomRegistry.isMember(param1, cmd.user)) {
                clientRegistry.sendDirectMessage(cmd.user, ">>> Say to " + param1 + ": " + param2);
                BroadcastTask task = new BroadcastTask(param1, cmd.user + ": " + param2);
                broadcasterPool.submitTask(task);
            } else {
                clientRegistry.sendDirectMessage(cmd.user,
                        "[Server]: You are not in room " + param1 + " Can't send message");
            }
        } else if (command.equals("DM")) {
            if (clientRegistry.hasClientId(param1)) {
                clientRegistry.sendDirectMessage(cmd.user, ">>> Direct message to " + param1 + ": " + param2);
                String formatted = "[DM]" + cmd.user.getClientId() + ": " + param2;
                clientRegistry.sendDirectMessage(clientRegistry.getUserById(param1), formatted);
            } else {
                clientRegistry.sendDirectMessage(cmd.user, "[System]: Receiver not found " + param1);
            }
        } else if (command.equals("WHO")) {
            if (roomRegistry.isMember(param1, cmd.user)) {
                Set<User> members = roomRegistry.getMembers(param1);
                Set<String> AllClientId = ConcurrentHashMap.newKeySet();
                for (User u : members) {
                    AllClientId.add(u.getClientId());
                }

                clientRegistry.sendDirectMessage(cmd.user, ">>> Members in " + param1 + ": " + AllClientId);
            } else {
                clientRegistry.sendDirectMessage(cmd.user, "[System]: You are not in room " + param1);
            }
        } else if (command.equals("LEAVE")) {
            clientRegistry.sendDirectMessage(cmd.user, ">>> Leave room " + param1);
            roomRegistry.leaveRoom(param1, cmd.user);
            broadcasterPool
                    .submitTask(new BroadcastTask(param1, ": " + cmd.user.getClientId() + " left the room."));
        } else if (command.equals("QUIT")) {
            clientRegistry.sendDirectMessage(cmd.user, ">>> Goodbye " + cmd.user.getClientId());
            roomRegistry.removeUserFromAllRooms(cmd.user, broadcasterPool);
            clientRegistry.unregisterClient(cmd.user);
            heartbeatMap.remove(cmd.user.getClientId());
        } else if (command.equals("CLIENT_OVERLOADED")) {
            String originalMsg = cmd.command.substring("CLIENT_OVERLOADED".length()).trim();
            String senderName = null;

            // พยายามดึงชื่อผู้ส่งจากข้อความ เช่น [DM]Alice: หรือ [#room]Bob:
            int start = originalMsg.indexOf(']');
            int colon = originalMsg.indexOf(':');
            if (start != -1 && colon != -1 && colon > start) {
                senderName = originalMsg.substring(start + 1, colon).trim();
            }

            if (senderName != null && clientRegistry.hasClientId(senderName)) {
                User sender = clientRegistry.getUserById(senderName);
                clientRegistry.sendDirectMessage(sender,
                        "[System]: Your message could not be delivered (recipient overloaded). Please resend later.");
                System.out.println("[System]: Notified " + senderName + " about delivery failure.");
            } else {
                System.out.println("[System]: Could not parse sender from overloaded message: " + originalMsg);
            }

        } else {
            clientRegistry.sendDirectMessage(cmd.user, "[System]: Unknown command " + command);
        }
    }

    // -------------------- ClientHandler --------------------
    private class ClientHandler implements Runnable {
        private final Socket socket;
        private User user;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                String firstLine = in.readLine();
                if (firstLine == null || !firstLine.startsWith("HELLO ")) {
                    out.println("[System]: Invalid handshake. Closing connection.");
                    socket.close();
                    return;
                }

                String clientId = firstLine.substring(6).trim();

                if (clientRegistry.hasClientId(clientId)) {
                    out.println("[System]: Error - Name already in use. Please try another name.");
                    socket.close();
                    return;
                }

                user = new User(clientId, out);
                clientRegistry.registerClient(user);
                clientRegistry.sendDirectMessage(user, "[System]: Welcome " + user.getClientId() + "!");

                System.out.println("[System] " + user.getClientId() + " connected.");

                String input;
                while ((input = in.readLine()) != null) {
                    if (input.trim().equalsIgnoreCase("PING")) {
                        heartbeatQueue.offer(new ClientCommand(user, input.trim()));
                    } else {
                        if (!controlQueue.offer(new ClientCommand(user, input.trim()))) {
                            System.out.println(
                                    "[System] Control queue full! Dropping command from " + user.getClientId());
                            clientRegistry.sendDirectMessage(user, "[System]: Server busy. Your command was dropped.");
                        }
                        // controlQueue.offer(new ClientCommand(user, input.trim()));
                    }
                }

            } catch (IOException e) {
                System.out.println("[System] " + (user != null ? user.getClientId() : "unknown") + " disconnected.");
            } finally {
                if (user != null) {
                    clientRegistry.unregisterClient(user);
                    heartbeatMap.remove(user.getClientId());
                    roomRegistry.removeUserFromAllRooms(user, broadcasterPool);
                }
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
