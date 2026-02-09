import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class DataLoader {

    /**
     * Loads session data from file into the given store.
     * Format expected: session:userXXX → {"userId": "...", ...}
     */
    public static int loadSessions(String filePath, ConcurrentHashMap<String, String> store) {
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || !line.startsWith("session:")) continue;

                String[] parts = line.split(" → ", 2);
                if (parts.length != 2) {
                    System.out.println("Skipping invalid line: " + line);
                    continue;
                }

                String key = parts[0].trim();   // session:user001
                String value = parts[1].trim(); // JSON string

                store.put(key, value);
                count++;
            }
            System.out.println("Successfully loaded " + count + " sessions from " + filePath);
        } catch (IOException e) {
            System.err.println("Failed to load sessions: " + e.getMessage());
        }
        return count;
    }

    // Quick standalone test
    public static void main(String[] args) {
        ConcurrentHashMap<String, String> testStore = new ConcurrentHashMap<>();
        int loaded = loadSessions("user_sessions.txt", testStore);

        if (loaded > 0) {
            System.out.println("\nFirst few entries for verification:");
            testStore.forEach((k, v) -> {
                if (k.compareTo("session:user020") < 0) {  // show only first ~19
                    System.out.println(k + " → " + v);
                }
            });
        }
    }
}