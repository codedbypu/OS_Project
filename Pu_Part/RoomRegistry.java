import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RoomRegistry {
    // Map<ชื่อห้อง, รายชื่อสมาชิกในห้อง>
    private final Map<String, Set<String>> rooms = new HashMap<>();
    // ตัวล็อกอ่าน–เขียน เพื่อกันหลาย thread
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    // ----------------------------- เข้าห้อง -----------------------------
    public void joinRoom(String roomName, String clientId) {
        lock.writeLock().lock(); // เขียนข้อมูล ต้องล็อกแบบ write
        try {
            rooms.putIfAbsent(roomName, new HashSet<>()); // ถ้ายังไม่มีห้อง → สร้างห้องใหม่
            rooms.get(roomName).add(clientId); // เพิ่มสมาชิก
            System.out.println("Server: " + clientId + " joined " + roomName);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ----------------------------- ออกจากห้อง -----------------------------
    public void leaveRoom(String roomName, String clientId) {
        lock.writeLock().lock();
        try {
            if (rooms.containsKey(roomName)) {
                rooms.get(roomName).remove(clientId);
                System.out.println(clientId + " left " + roomName);

                // ถ้าห้องว่าง → ลบทิ้ง
                if (rooms.get(roomName).isEmpty()) {
                    rooms.remove(roomName);
                    System.out.println("Server: " + "Room " + roomName + " is now empty -> deleted.");
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ----------------------------- ดูสมาชิกในห้อง -----------------------------
    public Set<String> getMembers(String roomName) {
        lock.readLock().lock();
        try {
            return new HashSet<>(rooms.getOrDefault(roomName, Collections.emptySet()));
        } finally {
            lock.readLock().unlock();
        }
    }

    // ----------------------------- debug ดูข้อมูลทั้งหมด -----------------------------
    public void printAllRooms() {
        lock.readLock().lock();
        try {
            System.out.println("===== Current Rooms =====");
            for (String room : rooms.keySet()) {
                System.out.println(room + " -> " + rooms.get(room));
            }
        } finally {
            lock.readLock().unlock();
        }
    }
}
