import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.*;

public class ChatClientTester {
    // ---------------- CONFIG ----------------
    private static final ServerConnection serverConnection = new ServerConnection();
    private static final String ROOM = "#os-lab";

    private static final int NUM_CLIENTS = 250; // จำนวน client ที่จะจำลอง
    private static final int MESSAGES_PER_CLIENT = 1; // จำนวนข้อความที่แต่ละ client จะส่ง
    private static final int[] THREAD_COUNTS = {1, 2, 4, 8, 12, 16, 24}; // จำนวน threads ของ Broadcaster ที่จะทดสอบ
    private static final int ROUND_TEST_PER_THREAD = 1; // จำนวนรอบทดสอบต่อ thread setting

    private static final int SLEEPTIME_MS = 1000; // เวลารอหลังเปลี่ยนค่า threads

    private static final int JOIN_ACK_TIMEOUT_MS = 5000; // timeout ตอนรอ join room
    private static final int MESSAGE_READ_TIMEOUT_MS = 2000; // timeout รอ message broadcast กลับ ถ้าเกินจะข้ามไป
    // -----------------------------------------

    public static void main(String[] args) throws Exception {
        System.out.println("====== ChatClientTester started ======");
        System.out.println("Connecting to " + serverConnection.getAddress() + ":" + serverConnection.getPort());

        List<TestResult> results = new ArrayList<>();

        // ทดสอบแต่ละจำนวน thread ของ Broadcaster
        for (int threads : THREAD_COUNTS) {
            System.out.println("\n======================================");
            System.out.println(">>> Testing Broadcaster Threads = " + threads);
            System.out.println("======================================");

            sendThreadChangeCommand(threads);
            Thread.sleep(SLEEPTIME_MS);

            // ทดสอบหลายรอบต่อค่า thread เดียว
            for (int round = 1; round <= ROUND_TEST_PER_THREAD; round++) {
                TestResult r = runTestRound(threads); // รันการทดสอบ
                results.add(r); // เก็บผลลัพธ์หนึ่งรอบ
            }
        }

        // ---------------- SUMMARY ----------------
        System.out.println("\nClient number: " + NUM_CLIENTS);
        System.out.println("Messages per client: " + MESSAGES_PER_CLIENT);
        System.out.println("======== PERFORMANCE SUMMARY =========");
        System.out.printf("%-10s %-15s %-20s %-15s%n",
                "Threads", "Total Time (s)", "Avg Latency (ms/msg)", "Throughput (msg/s)"); // หัวตาราง
        for (TestResult r : results) {
            System.out.printf("%-10d %-15.3f %-20.3f %-15.3f%n",
                    r.threads, r.totalTime / 1000.0, r.avgLatency, r.throughput); // ผลลัพธ์แต่ละรอบ
        }
        System.out.println("======================================");
    }

    // ---------------- ฟังก์ชันส่งคำสั่งเปลี่ยนจำนวน threads ----------------
    private static void sendThreadChangeCommand(int threads) {
        try (Socket socket = new Socket(serverConnection.getAddress(), serverConnection.getPort());
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            out.println("HELLO Admin");
            out.println("SET_THREADS " + threads); // ส่งคำสั่งเปลี่ยนจำนวน Thread ของ Broadcaster ไปยัง Server
            out.println("QUIT");
            out.flush();

        } catch (Exception e) {
            System.err.println("[Admin] Error: " + e.getMessage());
        }
    }

    // ---------------- ฟังก์ชันรันการทดสอบหนึ่งรอบ ----------------
    private static TestResult runTestRound(int threads) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(NUM_CLIENTS);
        List<Future<ClientResult>> futures = new ArrayList<>();

        long globalStart = System.currentTimeMillis(); // เริ่มจับเวลารวมของการทดสอบ

        for (int i = 0; i < NUM_CLIENTS; i++) {
            final int id = i;
            futures.add(pool.submit(() -> runClient("User" + threads + "T_" + id)));
        }

        // ปิดการสร้างงานใหม่
        pool.shutdown();
        // รอจนกว่า runClient ทุกตัวทำงานเสร็จ แต่ถ้าเกิน 300 มิลวิ จะเชื่อดทิ้ง
        pool.awaitTermination(300, TimeUnit.SECONDS);

        long totalLatencyNs = 0L;
        int totalMessagesSent = 0;
        int totalReceived = 0;

        // รวมผลจากทุก client
        for (Future<ClientResult> f : futures) {
            try {
                ClientResult cr = f.get();
                totalLatencyNs += cr.totalLatencyNs;
                totalMessagesSent += cr.messagesSent;
                totalReceived += cr.messagesReceived;
            } catch (Exception e) {
                System.err.println("[Test] Error getting future: " + e.getMessage());
                e.printStackTrace();
            }
        }

        long totalTimeMs = System.currentTimeMillis() - globalStart; // หยุดจับเวลารวมของการทดสอบ
        double avgLatencyMs = (totalMessagesSent > 0)
                ? (totalLatencyNs / (double) totalMessagesSent) / 1_000_000.0
                : 0.0;
        double throughput = (totalTimeMs > 0)
                ? (totalMessagesSent / (totalTimeMs / 1000.0))
                : 0.0;

        System.out.printf(
                "[%dThreads]: Time = %.3fs, AvgLatency = %.3fms, Throughput = %.3f msg/s, TotalSent=%d, TotalReceived=%d%n",
                threads, totalTimeMs / 1000.0, avgLatencyMs, throughput, totalMessagesSent, totalReceived);

