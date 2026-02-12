import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TestClient {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java TestClient <ip> <port> <command>");
            return;
        }

        String ip = args[0];
        int port = Integer.parseInt(args[1]);
        String command = args[2];

        // Reconstruct command if it had spaces (e.g. PUT:key:val)
        // Actually arguments are space separated by shell, but our protocol uses
        // colons.
        // So "PUT:k:v" is one arg.

        try (Socket socket = new Socket(ip, port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Sending: " + command);
            out.println(command);
            String response = in.readLine();
            System.out.println("Response: " + response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
