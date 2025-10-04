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

import java.util.Scanner;
import java.util.LinkedList;
import java.util.Queue;

public class Server_Os {
    private Queue<String> control_queue = new LinkedList<>();

    public String Pre_client() {
        Scanner sc = new Scanner(System.in);
        System.out.println("============================= All Instructions =============================\n" +
                "DM \"receiver_name\" \"message\"   #send a direct message to a specific friend#\r\n" +
                "JOIN \"#room_name\"              #join a chat room#\r\n" +
                "WHO \"#room_name\"               #see who is in the room#\r\n" +
                "SAY \"#room_name\" \"message\"     #send a message to everyone in the room#\r\n" +
                "LEAVE \"#room_name\"             #leave the room#\r\n" +
                "QUIT                           #exit the program# \r\n" +
                "============================================================================");
        System.out.print("Your Instruction: ");
        String instruction = sc.nextLine();
        return instruction;
    }

    public void Router() {
        if (control_queue.isEmpty())
            return;

        String cur_instruction = control_queue.poll().trim();
        if (cur_instruction.isEmpty()) {
            System.out.println("Command : ERROR");
            return;
        }

        // แยกคำสั่งตามช่องว่าง
        String[] parts = cur_instruction.split(" ", 4);
        if (parts.length > 3) {
            System.out.println("Command : ERROR");
            return;
        }
        String command = parts[0].toUpperCase();
        String param1 = (parts.length > 1) ? parts[1] : "";
        String param2 = (parts.length > 2) ? parts[2] : "";

        System.out.println("Command : " + command);
        System.out.println("Parameter1 : " + param1);
        System.out.println("Parameter2 : " + param2);

        if (command.equals("JOIN")) {
            System.out.println(">>> เข้าห้อง: " + param1);
            
            // TODO: เรียก RoomRegistry.joinRoom(param1, clientId);

        } else if (command.equals("SAY")) {
            System.out.println(">>> พูดในห้อง " + param1 + " : " + param2);
            // TODO: broadcast ไปยังสมาชิกในห้อง

        } else if (command.equals("DM")) {
            System.out.println(">>> ส่งข้อความส่วนตัวถึง " + param1 + " : " + param2);
            // TODO: ส่งตรงไปยัง reply queue ของผู้รับ

        } else if (command.equals("WHO")) {
            System.out.println(">>> ขอรายชื่อสมาชิกในห้อง " + param1);
            // TODO: ดึงจาก RoomRegistry.getMembers(param1);

        } else if (command.equals("LEAVE")) {
            System.out.println(">>> ออกจากห้อง " + param1);
            // TODO: RoomRegistry.leaveRoom(param1, clientId);

        } else if (command.equals("QUIT")) {
            System.out.println(">>> ออกจากโปรแกรม");
            System.exit(0);

        } else {
            System.out.println("Unknown command: " + command);
        }
    }

    public static void main(String[] args) {
        Server_Os server = new Server_Os();
        String cur_instruction;

        while (true) {
            cur_instruction = server.Pre_client();
            server.control_queue.add(cur_instruction);
            System.out.println("Current Insruction : " + cur_instruction);
            System.out.println("Control Queue : " + server.control_queue + "\n");
            server.Router();
        }
    }

}
