// ----------- BroadcastTask -------------
// คลาสนี้ใช้เป็น "หน่วยงาน (Task)" สำหรับกระจายข้อความไปยังห้อง (room)
// จะถูกสร้างและส่งเข้าไปในคิวของ BroadcasterPool เพื่อให้ thread อื่นนำไปส่งต่อจริง
public class BroadcastTask {

    private final String roomName;
    private final String message;
    private final long enqueueTime;

    // ---------------- Constructor ----------------
    // สร้าง BroadcastTask พร้อมบันทึกเวลาที่ถูกสร้าง
    public BroadcastTask(String roomName, String message) {
        this.roomName = roomName;
        this.message = message;
        this.enqueueTime = System.currentTimeMillis(); // จับเวลาขณะสร้าง task
    }

    // ---------------- Getter Methods ----------------

    // คืนค่าชื่อห้องที่ต้องการส่งข้อความ
    public String getRoomName() {
        return roomName;
    }

    // คืนค่าข้อความที่จะส่ง
    public String getMessage() {
        return message;
    }

    // คืนค่าเวลาที่ task ถูก enqueue (หน่วยเป็นมิลลิวินาที)
    // ใช้ประโยชน์ในการตรวจสอบ performance หรือ delay ได้
    public long getEnqueueTime() {
        return enqueueTime;
    }
}
