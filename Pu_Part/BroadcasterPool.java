import java.util.Set;
import java.util.concurrent.*;

public class BroadcasterPool {
    private final BlockingQueue<BroadcastTask> taskQueue = new LinkedBlockingQueue<>();
    private final ExecutorService pool;

     public BroadcasterPool(int numThreads, RoomRegistry roomRegistry, ClientRegistry clientRegistry) {
        pool = Executors.newFixedThreadPool(numThreads);
        for (int i = 0; i < numThreads; i++) {
            pool.submit(new Broadcaster(taskQueue, roomRegistry, clientRegistry));
        }
    }

    // เพิ่มงานใหม่เข้า queue
    public void submitTask(BroadcastTask task) {
        taskQueue.offer(task);
    }

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

                    Set<String> members = roomRegistry.getMembers(room);
                    if (members == null || members.isEmpty())
                        continue;

                    for (String member : members) {
                        clientRegistry.sendDirectMessage(member, "["+ room +"]" + message);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
