package march_ZA;

import java.io.BufferedReader;
import java.util.concurrent.BlockingQueue;
public class ReceiverThread implements Runnable {
    private BufferedReader in;
    private BlockingQueue<String> replyQueue;

    public ReceiverThread(BufferedReader in, BlockingQueue<String> replyQueue) {
        this.in = in;
        this.replyQueue = replyQueue;
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (!replyQueue.offer(line)) {
                    System.out.println("⚠ Reply queue เต็ม — ทิ้งข้อความ: " + line);
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Lost connection to server.");
        }
    }
}
