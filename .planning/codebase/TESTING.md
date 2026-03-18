# Z-Chess Testing

## Test Framework

- **JUnit Jupiter** 5.13.1 (JUnit 5)
- **Spring Boot Test** (`spring-boot-starter-test`)
- **JUnit Platform** 1.13.1

## Mocking

- **Mockito** 5.18.0 (`mockito-core`, `mockito-junit-jupiter`)

## Additional Libraries

- **AssertJ** 3.26.3 - fluent assertions
- **Awaitility** 4.2.2 - async assertion
- **H2 Database** - in-memory test database

## Test Structure

```
Z-Audience/src/test/java/com/isahl/chess/audience/
├── testing/           # Test utilities
├── bishop/            # Protocol tests
├── knight/            # Cluster/RAFT tests
├── pawn/              # Endpoint tests
├── queen/             # IO tests
├── king/              # Base/util tests
└── rook/              # Storage tests
```

## Test Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| Unit tests | `*Test.java` | `DeviceServiceTest.java` |
| Integration tests | `*IT.java` or `*IntegrationTest.java` | `MqttBrokerIT.java` |

## Test Utilities

Location: `Z-Audience/src/test/java/com/isahl/chess/audience/testing/`

| File | Purpose |
|------|---------|
| `BaseTest.java` | Base class with common assertions (`assertNotBlank`, `assertHexEquals`, etc.) |
| `TestData.java` | Random test data generators (strings, UUIDs, emails, URLs, etc.) |
| `Mockery.java` | Mockito wrapper simplifying mock creation |
| `IntegrationTest.java` | Annotation for Spring Boot integration tests |

## Coverage Requirements

- **JaCoCo** 0.8.11 for coverage reporting
- **Line coverage:** >= 30%
- **Branch coverage:** >= 20%

## Test Execution Commands

```bash
# Run unit tests
mvn test

# Run tests with coverage report
mvn test jacoco:report

# Run integration tests (requires PostgreSQL)
mvn verify -Pintegration-test

# Fast test (skip coverage check)
mvn test -Pfast
```

## Key Configuration Files

| File | Purpose |
|------|---------|
| `pom.xml` | JaCoCo, surefire, failsafe plugin definitions |
| `Z-Audience/pom.xml` | Test dependencies |
| `docs/TESTING.md` | Testing guidelines |
| `Z-Audience/src/test/resources/application-test.yml` | Test-specific config |

## Writing Tests

### Using BaseTest
```java
public class MyServiceTest extends BaseTest {
    @Test
    void testSomething() {
        assertNotBlank(someString);
    }
}
```

### Using Mockery
```java
public class MyServiceTest extends BaseTest {
    @Mock
    private MyRepository repository;
    
    @InjectMocks
    private MyService service;
}
```

### Using TestData
```java
String randomEmail = TestData.randomEmail();
String randomUuid = TestData.randomUuid();
```
