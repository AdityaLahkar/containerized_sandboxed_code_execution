# SandCode 🚀

A REST API for executing untrusted user code in isolated Docker containers. Built with **Spring Boot** and **Java**, this project is designed to evaluate raw code snippets (like C, C++, Java, Python)(current version 1.0 is limited to C only) safely on a host machine by leveraging container isolation, strict resource quotas, and stream pipeline management.

## 🏗 System Architecture & Design (V1.0)

The core architecture follows a **synchronous execution model**. When a request hits the API, the backend provisions a fully isolated, ephemeral Docker container specifically for that payload, executes the code, captures the streams, and destroys the container before returning the output.

### 🛡 Core Security & Isolation Principles
- **Ephemeral Containers:** Every execution triggers a fresh `docker run --rm` command. The container is completely obliterated upon exit.
- **Strict Resource Limits:** Containers are locked down using `CPUs` (e.g., `0.5` cores), `Memory` (e.g., `128m`), and process IDs (`--pids-limit 16`) to prevent fork-bombs and memory leak attacks against the host.
- **Disk Protection & Orphan Prevention:** Containers are uniquely tracked by request UUID and explicitly annihilated via `docker rm -f` hook. Logging is completely disabled (`--log-driver=none`) to prevent script infinite loops from ever consuming host disk space via endless `stdout` streams.
- **Read-Only Volume Mounts:** Source code is mounted into the container securely via a read-only volume (`:ro`). Compilers generate binaries exclusively in the container's ephemeral internal `/tmp` directory, preventing the container from poisoning the host filesystem.
- **Network Isolation:** Completely offline execution (`--network none`). Untrusted code cannot trigger webhooks, mine crypto, or pull internet payloads.
- **Deadlock-Free Stream Handling:** Solves JVM `ProcessBuilder` OS buffer deadlocks by aggressively multiplexing `stdout` and `stderr` streams directly into an asynchronous background reading thread.
- **Defensive Timeouts:** Enforces strict wall-clock time limits (e.g., 5 seconds). Infinite loops (`while(true)`) result in forceful destruction (`destroyForcibly()`) without locking host resources.
- **Exception Masking:** Handled via a `@ControllerAdvice` global exception handler to guarantee backend host details (stack traces, paths) are never leaked via the API; exceptions mapping to `HTTP 500` or `HTTP 400` cleanly.

### 🧩 The Strategy Pattern (SOLID)
Adhering to the Open/Closed Principle, the execution logic is heavily abstracted using the **Strategy Pattern**. The core `ExecutorService` acts as an orchestrator and holds no language-specific compilation logic. Adding support for a new language only requires injecting a new `@Component` that implements `LanguageStrategy`—automatically discovered by the Spring `StrategyFactory` without touching legacy core code.

### 💻 Frontend (SandCode UI)
SandCode natively ships with a premium, zero-dependency IDE interface served directly from the Spring Boot static resources directory.
- **Modern Responsive Design:** A sleek dark-mode aesthetic utilizing bespoke CSS, dynamic hover states, and seamless layout handling.
- **Dynamic Payload Routing:** Uses native REST API `fetch()` pipelines to asynchronously submit user-code to the backend (`POST /api/run`) and render output natively without hard reloading.
- **XSS Payload Defense:** Uses proactive regex text-serialization filtering to protect the browser terminal from rendering malicious HTML or `<script>` outputs maliciously compiled inside Docker.

---

## 🔌 API Usage

**Endpoint:** `POST /api/run`  
**Content-Type:** `application/json`

### Success Example
**Request:**
```json
{
  "language": "c",
  "code": "#include <stdio.h>\nint main() { printf(\"Hello World Server!\\n\"); return 0; }"
}
```
**Response (HTTP 200 OK):**
```json
{
  "error": "",
  "output": "Hello World Server!\n",
  "status": "success"
}
```

### Timeout/Infinite Loop Example
**Request:**
```json
{
  "language": "python",
  "code": "while True: pass"
}
```
**Response (HTTP 200 OK):**
```json
{
  "error": "Execution timed out",
  "output": "",
  "status": "timeout"
}
```

---

## 🔮 Future Improvements & Limitations (V2.0)

While V1.0 is extremely robust on a single-request basis, there are known areas for horizontal expansion and optimization that will be addressed in future phases:

1. **Concurrency Limitations (DDoS via Volume):**
    - *Limitation:* If 100 users hit `/api/run` simultaneously, the JVM attempts to spawn 100 `docker run` commands synchronously, saturating the host CPU and triggering timeouts en masse.
    - *Improvement:* Introduce an **Asynchronous Message Queue** (e.g., RabbitMQ, Redis) + Worker Pool. The API returns a `job_id`, and workers process the execution pool synchronously, storing results in a DB. Client polling/WebSockets retrieve the result.
2. **"Cold Start" Latency:**
    - *Limitation:* Spinning up a fresh Docker container on every request incurs ~0.5-1.5s overhead.
    - *Improvement:* Keep a **"Warm Pool"** of idle, long-running containers. Inject the code and execute it over an internal HTTP socket or use `docker exec`, drastically slashing execution latency to <10ms.
3. **Interactive Terminals:**
    - *Limitation:* Currently supports static code evaluation without arbitrary raw `stdin` interaction during runtime over multiple steps.
    - *Improvement:* Expand the `CodeRequest` payload to accept an `input_data` array, pipe it exclusively to the execution thread's InputStream, or transition fully to WebSocket-based execution framing.
4. **Host File Cleanup Risk (`/tmp` Graveyard):**
    - *Limitation:* Java relies on a `try...finally` block to wipe the `/tmp/{uuid}` host directories. A hard, unhandled JVM crash (e.g. OutOfMemoryError) risks leaving orphaned folders on the disk.
    - *Improvement:* Introduce a Spring `@Scheduled` cron job to periodically prune all files from the workspace directory older than 10 minutes.
