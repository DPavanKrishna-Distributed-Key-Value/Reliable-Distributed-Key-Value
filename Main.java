import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage:");
            System.out.println("  java Main coordinator <port>");
            System.out.println("  java Main node <port> <nodeId>");
            return;
        }

        String type = args[0];
        int port = Integer.parseInt(args[1]);

        try {
            if (type.equalsIgnoreCase("node")) {
                if (args.length < 3) {
                    System.out.println("Usage: java Main node <port> <nodeId>");
                    return;
                }
                String nodeId = args[2];
                Node node = new Node(nodeId, port);

                // Keep keeping it simple, just wait indefinitely
                node.join();
            } else if (type.equalsIgnoreCase("coordinator")) {
                Coordinator coordinator = new Coordinator(port);
                coordinator.join();
            } else {
                System.out.println("Unknown type: " + type);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
