# AGENTS.md: Redis Chat App

Read this file fully before writing any code, and follow the process below for every task. If a request in chat conflicts with this file, stop and ask before acting.

## 1. Goal and hard constraints

Build a Spring Boot chat application that uses Redis as its primary data store (a take-home backend assignment with a 3-day deadline). Users create chat rooms, join them, send messages and fetch history. Redis Pub/Sub delivers real-time updates.

- Java 17, Spring Boot 3.x, Maven.
- REST API only, no UI. It must be testable from Postman.
- Redis is the only datastore. No SQL database.
- Required Redis structures: Hash (room metadata), Set (participants), List (messages), Pub/Sub (real time).
- Graded on: correct use of Redis structures, clean layered design with design patterns, TDD with JUnit, error handling, and a clear README on how to run it.

## 2. Working process (follow for every slice)

Work in small vertical slices (section 10). For each slice:

1. **Read** the slice and its "done when" check.
2. **Red:** write the failing test first. Run it and confirm it fails for the right reason (missing behaviour, not a typo). Commit: `test: <behaviour>`.
3. **Green:** write the least code that makes it pass. Run the tests. Commit: `feat: <behaviour>`.
4. **Refactor:** clean up with tests green (names, duplication, constants). Run the full suite with `mvn test`. Commit `refactor: <what>` only if something changed.
5. **Docs:** if an endpoint, Redis key or behaviour changed, update the README in the same slice.
6. **Record:** tick the slice in section 13. Log any non-obvious design decision in `DECISIONS.md` (date, decision, why, what was rejected), one short entry each.
7. **Report** in two or three lines: what was done, how many tests pass, which slice is next.
8. Continue to the next slice. Stop and ask only when a requirement is ambiguous or a change would alter the API contract in section 6.

Never mark a slice done while any test fails. Never write production code before its failing test.

## 3. Tech stack (fixed, do not swap)

| Need | Use |
| --- | --- |
| Framework | Spring Boot 3, Java 17, Maven |
| Redis client | `spring-boot-starter-data-redis` (Lettuce) with `StringRedisTemplate` |
| JSON | Jackson `ObjectMapper` (already in Spring Boot) |
| Validation | `spring-boot-starter-validation` |
| DTOs | Java `record`s |
| Real-time delivery | Redis Pub/Sub, then SSE (`SseEmitter`) |
| Unit tests | JUnit 5 + Mockito (from `spring-boot-starter-test`) |
| Integration tests | Testcontainers (`redis:7-alpine`) |
| Coverage | JaCoCo |
| Local Redis | `docker-compose.yml` with `redis:7`, `--appendonly yes` |

Do not add other dependencies without asking. Do not use Lombok, `@RedisHash` repositories, Jedis, Redisson or `GenericJackson2JsonRedisSerializer`.

## 4. Package structure

Base package: `com.example.chatapp` (change only if the user gives another).

| Package | Contents | Responsibility |
| --- | --- | --- |
| `controller` | `ChatRoomController` | HTTP mapping, `@Valid`, status codes |
| `service` | `ChatRoomService`, `ChatMessageService` | Business rules (room exists, participant joined) |
| `repository` | `ChatRoomRepository`, `MessageRepository` (interfaces) and `Redis...Repository` implementations | All Redis commands live here |
| `pubsub` | `MessagePublisher`, `MessageSubscriber`, `SseBroadcaster` | Publish on send, listen on `chatroom:*:channel`, push to SSE clients |
| `dto` | request and response records | API shapes and validation rules |
| `model` | `ChatMessage` | Stored message |
| `config` | `RedisConfig`, `RedisKeys` | Beans, listener container, key constants |
| `exception` | custom exceptions, `GlobalExceptionHandler` | One place that maps errors to status codes |

Rules:
- Only the `repository` and `pubsub` packages call Redis. Controllers and services never touch `StringRedisTemplate`.
- Services depend on repository interfaces, so unit tests can mock them.
- Inject `java.time.Clock` into services that create timestamps.
- Constructor injection only. No field `@Autowired`.

## 5. Redis key design

`roomId` is the room name lowercased. Keep every key name in `RedisKeys`, nowhere else.

| Data | Type | Key | Commands |
| --- | --- | --- | --- |
| Room registry | Set | `chatrooms:names` | `SADD`, `SISMEMBER`, `SREM` |
| Room metadata | Hash | `chatroom:{roomId}` | `HSET`, `HGETALL` (fields: `roomId`, `roomName`, `createdAt`) |
| Participants | Set | `chatroom:{roomId}:participants` | `SADD`, `SISMEMBER`, `SREM` |
| Messages | List | `chatroom:{roomId}:messages` | `RPUSH`, `LRANGE`, `LTRIM` |
| Real-time | Pub/Sub | `chatroom:{roomId}:channel` | `PUBLISH`, pattern `chatroom:*:channel` |

