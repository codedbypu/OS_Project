package march_ZA;

import java.util.concurrent.BlockingQueue;

public class ReplyPrinter implements Runnable {
    private BlockingQueue<String> replyQueue;

    public ReplyPrinter(BlockingQueue<String> replyQueue) {
        this.replyQueue = replyQueue;
    }

    @Override
    public void run() {
        try {
            while (true) {
                String msg = replyQueue.take(); // block รอข้อความ
                System.out.println(msg);
            }
        } catch (InterruptedException e) {
            System.out.println("Reply printer stopped.");
        }
    }
}

