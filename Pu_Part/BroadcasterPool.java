import java.util.concurrent.*;

public class BroadcasterPool {
    private final BlockingQueue<BroadcastTask> taskQueue = new LinkedBlockingQueue<>();
    private final ExecutorService pool;

    public BroadcasterPool(int numThreads, RoomRegistry roomRegistry) {
        pool = Executors.newFixedThreadPool(numThreads);

        // สร้าง thread workers
        for (int i = 0; i < numThreads; i++) {
            pool.submit(new Broadcaster(taskQueue, roomRegistry));
        }
    }

    // เพิ่มงานใหม่เข้า queue
    public void submitTask(BroadcastTask task) {
        taskQueue.offer(task);
    }

    // ปิดระบบ
    public void shutdown() {
        pool.shutdownNow();
    }

    public class Broadcaster implements Runnable {
        private BlockingQueue<BroadcastTask> taskQueue;
        private RoomRegistry roomRegistry;

        public Broadcaster(BlockingQueue<BroadcastTask> taskQueue, RoomRegistry roomRegistry) {
            this.taskQueue = taskQueue;
            this.roomRegistry = roomRegistry;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    // ดึงงานจาก queue
                    BroadcastTask task = taskQueue.take();
                    String room = task.getRoomName();
                    String message = task.getMessage();

                    System.out.println("[Room: " + room + "] " + message);
                    // ส่งข้อความถึงทุกคนในห้อง
                    for (String member : roomRegistry.getMembers(room)) {
                        // TODO: ในระบบจริงจะต้องส่งเข้า reply queue ของ member
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break; // หยุด thread ถ้าโดน interrupt
                }
            }
        }
    }
}
