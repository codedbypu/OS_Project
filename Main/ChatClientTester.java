import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class ChatClientTester {
    // การตั้งค่า server connection และห้องแชท
    private static final ServerConnection serverConnection = new ServerConnection();
    private static final String ROOM = "#os-lab";

    // จำนวน clients และข้อความต่อ client
    private static final int NUM_CLIENTS = 1;
    private static final int MESSAGES_PER_CLIENT = 10;

    // จำนวน threads ที่จะใช้ทดสอบ และเวลารอหลังเปลี่ยนแปลง
    private static final int[] THREAD_COUNTS = {1};
    private static final int SLEEPTIME_MS = 1000;

    public static void main(String[] args) throws Exception {
        System.out.println("=== ChatClientTester started ===");
        System.out.println("Connecting to " + serverConnection.getAddress() + ":" + serverConnection.getPort());

        List<TestResult> results = new ArrayList<>();

        for (int threads : THREAD_COUNTS) {
            System.out.println("\n===============================");
            System.out.println(">>> Testing Broadcaster Threads = " + threads);
            System.out.println("===============================");

            // 🔹 สั่ง server ให้เปลี่ยนจำนวน thread ผ่าน client พิเศษ
            sendThreadChangeCommand(threads);
            Thread.sleep(SLEEPTIME_MS); // รอให้ server ปรับ pool เสร็จก่อน

            // 🔹 เริ่มการทดสอบ
            TestResult r = runTestRound(threads);
            results.add(r);
        }

        // 🔹 แสดงตารางสรุปผลเปรียบเทียบ
        System.err.println("\nClient number: " + NUM_CLIENTS + ", Messages per client: " + MESSAGES_PER_CLIENT);
        System.out.println("====== PERFORMANCE SUMMARY =======");
        System.out.printf("%-10s %-15s %-20s %-15s%n", "Threads", "Total Time (s)", "Avg Latency (ms/msg)",
                "Throughput (msg/s)");
        for (TestResult r : results) {
            System.out.printf("%-10d %-15.4f %-20.4f %-15.4f%n",
                    r.threads, r.totalTime / 1000.0, r.avgLatency, r.throughput);
        }
        System.out.println("====================================");
    }

    // ส่งคำสั่งไปสั่งให้ Server เปลี่ยน thread
    private static void sendThreadChangeCommand(int threads) {
        try (Socket socket = new Socket(serverConnection.getAddress(), serverConnection.getPort());
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            // ใช้ client "Admin" ส่งคำสั่งพิเศษที่ server เข้าใจ เช่น "SET_THREADS <n>"
            out.println("HELLO Admin");
            in.readLine(); // read welcome
            out.println("SET_THREADS " + threads); // 🔸 Server มี setThreadCount() อยู่แล้ว
            out.println("QUIT");
        } catch (IOException e) {
            System.err.println("[Admin] Error: " + e.getMessage());
        }
    }

    // รันการทดสอบหนึ่งรอบ (ที่จำนวน thread เฉพาะ)
    private static TestResult runTestRound(int threads) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(NUM_CLIENTS);
        List<Future<Long>> latencyResults = new ArrayList<>();

        long globalStart = System.currentTimeMillis();

        for (int i = 0; i < NUM_CLIENTS; i++) {
            final int id = i;
            latencyResults.add(pool.submit(() -> runClient("User" + threads + "T_" + id)));
        }

        pool.shutdown();
        pool.awaitTermination(120, TimeUnit.SECONDS);

        long totalLatency = 0;
        int totalMessages = NUM_CLIENTS * MESSAGES_PER_CLIENT;
        for (Future<Long> f : latencyResults)
            totalLatency += f.get();

        long totalTimeMs = System.currentTimeMillis() - globalStart;
        double avgLatencyMs = totalLatency / (double) totalMessages / 1_000_000.0;
        double throughput = totalMessages / (totalTimeMs / 1000.0);

        System.out.printf("[Threads=%d]: Time = %.4fs, AvgLatency = %.4fms, Throughput = %.4f msg/s%n",
                threads, totalTimeMs / 1000.0, avgLatencyMs, throughput);

        return new TestResult(threads, totalTimeMs, avgLatencyMs, throughput);
    }

    // จำลอง client ที่ส่งข้อความไปยัง server
    private static long runClient(String clientName) {
        long totalLatency = 0;
        try (Socket socket = new Socket(serverConnection.getAddress(), serverConnection.getPort());
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            socket.setSoTimeout(1000);

            out.println("HELLO " + clientName);
            safeRead(in);

            out.println("JOIN " + ROOM);
            safeRead(in);
            while (true) {
                String line = in.readLine();
                if (line != null && line.contains("joined the room."))
                    break;
            }

            for (int i = 1; i <= MESSAGES_PER_CLIENT; i++) {
                String msg = "Hello" + i + "from" + clientName;
                long sendTime = System.nanoTime();
                out.println("SAY " + ROOM + " " + msg);
                totalLatency += System.nanoTime() - sendTime; // latency เป็น ns
            }

            out.println("QUIT");
            safeRead(in);

        } catch (Exception ignored) {
        }
        return totalLatency;
    }

    // อ่านข้อมูลจาก server อย่างปลอดภัย (ไม่บล็อก)
    private static void safeRead(BufferedReader in) {
        try {
            if (in.ready())
                in.readLine(); // อ่านแล้ว discard
        } catch (Exception ignore) {
        }
    }

    // โครงสร้างเก็บผลการทดสอบ
    private static class TestResult {
        int threads;
        long totalTime;
        double avgLatency;
        double throughput;

        TestResult(int threads, long totalTime, double avgLatency, double throughput) {
            this.threads = threads;
            this.totalTime = totalTime;
            this.avgLatency = avgLatency;
            this.throughput = throughput;
        }
    }
}
