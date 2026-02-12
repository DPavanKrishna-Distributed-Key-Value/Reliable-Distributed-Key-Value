import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HeartbeatManager {
    private final List<Coordinator.NodeInfo> nodes;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final int HEARTBEAT_INTERVAL_MS = 2000;
    private final int TIMEOUT_MS = 5000;

    private final Coordinator coordinator;

    public HeartbeatManager(Coordinator coordinator, List<Coordinator.NodeInfo> nodes) {
        this.coordinator = coordinator;
        this.nodes = nodes;
    }

    public void start() {
        System.out.println("[HeartbeatManager] Starting heartbeat monitoring...");
        scheduler.scheduleAtFixedRate(this::checkNodes, 0, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void checkNodes() {
        for (Coordinator.NodeInfo node : nodes) {
            boolean previousStatus = node.isAlive;
            boolean currentStatus = sendHeartbeat(node);

            if (previousStatus && !currentStatus) {
                // Node just failed
                node.isAlive = false;
                node.lastSeen = System.currentTimeMillis(); // Log failure time?
                System.out.println("[HeartbeatManager] ALERT: Node " + node.id + " FAILED (Heartbeat timeout)");
            } else if (!previousStatus && currentStatus) {
                // Node just recovered
                node.isAlive = true;
                node.lastSeen = System.currentTimeMillis();
                System.out.println("[HeartbeatManager] ALERT: Node " + node.id + " RECOVERED");

                // Trigger Phase 8: Auto-Sync
                coordinator.synchronizeNode(node);
                // In a real event system, we'd fire an event.
                // For now, we rely on Coordinator to handle sync or just log it.
                // We will add the Sync call in Phase 8.
            }
            // Update timestamp if alive
            if (currentStatus) {
                node.lastSeen = System.currentTimeMillis();
            }
        }
    }

    private boolean sendHeartbeat(Coordinator.NodeInfo node) {
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(node.ip, node.port), 1000); // 1s connection timeout
            try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                out.println("HEARTBEAT");
                String response = in.readLine();
                return response != null && response.equals("ALIVE");
            }
        } catch (IOException e) {
            return false;
        }
    }

    public void stop() {
        scheduler.shutdown();
    }
}
