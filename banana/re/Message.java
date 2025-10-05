package banana.re;

import java.io.Serializable;

public class Message implements Serializable {
    private final String sender; // "System" หรือชื่อคนส่ง
    private final String text;   // เนื้อความ
    private final String room;   // ห้อง (หรือ "(DM)")
    private final String type;   // "system" | "chat" | "error"

    public Message(String sender, String text, String room, String type) {
        this.sender = sender; this.text = text; this.room = room; this.type = type;
    }
    @Override public String toString() { return "[" + room + "] " + sender + ": " + text; }
}

