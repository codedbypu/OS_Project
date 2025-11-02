import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ChatClient {
    private final int MAX_REPLYQUEUE_SIZE = 1000;
    private final ServerConnection serverConnection = new ServerConnection();

    public static void main(String[] args) {
        ChatClient ChatClient = new ChatClient();
        ChatClient.ClientRunning();
    }

    void showInstructions() {
        System.out.println("""
                ============================= All Instructions =============================
                DM <receiver_name> <message>   # send a direct message to a specific friend\r
                JOIN <#room_name>              # join a chat room\r
                WHO <#room_name>               # see who is in the room\r
                SAY <#room_name> <message>     # send a message to everyone in the room\r
                LEAVE <#room_name>             # leave the room\r
                QUIT                           # exit the program \r
                ============================================================================""");
    }

    void ClientRunning() {
        try (Scanner scanner = new Scanner(System.in)) {
            boolean hasPrintedConnectedMsg = false;
            boolean connected = false;
            int tryConnect = 0;

            while (!connected && tryConnect <= 5) {
                try {
                    Socket socket = new Socket(serverConnection.getAddress(), serverConnection.getPort());
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BlockingQueue<String> replyQueue = new LinkedBlockingQueue<>(MAX_REPLYQUEUE_SIZE);

                    if (tryConnect == 0 && !hasPrintedConnectedMsg) {
                        System.out.println("[Client]: Connected to server at " + serverConnection.getAddress() + ":"
                                + serverConnection.getPort());
                        hasPrintedConnectedMsg = true;
                    }

                    System.out.print("[Input] Enter your name: ");
                    String name = scanner.nextLine().trim();

                    if (name.isEmpty()) {
                        System.out.println("[System]: Name cannot be empty. Please try again.");
                        socket.close();
                        continue;
                    }
                    if (name.contains(" ")) {
                        System.out.println("[System]: Name cannot contain spaces. Please try again.");
                        socket.close();
                        continue;
                    }

                    out.println("HELLO " + name);

                    // รอรับการตอบกลับจาก server
                    String response = in.readLine();
                    if (response != null && response.contains("Error")) {
                        System.out.println(response);
                        socket.close();
                        continue; // ลองใหม่อีกครั้ง
                    }

                    System.out.println(response);
                    connected = true;

                    // Thread 0 (heartbeatThread): เช็คสถานะการเชื่อมต่อทุก 10 วินาที
                    Thread heartbeatThread = new Thread(() -> {
                        try {
                            while (true) {
                                Thread.sleep(10000);
                                out.println("PING");
                            }
                        } catch (InterruptedException ignored) {
                        }
                    }, "Heartbeat");
                    heartbeatThread.setDaemon(true);
                    heartbeatThread.start();

                    // Thread 1 (receiverThread): รับข้อความจาก server -> ใส่ replyQueue
                    Thread receiverThread = new Thread(() -> {
                        try {
                            String msg;
                            while ((msg = in.readLine()) != null) {
                                if (!replyQueue.offer(msg)) {
                                    System.out.println(
                                            "[System]: Reply queue full. Please try again later. Message dropped.");
                                    out.println("CLIENT_OVERLOADED " + msg);
                                }
                            }
                        } catch (IOException ignored) {
                        }
                    }, "Receiver");
                    receiverThread.start();

                    // Thread 2 (displayThread): แสดงข้อความจาก replyQueue
                    Thread displayThread = new Thread(() -> {
                        try {
                            while (true) {
                                String msg = replyQueue.take();
                                System.out.print("\r" + " ".repeat(80) + "\r");
                                System.out.println(msg);
                                if (msg.equals(">>> Goodbye " + name)) { // ตรวจสอบคำสั่ง QUIT
                                    System.out.println("[System]: Client terminated.");
                                    System.exit(0);
                                }
                                System.out.print("[Input] Instruction: ");
                                System.out.flush();
                            }
                        } catch (InterruptedException ignored) {
                        }
                    }, "Display");
                    displayThread.start();

                    // แสดงคำสั่งทั้งหมดเฉยๆ
                    showInstructions();

                    // Main Thread: รับคำสั่งจากผู้ใช้ -> ส่งไปที่ server
                    while (true) {
                        System.out.print("[Input] Instruction: ");
                        String input = scanner.nextLine().trim();

                        String[] parts = input.split(" ", 3);
                        if (input.isEmpty()) {
                            System.out.println("[Client]: Command : ERROR");
                            continue;
                        }

                        String command = parts[0].toUpperCase();
                        // ตรวจสอบคำสั่งที่ต้องมีชื่อห้อง
                        if (command.equals("JOIN") || command.equals("WHO") || command.equals("SAY")
                                || command.equals("LEAVE")) {
                            if (parts.length < 2) {
                                System.out.println("[System]: Please specify a room name.");
                                continue;
                            }
                            String room = parts[1];
                            if (!room.startsWith("#")) {
                                System.out.println(
                                        "[System]: Room name must start with '#' (#room_name).");
                                continue;
                            }
                        }

                        // ตรวจสอบ SAY ต้องมีข้อความตามหลัง
                        if (command.equals("SAY") && parts.length < 3) {
                            System.out.println("[System]: Usage: SAY #room_name <message>");
                            continue;
                        }

                        // ตรวจสอบ DM ต้องมีชื่อและข้อความ
                        if (command.equals("DM")) {
                            if (parts.length < 3) {
                                System.out.println("[System]: Usage: DM <receiver_name> <message>");
                                continue;
                            }
                        }

                        out.println(input);
                        // ตรวจสอบคำสั่ง QUIT เพื่อปิด client อย่างถูกต้อง
                        if (command.equals("QUIT")) {
                            new Thread(() -> { // ปิด client ถ้าไม่มีการตอบกลับจาก server ภายใน 5 วินาที
                                try {
                                    Thread.sleep(5000);
                                    System.out.println("[System]: No response from server. Closing...");
                                    System.exit(0);
                                } catch (InterruptedException ignored) {
                                }
                            }).start();
                            break;
                        }
                    }
                } catch (IOException e) { // การเชื่อมต่อล้มเหลว
                    tryConnect++;
                    System.out.println("[Client]: Connection error! Retrying in 2 seconds...");

                    if (tryConnect >= 5) {
                        System.out.println("[Client]: Failed to connect after 5 attempts. Exiting.");
                        break;
                    }
                    try {
                        Thread.sleep(2000); // รอ 2 วินาทีก่อนลองใหม่
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
    }
}
