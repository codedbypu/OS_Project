package banana.re;

import java.io.*;
import java.net.*;
import java.util.*;

class client {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter your name: ");
        String clientId;
        while (true) {
            clientId = sc.nextLine().trim();
            if (clientId.isEmpty()) {
            System.out.print("ชื่อห้ามว่าง ใส่อีกครั้ง: ");
            continue;
            }
            File store = new File(System.getProperty("user.home"), ".banana_client_names");
            Set<String> names = new HashSet<>();
            if (store.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(store))) {
                String line;
                while ((line = br.readLine()) != null) {
                names.add(line.trim());
                }
            } catch (IOException ignored) {}
            }
            if (names.contains(clientId)) {
            System.out.print("ชื่อนี้ถูกใช้แล้ว ใส่อีกชื่อ: ");
            continue;
            }
            try (FileWriter fw = new FileWriter(store, true)) {
            fw.write(clientId + System.lineSeparator());
            } catch (IOException ignored) {}
            break;
        }

        Socket socket = new Socket("localhost", 5000);
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in  = new ObjectInputStream(socket.getInputStream());

        // ส่ง clientId ให้ server เหมือนเดิม
        out.writeObject(clientId);
        out.flush();

        // รับข้อความจาก server (ไม่เปลี่ยน)
        new Thread(() -> {
            try {
                Object obj;
                while ((obj = in.readObject()) != null) {
                    System.out.println(obj);
                }
            } catch (Exception e) {
                System.out.println("Disconnected from server");
            }
        }).start();

        // ส่งทุกอย่างที่ผู้ใช้พิมพ์ไปให้ server โดยไม่ตรวจสอบ/แยกคำสั่งที่ฝั่ง client
        while (true) {
            String line = sc.nextLine();
            out.writeObject(line);   // ให้ server เป็นคน parse เอง
            out.flush();

            if (line.equalsIgnoreCase("/quit")) {
                break; // ออกจากลูป (client ปิดเองหลังแจ้ง server แล้ว)
            }
        }

        socket.close();
    }
}
