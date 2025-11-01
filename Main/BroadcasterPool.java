import java.util.Set;
import java.util.concurrent.*;

public class BroadcasterPool {
    private final BlockingQueue<BroadcastTask> taskQueue = new LinkedBlockingQueue<>(); // ใช้เก็บงานกระจายข้อความ
    private final ClientRegistry clientRegistry;
    private final RoomRegistry roomRegistry;
    private ExecutorService pool;
    private int numThreads;
    private final boolean SHOWDEBUG = false;

    // ---------------- Initialization ----------------
    public BroadcasterPool(int numThreads, RoomRegistry roomRegistry, ClientRegistry clientRegistry) {
        this.numThreads = numThreads;
        this.roomRegistry = roomRegistry;
        this.clientRegistry = clientRegistry;
        startPool(numThreads);
    }

    private void startPool(int threads) {
        if (SHOWDEBUG)
            System.out.println("[BroadcasterPool]: Starting Broadcaster pool with " + threads + " threads.");
        pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            if (SHOWDEBUG)
                System.err.println("[BroadcasterPool]: Starting Broadcaster thread " + (i + 1));
            pool.submit(new Broadcaster(taskQueue, roomRegistry, clientRegistry));
        }
    }

    // ---------------- ฟังก์ชันปรับจำนวน threads ----------------
    // public void setThreadCount(int threads) {
    // if (threads != this.numThreads) {
    // pool.shutdownNow(); // ปิด pool เก่า
    // this.numThreads = threads; // อัปเดตจำนวน threads
    // startPool(threads); // สร้าง pool ใหม่
    // System.out.println("[BroadcasterPool]: Thread count set to " + threads);
    // }
    // }
    public synchronized void setThreadCount(int threads) {
        if (threads == this.numThreads)
            return;

        System.out.println("[BroadcasterPool] Adjusting thread count: " + threads);

        int diff = threads - this.numThreads;
        this.numThreads = threads;

        if (diff > 0) {
            // เพิ่ม thread ใหม่
            for (int i = 0; i < diff; i++) {
                pool.submit(new Broadcaster(taskQueue, roomRegistry, clientRegistry));
            }
        } else if (diff < 0) {
            // ลด thread (ใช้ trick: interrupt thread บางตัวให้หลุดจาก loop)
            for (int i = 0; i < -diff; i++) {
                taskQueue.offer(new BroadcastTask("__CONTROL__", "STOP_THREAD"));
            }
        }
    }

    // ---------------- ฟังก์ชันส่งงานกระจายข้อความ ----------------
    public void submitTask(BroadcastTask task) {
        if (SHOWDEBUG)
            System.err.println("[BroadcasterPool]: Submitting broadcast task for room " + task.getRoomName());
        taskQueue.offer(task);
        if (SHOWDEBUG)
            System.err.println("[BroadcasterPool]: Submitted to queue. Current queue size: " + taskQueue.size());
    }

    // --------------------------- Broadcaster class ---------------------------
    public class Broadcaster implements Runnable {
        private final BlockingQueue<BroadcastTask> taskQueue;
        private final RoomRegistry roomRegistry;
        private final ClientRegistry clientRegistry;

        public Broadcaster(BlockingQueue<BroadcastTask> taskQueue, RoomRegistry roomRegistry,
                ClientRegistry clientRegistry) {
            this.taskQueue = taskQueue; // ดึงมาจาก pool เพื่อดูทำงานที่ต้องทำ
            this.roomRegistry = roomRegistry; // ดึงมาเพื่อดูสมาชิกในห้อง
            this.clientRegistry = clientRegistry; // ดึงมาเพื่อส่งข้อความไปยังสมาชิก
        }

        @Override
        public void run() {
            try {
                while (true) {
                    BroadcastTask task = taskQueue.take(); // ดึงงานจาก taskQueue ออกมาทำ
                    if ("__CONTROL__".equals(task.getRoomName()) && "STOP_THREAD".equals(task.getMessage())) {
                        System.out.println("[Broadcaster] Thread stopped gracefully");
                        return; // ออกจาก loop
                    }

                    String room = task.getRoomName();
                    String message = task.getMessage();

                    System.out.println("[Broadcaster]: Broadcasting message to room " + room + ": " + message);

                    Set<User> members = roomRegistry.getMembers(room); // ดึงสมาชิกในห้องนั้นๆ
                    if (SHOWDEBUG)
                        System.out.println("[Broadcaster]: Found " + members + " members in room " + room);
                    if (members == null || members.isEmpty())
                        continue;
                    for (User member : members) {
                        if (SHOWDEBUG)
                            System.out.println("Sending message to " + member.getClientId() + ": " + message);
                        clientRegistry.sendDirectMessage(member, "[" + room + "]" + message); // ส่งข้อความไปยังสมาชิกแต่ละคน
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
