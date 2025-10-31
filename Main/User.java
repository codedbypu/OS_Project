import java.io.PrintWriter;

public class User {
    private String clientId;
    private PrintWriter PrintWriter;

    User(String clientId, PrintWriter PrintWriter) {
        this.clientId = clientId;
        this.PrintWriter = PrintWriter;
    }

    public String getClientId() {
        return clientId;
    }

    public PrintWriter getClientPrintWriter() {
        return PrintWriter;
    }
}
