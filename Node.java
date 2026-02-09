import java.util.concurrent.ConcurrentHashMap;

public class Node {
    private final String nodeId;
    private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();
    private volatile boolean isAlive = true;

    public Node(String nodeId) {
        this.nodeId = nodeId;
        System.out.println("Node created: " + nodeId);   
    }

    public String getId() {
        return nodeId;
    }

    public void put(String key, String value) {
        if (!isAlive) return;
        store.put(key, value);
        System.out.println("[" + nodeId + "] PUT " + key);
    }

    public String get(String key) {
        if (!isAlive) return null;
        return store.get(key);
    }

    public void kill() {
        isAlive = false;
        System.out.println("[" + nodeId + "] KILLED");
    }

    public void revive() {
        isAlive = true;
        System.out.println("[" + nodeId + "] REVIVED");
    }

    public boolean isAlive() {
        return isAlive;
    }

    public int size() {
        return store.size();
    }

    public void printContent() {
        System.out.println("\n[" + nodeId + "] " + store.size() + " entries");
        store.entrySet().stream().limit(5).forEach(e ->
            System.out.println("  " + e.getKey() + " → " + e.getValue())
        );
    }

    public static void main(String[] args) {
        Node node = new Node("Node-A");
        node.printContent();
    }
}