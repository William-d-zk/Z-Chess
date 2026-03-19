# Code Formatting Skill

## Pre-commit Code Formatting

Before creating a git commit, ALWAYS run the code formatter to ensure code follows the project style guide.

### Formatter Configuration
- Uses Spotless Maven Plugin with Google Java Format 1.18.1
- Style: Google Java Style Guide

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
- Google Java Style (4-space indentation)
- Line length: 100 characters
- Braces on same line for classes, methods, control structures
- Automatic import organization

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

### Fast Test (Skip Coverage)
```bash
mvn test -Pfast
```

### Test Utilities
The project provides test utilities in `Z-Audience/src/test/java/com/isahl/chess/audience/testing/`:
- `BaseTest` - Base test class with common assertions
- `TestData` - Random test data generators
- `Mockery` - Mockito utilities
- `IntegrationTest` - Integration test annotation

See `docs/TESTING.md` for detailed testing guidelines.
