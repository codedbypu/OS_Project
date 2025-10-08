import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ChatClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 5000;

    public static void main(String[] args) {
        BlockingQueue<String> replyQueue = new LinkedBlockingQueue<>();

        try (Socket socket = new Socket(SERVER_ADDRESS, PORT);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                Scanner scanner = new Scanner(System.in)) {
            System.out.println("[Client]: Connected to server at " + SERVER_ADDRESS + ":" + PORT);

            System.out.print("Enter your name: ");
            String name = scanner.nextLine().trim();
            out.println("HELLO " + name);

            // Thread 1: รับข้อความจาก server -> ใส่ replyQueue+
            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        replyQueue.offer(msg);
                    }
                } catch (IOException ignored) {
                }
            }, "Receiver").start();

            // Thread 2: แสดงข้อความจาก replyQueue
            new Thread(() -> {
                try {
                    while (true) {
                        String msg = replyQueue.take();

                        // ลบ prompt เดิมออก (ขึ้นบรรทัดใหม่)
                        System.out.print("\r" + " ".repeat(80) + "\r");

                        // แสดงข้อความที่ได้รับจาก server
                        System.out.println(msg);

                        // พิมพ์ prompt กลับมาใหม่
                        System.out.print("Instruction: ");
                        System.out.flush();
                    }
                } catch (InterruptedException ignored) {
                }
            }, "Display").start();

            System.out.println("============================= All Instructions =============================\n" +
                        "DM <receiver_name> <message>   # send a direct message to a specific friend\r\n" +
                        "JOIN <#room_name>              # join a chat room\r\n" +
                        "WHO <#room_name>               # see who is in the room\r\n" +
                        "SAY <#room_name> <message>     # send a message to everyone in the room\r\n" +
                        "LEAVE <#room_name>             # leave the room\r\n" +
                        "QUIT                           # exit the program \r\n" +
                        "============================================================================");
                        
            while (true) {
                System.out.print("Instruction: ");
                String input = scanner.nextLine().trim();

                if (input.isEmpty()) {
                    System.out.println("Command : ERROR");
                    continue;
                }

                String[] parts = input.split(" ", 4);
                if (parts.length > 3) {
                    System.out.println("Command : ERROR");
                    continue;
                }
                String command = parts[0].toUpperCase();

                out.println(input);
                
                if (command.equals("QUIT"))
                    break;
            }
        } catch (IOException e) {
            System.out.println("[Client]: Connection error!");
            e.printStackTrace();
        }
    }
}
