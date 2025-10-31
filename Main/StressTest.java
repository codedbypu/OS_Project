public class StressTest {
    public static void main(String[] args) throws InterruptedException {
        ServerConnection Cn = new ServerConnection();
        int[] broadcasterThreads = { 1, 2, 4, 8 }; // ปรับจำนวน threads
        int clients = 10; // จำนวน client
        int messagesPerClient = 1000; // จำนวนข้อความต่อ client
        String serverAddress = Cn.getAddress();
        int serverPort = Cn.getPort();

        for (int threads : broadcasterThreads) {
            System.out.println("=== Testing with " + threads + " broadcaster threads ===");

            Server_Os.initServer();
            // ปรับ thread count
            Server_Os.broadcasterPool.setThreadCount(threads);

            // รีเซ็ต latency tracker ก่อนทดสอบ
            LatencyTracker.reset();

            // สร้างและรัน client หลายตัว
            Thread[] testClients = new Thread[clients];
            for (int i = 0; i < clients; i++) {
                testClients[i] = new Thread(new TestClient("Client" + i, messagesPerClient, serverAddress, serverPort));
                testClients[i].start();
            }

            // รอ client ทุกตัวส่งข้อความเสร็จ
            for (Thread t : testClients)
                t.join();

            System.out.println("[Result] Average Latency: " + LatencyTracker.getAverageLatency() + " ms");
            System.out.println();
        }
    }
}
