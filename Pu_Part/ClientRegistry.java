import java.util.concurrent.*;

public class ClientRegistry {
    // เก็บ reply queue ของแต่ละ client
    private final ConcurrentHashMap<String, BlockingQueue<String>> replyQueues = new ConcurrentHashMap<>();

    public void registerClient(String clientId, BlockingQueue<String> queue) {
        replyQueues.put(clientId, queue);
    }

    public void unregisterClient(String clientId) {
        replyQueues.remove(clientId);
    }

    public boolean hasClient(String clientId) {
        return replyQueues.containsKey(clientId);
    }

    // ส่งข้อความตรง
    public void sendDirectMessage(String receiver, String message) {
        BlockingQueue<String> queue = replyQueues.get(receiver);
        if (queue != null) {
            queue.offer(message);
        } else {
            System.out.println("[System] ไม่พบผู้รับ: " + receiver);
        }
    }
}
