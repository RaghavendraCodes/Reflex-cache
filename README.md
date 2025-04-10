# ReflexCache 

reflex-cache is an in-memory key-value store designed for fast access, uses flexible data structures and persistance of data via log files and aof files with a CLI interaction.

## Features

### Core Features

- **SET/GET Support**: Standard `SET key value` and `GET key` operations.
- **Multi Cachebases**: User can create new cachebases `CREATE client_name cachebase_name`and use it `USE cachebase_name`.
- **In-Memory Speed**: Uses an internal `MemoryStore` for fast access.
- **Structured Output**: Use `DISPLAY` to see all key-value pairs in a table.

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
 
### Multi-Database Support

- **CREATE clientName dbName**: Creates a logical database under a client.
- **USE dbName**: Switch to a particular database for active operations.
- **EXITDB**: Exit current active database. SET/GET disabled unless one is active.
- **LISTDB clientName**: Lists all databases of a client.

### Misc Commands

| Command       | Description                                  |
|---------------|----------------------------------------------|
| `PING`        | Health check. Responds with `pong`.          |
| `TIME`        | Returns current server time.                 |
| `FILE`        | Displays contents of AOF log.                |
| `EXIT`        | Closes the current client session.           |

---

