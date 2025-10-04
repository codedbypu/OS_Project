package banana.re;

import java.io.Serializable;
import java.util.Map;

public class Command implements Serializable {
    private final String type;          // JOIN, SAY, DM, WHO, LEAVE, QUIT
    private final String clientId;      // ใครเป็นคนสั่ง
    private final Map<String, String> args; // room, text, target ...

    public Command(String type, String clientId, Map<String, String> args) {
        this.type = type; this.clientId = clientId; this.args = args;
    }
    public String getType() { return type; }
    public String getClientId() { return clientId; }
    public Map<String, String> getArgs() { return args; }

    @Override public String toString() { return type + " from " + clientId + " " + args; }
}
