# Z-Chess Concerns & Technical Debt

## Incomplete Implementations (Phase Markers)

### Modbus TLS Phase 3
| File | Issue |
|------|-------|
| `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/tcp/ModbusTlsClient.java:84` | `sendRequest()` throws `UnsupportedOperationException("TLS encrypted transmission requires Phase 3 implementation")` |
| `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/tcp/ModbusTlsServer.java:80-96` | TLS async handling is placeholder, logs "Phase 3 implementation pending" |
| `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/tcp/ModbusTlsAsyncServer.java:172` | Phase 3 TLS async implementation marked as simplified |
| `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/tcp/ModbusTlsAsyncClient.java` | Phase 3 TLS implementation incomplete |

### Modbus Protocol Phase 2
| File | Issue |
|------|-------|
| `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/slave/ModbusSlave.java:31` | "Phase 2: 完整实现服务端监听和请求处理" - still incomplete |
| `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/function/ModbusExtendedFunctions.java:11` | "支持 Phase 2 功能码：0x07, 0x08, 0x11, 0x16, 0x17" - only partial support |

## Known Limitations (from README.md)

| Item | Limitation |
|------|------------|
| UDP | ZLS有待实现 (UDP not implemented) |
| ZLS Security | 仅支持单边认证，NTRU(RC4)需注意密钥强度 |
| MQTT | 尚有大量协议规范的broker行为尚未支持 |
| Protocol Extension | 尚未提供便于二次开发使用的协议处理链扩展点 |
| Cluster Management | 集群管理职能开发中 |

## Security Concerns

### NTRU/RC4 Key Strength
- ZLS protocol uses NTRU(RC4) and key strength needs review
- Single-sided authentication only for ZLS

### MQTT Authentication
- Supports SCRAM-SHA-256 but many broker behaviors not fully implemented

### No OAuth2/JWT
- Custom security stack (not Spring Security)
- No standard token-based auth

## Code Quality Issues

### System.exit() Usage (Production Anti-pattern)
| File | Line | Context |
|------|------|---------|
| `Z-Rook/src/main/java/com/isahl/chess/rook/storage/db/health/DatabaseHealthIndicator.java` | 83, 114 | Uses `System.exit(1)` on database connection failure |
| `Z-Arena/src/main/java/com/isahl/chess/arena/start/ApplicationArena.java` | 98 | Uses `System.exit(1)` for missing env vars |

### System.out/System.err Usage
- 150+ instances found across codebase
- Example: `Z-Arena/src/main/java/com/isahl/chess/arena/start/ApplicationArena.java:77-96`
- Should use proper logging framework

### Volatile Usage (59 instances)
Thread safety via volatile fields in:
- `Z-Knight/src/main/java/com/isahl/chess/knight/raft/service/RaftPeer.java`
- `Z-King/src/main/java/com/isahl/chess/king/base/cron/TimeWheel.java`
- `Z-Bishop/src/main/java/com/isahl/chess/bishop/io/ssl/SslProviderFactory.java`

## JPA Enum Mapping Risk

From `Z-Pawn/Comments.md`:
> JPA 存储时 entity 中存在 enum 项目需要加入 `@Enumerated(EnumType.STRING)`注解，否则数据库中将以数字进行存储

Risk of forward-incompatibility if not followed.

## TODO/FIXME Markers

| File | Line | Note |
|------|------|------|
| `Z-Pawn/src/main/java/com/isahl/chess/pawn/endpoint/device/db/central/repository/IDeviceRepository.java` | 34 | Comment about JPA repository naming conventions |

## Performance Notes

| Component | Metric |
|-----------|--------|
| CRC Calculation | 7,500,000 ops/sec |
| Disruptor | 1,000,000+ TPS (single thread) |
| TLS Handshake | WolfSSL optimized |
| Connections | 100,000+ concurrent |

## Priority Areas Needing Attention

1. **Modbus TLS Phase 3** - `sendRequest()` throws UnsupportedOperationException
2. **MQTT 5.0 Completeness** - many broker behaviors not yet supported
3. **ZLS Security** - NTRU(RC4) key strength concerns, single-sided auth
4. **Cluster Management** - still in development per README
5. **UDP Support** - documented as "有待实现"
6. **Protocol SPI** - no extension point for custom protocols yet
