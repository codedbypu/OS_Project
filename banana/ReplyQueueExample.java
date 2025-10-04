package banana;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ReplyQueueExample {
    public static void main(String[] args) throws InterruptedException {
        // reply queue ของ client คนหนึ่ง
        BlockingQueue<String> replyQueue = new LinkedBlockingQueue<>();

        // ฝั่ง server ส่งข้อความมาที่ reply queue
        replyQueue.put("[Server]: Welcome to #os-lab!");
        replyQueue.put("Alice: Hello everyone!");
        replyQueue.put("Bob: Hi Alice!");

        // ฝั่ง client มี thread คอยอ่านจาก reply queue
        new Thread(() -> {
            try {
                while (true) {
                    String message = replyQueue.take(); // รอจนกว่าจะมีข้อความ
                    System.out.println("🙈" + message);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // จำลองว่ามีข้อความเพิ่มมาทีหลัง
        Thread.sleep(2000);
        replyQueue.put("Charlie: Nice to meet you all!");
    }
}
