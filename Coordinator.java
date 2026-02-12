import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Coordinator {
    private final int port;
    private final List<NodeInfo> nodes = new ArrayList<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, Integer> keyVersions = new HashMap<>(); // Track versions for keys
    private HeartbeatManager heartbeatManager;

    // Hardcoded node configuration for academic simplicity
    public Coordinator(int port) {
        this.port = port;
        // As per requirement: 3 nodes
        nodes.add(new NodeInfo("NodeA", "127.0.0.1", 8081));
        nodes.add(new NodeInfo("NodeB", "127.0.0.1", 8082));
        nodes.add(new NodeInfo("NodeC", "127.0.0.1", 8083));

        System.out.println("Coordinator started on port " + port);
        System.out.println("Registered 3 nodes: NodeA(8081), NodeB(8082), NodeC(8083)");

        // Start Heartbeat Manager
        heartbeatManager = new HeartbeatManager(this, nodes);
        heartbeatManager.start();

        startServer();
    }

    private void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(() -> handleClientRequest(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Coordinator Server Error: " + e.getMessage());
        }
    }

    private void handleClientRequest(Socket socket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            String inputLine = in.readLine();
            if (inputLine == null)
                return;

            System.out.println("[Coordinator] Received client request: " + inputLine);
            String response = processClientCommand(inputLine);
            out.println(response);

        } catch (IOException e) {
            System.err.println("Client Handling Error: " + e.getMessage());
        }
    }

    private String processClientCommand(String command) {
        String[] parts = command.split(":");
        String type = parts[0];

        if (type.equals("PUT")) {
            // PUT:key:value
            if (parts.length < 3)
                return "ERROR:InvalidPUTFormat";
            String key = parts[1];
            String value = parts[2];
            return handlePut(key, value);
        } else if (type.equals("GET")) {
            // GET:key
            if (parts.length < 2)
                return "ERROR:InvalidGETFormat";
            String key = parts[1];
            return handleGet(key);
        } else {
            return "ERROR:UnknownCommand";
        }
    }

    // PHASE 3 & 4: Write Logic (Quorum W=2)
    private String handlePut(String key, String value) {
        // Increment version
        int newVersion = keyVersions.getOrDefault(key, 0) + 1;
        keyVersions.put(key, newVersion);

        String commandForKey = "PUT:" + key + ":" + value + ":" + newVersion;
        int acks = 0;
        int activeNodes = 0;

        // Broadcast to all ALIVE nodes
        for (NodeInfo node : nodes) {
            if (!node.isAlive)
                continue;

            activeNodes++;
            String response = sendToNode(node, commandForKey);
            if (response != null && response.equals("ACK")) {
                acks++;
            }
        }

        System.out.println("[Coordinator] Active Nodes: " + activeNodes + ", ACKs: " + acks);

        // Check Quorum (W=2)
        if (acks >= 2) {
            System.out.println("[Coordinator] Write Quorum Achieved (" + acks + "/3) for " + key);
            return "SUCCESS:WriteQuorumMet";
        } else {
            System.out.println("[Coordinator] Write FAILED - Quorum Not Met (" + acks + "/3) for " + key);
            return "FAILURE:WriteQuorumNotMet";
        }
    }

    // PHASE 5: Read Logic (Quorum R=2)
    private String handleGet(String key) {
        String command = "GET:" + key;
        List<VersionedValue> readings = new ArrayList<>();
        int activeNodes = 0;

        for (NodeInfo node : nodes) {
            if (!node.isAlive)
                continue;

            activeNodes++;
            String response = sendToNode(node, command);
            // Expected response: VALUE:key:value:version OR NULL
            if (response != null && response.startsWith("VALUE:")) {
                try {
                    String[] parts = response.split(":");
                    // parts[0]=VALUE, parts[1]=key, parts[2]=value, parts[3]=version
                    String val = parts[2];
                    int ver = Integer.parseInt(parts[3]);
                    readings.add(new VersionedValue(val, ver));
                } catch (Exception e) {
                    System.err.println("Error parsing response from " + node.id + ": " + response);
                }
            }
        }

        System.out.println("[Coordinator] Read responses received from " + readings.size() + " nodes (Active: "
                + activeNodes + ")");

        if (readings.size() < 2) {
            return "FAILURE:ReadQuorumNotMet";
        }

        // Compare versions (Latest wins)
        VersionedValue best = null;
        for (VersionedValue v : readings) {
            if (best == null || v.version > best.version) {
                best = v;
            }
        }

        if (best != null) {
            System.out.println("[Coordinator] Consolidated Read: " + best.value + " (v" + best.version + ")");
            return "VALUE:" + key + ":" + best.value + ":" + best.version;
        } else {
            return "NULL";
        }
    }

    // PHASE 8: Automatic Re-Synchronization
    public void synchronizeNode(NodeInfo recoveredNode) {
        System.out.println("[Coordinator] Synchronizing data to recovered node: " + recoveredNode.id + "...");

        executor.submit(() -> {
            int syncedCount = 0;
            for (String key : keyVersions.keySet()) {
                // Get latest value from QUORUM
                String getResponse = handleGet(key);
                if (getResponse != null && getResponse.startsWith("VALUE:")) {
                    String[] parts = getResponse.split(":");
                    String val = parts[2];
                    String ver = parts[3];

                    // Send SYNC_DATA to recovered node
                    // SYNC_DATA:key:value:version
                    String syncCmd = "SYNC_DATA:" + key + ":" + val + ":" + ver;
                    String resp = sendToNode(recoveredNode, syncCmd);
                    if (resp != null && resp.equals("ACK")) {
                        syncedCount++;
                    }
                }
            }
            System.out.println(
                    "[Coordinator] Sync complete for " + recoveredNode.id + ". Entries updated: " + syncedCount);
        });
    }

    private String sendToNode(NodeInfo node, String command) {
        try (Socket socket = new Socket(node.ip, node.port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(command);
            return in.readLine();
        } catch (IOException e) {
            System.out.println("[Coordinator] Failed to contact " + node.id + ": " + e.getMessage());
            return null;
        }
    }

    // For Main.java
    public void join() throws InterruptedException {
        synchronized (this) {
            wait();
        }
    }

    // Node Registry
    public static class NodeInfo {
        String id;
        String ip;
        int port;
        volatile boolean isAlive = true;
        long lastSeen = System.currentTimeMillis();

        NodeInfo(String id, String ip, int port) {
            this.id = id;
            this.ip = ip;
            this.port = port;
        }
    }
}
