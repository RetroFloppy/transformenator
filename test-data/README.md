# Test Data Directory

This directory contains static test data files for validating transform operations and ensuring consistent behavior across different input formats.

## Directory Structure

```
test-data/
├── input/              # Input files for testing transforms
│   ├── binary/         # Raw binary files from legacy systems
│   ├── text/           # Text-based input files
│   ├── rtf/            # RTF format inputs
│   ├── html/           # HTML format inputs
│   └── csv/            # CSV format inputs
├── expected/           # Expected output files after transformation
│   ├── binary/         # Expected binary outputs
│   ├── text/           # Expected text outputs
│   ├── rtf/            # Expected RTF outputs
│   ├── html/           # Expected HTML outputs
│   └── csv/            # Expected CSV outputs
├── transforms/         # Sample transform specification files
└── samples/            # Real-world sample files from various systems
```

## Usage in Tests

### Data-Driven Testing Pattern
```java
@ParameterizedTest
@MethodSource("getTransformTestCases")
@DisplayName("Should transform files correctly")
public void testTransformations(String transformName, String inputFile, String expectedFile) {
    // Load input file
    File input = new File("test-data/input/" + inputFile);
    
    // Apply transformation
    FileInterpreter interpreter = new FileInterpreter(transformName);
    String result = interpreter.process(input.getAbsolutePath(), tempDir.getAbsolutePath());
    
    // Compare with expected output
    File expected = new File("test-data/expected/" + expectedFile);
    assertEquals(Files.readString(expected.toPath()), Files.readString(result.toPath()));
}
```

## File Naming Convention

### Input Files
- `{system}_{format}_{description}.{ext}`
- Examples:
  - `wordstar_rtf_simple_document.bin`
  - `apple2_text_hello_world.dat`
  - `brother_wpt_letter.bin`

### Expected Files
- `{system}_{format}_{description}_expected.{ext}`
- Examples:
  - `wordstar_rtf_simple_document_expected.rtf`
  - `apple2_text_hello_world_expected.txt`
  - `brother_wpt_letter_expected.rtf`

### Transform Files
- `{system}_{format}_transform.txt`
- Examples:
  - `wordstar_rtf_transform.txt`
  - `apple2_text_transform.txt`

## Adding New Test Data

1. **Create input file**: Place in appropriate `input/` subdirectory
2. **Generate expected output**: Run transformation manually and verify output
3. **Save expected file**: Place in corresponding `expected/` subdirectory
4. **Document the test case**: Add entry to test data inventory
5. **Create unit test**: Reference the files in parameterized tests

## Test Data Categories

### Binary Formats
- Legacy word processor documents
- Disk images and archives
- Proprietary binary formats

### Text Formats
- Plain text with special encoding
- Structured text files
- Configuration files

### Markup Formats
- RTF documents
- HTML files
- XML-based formats

### Structured Data
- CSV files
- Database exports
- Tabular data

## Quality Guidelines

- **Small files**: Keep test files under 1KB when possible
- **Focused tests**: Each file should test specific transformation aspects
- **Clear naming**: Use descriptive filenames
- **Documentation**: Include comments explaining special cases
- **Version control**: All test data should be committed to git