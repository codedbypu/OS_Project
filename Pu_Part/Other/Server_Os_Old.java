import java.util.Scanner;
import java.util.Set;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

public class Server_Os {
    private static final int PORT = 5000;

    private Queue<String> controlQueue = new LinkedList<>();
    private RoomRegistry roomRegistry = new RoomRegistry();
    private ClientRegistry clientRegistry = new ClientRegistry();
    private BroadcasterPool broadcasterPool = new BroadcasterPool(3, roomRegistry);

    // จำลองชื่อ client
    //private String clientId = "Alice";

    public String Pre_client() {
        Scanner sc = new Scanner(System.in);
        System.out.println("============================= All Instructions =============================\n" +
                "DM <receiver_name> <message>   # send a direct message to a specific friend\r\n" +
                "JOIN <#room_name>              # join a chat room\r\n" +
                "WHO <#room_name>               # see who is in the room\r\n" +
                "SAY <#room_name> <message>     # send a message to everyone in the room\r\n" +
                "LEAVE <#room_name>             # leave the room\r\n" +
                "QUIT                           # exit the program \r\n" +
                "============================================================================");
        System.out.print("Instruction: ");
        String instruction = sc.nextLine();
        return instruction;
    }

    public void Router() {
        if (controlQueue.isEmpty())
            return;

        String cur_instruction = controlQueue.poll().trim();
        if (cur_instruction.isEmpty()) {
            return;
        }

        // แยกคำสั่งตามช่องว่าง
        String[] parts = cur_instruction.split(" ", 4);
        if (parts.length > 3) {
            System.out.println("Command : ERROR");
            return;
        }
        String command = parts[0].toUpperCase();
        String param1 = (parts.length > 1) ? parts[1] : "";
        String param2 = (parts.length > 2) ? parts[2] : "";

        if (command.equals("JOIN")) { // JOIN
            roomRegistry.joinRoom(param1, clientId);

        } else if (command.equals("SAY")) {
            String roomName = param1;
            String message = param2;

            if (roomRegistry.isMember(roomName, clientId)) {
                BroadcastTask task = new BroadcastTask(roomName, clientId + ": " + message);
                broadcasterPool.submitTask(task);
            } else {
                System.out.println("(Server): You are not in room " + roomName + " Can't send message");
            }

        } else if (command.equals("DM")) {
            String receiver = param1;
            String message = param2;

            if (clientRegistry.hasClient(receiver)) {
                String formatted = "[DM from " + clientId + "] " + message;
                clientRegistry.sendDirectMessage(receiver, formatted);
            } else {
                System.out.println("(System): ไม่พบผู้รับชื่อ " + receiver);
            }

        } else if (command.equals("WHO")) { // WHO
            if (roomRegistry.isMember(param1, clientId)) {
                Set<String> members = roomRegistry.getMembers(param1);
                System.out.println("Members in " + param1 + ": " + members);
            } else {
                System.out.println("(Server): You are not in room " + param1);
            }

        } else if (command.equals("LEAVE")) { // LEAVE
            roomRegistry.leaveRoom(param1, clientId);

        } else if (command.equals("QUIT")) { // QUIT
            System.out.println(">>> Exit the program");
            System.exit(0);

        } else {
            System.out.println("Unknown command: " + command);
        }
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            Socket socket = serverSocket.accept(); // รอ Client เชื่อมต่อ
            System.out.println("Client connected");
            
            Server_Os server = new Server_Os();
            String cur_instruction = "";

            while (true) {
                //cur_instruction = server.Pre_client();
                server.controlQueue.add(cur_instruction);
                server.Router();
            }
        } catch (IOException e) {
            System.out.println("Server error!");
            e.printStackTrace();
        }
    }

}
