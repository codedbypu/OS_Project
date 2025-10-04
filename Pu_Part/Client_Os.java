import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client_Os {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) throws Exception {
        Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        Scanner sc = new Scanner(System.in);
        System.out.print("Enter your name: ");
        String clientName = sc.nextLine();
    }
}
