import java.io.*;
import java.net.Socket;

public class TestClient implements Runnable {
    private final String clientId;
    private final int messageCount;
    private final String serverAddress;
    private final int serverPort;

    public TestClient(String clientId, int messageCount, String serverAddress, int serverPort) {
        this.clientId = clientId;
        this.messageCount = messageCount;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(serverAddress, serverPort);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("HELLO " + clientId); // handshake
            System.out.println(in.readLine());

            out.println("JOIN #testroom"); // join ห้องทดสอบ

            long startTime = System.currentTimeMillis();
            for (int i = 0; i < messageCount; i++) {
                out.println("SAY #testroom Test " + i);
            }
            long endTime = System.currentTimeMillis();

            double durationSec = (endTime - startTime) / 1000.0;
            System.out.println(clientId + " sent " + messageCount + " messages in " + durationSec + " sec. MPS: "
                    + (messageCount / durationSec));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
