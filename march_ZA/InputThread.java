package march_ZA;

import java.io.PrintWriter;
import java.util.Scanner;

// Thread สำหรับอ่านคำสั่งจากแป้นพิมพ์ แล้วส่งคำสั่งไปยัง Server
public class InputThread implements Runnable {
    private PrintWriter out; // ใช้ส่งข้อมูลออกไปหา Server

    public InputThread(PrintWriter out) {
        this.out = out;
    }

    @Override
    public void run() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            String cmd = sc.nextLine(); // รอผู้ใช้พิมพ์คำสั่ง
            out.println(cmd);           // ส่งคำสั่งไปยัง Server

            // ถ้าผู้ใช้พิมพ์ QUIT ให้หยุดโปรแกรม
            if (cmd.equalsIgnoreCase("QUIT")) break;
        }
        sc.close();
    }
}
