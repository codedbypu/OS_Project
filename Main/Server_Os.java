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
    private final int MAX_SOCKET_SIZE = 250;
    private final int MAX_CONTROLQUEUE_SIZE = 5000;
    private final int START_THREADS = 12;
    private final ServerConnection serverConnection = new ServerConnection();
    private final RoomRegistry roomRegistry = new RoomRegistry(); // ใช้เก็บห้องแชททั้งหมด
    private final ClientRegistry clientRegistry = new ClientRegistry(); // ใช้เก็บ client ที่เชื่อมต่อเข้ามา
    private final BroadcasterPool broadcasterPool = new BroadcasterPool(START_THREADS, roomRegistry, clientRegistry); 
    private final BlockingQueue<ClientCommand> controlQueue = new LinkedBlockingQueue<>(MAX_CONTROLQUEUE_SIZE);
    private final BlockingQueue<ClientCommand> heartbeatQueue = new LinkedBlockingQueue<>();

    public static void main(String[] args) {
        new Server_Os().startServer();
    }

    public void startServer() {
        new Thread(this::routerLoop, "RouterThread").start();
        new Thread(this::heartbeatWorker, "HeartbeatWorker").start();

        try (ServerSocket serverSocket = new ServerSocket(serverConnection.getPort(),MAX_SOCKET_SIZE)) {
            System.out.println("[System]: Server started on port " + serverConnection.getPort());
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new ClientHandler(socket), "ClientHandler").start();
                System.out.println("[System]: New client connected: " + socket.getInetAddress().getHostName() + " ("
                        + socket.getLocalSocketAddress() + ")");
            }
        } catch (IOException e) {
            System.out.println("[System]: Server error!");
            e.printStackTrace();
        }
    }

    // ---------------- Router Loop ----------------
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

    // ---------------- Heartbeat ----------------
    private final Map<String, Long> heartbeatMap = new ConcurrentHashMap<>(); // <clientId, lastPingTime>

    private void heartbeatWorker() {
        final long CHECK_INTERVAL = 5000; // ตั้งค่าไว้ที่ 5 วิ จะเช็คครั้ง
        final long TIMEOUT = 30000; // ตั้งค่าไว้ที่ 30 วิ จะตัดการเชื่อมต่อถ้าไม่มี ping

        while (true) {
            try {
                // รอคำสั่งping ทุก5วิ จะไปดูในคิว
                ClientCommand command = heartbeatQueue.poll(CHECK_INTERVAL, java.util.concurrent.TimeUnit.MILLISECONDS);
                long now = System.currentTimeMillis();

                if (command != null) {
                    heartbeatMap.put(command.user.getClientId(), now);
                }

                // ตรวจ timeout
                for (var entry : heartbeatMap.entrySet()) {
                    String clientId = entry.getKey();
                    long lastPing = entry.getValue();

                    if (now - lastPing > TIMEOUT) {
                        System.out.println("[Heartbeat]: Client " + clientId + " timed out (zombie). Removing...");

                        User user = clientRegistry.getUserById(clientId);
                        if (user != null) {
                            clientRegistry.unregisterClient(user);
                            roomRegistry.removeUserFromAllRooms(user, broadcasterPool);
                        }

                        heartbeatMap.remove(clientId);
                        System.out.println("[Heartbeat]: Client " + clientId + " removed.");
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

        String[] parts = cmd.command.split(" ", 3);
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
                BroadcastTask task = new BroadcastTask(param1, cmd.user.getClientId() + ": " + param2);
                broadcasterPool.submitTask(task);
            } else {
                String text = "[Server]: You are not in room " + param1 + " Can't send message";
                clientRegistry.sendDirectMessage(cmd.user, text);
            }
        } else if (command.equals("DM")) {
            if (clientRegistry.hasClientId(param1)) {
                clientRegistry.sendDirectMessage(cmd.user, ">>> Direct message to " + param1 + ": " + param2);
                String text = "[DM]" + cmd.user.getClientId() + ": " + param2;
                clientRegistry.sendDirectMessage(clientRegistry.getUserById(param1), text);
            } else {
                clientRegistry.sendDirectMessage(cmd.user, "[Server]: Receiver not found " + param1);
            }
        } else if (command.equals("WHO")) {
            if (roomRegistry.isMember(param1, cmd.user)) {
                Set<User> members = roomRegistry.getMembers(param1);
                Set<String> allClientId = ConcurrentHashMap.newKeySet();
                for (User u : members) {
                    allClientId.add(u.getClientId());
                }
                clientRegistry.sendDirectMessage(cmd.user, ">>> Members in " + param1 + ": " + allClientId);
            } else {
                clientRegistry.sendDirectMessage(cmd.user, "[Server]: You are not in room " + param1);
            }
        } else if (command.equals("LEAVE")) {
            if (roomRegistry.isMember(param1, cmd.user)) {
                clientRegistry.sendDirectMessage(cmd.user, ">>> Leave room " + param1);
                roomRegistry.leaveRoom(param1, cmd.user);
                String text = ": " + cmd.user.getClientId() + " left the room.";
                broadcasterPool.submitTask(new BroadcastTask(param1, text));
            } else {
                clientRegistry.sendDirectMessage(cmd.user, "[Server]: You are not in room " + param1);
            }
        } else if (command.equals("QUIT")) {
            clientRegistry.sendDirectMessage(cmd.user, ">>> Goodbye " + cmd.user.getClientId());
            roomRegistry.removeUserFromAllRooms(cmd.user, broadcasterPool);
            clientRegistry.unregisterClient(cmd.user);
            heartbeatMap.remove(cmd.user.getClientId());

        } else if (command.equals("CLIENT_OVERLOADED")) { // คำสั่งทำงานเมื่อ client แจ้งมาว่าตัวเอง overload
            String originalMsg = cmd.command.substring("CLIENT_OVERLOADED".length()).trim(); // ดึงข้อความต้นฉบับ
            String senderName = null;

            // พยายามดึงชื่อผู้ส่งจากข้อความ เช่น [DM]Alice: หรือ [#room]Bob:
            int start = originalMsg.indexOf(']');
            int colon = originalMsg.indexOf(':');
            if (start != -1 && colon != -1 && colon > start) {
                senderName = originalMsg.substring(start + 1, colon).trim();
            }

            if (senderName != null && clientRegistry.hasClientId(senderName)) {
                User sender = clientRegistry.getUserById(senderName);
                String text = "[Server]: " + cmd.user.getClientId()
                        + " overloaded. Message failed to deliver. Try later.";
                clientRegistry.sendDirectMessage(sender, text);
                String textNotified = "[System]: Notified " + senderName + " about overload from "
                        + cmd.user.getClientId();
                System.out.println(textNotified);
            } else {
                System.out
                        .println("[System]: Cannot identify sender in message. Overload notification failed.  Message: "
                                + originalMsg);
            }
        } else if (command.equals("SET_THREADS")) {
            if (!cmd.user.getClientId().equals("Admin")) {
                clientRegistry.sendDirectMessage(cmd.user, "[Server]: Permission denied.");
                return;
            }
            try {
                int threads = Integer.parseInt(param1);
                System.out.println("[System]: Broadcaster threads set to " + threads);
                broadcasterPool.setThreadCount(threads);
            } catch (NumberFormatException e) {
                System.out.println("[System]: Invalid thread count: " + param1);
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
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                String firstLine = in.readLine();
                if (firstLine == null || !firstLine.startsWith("HELLO ")) { // เกิดถ้าไม่ได้ส่ง HELLO มา
                    out.println("[Server]: Invalid handshake. Closing connection.");
                    socket.close();
                    return;
                }

                String clientId = firstLine.substring(6).trim(); // ดึงชื่อที่ส่งมา(ตัดคำว่า HELLO ออก)
                if (clientRegistry.hasClientId(clientId)) {
                    out.println("[Server]: Error - Name already taken. Choose another.");
                    socket.close();
                    return;
                }

                user = new User(clientId, out);
                clientRegistry.registerClient(user);
                clientRegistry.sendDirectMessage(user, "[Server]: Welcome " + user.getClientId() + "!");

                System.out.println("[System]: " + user.getClientId() + " connected.");

                String input;
                while ((input = in.readLine()) != null) {
                    if (input.trim().equalsIgnoreCase("PING")) {
                        heartbeatQueue.offer(new ClientCommand(user, input.trim()));
                    } else {
                        if (!controlQueue.offer(new ClientCommand(user, input.trim()))) {
                            System.out.println(
                                    "[System]: Control queue full! Dropping command from " + user.getClientId());
                            clientRegistry.sendDirectMessage(user, "[Server]: Server busy. Your command was dropped.");
                        }
                    }
                }

            } catch (IOException e) {
                System.out.println("[System]: " + (user != null ? user.getClientId() : "unknown") + " disconnected.");
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
