//--------------- ฝั่ง Server -----------------------
//ออกแบบคำสั่ง แต่ละส่วน
//DM "ชื่อคนผู้รับ" "ข้อความ"  #ส่งตรงถึงเพื่อนเจาะจงคน#
//JOIN "#ชื่อห้อง"          #เข้าห้อง#
//WHO "#ชื่อห้อง"           #ใช้ดูว่าห้องนี้มีใครบ้าง#
//SAY "#ชื่อห้อง" "ข้อความ"  #ส่งข้อความถึงทุกคนในห้อง#
//LEAVE "#ชื่อห้อง"         #ออกจากห้อง#
//QUIT                   #ออกโปรแกรม#

//เวลามีคน JOIN ห้องคนแรก -> สร้างห้อง
//ออกจากห้องหมดทุกคน -> ลบห้อง

//เก็บเป็น array -> jason
//Room Registry – บันทึกว่าห้องไหนมี client ID อะไรอยู่บ้าง -> Server
//Client Registry – บันทึกว่า client ID ไหนมี reply queue อะไรเพื่อใช้ส่งข้อความกลับ
//ต้องมีการป้องกัน (ไม่ให้เห็นข้อความทั้งหมด ขอเรื่องๆมาแล้วค่อยส่งกลับ)

//Server ก็ต้องส่งข้อความหาทุกคนเป็น system even เช่น “Alice joined”, “ห้องว่างแล้ว” -> ลบห้องนะ

import java.io.Console;
import java.util.Scanner;

public class Server_Os {
    public String Pre_client() {
        Scanner sc = new Scanner(System.in);
        System.out.println("============= All Instructions =============\n" +
                "DM \"receiver_name\" \"message\"   #send a direct message to a specific friend#\r\n" +
                "JOIN \"#room_name\"              #join a chat room#\r\n" +
                "WHO \"#room_name\"               #see who is in the room#\r\n" +
                "SAY \"#room_name\" \"message\"     #send a message to everyone in the room#\r\n" +
                "LEAVE \"#room_name\"             #leave the room#\r\n" +
                "QUIT                           #exit the program# \r\n" +
                "============================================\n");

        System.out.print("Your Instruction: ");
        String instruction = sc.nextLine();
        return instruction;
    }

    public void main(String[] args) {
        String cur_instruction;
        cur_instruction = Pre_client();
        System.out.print("current insruction : " + cur_instruction);

    }

}