Message format, one JSON string per list entry, exactly these three fields (no `@class` or extra fields):

```json
{"participant":"guest_user","message":"Hello, everyone!","timestamp":"2024-01-01T10:00:00Z"}
```

Timestamps are UTC ISO-8601 truncated to seconds (`Instant.now(clock).truncatedTo(ChronoUnit.SECONDS)`).

Rules:
- Create a room with the return value of `SADD chatrooms:names {roomId}`. A result of 0 means duplicate, so throw `RoomAlreadyExistsException`. Never check-then-create.
- Fetch the last N messages with `LRANGE key -N -1`. Never read the whole list.
- After each `RPUSH`, `LTRIM` to the latest 1000 messages (constant `MAX_MESSAGES_PER_ROOM`).
- Never use the `KEYS` command.

## 6. API contract (do not change without asking)

Base path: `/api/chatapp`

| Endpoint | Success | Errors |
| --- | --- | --- |
| `POST /chatrooms` | 201 | 409 duplicate name, 400 invalid name |
| `POST /chatrooms/{roomId}/join` | 200 | 404 no room, 400 blank participant |
| `POST /chatrooms/{roomId}/messages` | 200 | 404 no room, 403 not a participant, 400 blank fields |
| `GET /chatrooms/{roomId}/messages?limit=N` | 200 | 404 no room, 400 bad limit |
| `GET /chatrooms/{roomId}/stream` | 200 (SSE) | 404 no room |
| `DELETE /chatrooms/{roomId}` | 200 | 404 no room |

Request and response bodies:

```json
// POST /chatrooms
{ "roomName": "general" }
// 201
{ "message": "Chat room 'general' created successfully.", "roomId": "general", "status": "success" }

// POST /chatrooms/general/join
{ "participant": "guest_user" }
// 200
{ "message": "User 'guest_user' joined chat room 'general'.", "status": "success" }

// POST /chatrooms/general/messages
{ "participant": "guest_user", "message": "Hello, everyone!" }
// 200
{ "message": "Message sent successfully.", "status": "success" }

// GET /chatrooms/general/messages?limit=10   (oldest to newest within the window)
{ "messages": [ { "participant": "guest_user", "message": "Hello, everyone!", "timestamp": "2024-01-01T10:00:00Z" } ] }
```

Behaviour rules:
- `limit` defaults to 10, minimum 1, maximum 100. `limit=0` or negative returns 400 (it must never return the whole list).
- Room name: letters, digits, `-` and `_` only, 1 to 50 characters, stored lowercased. Participant: not blank, at most 50 characters. Message: not blank, at most 1000 characters.
- Joining twice is allowed and returns 200 (the Set ignores the duplicate).
- Only a participant who joined can send a message.
- `DELETE` removes the metadata Hash, participants Set, messages List and the registry entry, and completes any open SSE emitters for that room.
- SSE sends each message JSON as an event named `message`. Keep emitters in a `ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>>` and remove them on completion, timeout and error.

## 7. Error response format

Every error uses one shape from one `@RestControllerAdvice`:

```json
{ "status": "error", "message": "Chat room 'general' already exists." }
```

Validation errors add `"details": ["roomName: must not be blank"]`.

| Case | Status | Message |
| --- | --- | --- |
| Duplicate room | 409 | `Chat room '{roomId}' already exists.` |
| Room not found | 404 | `Chat room '{roomId}' not found.` |
| Not a participant | 403 | `User '{participant}' has not joined chat room '{roomId}'.` |
| Invalid input | 400 | field messages in `details` |
| Anything unexpected | 500 | `Unexpected error.` (log the exception, never return a stack trace) |

## 8. Code conventions

- Controller, then service, then repository. No business logic in controllers, no HTTP concepts in services or repositories.
- Patterns to use and name in the README: Repository (Redis access behind interfaces), Observer (Pub/Sub publisher and subscriber), DTOs separate from the stored model, dependency inversion (services use interfaces).
- Configuration through `application.yml`. Redis host and port come from environment variables with defaults: `${REDIS_HOST:localhost}`, `${REDIS_PORT:6379}`.
- Log with SLF4J (`LoggerFactory.getLogger(...)`). No `System.out`.
- Comments explain why, not what. Add a short Javadoc on each public service and repository method.
- Catch `Exception` only inside `GlobalExceptionHandler`.
- No commented-out code, no unused imports.

