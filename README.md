# Redis Chat Application

A high-performance, real-time backend chat service built with **Spring Boot 3** and **Redis** as the primary datastore and message broker. The service provides RESTful endpoints for room management and message history, coupled with Redis Pub/Sub for instantaneous real-time event broadcasting.

---

## 1. Prerequisites

- **Java Development Kit (JDK)**: Version 17+ (JDK 17 LTS recommended)
- **Docker Desktop**: For running the containerized Redis instance
- **Maven**: Version 3.9+ (or use the bundled `./mvnw.cmd` / `./mvnw`)

---

## 2. Quickstart & Running Locally

### Step 1: Start Redis
Start Redis 7 with AOF persistence enabled:
```powershell
docker compose up -d
```
Verify the container is healthy:
```powershell
docker compose ps
```

### Step 2: Run the Spring Boot Application
```powershell
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```
The application starts on `http://localhost:8080`.

### Step 3: Run Tests
Execute the automated test suite with coverage:
```powershell
.\mvnw.cmd test
```

---

## 3. Interactive Documentation & Swagger
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI 3 JSON Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## 4. Redis Key Architecture

All Redis keys are centralized in `RedisKeys.java`:

| Data | Redis Structure | Key Pattern | Commands | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| **Room Registry** | `Set` | `chatrooms:names` | `SADD`, `SISMEMBER`, `SREM` | $O(1)$ duplicate prevention and room existence check |
| **Room Metadata** | `Hash` | `chatroom:{roomId}` | `HSET`, `HGETALL` | Stores `roomId`, `roomName`, `createdAt` |
| **Participants** | `Set` | `chatroom:{roomId}:participants` | `SADD`, `SISMEMBER`, `SREM` | Tracks unique users who joined the room |
| **Messages History** | `List` | `chatroom:{roomId}:messages` | `RPUSH`, `LRANGE`, `LTRIM` | Chronological message log (capped at 1,000 items) |
| **Live Broadcast** | `Pub/Sub` | `chatroom:{roomId}:channel` | `PUBLISH`, `SUBSCRIBE` | Real-time event broadcasting to active subscribers |

---

## 5. Architectural Design Patterns

- **Repository Pattern**: Data access to Redis is hidden behind interfaces (`ChatRoomRepository`, `MessageRepository`), decoupling business logic from Redis templates.
- **Observer Pattern**: Real-time message dispatching powered by Redis Pub/Sub (publishers and listener subscribers).
- **Dependency Inversion (SOLID DIP)**: Services rely exclusively on repository interfaces and an injectable `java.time.Clock`.
- **Single Responsibility (SOLID SRP)**: Separation between HTTP controllers, domain services, Redis repositories, and centralized key definitions (`RedisKeys`).

---

## 6. API Reference

### 1. Create a Chat Room
- **Method & Path**: `POST /api/chatapp/chatrooms`
- **Request Body**:
  ```json
  {
    "roomName": "general"
  }
  ```
- **Success Response (`201 Created`)**:
  ```json
  {
    "message": "Chat room 'general' created successfully.",
    "roomId": "general",
    "status": "success"
  }
  ```
- **Duplicate Room Error (`409 Conflict`)**:
  ```json
  {
    "status": "error",
    "message": "Chat room 'general' already exists."
  }
  ```
- **Validation Error (`400 Bad Request`)**:
  ```json
  {
    "status": "error",
    "message": "Validation failed.",
    "details": [
      "roomName: roomName must not be blank"
    ]
  }
  ```

### 2. Join a Chat Room
- **Method & Path**: `POST /api/chatapp/chatrooms/{roomId}/join`
- **Request Body**:
  ```json
  {
    "participant": "guest_user"
  }
  ```
- **Success Response (`200 OK`)**:
  ```json
  {
    "message": "User 'guest_user' joined chat room 'general'.",
    "status": "success"
  }
  ```
- **Room Not Found Error (`404 Not Found`)**:
  ```json
  {
    "status": "error",
    "message": "Chat room 'general' not found."
  }
  ```
- **Validation Error (`400 Bad Request`)**:
  ```json
  {
    "status": "error",
    "message": "Validation failed.",
    "details": [
      "participant: participant must not be blank"
    ]
  }
  ```

### 3. Send a Message
- **Method & Path**: `POST /api/chatapp/chatrooms/{roomId}/messages`
- **Request Body**:
  ```json
  {
    "participant": "guest_user",
    "message": "Hello, everyone!"
  }
  ```
- **Success Response (`200 OK`)**:
  ```json
  {
    "message": "Message sent successfully.",
    "status": "success"
  }
  ```
- **Room Not Found Error (`404 Not Found`)**:
  ```json
  {
    "status": "error",
    "message": "Chat room 'general' not found."
  }
  ```
- **Unauthorized Sender Error (`403 Forbidden`)**:
  ```json
  {
    "status": "error",
    "message": "User 'guest_user' is not a participant of chat room 'general'."
  }
  ```
