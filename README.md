![](https://github.com/RaghavendraCodes/Reflex-cache/edit/main/Screenshot 2025-04-14 230527.png)
# ReflexCache 

reflex-cache is an in-memory key-value store designed for fast access, uses flexible data structures and persistance of data via log files and aof files with a CLI interaction.

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

