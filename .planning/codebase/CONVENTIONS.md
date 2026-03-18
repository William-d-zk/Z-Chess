# Z-Chess Coding Conventions

## Code Formatting

**Tool:** Spotless Maven Plugin with Eclipse formatter
**Config:** `docs/eclipse-formatter.xml`

| Rule | Value |
|------|-------|
| Indentation | 4-space (smart tabs) |
| Line length | 120 characters |
| Braces | New line for classes, methods, control structures |
| Control structures | No space before parenthesis |
| Lambda arrow | Space around (`x -> x + 1`) |
| Single-line blocks | Allowed |

**Commands:**
```bash
mvn spotless:apply    # Format all code
mvn spotless:check    # Check without modifying
```

## Naming Conventions

| Element | Convention | Examples |
|---------|------------|----------|
| Classes | PascalCase | `DeviceController`, `SerialProcessor` |
| Interfaces | PascalCase with 'I' prefix | `IFactory`, `IAnnotationProcessor`, `ISerial` |
| Private fields | `m` prefix | `mId`, `mPassword`, `mLogger` |
| Protected/package fields | `_` prefix | `_ZProcessor`, `_Head`, `_MixOpenService` |
| Constants | UPPER_SNAKE_CASE | `serialVersionUID` |
| Methods | camelCase | `findByToken`, `updateDevice` |
| Packages | lowercase | `com.isahl.chess.board.processor` |

## Common Patterns

### Module Naming
Chess pieces: `Z-Queen`, `Z-Bishop`, `Z-Rook`, `Z-Pawn`, `Z-King`, `Z-Knight`, `Z-Board`, `Z-Arena`, `Z-Audience`, `Z-Player`

### Logger Initialization
```java
Logger _Logger = Logger.getLogger("category." + getClass().getSimpleName());
```

### Factory Pattern
Static instance via `*Factory._Instance`

### Dependency Injection
Constructor-based with `@Autowired`

### Response Wrapper
```java
ZResponse.success(data)
ZResponse.error(code, message)
```

### Entity to DTO
Static `of()` factory methods:
```java
DeviceDo.of(DeviceEntity)
```

### Filter Chains
`linkFront()` method for chaining filters

## Error Handling

| Approach | Usage |
|----------|-------|
| Try-catch | Catch specific exceptions, log with `_Logger.warn()`, `_Logger.debug()`, `_Logger.fatal()` |
| Thrown exceptions | `IllegalStateException`, `IOException`, `IllegalArgumentException` |
| Error codes | `CodeKing.ERROR.getCode()` |
| Validation | Annotations: `@NotBlank`, `@Size`, `@Valid`, `@Validated` |
| Null checks | `ObjectUtils.isEmpty()`, `ObjectUtils.nullSafeEquals()` |
| Guard clauses | Early returns with `if (!isBlank(x))` checks |

## Documentation Patterns

### License Header (required)
```java
/*
 * MIT License
 *
 * Copyright (c) 2016~2021. Z-Chess
 *
 * Permission is hereby granted, free of charge...
 * THE SOFTWARE IS PROVIDED "AS IS"...
 */
```

### Javadoc Author Tag
```java
/**
 * @author william.d.zk
 * @author william.d.zk {@code @date} 2019/05/01
 * @since 2019-06-15
 */
```

### Inline Comments (Chinese)
```java
// 安全修复: 从 MD5 升级
// NPE 防护：检查 profile 及 MAC 地址字段
```

## Test Conventions

| Test Type | Naming | Location |
|-----------|--------|----------|
| Unit tests | `*Test.java` | `src/test/java` |
| Integration tests | `*IT.java` or `*IntegrationTest.java` | `src/test/java` |

See `TESTING.md` for full testing conventions.

## JPA Enum Mapping

**Important:** When storing enums in JPA entities, use `@Enumerated(EnumType.STRING)`:
```java
@Enumerated(EnumType.STRING)
private MyEnum myField;
```
Otherwise, the database stores numeric values instead of string names.
