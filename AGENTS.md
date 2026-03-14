# Code Formatting Skill

## Pre-commit Code Formatting

Before creating a git commit, ALWAYS run the code formatter to ensure code follows the project style guide.

### Formatter Configuration
- Uses Spotless Maven Plugin with Eclipse formatter
- Configuration file: `docs/eclipse-formatter.xml`
- Based on IDEA code style: `idea_java_style_william_d_zk.xml`

### Commands

**Format all Java code:**
```bash
mvn spotless:apply
```

**Check formatting without modifying files:**
```bash
mvn spotless:check
```

### Workflow
1. Before running `git commit`, always run `mvn spotless:apply`
2. Review any changes made by the formatter
3. If there are uncommitted formatting changes, add them to the commit

### Key Style Rules
- Braces on new line for classes, methods, control structures
- 4-space indentation (smart tabs)
- Line length: 120 characters
- Single-line blocks, methods, lambdas, and classes allowed
- Space before control structure parentheses: NO (if, while, for, catch, switch, synchronized)
- Space around lambda arrow: YES

---

# Testing Skill

## Test Requirements

This project enforces test coverage requirements:
- **Line coverage**: ≥ 30%
- **Branch coverage**: ≥ 20%

### Running Tests

```bash
# Run unit tests
mvn test

# Run integration tests (requires PostgreSQL)
mvn verify -Pintegration-test

# Run tests with coverage report
mvn test jacoco:report
```

### Test Naming Conventions
- Unit tests: `*Test.java`
- Integration tests: `*IT.java` or `*IntegrationTest.java`

See `docs/TESTING.md` for detailed testing guidelines.
