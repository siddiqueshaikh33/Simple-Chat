# Redis Real-Time Chat Application

A high-performance, resilient, real-time backend chat service built with **Spring Boot 3** and **Redis** as the primary datastore and message broker. The service provides RESTful endpoints for room lifecycle management, participant tracking, and message history, coupled with Redis Pub/Sub for real-time event broadcasting.

---

## 🚀 Live Production Deployment (Reviewer Quick-Links)

The application is deployed live in production on Railway with a managed Redis instance:

| Resource | URL | Description |
| :--- | :--- | :--- |
| **Interactive Swagger UI** | [https://simple-chat-production-a070ca1f.up.railway.app/swagger-ui/index.html](https://simple-chat-production-a070ca1f.up.railway.app/swagger-ui/index.html) | Live interactive OpenAPI 3 documentation & API test console |
| **API Base URL** | `https://simple-chat-production-a070ca1f.up.railway.app/api/chatapp` | Production REST API root |
| **Health Check Endpoint** | [https://simple-chat-production-a070ca1f.up.railway.app/health](https://simple-chat-production-a070ca1f.up.railway.app/health) | Container health & live Redis connectivity probe |
| **OpenAPI 3 JSON Spec** | [https://simple-chat-production-a070ca1f.up.railway.app/v3/api-docs](https://simple-chat-production-a070ca1f.up.railway.app/v3/api-docs) | Raw OpenAPI schema |

---

## 🔍 Instructions for Reviewer

You can test and evaluate all assignment requirements either directly in your browser via **Swagger UI** or in **Postman** using our one-click automated test collection.

### Option A: Interactive Testing via Swagger UI (No Tools Required)

1. Open **[Swagger UI](https://simple-chat-production-a070ca1f.up.railway.app/swagger-ui/index.html)** in your browser.
2. Follow this step-by-step test sequence:

   - **Step 1: Verify Infrastructure Health (`GET /health`)**  
     Expand `GET /health` -> Click **Try it out** -> **Execute**.  
     *Expected Result:* `200 OK` with `{"status": "UP", "redis": "UP"}` confirming the container is healthy and actively pinging Redis.

   - **Step 2: Create a Chat Room (Test Case 1 — `POST /api/chatapp/chatrooms`)**  
     Expand `POST /api/chatapp/chatrooms` -> Click **Try it out** -> Pass body:
     ```json
     {
       "roomName": "general"
     }
     ```
     Click **Execute**.  
     *Expected Result:* `201 Created` with `{"message": "Chat room 'general' created successfully.", "roomId": "general", "status": "success"}`.

   - **Step 3: Join the Chat Room (Test Case 1 — `POST /api/chatapp/chatrooms/{roomId}/join`)**  
     Expand `POST /api/chatapp/chatrooms/{roomId}/join` -> Click **Try it out** -> Set `roomId: general` and body:
     ```json
     {
       "participant": "Alice"
     }
     ```
     Click **Execute**.  
     *Expected Result:* `200 OK` with `{"message": "User 'Alice' joined chat room 'general'.", "status": "success"}`.

   - **Step 4: Send Messages (Test Case 2 & 3 — `POST /api/chatapp/chatrooms/{roomId}/messages`)**  
     Expand `POST /api/chatapp/chatrooms/{roomId}/messages` -> Click **Try it out** -> Set `roomId: general` and body:
     ```json
     {
       "participant": "Alice",
       "message": "Hello world!"
     }
     ```
     Click **Execute**.  
     *Expected Result:* `200 OK` with `{"message": "Message sent successfully.", "status": "success"}`.  
     *(This atomically saves the message to Redis and broadcasts it via Redis Pub/Sub).*

   - **Step 5: Retrieve Message History (Test Case 2 — `GET /api/chatapp/chatrooms/{roomId}/messages`)**  
     Expand `GET /api/chatapp/chatrooms/{roomId}/messages` -> Set `roomId: general` and `limit: 10`.  
     Click **Execute**.  
     *Expected Result:* `200 OK` returning messages in strict chronological order with ISO-8601 timestamps.

   - **Step 6: Test Error Handling (Test Case 4)**  
     - **Duplicate Room (`409 Conflict`):** Execute `POST /api/chatapp/chatrooms` with `"roomName": "general"` again.
     - **Unauthorized Sender (`403 Forbidden`):** Execute `POST .../messages` with `"participant": "UnjoinedUser"`.
     - **Missing Room (`404 Not Found`):** Execute `GET /api/chatapp/chatrooms/nonexistent/messages`.
     - **Invalid Limit (`400 Bad Request`):** Execute `GET .../messages?limit=-5` or `limit=500`.

   - **Step 7: Delete Room (Test Case 5 — `DELETE /api/chatapp/chatrooms/{roomId}`)**  
     Expand `DELETE /api/chatapp/chatrooms/{roomId}` -> Set `roomId: general`.  
     Click **Execute**.  
     *Expected Result:* `200 OK` with `{"message": "Chat room 'general' deleted successfully.", "status": "success"}`.  
     Subsequent attempts to query `general` immediately return `404 Not Found`.

---

### Option B: One-Click Automated Testing via Postman

A pre-configured Postman Collection with automated test scripts and environments is provided in the repository under [`docs/postman/`](file:///C:/Users/dell/OneDrive/Desktop/Chat_Assignment/chatBackend/docs/postman):

- **Collection File:** `docs/postman/chat-app.postman_collection.json`
- **Production Environment:** `docs/postman/Railway_Production.postman_environment.json`
- **Local Environment:** `docs/postman/Local_Environment.postman_environment.json`

#### Steps to Run in Postman (1-Minute Setup):
1. Open **Postman**.
2. Click **Import** (top left).
3. Drag & drop the two files:
   - `chat-app.postman_collection.json`
   - `Railway_Production.postman_environment.json`
4. In the top-right environment dropdown, select **Railway Production Environment**.
5. Click on the collection name **Redis Chat Application API** -> Click **Run collection** -> Click **Run Redis Chat Application API**.
6. **Result:** All 12 automated tests run sequentially, verifying status codes (`200`, `201`, `400`, `403`, `404`, `409`), chronological message ordering, and response contracts with **100% passing test assertions**.

---

## 💻 Running the Application Locally

### Prerequisites
- **Java Development Kit (JDK)**: Version 17+ (JDK 17 LTS recommended)
- **Docker & Docker Compose**: For running the containerized Redis instance
- **Maven**: Version 3.9+ (or use the included wrapper `./mvnw` / `.\mvnw.cmd`)

### Step 1: Clone the Repository
```bash
git clone https://github.com/siddiqueshaikh33/Simple-Chat.git
cd Simple-Chat
```

### Step 2: Start Redis Container
Start Redis 7 with Append-Only File (AOF) persistence enabled:
```bash
docker compose up -d
```
Verify the container is healthy:
```bash
docker compose ps
```

### Step 3: Launch the Spring Boot Application
```bash
# On Linux / macOS:
./mvnw spring-boot:run

# On Windows (PowerShell / CMD):
.\mvnw.cmd spring-boot:run
```
The application will start on `http://localhost:8080`.

### Step 4: Access Local Documentation & Tests
- **Local Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Local Health Check:** [http://localhost:8080/health](http://localhost:8080/health)
- **Local OpenAPI Spec:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### Step 5: Run the Automated Test Suite
Run all unit and integration tests (75 tests):
```bash
# On Linux / macOS:
./mvnw test

# On Windows:
.\mvnw.cmd test
```
*A JaCoCo code coverage report will be generated at `target/site/jacoco/index.html`.*

---

## 🏗️ Architecture & Redis Data Design

In strict compliance with the take-home requirements:
- **No SQL Database:** Redis is the sole and primary datastore.
- **No UI:** The service is an API-first backend adhering to Section 6 of `AGENTS.md`.

All Redis keys are centralized in `RedisKeys.java`:

| Data Entity | Redis Structure | Key Pattern | Redis Commands | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| **Room Registry** | `Set` | `chatrooms:names` | `SADD`, `SISMEMBER`, `SREM` | $O(1)$ duplicate room prevention & existence checks |
| **Room Metadata** | `Hash` | `chatroom:{roomId}` | `HSET`, `HGETALL`, `DEL` | Stores room ID, display name, and creation timestamp |
| **Participants** | `Set` | `chatroom:{roomId}:participants` | `SADD`, `SISMEMBER`, `DEL` | Tracks unique users who joined the room |
| **Messages History** | `List` | `chatroom:{roomId}:messages` | `RPUSH`, `LRANGE`, `LTRIM`, `DEL` | Chronological message log (capped at 1,000 items) |
| **Live Broadcast** | `Pub/Sub` | `chatroom:{roomId}:channel` | `PUBLISH`, `SUBSCRIBE` | Real-time event broadcasting to active subscribers |

---

## ⚡ Concurrency & Resilience Guarantees

1. **Atomic Room Creation & Deletion (Redis Lua Scripts):**
   - Room creation is executed via `CREATE_ROOM_LUA`, atomically checking `SISMEMBER` before `SADD` and `HSET`. This eliminates race conditions during concurrent room creation requests.
   - Room deletion is executed via `DELETE_ROOM_LUA`, atomically removing the room from the registry and deleting all related Hash, Set, and List keys in a single atomic transaction.

2. **Real-Time Pub/Sub with Fallback Protection:**
   - Messages are first appended to Redis List history. If a transient error occurs during Pub/Sub publishing, it is caught and logged gracefully so message persistence remains consistent without duplicated retries.

3. **Memory Optimization & Capped History:**
   - Every `RPUSH` to `chatroom:{roomId}:messages` is immediately followed by `LTRIM -1000 -1`, ensuring memory usage remains bounded and preventing runaway memory exhaustion in high-volume rooms.

4. **Reverse Proxy & HTTPS Support:**
   - Configured with `server.forward-headers-strategy=framework` and relative OpenAPI server URLs, allowing clean HTTPS termination behind cloud reverse proxies (Railway / AWS ALB / Cloudflare) without browser Mixed Content errors.

---

## 📡 Complete REST API Reference

### 1. Health & Status Check
- **`GET /health`** or **`GET /api/chatapp/health`**
- **Response (`200 OK`):**
  ```json
  {
    "status": "UP",
    "redis": "UP",
    "timestamp": "2026-11-21T03:00:00Z",
    "error": null
  }
  ```

### 2. Create Chat Room
- **`POST /api/chatapp/chatrooms`**
- **Request Body:**
  ```json
  {
    "roomName": "general"
  }
  ```
- **Response (`201 Created`):**
  ```json
  {
    "message": "Chat room 'general' created successfully.",
    "roomId": "general",
    "status": "success"
  }
  ```

### 3. Join Chat Room
- **`POST /api/chatapp/chatrooms/{roomId}/join`**
- **Request Body:**
  ```json
  {
    "participant": "guest_user"
  }
  ```
- **Response (`200 OK`):**
  ```json
  {
    "message": "User 'guest_user' joined chat room 'general'.",
    "status": "success"
  }
  ```

### 4. Send Message
- **`POST /api/chatapp/chatrooms/{roomId}/messages`**
- **Request Body:**
  ```json
  {
    "participant": "guest_user",
    "message": "Hello, everyone!"
  }
  ```
- **Response (`200 OK`):**
  ```json
  {
    "message": "Message sent successfully.",
    "status": "success"
  }
  ```

### 5. Retrieve Message History
- **`GET /api/chatapp/chatrooms/{roomId}/messages?limit=10`**
- **Query Parameter:** `limit` (default: 10, min: 1, max: 100)
- **Response (`200 OK`):**
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

### 6. Delete Chat Room
- **`DELETE /api/chatapp/chatrooms/{roomId}`**
- **Response (`200 OK`):**
  ```json
  {
    "message": "Chat room 'general' deleted successfully.",
    "status": "success"
  }
  ```

---

## 🛡️ Error Handling Contract

All error responses across all endpoints adhere to the unified JSON contract:

```json
{
  "status": "error",
  "message": "Descriptive error message"
}
```

For validation failures (`400 Bad Request`), a `details` array lists field-specific errors:
```json
{
  "status": "error",
  "message": "Validation failed.",
  "details": [
    "roomName: roomName must not be blank"
  ]
}
```

| HTTP Status | Error Scenario |
| :---: | :--- |
| **`400 Bad Request`** | Blank field, validation constraint violation, or out-of-range `limit` param |
| **`403 Forbidden`** | Sender has not joined the target chat room |
| **`404 Not Found`** | Targeted chat room does not exist or has been deleted |
| **`405 Method Not Allowed`** | Invalid HTTP verb called on endpoint |
| **`409 Conflict`** | Duplicate chat room name on creation |
| **`415 Unsupported Media Type`** | Request content type is not `application/json` |
| **`503 Service Unavailable`** | Redis datastore unreachable / connection failure |

---

## 🧪 Testing Real-Time Pub/Sub via Terminal (Optional)

To observe real-time message broadcasting directly:
1. Open a terminal and run the Redis CLI subscriber against your local container:
   ```bash
   docker exec -it redis-chat-app redis-cli SUBSCRIBE chatroom:general:channel
   ```
2. Post a message to `general` via Swagger UI or Postman.
3. The published JSON message payload will instantly appear on your terminal output in real time.
