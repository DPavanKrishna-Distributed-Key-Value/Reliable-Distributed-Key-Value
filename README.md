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
