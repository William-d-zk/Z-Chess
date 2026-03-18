# Z-Chess Directory Structure

## Project Root

```
Z-Chess/
├── pom.xml                 # Root Maven POM (framework.z-chess v1.0.22)
├── README.md
├── LICENSE.md
├── AGENTS.md               # Agent instructions
├── docs/
│   ├── eclipse-formatter.xml
│   ├── ARCHITECTURE.md
│   ├── CONFIGURATION.md
│   ├── DEPLOYMENT.md
│   ├── API_DOCUMENTATION.md
│   ├── DEVICE_API_GUIDE.md
│   ├── MQTT_TEST_GUIDE.md
│   ├── TESTING.md
│   └── superpowers/        # GSD workflow specs
├── scripts/
│   └── docker/
│       ├── docker-compose.yaml
│       ├── server/Dockerfile
│       └── config/
└── Z-*/                    # 10 Maven modules
```

## Maven Modules (10 total)

| Module | Artifact ID | Purpose |
|--------|-------------|---------|
| Z-Board | `base.z-board` | Base framework, annotation processing |
| Z-King | `crypto.z-king` | Base utilities, logging, exceptions, scheduling |
| Z-Queen | `gateway.z-queen` | Network events, async I/O |
| Z-Bishop | `protocol.z-bishop` | Protocol definitions (MQTT, Websocket) |
| Z-Knight | `cluster.z-knight` | RAFT clustering implementation |
| Z-Rook | `persistence.z-rook` | JPA/database persistence |
| Z-Pawn | `endpoint.z-pawn` | Device endpoint services |
| Z-Player | `biz.z-player` | Business logic, REST API |
| Z-Audience | `test.z-audience` | Testing, client applications |
| Z-Arena | `gateway.z-arena` | API gateway, main entry point |

## Standard Module Structure

Each module follows:
```
Z-*/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/isahl/chess/{module}/
│   │   │   ├── {PackageRoot}.java
│   │   │   ├── start/           # Application entry points
│   │   │   ├── {feature}/
│   │   │   │   ├── config/
│   │   │   │   ├── domain/
│   │   │   │   ├── service/
│   │   │   │   ├── repository/
│   │   │   │   └── spi/
│   │   │   └── ...
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── logback*.xml
│   │       └── ehcache.xml (some modules)
│   └── test/
│       ├── java/com/isahl/chess/{module}/
│       │   ├── testing/         # Test utilities (Z-Audience)
│       │   └── {feature}/
│       └── resources/
│           └── application-test.properties
```

## Key Package Naming

Packages follow: `com.isahl.chess.{module}.{feature}`

| Module | Package Root |
|--------|--------------|
| Z-Board | `com.isahl.chess.board` |
| Z-King | `com.isahl.chess.king` |
| Z-Queen | `com.isahl.chess.queen` |
| Z-Bishop | `com.isahl.chess.bishop` |
| Z-Knight | `com.isahl.chess.knight` |
| Z-Rook | `com.isahl.chess.rook` |
| Z-Pawn | `com.isahl.chess.pawn` |
| Z-Player | `com.isahl.chess.player` |
| Z-Audience | `com.isahl.chess.audience` |
| Z-Arena | `com.isahl.chess.arena` |

## Key File Locations

| Item | Path |
|------|------|
| Arena entry point | `Z-Arena/src/main/java/com/isahl/chess/arena/start/ApplicationArena.java` |
| Audience entry point | `Z-Audience/src/main/java/com/isahl/chess/audience/start/` |
| Root pom.xml | `pom.xml` |
| Formatter config | `docs/eclipse-formatter.xml` |
| Test utilities | `Z-Audience/src/test/java/com/isahl/chess/audience/testing/` |
| Base test class | `Z-Audience/src/test/java/com/isahl/chess/audience/testing/BaseTest.java` |
| Database schemas | `Z-Rook/src/main/resources/*/schema.sql` |
| Docker compose | `scripts/docker/docker-compose.yaml` |

## Configuration File Locations

| Module | Config Files |
|--------|-------------|
| Z-Arena | `Z-Arena/src/main/resources/application*.properties` (local, daily, online, local-tls) |
| Z-Audience | `Z-Audience/src/main/resources/application*.properties`, `application-docker.yml`, `application-test.yml` |
| Z-Player | `Z-Player/src/main/resources/application.properties` |
| Z-Pawn | `Z-Pawn/src/main/resources/application.properties` |
| Global docker | `scripts/docker/config/application-docker.properties` |
