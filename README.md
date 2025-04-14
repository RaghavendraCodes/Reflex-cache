
<div align="center">
  <img src="reflex.png">
</div>

# ReflexCache 

ReflexCache is a fast, in-memory key-value store built for per-client isolation, and persistent logging. It uses a multi-cachebase architecture with the Cachebase Threading Model (CTM) to enable parallel, thread-safe operations per cachebase. Designed with simplicity and speed in mind.

## Features

### Core Commands

| Command                               | Description                                                      |
|----------------------------------------|------------------------------------------------------------------|
| `SET <cachebase> <key> <value>`       | Stores a key-value pair in the specified cachebase.              |
| `GET <cachebase> <key>`               | Retrieves the value associated with the given key.               |
| `DELETE <cachebase> <key>`            | Deletes the key from the given cachebase.                        |
| `DISPLAY`                             | Displays all key-value pairs in a table for the current cachebase.|
| `PING`                                | Health check, responds with `pong`.                              |
| `TIME`                                | Returns current server time.                                     |
| `EXIT`                                | Closes the client session.                                       |

### Client Session Management

- **Login/Registration**: Clients must register or login with credentials.
- **Multi-Tenant Isolation**: Each client’s data is fully isolated from others.
- **Session Logging**: All commands are logged per client in `logs/<client>.log`.

### Persistence

- **Append-Only File (AOF)**: `SET` operations are persisted in `aof/<client>.aof`.
- **Recovery**: Use `RECOVER` command to reload from AOF on startup.
- **FLUSH / FLUSHFULL**:
  - `FLUSH`: Clears only in-memory store.
  - `FLUSHFULL`: Clears memory and AOF logs for a full reset.
 
### Multi-Database (Cachebase) Support

| Command                          | Description                                                    |
|----------------------------------|----------------------------------------------------------------|
| `CREATE cachebase<cachebase>`    | Creates a new cachebase under the current client.              |
| `USE <cachebase>`               | Activates a specific cachebase for interactive commands.       |
| `EXITDB`                        | Exits the current active cachebase context.                    |
| `LISTDB`                        | Lists all cachebases available to the current client.          |
| `RENAME <old> <new>`            | Renames a cachebase.                                           |
| `REMOVE <cachebase>`           | Deletes a cachebase permanently.                               |

### Misc Commands

| Command       | Description                                  |
|---------------|----------------------------------------------|
| `PING`        | Health check. Responds with `pong`.          |
| `TIME`        | Returns current server time.                 |
| `FILE`        | Displays contents of AOF log.                |
| `EXIT`        | Closes the current client session.           |

---

## Architecture

### Cachebase Threading Model (CTM)

Each cachebase operates with:

- **One Dispatcher Thread**:
  - Handles client commands for that cachebase.
  - Distributes key operations to workers.

- **Multiple Worker Threads**:
  - Each handles a partition of keys.
  - Workers manage a segment of the keyspace without locking across threads.

This design offers **Redis-like single-thread predictability** with **multithreaded throughput** under cachebase-level isolation.

---

## Benchmark & Testing

### API Simulation with Real Data

Reflex supports fetching data from external APIs and pipelining `SET` operations for high performance. Below is an example benchmark demonstrating Reflex's speed under realistic load conditions.

#### Example Benchmark
Fetching API data in parallel for 1000 users... Sending 1000 SET commands (pipelined)... Time taken for SETs: 239 ms
Performing 20 random GETs... Time taken for GETs: 24 ms

---

## 📌 Coming Soon

- TTL (Time To Live) support
- JSON field-level querying

---

## Frequently Asked Questions (FAQ)

### Q: Is it as fast as Redis?
**A:** ReflexCache is designed with the same core insight as Redis — that eliminating overhead often beats multi-threading. It uses a single dispatcher for request handling and partitioned worker threads that each own a segment of keys. This allows us to parallelize safely without locks or synchronization, which keeps performance high and predictable. While Redis is incredibly optimized in C, ReflexCache proves that the same design philosophy can be brought into Java with strong performance results — especially for use cases where in-memory field querying adds value.

### Q: Why did you choose this architecture?
**A:** I was fascinated by Redis’s design: it's single-threaded, yet outperforms many multi-threaded systems. That made me realize — performance isn’t always about raw concurrency, but about minimizing contention and designing for CPU caches and predictable behavior. I took inspiration from that and added my own twist: partitioned multi-threaded workers that own their data exclusively. This gave me a sweet spot: high parallelism for reads/writes, while retaining a simple, elegant, and lock-free core.

### Q: What was the motivation behind building ReflexCache?
**A:** I wanted to learn by building — not just using — a database. ReflexCache was my way of deeply understanding what makes in-memory stores like Redis work so well. But I didn’t just want to reimplement Redis — I wanted to explore improvements and extensions, like lightweight field-level querying in JSON values, and a multithreaded architecture that remains deterministic and efficient. It was both a challenge and an experiment in thoughtful design.

### Q: What trade-offs did you consider?
**A:** I avoided aggressive multi-threading to keep things simple and safe. Instead, I used key-partitioned worker threads to get parallelism without introducing locking complexity. 

---

## ReflexCache Project Overview

**ReflexCache** is a high-performance, Redis-inspired in-memory key-value store that I designed and built from scratch in Java.

I was fascinated by Redis's core idea — that simplicity often beats complexity, and that even a single-threaded system can outperform multi-threaded ones if the design is tight. But I also wanted to experiment with parallelism without compromising determinism, so I came up with a hybrid model.

ReflexCache uses a single-threaded dispatcher to receive commands — just like Redis — but instead of processing them serially, it routes them to partitioned worker threads, each of which exclusively owns a subset of the keyspace. This gives me lock-free parallelism, great CPU cache locality, and scalability across cores.

Internally, ReflexCache supports logical multi-tenant databases (called *cachebases*), and each one can have its own dispatcher and set of worker threads. I'm also working on lightweight replication and failover, similar to Redis Sentinel — but tailored for a single-process design using dedicated threads.

It was a fun but intense project that taught me a lot about system design, memory management, and the power of thoughtful trade-offs.
