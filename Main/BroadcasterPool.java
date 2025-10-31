import java.util.Set;
import java.util.concurrent.*;

public class BroadcasterPool {
    private BlockingQueue<BroadcastTask> taskQueue = new LinkedBlockingQueue<>();
    private ExecutorService pool;
    private int numThreads;
    private final RoomRegistry roomRegistry;
    private final ClientRegistry clientRegistry;

    public BroadcasterPool(int numThreads, RoomRegistry roomRegistry, ClientRegistry clientRegistry) {
        this.numThreads = numThreads;
        this.roomRegistry = roomRegistry;
        this.clientRegistry = clientRegistry;
        startPool(numThreads);
    }

    private void startPool(int threads) {
        pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(new Broadcaster(taskQueue, roomRegistry, clientRegistry));
        }
    }

    // --- ฟังก์ชันสำหรับปรับจำนวน threads ---
    public void setThreadCount(int threads) {
        if (threads != this.numThreads) {
            pool.shutdownNow(); // ปิด pool เก่า
            this.numThreads = threads;
            startPool(threads); // สร้าง pool ใหม่
            System.out.println("[BroadcasterPool] Thread count set to " + threads);
        }
    }

    // เพิ่มงานใหม่เข้า queue
    public void submitTask(BroadcastTask task) {
        taskQueue.offer(task);
    }

    // --------------------------- Broadcaster class ---------------------------
    public class Broadcaster implements Runnable {
        private BlockingQueue<BroadcastTask> taskQueue;
        private RoomRegistry roomRegistry;
        private ClientRegistry clientRegistry;
        
        public Broadcaster(BlockingQueue<BroadcastTask> taskQueue, RoomRegistry roomRegistry,
                ClientRegistry clientRegistry) {
            this.taskQueue = taskQueue;
            this.roomRegistry = roomRegistry;
            this.clientRegistry = clientRegistry;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    BroadcastTask task = taskQueue.take(); // ดึงงานออกมาทำ
                    String room = task.getRoomName();
                    String message = task.getMessage();

                    Set<User> members = roomRegistry.getMembers(room);
                    if (members == null || members.isEmpty())
                        continue;

                    for (User member : members) {
                        clientRegistry.sendDirectMessage(member, "[" + room + "]" + message);

                        long latency = System.currentTimeMillis() - task.getEnqueueTime();
                        LatencyTracker.recordLatency(latency);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
