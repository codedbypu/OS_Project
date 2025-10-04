package march_ZA;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

// คลาสหลักของฝั่ง Client — จัดการการเชื่อมต่อกับ Server และสร้าง Thread ต่าง ๆ
public class ChatClient {
    private String clientName;                              // ชื่อของ client (ระบุเวลาเริ่มโปรแกรม)
    private Socket socket;                                  // ช่องทางเชื่อมต่อกับ Server
    private PrintWriter out;                                // สำหรับส่งข้อมูลออกไปยัง Server
    private BufferedReader in;                              // สำหรับรับข้อมูลจาก Server
    private BlockingQueue<String> replyQueue = new LinkedBlockingQueue<>(50); // คิวสำหรับเก็บข้อความตอบกลับจาก Server

    // Constructor — เมื่อสร้าง ChatClient จะเชื่อมต่อกับ Server ทันที
    public ChatClient(String host, int port, String name) throws IOException {
        this.clientName = name;
        socket = new Socket(host, port); // เชื่อมต่อกับ Server ผ่าน TCP socket
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // แจ้ง Server ว่า "นี่คือฉัน"
        out.println("REGISTER " + clientName);

        // สร้าง 2 Thread ทำงานพร้อมกัน
        new Thread(new InputThread(out)).start();               // อ่านคำสั่งจากคีย์บอร์ดและส่งให้ Server
        new Thread(new ReceiverThread(in, replyQueue)).start(); // รอรับข้อความจาก Server แล้วเก็บลงคิว
        new Thread(new ReplyPrinter(replyQueue)).start();       // แสดงข้อความจากคิวออกหน้าจอ
    }

    // ปิดการเชื่อมต่อ
    public void close() throws IOException {
        socket.close();
    }
}
