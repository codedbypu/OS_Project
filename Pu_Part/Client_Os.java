import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client_Os {
    private String clientId;
    private BlockingQueue<String> replyQueue = new LinkedBlockingQueue<>();

    public static void main(String[] args) throws Exception {
        private BlockingQueue<String> replyQueue = new LinkedBlockingQueue<>();
        Random random = new Random();
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter your name: ");
        String clientname = sc.nextLine();
        int clientId = random.nextInt();

        Socket socket = new Socket("localhost", 5000); // ระบุปลายทาง
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        while (true) {
            System.out.println("============================= All Instructions =============================\n" +
                    "DM <receiver_name> <message>   # send a direct message to a specific friend\r\n" +
                    "JOIN <#room_name>              # join a chat room\r\n" +
                    "WHO <#room_name>               # see who is in the room\r\n" +
                    "SAY <#room_name> <message>     # send a message to everyone in the room\r\n" +
                    "LEAVE <#room_name>             # leave the room\r\n" +
                    "QUIT                           # exit the program \r\n" +
                    "============================================================================");
            System.out.print("Instruction: ");
            String line = sc.nextLine();
            if (line.equalsIgnoreCase("QUIT"))
                break;
        }
        socket.close();
    }
}
