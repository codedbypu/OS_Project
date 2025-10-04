public class BroadcastTask {
    private String roomName;
    private String message;

    public BroadcastTask(String roomName, String message) {
        this.roomName = roomName;
        this.message = message;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getMessage() {
        return message;
    }
}