package march_ZA;

import java.io.BufferedReader;
import java.util.concurrent.BlockingQueue;

// Thread สำหรับ "รับข้อความจาก Server" แล้วเก็บลง reply queue
public class ReceiverThread implements Runnable {
    private BufferedReader in;                 // ใช้รับข้อความจาก Server
    private BlockingQueue<String> replyQueue;  // คิวเก็บข้อความที่รอพิมพ์

    public ReceiverThread(BufferedReader in, BlockingQueue<String> replyQueue) {
        this.in = in;
        this.replyQueue = replyQueue;
    }

    @Override
    public void run() {
        try {
            String line;
            // วนลูปรับข้อความจาก Server
            while ((line = in.readLine()) != null) {
                // ถ้า queue เต็ม — ทิ้งข้อความและแจ้งเตือน
                if (!replyQueue.offer(line)) {
                    System.out.println("⚠ Reply queue เต็ม — ทิ้งข้อความ: " + line);
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Lost connection to server."); // กรณีหลุดจาก Server
        }
    }
}