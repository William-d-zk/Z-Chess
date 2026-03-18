# Z-Chess Architecture

## Overview

Z-Chess is a distributed IoT message middleware platform built on Java 17 + Spring Boot 3.5.9. It provides high-concurrency, high-availability device connectivity and message processing.

## Architecture Pattern

**Layered Architecture** with Chess piece naming convention:
- Modules are named after chess pieces (Z-Board, Z-King, Z-Queen, etc.)
- Functionality increases from bottom to top (foundation → gateway)

## Layer Breakdown

```
┌─────────────────────────────────────────────────────────────────┐
│                         Gateway Layer                            │
│                     Z-Arena [Gateway/Load Balancer]             │
├─────────────────────────────────────────────────────────────────┤
│                         Business Layer                           │
│                     Z-Player [Open API Services]                │
├─────────────────────────────────────────────────────────────────┤
│                         Endpoint Layer                           │
│              Z-Pawn [MQTT/Websocket Device Access]               │
├─────────────────────────────────────────────────────────────────┤
│                         Cluster Layer                            │
│    Z-Knight [RAFT Consensus + Spring Boot REST Services]        │
├─────────────────────────────────────────────────────────────────┤
│                         Protocol Layer                           │
│         Z-Bishop [MQTT v5.0/v3.1.1, Websocket, ZChat]          │
├─────────────────────────────────────────────────────────────────┤
│                         IO Core Layer                            │
│        Z-Queen [AIO Async Network Core, TCP/TLS/ZLS]            │
├─────────────────────────────────────────────────────────────────┤
│                         Storage Layer                            │
│         Z-Rook [JPA/PostgreSQL + EhCache]                       │
├─────────────────────────────────────────────────────────────────┤
│                         Foundation Layer                         │
│    Z-King [Event Bus, TimeWheel, ZUID, Crypto, CRC]             │
├─────────────────────────────────────────────────────────────────┤
│                         Metadata Layer                           │
│              Z-Board [Annotation Processor, Serializer]          │
├─────────────────────────────────────────────────────────────────┤
│                         Test Layer                              │
│                   Z-Audience [Test Suite]                       │
└─────────────────────────────────────────────────────────────────┘
```

## Module Responsibilities

| Module | Responsibility |
|--------|---------------|
| **Z-Board** | Compile-time annotation processing, serialization code generation |
| **Z-King** | Shared infrastructure: Disruptor event bus, TimeWheel scheduler, ZUID generator, crypto utilities |
| **Z-Queen** | Async non-blocking network I/O (AIO sockets), TCP/TLS/ZLS support |
| **Z-Bishop** | Protocol encoding/decoding: MQTT, Websocket, ZChat, ZLS |
| **Z-Knight** | RAFT consensus implementation, cluster management, REST services |
| **Z-Rook** | Data persistence: JPA/PostgreSQL, EhCache multi-level caching |
| **Z-Pawn** | IoT device access: MQTT broker, device management, offline message persistence |
| **Z-Player** | Business API layer: REST controllers, device management, message push |
| **Z-Arena** | Unified entry point: gateway routing, cache queries, service aggregation |
| **Z-Audience** | Test suite: unit tests, integration tests, stress tests |

## Data Flow

```
Device → MQTT/Websocket → Z-Pawn → RAFT Cluster (Z-Knight)
                              ↓
                         Z-Player (API)
                              ↓
                         Z-Arena (Gateway)
                              ↓
                    External Systems / Admin
```

## Key Abstractions

| Component | Location | Purpose |
|-----------|----------|---------|
| `ServerCore` | Z-Queen | External service core with socket + cluster capabilities |
| `ClusterCore` | Z-Queen | Pure cluster communication core for REST microservices |
| `FilterChain` | Z-Queen | Chained filter architecture for request processing |
| `AioSession` | Z-Queen | Session management for async I/O |
| `ProtocolFactory` | Z-Bishop | Protocol creation and routing |
| `FrameFilter`, `CommandFilter` | Z-Bishop | Protocol filter chains |
| `ReloadableSSLContext` | Z-Bishop | SSL/TLS context with certificate hot-reload |
| `BaseRepository` | Z-Rook | Generic CRUD abstraction |

## Security Architecture

### Transport Layer Security
- TLS 1.3 default encryption
- Certificate hot-reload support
- SSL Provider priority: WolfSSL → OpenSSL → JDK

### Application Layer Security
- AES-256-GCM symmetric encryption
- SHA-256 digest algorithm
- SecureRandom for key generation
- Constant-time comparison for timing attack prevention

### Authentication
- MQTT: Device ID + password verification
- ZLS: NTRU-based post-quantum encryption

## High Availability Design

### Cluster Architecture
- RAFT consensus: 3/5 node clusters with automatic leader election
- Strong consistency log replication
- Automatic failover: < 1 second leader recovery

### Stateless Design
- Server maintains no device session state
- Session state persisted to PostgreSQL
- Horizontal scaling supported

## Performance Characteristics

| Component | Metric |
|-----------|--------|
| CRC Calculation | 7,500,000 ops/sec |
| Disruptor | 1,000,000+ TPS (single thread) |
| TLS Handshake | WolfSSL optimized |
| Connections | 100,000+ concurrent |

## Module Dependency Graph

```
Z-Board (compile-time)
    ↓
Z-King → all modules depend
    ↓
Z-Queen ← Z-King
    ↓
Z-Bishop ← Z-Queen
    ↓
Z-Knight ← Z-Bishop
    ↓
Z-Rook ← Z-Knight
Z-Pawn ← Z-Knight
    ↓
Z-Player ← Z-Pawn
    ↓
Z-Arena ← Z-Player
    ↓
Z-Audience (tests all modules)
```
