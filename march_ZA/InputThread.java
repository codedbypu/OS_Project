package march_ZA;

import java.io.PrintWriter;
import java.util.Scanner;

public class InputThread implements Runnable {
    private PrintWriter out;

    public InputThread(PrintWriter out) {
        this.out = out;
    }

    @Override
    public void run() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            String cmd = sc.nextLine();
            out.println(cmd); // ส่งคำสั่งไป server
            if (cmd.equalsIgnoreCase("QUIT")) break;
        }
        sc.close();
    }
}
