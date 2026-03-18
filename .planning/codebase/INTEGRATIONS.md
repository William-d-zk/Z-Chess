# Z-Chess External Integrations

## Database Integrations

### PostgreSQL (Primary)
- **Driver:** `org.postgresql:postgresql` 42.7.8
- **Config:** Spring Boot DataSource with JPA
- **Key Files:**
  - `Z-Rook/src/main/java/com/isahl/chess/rook/storage/db/config/RookSource.java`
  - `Z-Rook/src/main/java/com/isahl/chess/rook/storage/db/health/DatabaseHealthIndicator.java`

### SQLite (Local/Testing)
- **Driver:** `org.xerial:sqlite-jdbc` 3.51.2.0
- **Purpose:** Local device state storage in Z-Pawn

## MQTT Integration

### MQTT v5.0/v3.1.1 Broker & Client
- **Library:** Eclipse Paho `org.eclipse.paho.client.mqttv3`
- **Key Files:**
  - `Z-Bishop/src/main/java/com/isahl/chess/bishop/mqtt/v5/` - MQTT v5.0 protocol implementation
  - `Z-Pawn/src/main/java/com/isahl/chess/pawn/endpoint/device/spi/plugin/MQttPlugin.java`
  - `Z-Audience/src/main/java/com/isahl/chess/audience/client/stress/MqttDeviceTestController.java`

### MQTT v5.0 Features
- Enhanced Authentication (SCRAM-SHA-256, PASSWORD)
- Session management
- Retained messages, QoS, wildcards, shared subscriptions
- Topic aliases, message expiry, flow control

### Configuration
- `Z-Pawn/src/main/java/com/isahl/chess/pawn/endpoint/device/config/MqttConfig.java` (prefix: `z.chess.mqtt`)
- `Z-Pawn/src/main/java/com/isahl/chess/pawn/endpoint/device/config/MqttAuthConfig.java` (prefix: `z.chess.mqtt.auth`)
- `Z-Pawn/src/main/java/com/isahl/chess/pawn/endpoint/device/config/MqttSessionConfig.java`

### Auth Providers
- `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/mqtt/service/PasswordAuthProvider.java`
- `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/mqtt/service/ScramSha256AuthProvider.java`

## HTTP Client Integrations

### Java HttpClient (JDK 11+)
- **Usage:** `Z-Audience/src/main/java/com/isahl/chess/audience/client/stress/ApiTestController.java`
- Connect timeout: 10 seconds

### Apache HttpClient 5.x
- **Dependency:** `org.apache.httpcomponents.client5:httpclient5` 5.4
- **Core:** `org.apache.httpcomponents.core5:httpcore5` 5.3.1

### Spring RestTemplate
- **Usage:** `Z-Player/src/main/java/com/isahl/chess/player/api/service/BiddingRpaScheduleService.java`
- Timeout: 5 seconds (from `PlayerConfig.TIMEOUT`)
- Used for external RPA API calls (bidding/cancellation)

## External API Integrations

### RPA Bidding API
- **Config:** `Z-Player/src/main/java/com/isahl/chess/player/api/config/PlayerConfig.java`
  - `bidding.rpa.api.url` - Booking API endpoint
  - `bidding.cancel.api.url` - Cancel booking API endpoint
- **Service:** `Z-Player/src/main/java/com/isahl/chess/player/api/service/BiddingRpaScheduleService.java`

## Authentication / Security

### MQTT Authentication (v5.0 Enhanced Auth)
- **Interface:** `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/mqtt/service/IQttAuthProvider.java`
- Supports: PASSWORD, SCRAM-SHA-256
- Re-authentication support

### SSL/TLS Providers
- **wolfSSL** (primary, version 5.8.4) - configurable via `ssl.provider.force=wolfssl`
- **BouncyCastle** (version 1.83) - cryptographic operations
- **No Spring Security / OAuth2 / JWT** - custom security stack

### NTRU Encryption (ZLS Protocol)
- Custom quantum-resistant encryption in Z-King module
- Used for ZLS protocol authentication

## No External Dependencies For
- Service discovery (Eureka, Consul, Zookeeper)
- Message queues (Redis, RabbitMQ, Kafka)
- Caching (Redis)
