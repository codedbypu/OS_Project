package march_ZA;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ChatClient {
    private String clientName;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private BlockingQueue<String> replyQueue = new LinkedBlockingQueue<>(50); // reply queue (จำกัด 50 ข้อความ)

    public ChatClient(String host, int port, String name) throws IOException {
        this.clientName = name;
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // บอก server ว่านี่คือใคร
        out.println("REGISTER " + clientName);

        // สร้าง 2 threads
        new Thread(new InputThread(out)).start();
        new Thread(new ReceiverThread(in, replyQueue)).start();
        new Thread(new ReplyPrinter(replyQueue)).start();
    }

    public void close() throws IOException {
        socket.close();
    }
}

