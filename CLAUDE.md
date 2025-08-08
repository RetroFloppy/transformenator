# Transformenator - Claude Development Notes

## Project Overview

Transformenator is a Java-based tool for converting legacy document formats from obsolete word processors and computer systems into modern formats like RTF, HTML, and plain text. It's particularly valuable for digital preservation and archival work.

## Architecture

### Core Components
- **Transform.java**: Main entry point and command-line interface
- **FileInterpreter.java**: Core transformation engine using pattern matching
- **CSVInterpreter.java**: Structured data extraction and CSV generation
- **Detanglers**: Specialized processors for complex legacy formats
- **Transform Specifications**: Pattern-based rules in `/transforms/` directory

### Key Classes
- `org.transformenator.Transform`: CLI and file processing orchestration
- `org.transformenator.internal.FileInterpreter`: Binary pattern matching engine
- `org.transformenator.internal.UnsignedByte`: Byte conversion utilities
- `org.transformenator.detanglers.ADetangler`: Base class for format-specific processors

## Build System

### Apache Ant Build
- **Java 8 compatible** (updated from Java 10 due to environment constraints)
- **Main targets**:
  - `ant all` - Full build, test, and package
  - `ant build` - Compile source and create JAR
  - `ant test` - Run complete test suite
  - `ant compile-tests` - Compile test classes only

### Dependencies
- **JUnit 5**: Testing framework (`lib/junit-platform-console-standalone-1.10.0.jar`)
- **No external runtime dependencies** - self-contained application

## Testing Framework

### Unit Testing (JUnit 5)
- **20 tests total** across core functionality
- **Test Structure**:
  ```
  test/org/transformenator/
  ├── TransformTest.java           # Filename sanitization (6 tests)
  ├── TransformDataTest.java       # Data-driven testing (6 tests)  
  ├── TestDataHelper.java          # Test utilities
  └── internal/
      ├── FileInterpreterTest.java # Transform validation (5 tests)
      └── UnsignedByteTest.java    # Byte conversion (3 tests)
  ```

### Data-Driven Testing
- **Static test data** in `test-data/` directory
- **Organized structure**:
  ```
  test-data/
  ├── input/       # Test input files by format
  ├── expected/    # Expected transformation outputs
  ├── transforms/  # Transform specification samples
  └── samples/     # Real-world legacy files
  ```
- **Parameterized tests** for multiple format validation
- **Test data inventory** tracking in `TEST_DATA_INVENTORY.md`

### Running Tests
```bash
# Full test suite
ant test

# Direct JUnit execution
java -jar lib/junit-platform-console-standalone-1.10.0.jar --class-path bin:bin-test --scan-class-path
```

## Development Guidelines

### Code Compatibility
- **Target Java 8** for maximum compatibility
- **Avoid modern Java features** like `HexFormat`, `Files.readString()`
- **Use String.format()** for hex formatting instead of HexFormat
- **Use Files.readAllBytes()** with String constructor instead of Files.readString()

### Transform Development
1. **Create transform specification** in `/transforms/` directory
2. **Add test data pairs** in `test-data/input/` and `test-data/expected/`
3. **Update test inventory** in `TEST_DATA_INVENTORY.md`
4. **Add parameterized test cases** in `TransformDataTest.java`
5. **Validate with real legacy files** when possible

### File Naming Conventions
- **Input files**: `{system}_{format}_{description}.{ext}`
- **Expected files**: `{system}_{format}_{description}_expected.{ext}`
- **Transform specs**: `{system}_{format}_transform.txt`

## Known Issues & Fixes Applied

### Java Compatibility Issues
- **Fixed**: Replaced `HexFormat` usage with `String.format()` in:
  - `PanasonicKX.java:201,210`
  - `Split.java:61`
- **Fixed**: Replaced `Files.readString()` with `Files.readAllBytes()` + String constructor

### Build Configuration
- **Updated**: Ant build targets from Java 10 to Java 8
- **Added**: Test compilation and execution targets
- **Integrated**: Testing into main build process

## Project Structure

```
transformenator/
├── src/org/transformenator/           # Main source code
│   ├── Transform.java                 # CLI entry point
│   ├── detanglers/                    # Format-specific processors
│   ├── internal/                      # Core engine components
│   ├── transforms/                    # Transform specifications
│   └── util/                          # Utility classes
├── test/org/transformenator/          # Unit tests
├── test-data/                         # Static test files
├── build/build.xml                    # Ant build configuration
├── lib/junit-platform-console-*.jar  # Testing framework
└── out/                               # Build artifacts
```

## Legacy Format Support

### Currently Supported
- **Word Processors**: WordStar, AppleWriter, Perfect Writer, MultiMate
- **Computer Systems**: Apple II, Commodore 64, TRS-80, IBM PC
- **Specialized**: Brother, Smith Corona, Xerox, Wang systems
- **Formats**: Binary documents, disk images, structured records

### Transform Types
- **File-based**: Binary pattern matching and replacement
- **CSV-based**: Structured data extraction
- **Detangler-based**: Complex format-specific processing

## Recent Enhancements

### Unit Testing Framework (Branch: claude-unit-testing)
- **Added**: Comprehensive JUnit 5 test suite
- **Created**: Data-driven testing infrastructure
- **Implemented**: Static test data organization
- **Documented**: Testing procedures and guidelines

### Build System Updates
- **Enhanced**: Ant build with test integration
- **Fixed**: Java 8 compatibility issues
- **Added**: Test data validation and utilities

## Development Workflow

1. **Create feature branch** from main
2. **Add unit tests** for new functionality
3. **Include test data** for transforms
4. **Run full test suite** before commit
5. **Update documentation** as needed
6. **Create pull request** with test coverage

## Resources

- **Project Wiki**: https://github.com/RetroFloppy/transformenator/wiki
- **Transform Specs**: https://github.com/RetroFloppy/transformenator/wiki/Transform-Specification
- **CSV Transforms**: https://github.com/RetroFloppy/transformenator/wiki/CSV-Transform-Specification