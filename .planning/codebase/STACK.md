# Z-Chess Technology Stack

## Languages & Runtime

| Language | Version | Purpose |
|----------|---------|---------|
| Java | 17 (LTS) | Primary language |
| SQL | - | Database scripts |

## Build System

- **Maven** 3.9+ (multi-module project)
- Root POM: `pom.xml`
- Module POMs: Each `Z-*/pom.xml`

## Frameworks & Libraries

### Core Framework
- **Spring Boot** 3.5.9
  - spring-boot-starter-web
  - spring-boot-starter-data-jpa
  - spring-boot-starter-validation
  - spring-boot-starter-cache
  - spring-boot-starter-actuator
  - spring-boot-starter-undertow

### Networking
- **Apache HttpComponents** core5 5.3.1, client5 5.4
- **Eclipse Paho** MQTT Client 1.2.5
- **LMAX Disruptor** 4.0.0 (high-performance async events)

### Database
- **PostgreSQL** JDBC 42.7.8
- **SQLite** JDBC 3.51.2.0
- **Hibernate Types** 2.21.1

### Caching
- **Ehcache** 3.11.1

### Security / Cryptography
- **BouncyCastle** 1.83
- **WolfSSL** 5.8.4 (JSSE provider)
- Custom NTRU encryption (Z-King module)

### JSON Processing
- **Jackson** 2.18.6
- **JSON Schema Validator** 3.0.0

### Logging
- **slf4j** 2.0.17
- **Logback** 1.5.25

### Testing
- **JUnit Jupiter** 5.13.1
- **Mockito** 5.18.0
- **AssertJ** 3.26.3
- **Awaitility** 4.2.2
- **JaCoCo** 0.8.11 (coverage)

### Code Formatting
- **Spotless Maven Plugin** 3.3.0 with Eclipse formatter
- Config: `docs/eclipse-formatter.xml`

## Key Dependencies by Module

| Module | Key Dependencies |
|--------|-------------------|
| Z-Board | annotation processing |
| Z-King | Disruptor, CRC, Crypto, TimeWheel |
| Z-Queen | AIO socket, TLS |
| Z-Bishop | Paho MQTT, HttpComponents, WolfSSL |
| Z-Knight | Spring Boot, JPA, RAFT |
| Z-Rook | PostgreSQL, SQLite, Ehcache |
| Z-Pawn | MQTT broker, device management |
| Z-Player | Spring Web, REST |
| Z-Arena | Spring Boot, undertow |
| Z-Audience | All test dependencies |

## Configuration Files

| File | Purpose |
|------|---------|
| `pom.xml` | Root Maven config |
| `docs/eclipse-formatter.xml` | Code formatting rules |
| `Z-Arena/src/main/resources/application*.properties` | Arena config (local, docker, online) |
| `Z-Audience/src/main/resources/application*.properties` | Audience config |
| `Z-Player/src/main/resources/application.properties` | Player config |
| `Z-*/src/main/resources/ehcache.xml` | Cache config |
| `Z-Rook/src/main/resources/*/schema.sql` | Database schemas |
| `scripts/docker/docker-compose.yaml` | Docker deployment |
