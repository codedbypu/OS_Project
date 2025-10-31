import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RoomRegistry {
    // Map<ชื่อห้อง, รายชื่อสมาชิกในห้อง>
    private final Map<String, Set<User>> rooms = new HashMap<>();
    // ตัวล็อกอ่าน–เขียน เพื่อกันหลาย thread
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    // ----------------------------- เข้าห้อง -----------------------------
    public void joinRoom(String roomName, User user) {
        lock.writeLock().lock(); // เขียนข้อมูล ต้องล็อกแบบ write
        try {
            rooms.putIfAbsent(roomName, new HashSet<>()); // ถ้ายังไม่มีห้อง → สร้างห้องใหม่
            rooms.get(roomName).add(user); // เพิ่มสมาชิก
            System.out.println("[Server]: " + user.getClientId() + " joined " + roomName);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ----------------------------- ออกจากห้อง -----------------------------
    public void leaveRoom(String roomName, User user) {
        lock.writeLock().lock();
        try {
            if (rooms.containsKey(roomName)) {
                rooms.get(roomName).remove(user);
                System.out.println(user.getClientId() + " left " + roomName);

                // ถ้าห้องว่าง → ลบทิ้ง
                if (rooms.get(roomName).isEmpty()) {
                    rooms.remove(roomName);
                    System.out.println("[Server]: " + "Room " + roomName + " is now empty -> deleted.");
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ----------------------------- ดูสมาชิกในห้อง -----------------------------
    public Set<User> getMembers(String roomName) {
        lock.readLock().lock();
        try {
            return new HashSet<>(rooms.getOrDefault(roomName, Collections.emptySet()));
        } finally {
            lock.readLock().unlock();
        }
    }

    // ------------------------- เช็กสมาชิกใน RoomRegistry ----------------------
    public boolean isMember(String roomName, User user) {
        lock.readLock().lock();
        try {
            return rooms.containsKey(roomName) && rooms.get(roomName).contains(user);
        } finally {
            lock.readLock().unlock();
        }
    }

    // ----------------------------- ลบ user ออกจากทุกห้อง ----------------

    public void removeUserFromAllRooms(User user, BroadcasterPool broadcasterPool) {
        lock.writeLock().lock();
        try {
            Iterator<Map.Entry<String, Set<User>>> it = rooms.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Set<User>> entry = it.next();
                if (entry.getValue().remove(user)) {
                    System.out.println("[Server]: " + user.getClientId() + " removed from " + entry.getKey());
                    broadcasterPool.submitTask(
                            new BroadcastTask(entry.getKey(), ": " + user.getClientId() + " left the room."));
                    if (entry.getValue().isEmpty()) {
                        it.remove();
                        System.out.println("[Server]: Room " + entry.getKey() + " deleted.");
                    }
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}
