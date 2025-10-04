package Dew;
// "*" = เอามาทั้งหมด
import java.io.*;
import java.net.*;
import java.nio.channels.ScatteringByteChannel;
import java.util.*;
import java.util.concurrent.*;

class client {
//"throws" = จะไม่จัดการกับข้อผิดพลาดเหล่านั้นเอง แต่จะ "โยน" ข้อผิดพลาดนั้นไปให้เมธอดที่เรียกมันมาจัดการแทน
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter your name: ");
        String clientId = sc.nextLine();

        //connect server and client prepare to send or call data
        Socket socket = new Socket("localhost", 5000); //ระบุปลายทาง
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        //send clientID to server
        out.writeObject(clientId);//แปลง object ให้เป็นลำดับของไบต์ (Serialization)
        out.flush();//บังคับให้ข้อมูลทั้งหมดที่อยู่ใน Buffer เขียนออกไปทันทีและล้าง Buffer นั้น

//เพื่อให้การรับข้อความจาก Server เป็นไปอย่าง ไม่ปิดกั้น (Non-blocking) หากไม่มีข้อความใด ๆ ส่งมาจาก Server เมธอด in.readObject() จะรอ (Block) การทำงานไว้ชั่วคราว การทำเช่นนี้ในเธรดแยกต่างหากจึงช่วยให้เธรดหลักของ Client ยังสามารถทำงานอื่น ๆ ต่อไปได้
        new Thread(() -> { //Runnable เป็น Task ที่จะถูกรันในเธรดแยกต่างหาก
            try {
                Object obj;
                while ((obj = in.readObject()) != null) { //call data from server
                    Message msg = (Message) obj;//convert type to object
                    System.out.println(msg);
                }
            } catch (Exception e) {
                System.out.println("Disconnected from server");
            }
        }).start();

        //correct input instruction from user
        while (true) {
            String line = sc.nextLine();

            if (line.equalsIgnoreCase("/quit")) { //ไม่สน lower-upper case
                sendCommand(out, new Command("QUIT", clientId, Map.of()));//create obj with clientID then send to server
                break;

            } else if (line.startsWith("/join ")) {
                String room = line.substring(6).trim();//ดึงชื่อห้องเริ่มตัว 6 เพราะตัด join
                sendCommand(out, new Command("JOIN", clientId, Map.of("room", room)));

            } else if (line.startsWith("/say ")) {
                String[] parts = line.split(" ", 3);
                if (parts.length >= 3) {//แบ่งเป็น 3 ส่วน (ClientID + ROOMID + TEXT)
                    sendCommand(out, new Command("SAY", clientId, Map.of("room", parts[1], "text", parts[2])));
                }
        
            } else if (line.startsWith("/dm ")) {
                String[] parts = line.split(" ", 3);
                if (parts.length >= 3) {//แบ่งเป็น 3 ส่วน (ClientIDส่ง + ClientIDรับ + TEXT)
                    sendCommand(out, new Command("DM", clientId, Map.of("target", parts[1], "text", parts[2])));
                }

            } else if (line.startsWith("/who ")) {
                String room = line.substring(5).trim();//ตัดคำสั่งออกเอาแค่ชื่อห้อง
                sendCommand(out, new Command("WHO", clientId, Map.of("room", room)));

            } else if (line.startsWith("/leave ")) {
                String room = line.substring(7).trim();//sometimes join many room
                sendCommand(out, new Command("LEAVE", clientId, Map.of("room", room)));

            } else {
                System.out.println("Unknown command");
            }
        }

        socket.close();//disconnected code from server-side

    }
}