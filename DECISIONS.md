# Architecture & Design Decisions Log

## 2026-11-18 - Decision 1: Project Setup & Baseline Stack (Slice 1)
- **Decision:** Spring Boot 3.3.5, Java 17, Maven with wrapper (`mvnw.cmd`), Lettuce client via `StringRedisTemplate`.
- **Why:** Complies strictly with the assignment constraints and AGENTS.md requirements (no SQL, lightweight, fast, deterministic).
- **What was rejected:** Lombok (rejected per AGENTS.md in favor of native Java records), `@RedisHash` (rejected to use explicit Redis Hash/Set/List data structures), Jedis/Redisson (rejected in favor of standard Lettuce).

## 2026-11-18 - Decision 2: OpenAPI / Swagger Documentation
- **Decision:** Added `springdoc-openapi-starter-webmvc-ui` (v2.6.0).
- **Why:** The assignment submission form requires a live Swagger documentation link (`/swagger-ui.html`) and OpenAPI JSON (`/v3/api-docs`) to verify and test endpoints.
- **What was rejected:** Manual-only documentation (rejected because form submission requires a live Swagger link).

## 2026-11-18 - Decision 3: Atomic Duplicate Detection via SADD (Slice 2)
- **Decision:** Used Redis `SADD` return value (0 vs 1) directly in `RedisChatRoomRepository.saveRoom` to detect duplicates.
- **Why:** Guarantees atomic $O(1)$ duplicate prevention without check-then-act race conditions between concurrent room creation requests.
- **What was rejected:** Check-then-create pattern (`SISMEMBER` then `SADD`) which is vulnerable to concurrency race conditions.
