import java.util.Set;
import java.util.concurrent.*;

public class BroadcasterPool {
    private final BlockingQueue<BroadcastTask> taskQueue = new LinkedBlockingQueue<>(); // ใช้เก็บงานกระจายข้อความ
    private final ClientRegistry clientRegistry;
    private final RoomRegistry roomRegistry;
    private ExecutorService pool;
    private int numThreads;

    // ---------------- Initialization ----------------
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

    // ---------------- ฟังก์ชันปรับจำนวน threads ----------------
    public void setThreadCount(int threads) {
        if (threads != this.numThreads) {
            pool.shutdownNow(); // ปิด pool เก่า
            this.numThreads = threads; // อัปเดตจำนวน threads
            startPool(threads); // สร้าง pool ใหม่
            System.out.println("[BroadcasterPool]: Thread count set to " + threads);
        }
    }

    // ---------------- ฟังก์ชันส่งงานกระจายข้อความ ----------------
    public void submitTask(BroadcastTask task) {
        taskQueue.offer(task);
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
                    String room = task.getRoomName();
                    String message = task.getMessage();

                    Set<User> members = roomRegistry.getMembers(room); // ดึงสมาชิกในห้องนั้นๆ
                    if (members == null || members.isEmpty())
                        continue;
                    for (User member : members) {
                        clientRegistry.sendDirectMessage(member, "[" + room + "]" + message); // ส่งข้อความไปยังสมาชิกแต่ละคน
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