        return new TestResult(threads, totalTimeMs, avgLatencyMs, throughput);
    }

    // -------------------- จำลอง client หนึ่งตัว -------------------
    private static ClientResult runClient(String clientName) {
        long totalLatencyNs = 0;
        int messagesSent = 0;
        int messagesReceived = 0;

        try (Socket socket = new Socket(serverConnection.getAddress(), serverConnection.getPort());
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            // ส่ง HELLO เพื่อ register ไปที่ Server
            out.println("HELLO " + clientName);
            out.flush();
            String welcome = readLineWithTimeout(in, socket, 3000); // อ่านข้อความที่ได้จาก Server
            if (welcome == null) { // ถ้าเกิดข้อผิดพลาดในการ register
                System.err.println("[" + clientName + "] No welcome from server. Aborting.");
                return new ClientResult(totalLatencyNs, messagesSent, messagesReceived);
            }

            // ใช้คำสั่ง Join Room
            out.println("JOIN " + ROOM);
            out.flush();

            boolean joined = waitForJoinAck(in, socket, JOIN_ACK_TIMEOUT_MS); // รอจนกว่าจะได้เข้าห้องจริงๆ
            if (!joined) { // ถ้าเข้าห้องไม่ได้
                System.err.println("[" + clientName + "] join ack not received within timeout. Aborting.");
                return new ClientResult(totalLatencyNs, messagesSent, messagesReceived);
            }

            // ใช้คำสั่ง SAY เพื่อส่งข้อความไปตามจำนวนที่ตั้งไว้ แล้ววัด latency
            for (int i = 1; i <= MESSAGES_PER_CLIENT; i++) {
                String msg = "Hello_" + i;
                long startNs = System.nanoTime(); // เริ่มจับเวลาในการส่งข้อความ 1 ข้อความ

                out.println("SAY " + ROOM + " " + msg);
                out.flush();
                messagesSent++;

                // รอข้อความ broadcast กลับจากห้อง
                long msgDeadline = System.currentTimeMillis() + MESSAGE_READ_TIMEOUT_MS;
                boolean sawBroadcast = false;

                while (System.currentTimeMillis() < msgDeadline) {
                    String line = readLineWithTimeout(in, socket,
                            Math.max(500, (int) (msgDeadline - System.currentTimeMillis())));
                    if (line == null)
                        continue;

                    // ตรวจว่าเป็นข้อความ broadcast ของตัวเอง
                    if (line.startsWith("[" + ROOM + "]") && line.contains(clientName + ":")) {
                        long latencyNs = System.nanoTime() - startNs; // หยุดจับเวลาในการส่งข้อความ
                        totalLatencyNs += latencyNs;
                        messagesReceived++;
                        sawBroadcast = true;
                        break;
                    }
                }

                if (!sawBroadcast) { // ถ้าไม่เห็นข้อความตัวเอง
                    System.err.println(
                            "[" + clientName + "] Did not see broadcast for message " + i + " within timeout.");
                }
            }

            // ใช้คำสั่ง QUIT เพื่อปิดการเชื่อมต่อแบบปกติ
            out.println("QUIT");
            out.flush();
            readLineWithTimeout(in, socket, 500);
            Thread.sleep(80);

        } catch (Exception e) {
            System.err.println("[" + clientName + "] Error: " + e.getMessage());
            e.printStackTrace();
        }

        return new ClientResult(totalLatencyNs, messagesSent, messagesReceived);
    }

    // ------- อ่านข้อความที่ Server ส่งมา ถ้าเกินเวลาก็จะเชือดทิ้ง -------
    private static String readLineWithTimeout(BufferedReader in, Socket socket, int timeoutMs) throws IOException {
        // บันทึกค่า timeout เดิมไว้ (ถ้าตั้งไว้)
        int originalTimeout = socket.getSoTimeout();

        try {
            socket.setSoTimeout(timeoutMs);
            return in.readLine(); // รออ่านภายในเวลาที่กำหนด
        } catch (SocketTimeoutException e) {
            return null; // ถ้า timeout ก็คืนค่า null
        } finally {
            // คืนค่า timeout เดิมให้ socket (เพื่อไม่กระทบการอ่านครั้งถัดไป)
            socket.setSoTimeout(originalTimeout);
        }
    }

    // ----------- รอข้อความตอบรับจากการ JOIN ห้อง ---------------
    private static boolean waitForJoinAck(BufferedReader in, Socket socket, int timeoutMs) throws IOException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            String line = readLineWithTimeout(in, socket, Math.min(1000, timeoutMs));
            if (line == null)
                continue;
            if (line.contains(">>> Join room"))
                return true;
        }
        return false;
    }

    // ---------------- RESULT CLASSES ----------------
    private static class ClientResult {
        final long totalLatencyNs;
        final int messagesSent;
        final int messagesReceived;

        ClientResult(long totalLatencyNs, int messagesSent, int messagesReceived) {
            this.totalLatencyNs = totalLatencyNs;
            this.messagesSent = messagesSent;
            this.messagesReceived = messagesReceived;
        }
    }

    private static class TestResult {
        final int threads;
        final long totalTime;
        final double avgLatency;
        final double throughput;

        TestResult(int threads, long totalTime, double avgLatency, double throughput) {
            this.threads = threads;
            this.totalTime = totalTime;
            this.avgLatency = avgLatency;
            this.throughput = throughput;
        }
    }
}