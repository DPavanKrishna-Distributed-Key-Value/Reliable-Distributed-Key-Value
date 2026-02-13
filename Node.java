import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
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
    private final String storageFile; // UPGRADE 3: Persistence file
    // PHASE 1 & 2: Use VersionedValue
    private final ConcurrentHashMap<String, VersionedValue> store = new ConcurrentHashMap<>();
    private volatile boolean isAlive = true;
    private ServerSocket serverSocket;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public Node(String nodeId, int port) {
        this.nodeId = nodeId;
        this.port = port;
        this.storageFile = "storage_" + nodeId + ".txt"; // UPGRADE 3
        System.out.println("Node created: " + nodeId + " on port " + port);

        // UPGRADE 3: Restore data from disk
        loadFromDisk();

        // Load initial data (if needed, or maybe removed if purely relying on
        // sync/persistence)
        // DataLoader.loadSessions("user_sessions.txt", store);
        // NOTE: Keeping DataLoader for initial seeding if empty, but persistence is
        // primary.
        if (store.isEmpty()) {
            DataLoader.loadSessions("user_sessions.txt", store);
        }

        // Start server thread
        startServer();
    }

    // UPGRADE 3: Persistence - Restore
    private void loadFromDisk() {
        System.out.println("[" + nodeId + "] Loading data from " + storageFile + "...");
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(storageFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Format: key:value:version
                // Be careful with splitting if value contains colons.
                // Protocol assumes PUT:key:val:ver. Here we store key:val:ver.
                // Simpler parsing for academic purposes: assume keys/values don't break format
                // easily or use last index for version.

                int firstColon = line.indexOf(':');
                int lastColon = line.lastIndexOf(':');

                if (firstColon == -1 || lastColon == -1 || firstColon == lastColon)
                    continue;

                String key = line.substring(0, firstColon);
                String value = line.substring(firstColon + 1, lastColon);
                int version = Integer.parseInt(line.substring(lastColon + 1));

                store.put(key, new VersionedValue(value, version));
                count++;
            }
            System.out.println("[" + nodeId + "] Restored " + count + " records from disk.");
        } catch (IOException e) {
            System.out.println("[" + nodeId + "] No existing storage found or error reading: " + e.getMessage());
        }
    }

    // UPGRADE 3: Persistence - Append
    private synchronized void saveToDisk(String key, String value, int version) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(storageFile, true))) {
            bw.write(key + ":" + value + ":" + version);
            bw.newLine();
        } catch (IOException e) {
            System.err.println("[" + nodeId + "] Disk Write Error: " + e.getMessage());
        }
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
                // UPGRADE 3: Save to disk
                saveToDisk(key, value, version);
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