## 9. Testing rules

- Test names read as sentences, for example `sendMessage_toMissingRoom_throwsRoomNotFound`.
- Per slice, test from the inside out:
  1. Service unit test with a mocked repository.
  2. Controller test with MockMvc (`@WebMvcTest`): status codes, validation, JSON shape.
  3. Repository integration test with Testcontainers: the Redis commands really work.
- Do not mock Redis to prove command correctness.
- The five assignment test cases must exist as named integration tests:
  1. Create and join chat room (metadata stored, participant in the Set).
  2. Send and retrieve messages (correct order, last N).
  3. Real-time messaging (a published message reaches the subscriber).
  4. Error handling (send to a non-existent room returns an error).
  5. Chat room deletion (all room keys removed).
- Cover every branch of the services: success, duplicate, missing room, not a participant, boundary `limit` values.
- Run `mvn test` before every commit and `mvn verify` (JaCoCo) before finishing a phase.

## 10. Build order

### Phase 1: working solution
| Slice | Work | Done when |
| --- | --- | --- |
| S1 | Project setup: `pom.xml`, `application.yml`, `docker-compose.yml`, `RedisConfig`, `RedisKeys`, `.gitignore`, README skeleton | App starts and connects to Redis, `mvn test` passes |
| S2 | Create room (also create `GlobalExceptionHandler` with 409) | Create works, duplicate returns 409 |
| S3 | Join room | Participant in the Set, missing room returns 404 |
| S4 | Send message: check room, `RPUSH`, `PUBLISH` | Message stored as clean JSON and published |
| S5 | Get history with `LRANGE -N -1` | Last N returned oldest to newest |
| S6 | Error handling consolidation (404, 409, 400, generic 500) | Every error uses the section 7 shape |
| S7 | `RedisMessageListenerContainer` and `MessageSubscriber` that logs received messages | Subscriber receives a published message |

Phase 1 is done when this Postman flow works: create room, join, send two messages, fetch last 1 and last 10, duplicate room gives 409, missing room gives 404.

### Phase 2: improve
| Slice | Work | Done when |
| --- | --- | --- |
| S8 | Validation: `@Valid`, room name pattern, size limits, `limit` rules | Invalid input never returns 500 |
| S9 | Only joined participants can send (403) | Non-participant gets 403 |
| S10 | SSE endpoint and `SseBroadcaster` wired to the subscriber | `curl -N` on the stream shows a message sent from Postman |
| S11 | `LTRIM` cap on each room's list | List never exceeds 1000 entries |
| S12 | Delete room | All room keys gone, emitters completed |
| S13 | Integration tests for the five assignment test cases | All five pass against a real Redis |

### Phase 3: polish
| Slice | Work | Done when |
| --- | --- | --- |
| S14 | Docker Compose with AOF verified, full README, Postman collection in `docs/postman/`, JaCoCo report, fill coverage gaps, final refactor pass | A fresh clone runs with `docker compose up -d` and `mvn spring-boot:run`, and the Postman collection passes |

If time is short, cut in this order: S12 (delete room), then S10 (SSE). Never cut tests or the README.

## 11. README must contain

1. What the project is, in two sentences.
2. Prerequisites: JDK 17, Maven, Docker.
3. How to run: `docker compose up -d`, `mvn spring-boot:run`, `mvn test`.
4. API reference with a request and response example for every endpoint.
5. Redis key design table.
6. Architecture and design patterns used.
7. How to test real-time messaging with `curl -N` and Postman.
8. Known limitations (for example, room creation uses two Redis steps that are not atomic).

## 12. Do not

- Change the API contract, key design or package structure without asking.
- Serialize with `GenericJackson2JsonRedisSerializer` or any serializer that adds `@class`.
- Run `FLUSHALL` or `FLUSHDB` against anything except a Testcontainers Redis.
- Commit `target/`, `.idea/`, `*.iml` or secrets.
- Edit this file except to tick the progress list below.

## 13. Progress

- [x] S1 Setup
- [x] S2 Create room
- [x] S3 Join room
- [x] S4 Send message
- [x] S5 Get history
- [x] S6 Error handling
- [x] S7 Pub/Sub subscriber
- [x] S8 Validation
- [x] S9 Participant check
- [ ] S10 SSE stream
- [x] S11 LTRIM cap
- [x] S12 Delete room
- [x] S13 Assignment test cases
- [x] S14 Docs, Postman collection, coverage
