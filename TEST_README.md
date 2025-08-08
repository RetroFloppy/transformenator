# Unit Testing Setup

## Overview
This project includes **JUnit 5** unit testing framework with basic tests for core functionality.

## Running Tests

### Prerequisites
- Java 8 or higher
- JUnit 5 standalone JAR (included in `lib/` directory)

### Build and Test Commands

```bash
# Compile main source code
javac -source 8 -target 8 -cp . -d bin src/org/transformenator/*.java src/org/transformenator/internal/*.java src/org/transformenator/detanglers/*.java src/org/transformenator/util/*.java

# Copy resource files
cp -r src/org/transformenator/transforms bin/org/transformenator/
cp src/org/transformenator/help*.txt bin/org/transformenator/

# Compile tests  
javac -source 8 -target 8 -cp bin:lib/junit-platform-console-standalone-1.10.0.jar -d bin-test test/org/transformenator/*.java test/org/transformenator/internal/*.java

# Run tests
java -jar lib/junit-platform-console-standalone-1.10.0.jar --class-path bin:bin-test --scan-class-path
```

### Using Ant (if available)
```bash
ant test    # Runs full build and test cycle
ant compile-tests  # Just compile tests
```

## Test Coverage

### Current Tests
- **Transform.java**: Filename cleaning and sanitization (6 tests)
- **FileInterpreter.java**: Transform name validation and processing (5 tests)
- **UnsignedByte.java**: Byte to unsigned integer conversion (3 tests)
- **TransformDataTest.java**: Data-driven testing with static test files (6 tests)

### Data-Driven Testing
- **Test Data Structure**: Organized input/expected file pairs
- **Parameterized Tests**: Automated testing across multiple file formats
- **Test Data Helper**: Utilities for managing test files and validation

### Test Structure
```
test/
├── org/
│   └── transformenator/
│       ├── TransformTest.java
│       ├── TransformDataTest.java
│       ├── TestDataHelper.java
│       └── internal/
│           ├── FileInterpreterTest.java
│           └── UnsignedByteTest.java

test-data/
├── input/              # Test input files
│   ├── binary/         # Binary format inputs
│   ├── text/           # Text format inputs
│   └── csv/            # CSV format inputs
├── expected/           # Expected transformation outputs
│   ├── binary/         # Expected binary outputs
│   ├── text/           # Expected text outputs
│   └── csv/            # Expected CSV outputs
├── transforms/         # Transform specification files
└── samples/            # Real-world sample files
```

## Adding New Tests

1. Create test classes in the `test/` directory mirroring the `src/` structure
2. Use JUnit 5 annotations: `@Test`, `@DisplayName`, `@BeforeEach`, etc.
3. Tests run automatically with the build process

## Dependencies
- **JUnit 5 Platform Console Standalone**: `lib/junit-platform-console-standalone-1.10.0.jar`
  - Includes JUnit Jupiter, Vintage, and Platform modules
  - No additional dependencies required
