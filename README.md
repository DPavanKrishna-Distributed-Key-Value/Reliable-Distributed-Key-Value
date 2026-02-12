📌 Project Description

Reliable Distributed Key–Value Storage with Replication and Fault Tolerance is a distributed systems project that implements a strongly consistent, fault-tolerant key–value store using replication, quorum-based consistency, and heartbeat-based failure detection.

The system simulates multiple distributed nodes using Java threads. Each node stores replicated key–value data and participates in coordinated read and write operations. Data is replicated across multiple nodes (N=3 by default), and operations follow quorum rules (R=2, W=2) to ensure strong consistency. The condition R + W > N guarantees that reads always reflect the most recent successful write.

To maintain reliability, the system includes a heartbeat mechanism where nodes periodically send health signals to one another. If a node fails to send heartbeats within a defined timeout, it is marked as failed and excluded from quorum operations. This enables the system to tolerate up to one crash failure while continuing normal operations.

The project demonstrates core distributed systems concepts such as:

Data replication

Quorum-based consistency protocols

Failure detection using heartbeats

Crash (fail-stop) fault tolerance

Multithreading and concurrency control

A session-based dataset (e.g., session:user003 → {"userId":"user003","loginTime":"19:16","status":"active"}) is used to simulate real-world distributed storage scenarios like session management systems.

Overall, this project models the foundational principles used in real-world distributed databases such as Dynamo and Cassandra, providing a practical implementation of consistency and fault tolerance mechanisms in distributed environments.





Reliable Distributed Key-Value Store - Project Manual
📌 Project Summary
This project implements a Reliable Distributed Key-Value Storage System in Java. It mimics real-world distributed systems (like DynamoDB or Cassandra) by allowing data storage across multiple nodes with:

Replication: Data is copied to multiple nodes for safety.
Quorum Consistency: Reading/Writing requires agreement from a majority (W=2, R=2) of nodes.
Fault Tolerance: The system detects node failures via Heartbeats and continues operating.
Automatic Recovery: When a node returns, it automatically synchronizes missing data.
Core Features
Distributed Architecture: 1 Coordinator + 3 Storage Nodes.
Protocol: Simple text-based socket communication (PUT, GET, HEARTBEAT).
Versioning: Conflict resolution using version numbers (Last-Write-Wins).
Failure Detection: Heartbeat mechanism detects crashed nodes within 5 seconds.
Simulated Network: Artificial delay toggle to mimic real-world latency.
🚀 How to Run the Project (Real-Time Usage)
You will need 4 separate terminal windows (or 4 laptops on the same LAN). Note: If running on one machine, use localhost (127.0.0.1).

Step 1: Compile
Run this in the project folder:

bash
javac *.java
Step 2: Start Nodes (Terminals 1, 2, 3)
Start the three storage nodes on different ports. Terminal 1 (Node A):

bash
java Main node 8081 NodeA
Terminal 2 (Node B):

bash
java Main node 8082 NodeB
Terminal 3 (Node C):

bash
java Main node 8083 NodeC
Step 3: Start Coordinator (Terminal 4)
The coordinator manages requests and ensures consistency. Terminal 4:

bash
java Main coordinator 8080
(You should see logs indicating it has registered the 3 nodes and started the Heartbeat Manager)

Step 4: Send Requests (Terminal 5 or TestClient)
You can use the provided 
TestClient
 to send commands.

Write Data (PUT):

bash
java TestClient 127.0.0.1 8080 PUT:user1:{"name":"Alice"}:0
Expected Output: SUCCESS:WriteQuorumMet

Read Data (GET):

bash
java TestClient 127.0.0.1 8080 GET:user1
Expected Output: VALUE:user1:{"name":"Alice"}:1

🧪 Phase-by-Phase Testing & Verification Guide
Since the code is fully implemented, you can verify each phase's functionality as follows:

Phase 1: Node Structure
Goal: Ensure nodes accept connections.
Test: Start a node. Telnet to it (telnet localhost 8081) or use TestClient.
Files: 
Node.java
, 
Main.java
Phase 2: Versioning
Goal: Ensure data is stored with versions.
Test: Send PUT:key:val:1 then PUT:key:val2:2. Verify GET returns version 2. Send PUT:key:val:1 again (stale write) and verify it is ignored.
Files: 
VersionedValue.java
, 
Node.java
Phase 3: Replication
Goal: Coordinator forwards data to all nodes.
Test: Send PUT to Coordinator (Port 8080). Check Terminal 1, 2, and 3. All should log [NodeX] PUT ....
Files: 
Coordinator.java
Phase 4: Quorum Write (W=2)
Goal: Write succeeds if 2/3 nodes are alive.
Test: Kill Node C (Ctrl+C in Terminal 3). Send PUT to Coordinator.
Result: Should still return SUCCESS. Coordinator logs Write Quorum Achieved (2/3).
Phase 5: Quorum Read (R=2)
Goal: Read gets latest version from majority.
Test: With Node C still down, GET data.
Result: Returns value. Coordinator logs Read responses received from 2 nodes.
Phase 6 & 7: Failure Detection
Goal: System detects crash automatically.
Test: Kill Node B. Watch Coordinator terminal.
Result: After ~5 seconds, Coordinator logs: [HeartbeatManager] ALERT: Node NodeB FAILED.
Phase 8: Auto-Synchronization
Goal: Recovered node gets missing data.
Test:
Start Node B again (java Main node 8082 NodeB).
Watch Coordinator log: [HeartbeatManager] ALERT: Node NodeB RECOVERED.
Watch Coordinator log: Synchronizing data to recovered node....
Watch Node B log: Receives SYNC_DATA updates.
Phase 9: Network Delay
Goal: Simulate latency.
Test: Hardcode simulateNetworkDelay = true in 
Node.java
 (or add command to toggle it). Observe slower responses.
📂 Git Commit Guide (For Github Push)
To push this project to GitHub with a clean history corresponding to phases, you can use the following descriptions.

Initial Setup
Commit Msg: Initial commit: Basic project structure and data loader Files: 
DataLoader.java
, 
user_sessions.txt

Phase 1 & 2 Implementation
Commit Msg: Feat: Implement Node networking and VersionedValue storage Summary: Refactored Node to use ServerSocket. Implemented VersionedValue class to track data consistency. Added Main class for CLI startup. Files: 
Node.java
, 
VersionedValue.java
, 
Main.java

Phase 3, 4, 5 Implementation
Commit Msg: Feat: Add Coordinator with Quorum Replication (R=2, W=2) Summary: Implemented Coordinator to manage nodes. Added logic for Quorum-based Writes and Reads to ensure strong consistency. Files: 
Coordinator.java

Phase 6, 7, 8, 9 (Final Polish)
Commit Msg: Feat: Add Failure Detection, Auto-Sync, and Network Simulation Summary: Integrated HeartbeatManager for fault tolerance. Implemented automatic data synchronization on node recovery. Added network delay simulation. Files: 
HeartbeatManager.java
, 
Coordinator.java
, 
Node.java