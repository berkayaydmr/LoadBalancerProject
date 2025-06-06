# Load Balancer Project

A distributed load balancer implementation in Java that supports multiple load balancing strategies and various request types including directory listing, file transfer, computation tasks, and video streaming.

## 1. Load Balancing Strategies

### Static Load Balancing: Round Robin
- **Algorithm**: Distributes requests sequentially across available servers in circular order
- **Implementation**: Uses a static counter that cycles through the server list

### Dynamic Load Balancing: Least Load
- **Algorithm**: Routes requests to the server with the lowest current load
- **Implementation**: Maintains real-time load information from each server via heartbeat messages

### Hybrid Approach
The system intelligently selects the appropriate strategy based on request type:
- **Heavy requests** (COMPUTATION, VIDEO_STREAMING): Uses Least Load algorithm
- **Light requests** (DIRECTORY, FILE_TRANSFER): Uses Round Robin algorithm

## 2. High Level Approach

### Network Protocol
- **TCP**: Used for reliable communication between all components (Client ↔ Load Balancer ↔ Server)
- **Connection-oriented**: Ensures data integrity and proper connection management

### Application Layer Mechanisms

#### Server Registration & Discovery
- Servers register with load balancer on startup
- Support for different balancing preferences per server (`-v dynamic` or `-v static`)
- Automatic retry mechanism with exponential backoff

#### Health Monitoring
- **Heartbeat Protocol**: Servers send load updates every 5 seconds
- **Health Checks**: Load balancer monitors server health every 10 seconds
- **Failure Detection**: 15-second timeout for unresponsive servers
- **Automatic Cleanup**: Dead servers are automatically removed

#### Request Routing
- Two-phase routing: Client → Load Balancer → Selected Server
- Request type classification with estimated processing times
- Intelligent server selection based on request characteristics

#### Load Tracking
- Real-time load monitoring using atomic counters
- Concurrent request handling with thread-safe data structures
- Load information propagated via periodic heartbeat messages

### Design Properties & Features

1. **Scalability**: Multi-threaded architecture supporting concurrent connections
2. **Fault Tolerance**: Health monitoring and automatic server recovery
3. **Flexibility**: Support for multiple request types with different processing patterns
4. **Efficiency**: Hybrid load balancing optimized for different workload types
5. **Observability**: Comprehensive logging and status reporting
6. **Graceful Shutdown**: Proper cleanup and resource management

## 3. Challenges Faced

### 1. Thread Safety & Concurrency
- **Challenge**: Managing shared state across multiple threads safely
- **Solution**: Used `ConcurrentHashMap` and `Collections.synchronizedList()` for thread-safe collections
- **Outcome**: Eliminated race conditions while maintaining performance

### 2. Server Health Monitoring
- **Challenge**: Detecting and handling server failures gracefully
- **Solution**: Implemented heartbeat mechanism with timeout-based failure detection
- **Outcome**: Automatic failover without manual intervention

### 3. Connection Management
- **Challenge**: Preventing resource leaks from unclosed sockets
- **Solution**: Implemented proper try-catch-finally blocks and shutdown hooks
- **Outcome**: Clean resource management and graceful shutdowns

### 4. Load Balancing Strategy Selection
- **Challenge**: Choosing optimal algorithm for different request types
- **Solution**: Implemented hybrid approach based on request characteristics
- **Outcome**: Improved overall system performance and resource utilization

### 5. Request Type Estimation
- **Challenge**: Accurately estimating processing time for different request types
- **Solution**: Created request classification system with parameter parsing
- **Outcome**: Better server selection and load prediction

## 4. Testing

### Unit Testing Components
- **Server Registration**: Verify servers can join and leave the load balancer
- **Load Balancing Algorithms**: Test Round Robin and Least Load implementations
- **Health Monitoring**: Validate server failure detection and recovery

### Integration Testing
- **Multi-client Scenarios**: Concurrent request handling using `TestClient.java`
- **Failure Recovery**: Server shutdown and restart scenarios
- **Load Distribution**: Verify requests are properly distributed across servers

### Performance Testing
The included `TestClient.java` provides comprehensive load testing:
```bash
# Test with 4 concurrent threads, 8 total requests
java TestClient 4 8
```

**Test Coverage**:
- Concurrent request handling
- Different request types (DIRECTORY, FILE_TRANSFER, COMPUTATION, VIDEO_STREAMING)
- Response time measurement
- Server assignment verification

### Manual Testing Scenarios
1. **Basic Functionality**: Single client requests
2. **Load Balancing**: Multiple clients with different request types
3. **Server Failure**: Shutting down servers during operation
4. **Recovery**: Restarting failed servers

## 5. How to Run Project

### Prerequisites
- Java JDK 8 or higher
- Terminal/Command Prompt access

### Step 1: Compile All Classes
```bash
cd /path/to/LoadBalancerProject
javac *.java
```

### Step 2: Start the Load Balancer
```bash
# Start on default port 9001
java LoadBalancer

# Or specify custom port
java LoadBalancer 9001
```

### Step 3: Start Multiple Servers
Open separate terminal windows for each server:

```bash
# Server 1 (port 7001) with dynamic balancing
java Server 7001 -v dynamic

# Server 2 (port 7002) with dynamic balancing  
java Server 7002 -v dynamic

# Server 3 (port 7003) with static balancing
java Server 7003 -v static
```

