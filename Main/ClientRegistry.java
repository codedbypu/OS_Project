import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.*;

public class ClientRegistry {
    // เก็บ reply queue ของแต่ละ client
    private final List<User> clientWriters = new CopyOnWriteArrayList<>();

    public void registerClient(User user) {
        clientWriters.add(user);
    }

    public void unregisterClient(User user) {
        clientWriters.remove(user);
    }

    public synchronized boolean hasClientId(String clientId) {
        for (User user : clientWriters) {
            if (user.getClientId().equals(clientId)) {
                return true;
            }
        }
        return false;
    }

    public User getUserById(String id) {
    for (User u : clientWriters) {
        if (u.getClientId().equals(id)) return u;
    }
    return null;
}

    // ส่งข้อความตรง
    public void sendDirectMessage(User user, String message) {
        PrintWriter writer = user.getClientPrintWriter();
        if (writer  != null) {
            writer.println(message);
        } else {
            System.out.println("[System] Not found receiver: " + user.getClientId());
        }
    }

    public List<User> getAllUsers() {
        return clientWriters;
    }
}
