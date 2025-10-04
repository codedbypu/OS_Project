package march_ZA;

import java.util.concurrent.BlockingQueue;

// Thread สำหรับ "บล็อก" รอข้อความใน reply queue แล้วพิมพ์ออกหน้าจอ
public class ReplyPrinter implements Runnable {
    private BlockingQueue<String> replyQueue; // คิวที่เก็บข้อความรอแสดง

    public ReplyPrinter(BlockingQueue<String> replyQueue) {
        this.replyQueue = replyQueue;
    }

    @Override
    public void run() {
        try {
            while (true) {
                // take() จะบล็อกจนกว่าจะมีข้อความใหม่เข้ามาในคิว
                String msg = replyQueue.take();
                System.out.println(msg); // แสดงข้อความออกหน้าจอ
            }
        } catch (InterruptedException e) {
            System.out.println("Reply printer stopped."); // หาก thread ถูกหยุด
        }
    }
}