- **Validation Error (`400 Bad Request`)**:
  ```json
  {
    "status": "error",
    "message": "Validation failed.",
    "details": [
      "participant: participant must not be blank",
      "message: message must not be blank"
    ]
  }
  ```

### 4. Get Message History
- **Method & Path**: `GET /api/chatapp/chatrooms/{roomId}/messages?limit=10`
- **Query Parameters**:
  - `limit` (optional, default: `10`, min: `1`, max: `100`): Number of recent messages to return in chronological order (oldest to newest).
- **Success Response (`200 OK`)**:
  ```json
  {
    "messages": [
      {
        "participant": "guest_user",
        "message": "Hello, everyone!",
        "timestamp": "2024-01-01T10:00:00Z"
      }
    ]
  }
  ```
- **Room Not Found Error (`404 Not Found`)**:
  ```json
  {
    "status": "error",
    "message": "Chat room 'general' not found."
  }
  ```
- **Invalid Limit Error (`400 Bad Request`)**:
  ```json
  {
    "status": "error",
    "message": "Limit must be between 1 and 100."
  }
  ```

### 5. Delete a Chat Room
- **Method & Path**: `DELETE /api/chatapp/chatrooms/{roomId}`
- **Success Response (`200 OK`)**:
  ```json
  {
    "message": "Chat room 'general' deleted successfully.",
    "status": "success"
  }
  ```
- **Room Not Found Error (`404 Not Found`)**:
  ```json
  {
    "status": "error",
    "message": "Chat room 'general' not found."
  }
  ```


---

## 7. Real-Time Pub/Sub Architecture

Whenever a message is posted to `POST /api/chatapp/chatrooms/{roomId}/messages`, two actions take place atomically in the application:
1. **Persistence**: The message is serialized to clean JSON and appended to the Redis List `chatroom:{roomId}:messages` via `RPUSH`, then trimmed to 1,000 items with `LTRIM`.
2. **Real-time Broadcast**: The message JSON is published via `StringRedisTemplate.convertAndSend` to the Redis channel:
   ```
   chatroom:{roomId}:channel
   ```

A Spring `RedisMessageListenerContainer` with `MessageSubscriber` listens to the pattern `chatroom:*:channel`, allowing horizontal scaling across backend instances.

---

## 8. Unified Error Response Contract

All errors across every endpoint follow a standardized JSON envelope conforming to Section 7 of `AGENTS.md`:

```json
{
  "status": "error",
  "message": "<Error description>"
}
```

For validation failures (`400 Bad Request`), a `details` array is included:

```json
{
  "status": "error",
  "message": "Validation failed.",
  "details": [
    "roomName: roomName must not be blank"
  ]
}
```

---

## 9. Testing with Postman (No UI Required)

A complete, pre-configured Postman Collection is included at:
[`docs/postman/chat-app.postman_collection.json`](file:///C:/Users/dell/OneDrive/Desktop/Chat_Assignment/chatBackend/docs/postman/chat-app.postman_collection.json)

### How to Import & Run:
1. Open **Postman**.
2. Click **Import** (top left).
3. Drag and drop `docs/postman/chat-app.postman_collection.json` (or select the file).
4. Make sure the Spring Boot application is running (`.\mvnw.cmd spring-boot:run`).
5. Click on the collection name **Redis Chat Application API** and click **Run Collection**.

### Included Test Requests:
1. `1. Create Chat Room` — `POST /api/chatapp/chatrooms` (Status 201)
2. `2. Join Chat Room` — `POST /api/chatapp/chatrooms/general/join` (Status 200)
3. `3. Send Message 1` — `POST /api/chatapp/chatrooms/general/messages` (Status 200)
4. `4. Send Message 2` — `POST /api/chatapp/chatrooms/general/messages` (Status 200)
5. `5. Retrieve Chat History (Last 10)` — `GET /api/chatapp/chatrooms/general/messages?limit=10` (Status 200)
6. `6. Retrieve Chat History (Last 1)` — `GET /api/chatapp/chatrooms/general/messages?limit=1` (Status 200, checks latest message)
7. `7. Error: Duplicate Room Name` — `POST /api/chatapp/chatrooms` (Status 409 Conflict)
8. `8. Error: Send Message to Missing Room` — `POST /api/chatapp/chatrooms/non_existent_room/messages` (Status 404 Not Found)
9. `9. Error: Unauthorized Sender` — `POST /api/chatapp/chatrooms/general/messages` (Status 403 Forbidden)
10. `10. Delete Chat Room` — `DELETE /api/chatapp/chatrooms/general` (Status 200 OK)
11. `11. Error: Verify Room is Gone` — `GET /api/chatapp/chatrooms/general/messages` (Status 404 Not Found)

---

## 10. How to Test Real-Time Pub/Sub with Terminal

To verify real-time broadcasting directly against Redis:
1. Open a terminal and run the Redis CLI subscriber:
   ```bash
   docker exec -it redis-chat-app redis-cli SUBSCRIBE chatroom:general:channel
   ```
2. In Postman, execute `POST /api/chatapp/chatrooms/general/messages`.
3. You will immediately see the message JSON payload appear on the terminal in real time!






