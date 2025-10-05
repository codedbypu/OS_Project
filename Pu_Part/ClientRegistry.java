import java.io.PrintWriter;
import java.util.concurrent.*;

public class ClientRegistry {
    // เก็บ reply queue ของแต่ละ client
    private final ConcurrentHashMap<String, PrintWriter> clientWriters = new ConcurrentHashMap<>();

    public void registerClient(String clientId, PrintWriter writer) {
        clientWriters.put(clientId, writer);
    }

    public void unregisterClient(String clientId) {
        clientWriters.remove(clientId);
    }

    public boolean hasClient(String clientId) {
        return clientWriters.containsKey(clientId);
    }

    // ส่งข้อความตรง
    public void sendDirectMessage(String receiver, String message) {
        PrintWriter writer = clientWriters.get(receiver);
        if (writer  != null) {
            writer.println(message);
        } else {
            System.out.println("[System] ไม่พบผู้รับ: " + receiver);
        }
    }
}
