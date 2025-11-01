public class BroadcastTask {
    private final String roomName;
    private final String message;
    private final long enqueueTime;

    public BroadcastTask(String roomName, String message) {
        this.roomName = roomName;
        this.message = message;
        this.enqueueTime = System.currentTimeMillis();
    }

    public String getRoomName() {
        return roomName;
    }

    public String getMessage() {
        return message;
    }

    public long getEnqueueTime() {
        return enqueueTime;
    }
}