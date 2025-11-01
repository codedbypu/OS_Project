import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.*;

public class ClientRegistry {
    // เก็บ reply queue ของแต่ละ client
    private final List<User> clientWriters = new CopyOnWriteArrayList<>();

    // เพิ่ม client เข้าสู่ระบบ
    public void registerClient(User user) {
        clientWriters.add(user);
    }

    // ลบ client ออกจากระบบ
    public void unregisterClient(User user) {
        clientWriters.remove(user);
    }

    // ดึงรายชื่อ client ทั้งหมด
    public List<User> getAllUsers() {
        return clientWriters;
    }

    // เช็คว่ามี Client คนนี้อยู่ในระบบหรือไม่ โดยใช้ clientId
    public synchronized boolean hasClientId(String clientId) {
        for (User user : clientWriters) {
            if (user.getClientId().equals(clientId)) {
                return true;
            }
        }
        return false;
    }

    // ดึง User object โดยใช้ clientId
    public User getUserById(String id) {
        for (User u : clientWriters) {
            if (u.getClientId().equals(id))
                return u;
        }
        return null;
    }

    // ส่งข้อความตรงไปยัง client ที่ระบุ
    public void sendDirectMessage(User user, String message) {
        PrintWriter writer = user.getClientPrintWriter();
        if (writer != null) {
            writer.println(message);
        } else {
            System.out.println("[System] Not found receiver: " + user.getClientId());
        }
    }
}
