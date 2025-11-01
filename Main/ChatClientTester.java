import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

/**
 * ChatClientTester.java
 * 
 * โปรแกรมทดสอบฝั่ง Client สำหรับระบบ Chat Server
 * - ให้เปิด Server_Os ก่อนรัน
 * - ใช้คำสั่งภายในเพื่อเปลี่ยนจำนวน Broadcaster threads อัตโนมัติ
 * - วัด latency / throughput แล้วสรุปเปรียบเทียบ
 */
public class ChatClientTester {

    private static final ServerConnection serverConnection = new ServerConnection();
    private static final String ROOM = "#os-lab";

    // จำนวน clients และข้อความต่อ client
    private static final int NUM_CLIENTS = 5000;
    private static final int MESSAGES_PER_CLIENT = 50;

    // จำนวน threads ที่จะใช้ทดสอบ
    private static final int[] THREAD_COUNTS = {1, 2, 4, 6, 8};

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
            Thread.sleep(1000); // รอให้ server ปรับ pool เสร็จก่อน

            // 🔹 เริ่มการทดสอบ
            TestResult r = runTestRound(threads);
            results.add(r);
        }

        // 🔹 แสดงตารางสรุปผลเปรียบเทียบ
        System.out.println("\n=== PERFORMANCE SUMMARY ===");
        System.out.printf("%-10s %-15s %-20s %-15s%n", "Threads", "Total Time (s)", "Avg Latency (ms/msg)", "Throughput (msg/s)");
        for (TestResult r : results) {
            System.out.printf("%-10d %-15.2f %-20.2f %-15.2f%n",
                    r.threads, r.totalTime / 1000.0, r.avgLatency, r.throughput);
        }
        System.out.println("====================================");
    }

    // --------------------------------------------
    // ส่งคำสั่งไปสั่งให้ Server เปลี่ยน thread
    // --------------------------------------------
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

    // --------------------------------------------
    // รันการทดสอบหนึ่งรอบ (ที่จำนวน thread เฉพาะ)
    // --------------------------------------------
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
        for (Future<Long> f : latencyResults) totalLatency += f.get();

        long totalTime = System.currentTimeMillis() - globalStart;
        double avgLatency = totalLatency / (double) totalMessages;
        double throughput = totalMessages / (totalTime / 1000.0);

        System.out.printf("Threads=%d => Time=%.2fs, AvgLatency=%.2fms, Throughput=%.2f msg/s%n",
                threads, totalTime / 1000.0, avgLatency, throughput);

        return new TestResult(threads, totalTime, avgLatency, throughput);
    }

    // --------------------------------------------
    // จำลอง client ปกติ
    // --------------------------------------------
    private static long runClient(String clientName) {
        long totalLatency = 0;
        try (Socket socket = new Socket(serverConnection.getAddress(), serverConnection.getPort());
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            socket.setSoTimeout(1000);

            out.println("HELLO " + clientName);
            safeRead(in, "[" + clientName + "]: ");

            out.println("JOIN " + ROOM);
            safeRead(in, "[" + clientName + "]: ");

            for (int i = 1; i <= MESSAGES_PER_CLIENT; i++) {
                String msg = "Hello " + i + " from " + clientName;
                long sendTime = System.currentTimeMillis();
                out.println("SAY " + ROOM + " " + msg);
                safeRead(in, null);
                totalLatency += System.currentTimeMillis() - sendTime;
            }

            out.println("QUIT");
            safeRead(in, null);

        } catch (Exception ignored) {}
        return totalLatency;
    }

    // --------------------------------------------
    // อ่านบรรทัดแบบไม่ block
    // --------------------------------------------
    private static void safeRead(BufferedReader in, String label) {
        try {
            if (in.ready()) {
                String line = in.readLine();
                if (label != null) System.out.println(label + line);
            } else {
                long start = System.currentTimeMillis();
                while (!in.ready() && (System.currentTimeMillis() - start) < 500) Thread.sleep(20);
                if (in.ready()) {
                    String line = in.readLine();
                    if (label != null) System.out.println(label + line);
                }
            }
        } catch (Exception ignore) {}
    }

    // --------------------------------------------
    // โครงสร้างเก็บผลการทดสอบแต่ละรอบ
    // --------------------------------------------
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
