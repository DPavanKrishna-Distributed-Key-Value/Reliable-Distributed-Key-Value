import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Node {
    private final String nodeId;
    private final int port;
    // PHASE 1 & 2: Use VersionedValue
    private final ConcurrentHashMap<String, VersionedValue> store = new ConcurrentHashMap<>();
    private volatile boolean isAlive = true;
    private ServerSocket serverSocket;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public Node(String nodeId, int port) {
        this.nodeId = nodeId;
        this.port = port;
        System.out.println("Node created: " + nodeId + " on port " + port);

        // Load initial data
        DataLoader.loadSessions("user_sessions.txt", store);

        // Start server thread
        startServer();
    }

    private void startServer() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                System.out.println("[" + nodeId + "] Listening on port " + port);
                while (!serverSocket.isClosed()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        if (isAlive) {
                            executor.submit(() -> handleRequest(clientSocket));
                        } else {
                            clientSocket.close(); // Simulate failure by dropping connection
                        }
                    } catch (IOException e) {
                        if (!serverSocket.isClosed()) {
                            System.err.println("[" + nodeId + "] Accept error: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("[" + nodeId + "] Server start error: " + e.getMessage());
            }
        }).start();
    }

    // PHASE 9: Network Delay Simulation
    private volatile boolean simulateNetworkDelay = false;
    private final int randomDelay = 500; // ms

    public void setSimulateNetworkDelay(boolean simulate) {
        this.simulateNetworkDelay = simulate;
        System.out.println("[" + nodeId + "] Network Delay Simulation: " + simulate);
    }

    private void handleRequest(Socket socket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            String inputLine = in.readLine();
            if (inputLine == null)
                return;

            // PHASE 9: Artificial delay
            if (simulateNetworkDelay) {
                try {
                    Thread.sleep((long) (Math.random() * randomDelay));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Log received command (for debugging)
            // System.out.println("[" + nodeId + "] Received: " + inputLine);

            String response = processCommand(inputLine);
            out.println(response);

        } catch (IOException e) {
            System.err.println("[" + nodeId + "] Handling error: " + e.getMessage());
        }
    }

    // PHASE 1: Request Handler
    private String processCommand(String command) {
        String[] parts = command.split(":");
        String type = parts[0];

        switch (type) {
            case "PUT":
                // Format: PUT:key:value:version
                if (parts.length < 4)
                    return "ERROR:InvalidPUTFormat";
                String key = parts[1];
                String value = parts[2];
                int version = Integer.parseInt(parts[3]);
                return handlePut(key, value, version);

            case "GET":
                // Format: GET:key
                if (parts.length < 2)
                    return "ERROR:InvalidGETFormat";
                return handleGet(parts[1]);

            case "HEARTBEAT":
                return "ALIVE";

            case "SYNC_REQUEST":
                // Optional: If node asks for sync
                return "SYNC_ACK";

            case "SYNC_DATA":
                // Format: SYNC_DATA:key:value:version
                if (parts.length < 4)
                    return "ERROR:InvalidSyncFormat";
                return handlePut(parts[1], parts[2], Integer.parseInt(parts[3]));

            case "KILL":
                kill();
                return "ACK_KILL";

            case "REVIVE":
                revive();
                return "ACK_REVIVE";

            default:
                return "ERROR:UnknownCommand";
        }
    }

    // PHASE 2: Modified PUT logic
    private String handlePut(String key, String value, int version) {
        store.compute(key, (k, existing) -> {
            if (existing == null || version > existing.version) {
                System.out.println("[" + nodeId + "] PUT " + key + " v" + version + " (Updated)");
                return new VersionedValue(value, version);
            } else {
                System.out.println("[" + nodeId + "] PUT " + key + " v" + version + " (Ignored, current: v"
                        + existing.version + ")");
                return existing;
            }
        });
        return "ACK";
    }

    // PHASE 2: Modified GET logic
    private String handleGet(String key) {
        VersionedValue vv = store.get(key);
        if (vv == null) {
            return "NULL";
        }
        // Format: VALUE:key:value:version
        return "VALUE:" + key + ":" + vv.value + ":" + vv.version;
    }

    public void kill() {
        isAlive = false;
        System.out.println("[" + nodeId + "] KILLED (Stops accepting requests)");
    }

    public void revive() {
        isAlive = true;
        System.out.println("[" + nodeId + "] REVIVED");
    }

    // For Main.java to keep process running
    public void join() throws InterruptedException {
        synchronized (this) {
            wait();
        }
    }
}