### Step 4: Run Clients

#### Single Client (Interactive)
```bash
java Client
# Follow prompts to enter request type
```

#### Multiple Clients (Load Testing)
```bash
# Run with 4 threads, 8 total requests
java TestClient 4 8

# Run with 6 threads, 12 total requests
java TestClient 6 12
```

### Request Types Available
1. **DIRECTORY** - List server files (fast)
2. **FILE_TRANSFER filename** - Transfer a file (medium)
3. **COMPUTATION duration** - CPU intensive task (slow)
4. **VIDEO_STREAMING duration** - Stream video (very slow)

### Example Usage Session
```bash
# Terminal 1: Start Load Balancer
java LoadBalancer

# Terminal 2: Start Server 1
java Server 7001 -v dynamic

# Terminal 3: Start Server 2  
java Server 7002 -v dynamic

# Terminal 4: Run test client
java TestClient 2 4
```

### Monitoring
- Load Balancer shows server registrations and request assignments
- Servers display their current load and request processing
- Clients show response times and server assignments

### Graceful Shutdown
- Use `Ctrl+C` to stop any component
- Servers will notify the load balancer before shutting down
- Load balancer will clean up server references automatically

### An Example Outcome For TestClient

=== Multi-threaded Load Balancer Test ===
Threads: 6, Total requests: 12
Starting concurrent requests...

[Thread 1] Starting: DIRECTORY
[Thread 6] Starting: FILE_TRANSFER document1.pdf
[Thread 5] Starting: DIRECTORY
[Thread 4] Starting: VIDEO_STREAMING 10
[Thread 2] Starting: FILE_TRANSFER document1.pdf
[Thread 3] Starting: COMPUTATION 5
[Thread 2] Assigned to server port: 7001
[Thread 3] Assigned to server port: 7001
[Thread 6] Assigned to server port: 7002
[Thread 1] Assigned to server port: 7003
[Thread 4] Assigned to server port: 7001
[Thread 5] Assigned to server port: 7003
[Thread 1] Completed in 36ms
[Thread 1] Response preview:
    DIRECTORY_LISTING
    document1.pdf
    image1.jpg

[Thread 5] Completed in 36ms
[Thread 5] Response preview:
    DIRECTORY_LISTING
    document1.pdf
    image1.jpg

[Thread 7] Starting: COMPUTATION 5
[Thread 8] Starting: VIDEO_STREAMING 10
[Thread 7] Assigned to server port: 7001
[Thread 8] Assigned to server port: 7001
[Thread 6] Completed in 1043ms
[Thread 6] Response preview:
    FILE_TRANSFER_START document1.pdf
    FILE_CONTENT: [Simulated content of document1.pdf]
    FILE_TRANSFER_COMPLETE

[Thread 9] Starting: DIRECTORY
[Thread 2] Completed in 1043ms
[Thread 2] Response preview:
    FILE_TRANSFER_START document1.pdf
    FILE_CONTENT: [Simulated content of document1.pdf]
    FILE_TRANSFER_COMPLETE

[Thread 10] Starting: FILE_TRANSFER document1.pdf
[Thread 9] Assigned to server port: 7003
[Thread 10] Assigned to server port: 7001
[Thread 9] Completed in 5ms
[Thread 9] Response preview:
    DIRECTORY_LISTING
    document1.pdf
    image1.jpg

[Thread 11] Starting: COMPUTATION 5
[Thread 11] Assigned to server port: 7001
[Thread 10] Completed in 1010ms
[Thread 10] Response preview:
    FILE_TRANSFER_START document1.pdf
    FILE_CONTENT: [Simulated content of document1.pdf]
    FILE_TRANSFER_COMPLETE

[Thread 12] Starting: VIDEO_STREAMING 10
[Thread 12] Assigned to server port: 7001
[Thread 3] Completed in 5056ms
[Thread 3] Response preview:
    COMPUTATION_START duration=5s
    COMPUTATION_PROGRESS 20%
    COMPUTATION_PROGRESS 40%

[Thread 7] Completed in 5020ms
[Thread 7] Response preview:
    COMPUTATION_START duration=5s
    COMPUTATION_PROGRESS 20%
    COMPUTATION_PROGRESS 40%

[Thread 11] Completed in 5021ms
[Thread 11] Response preview:
    COMPUTATION_START duration=5s
    COMPUTATION_PROGRESS 20%
    COMPUTATION_PROGRESS 40%

[Thread 4] Completed in 10081ms
[Thread 4] Response preview:
    VIDEO_STREAMING_START duration=10s
    VIDEO_FRAME 1/10 [Frame data]
    VIDEO_FRAME 2/10 [Frame data]

[Thread 8] Completed in 10041ms
[Thread 8] Response preview:
    VIDEO_STREAMING_START duration=10s
    VIDEO_FRAME 1/10 [Frame data]
    VIDEO_FRAME 2/10 [Frame data]

[Thread 12] Completed in 10049ms
[Thread 12] Response preview:
    VIDEO_STREAMING_START duration=10s
    VIDEO_FRAME 1/10 [Frame data]
    VIDEO_FRAME 2/10 [Frame data]


=== Test Results ===
All 12 requests completed in 12107ms
Average time per request: 1008ms