# Test Data Inventory

This document tracks all test data files and their purposes.

## Current Test Cases

### Text Format Tests
| Input File | Expected Output | Transform | Purpose |
|------------|----------------|-----------|---------|
| `text/simple_text.txt` | `text/simple_text_expected.txt` | null | Basic text processing |

### Binary Format Tests  
| Input File | Expected Output | Transform | Purpose |
|------------|----------------|-----------|---------|
| `binary/simple_binary.dat` | `binary/simple_binary_expected.txt` | null | Binary to text conversion |
| `binary/wordstar_sample.bin` | `binary/wordstar_sample_expected.txt` | null | WordStar document processing |

### CSV Format Tests
| Input File | Expected Output | Transform | Purpose |
|------------|----------------|-----------|---------|  
| `csv/sample_data.csv` | `csv/sample_data_expected.csv` | null | CSV passthrough |

## Transform Specifications

### Available Transforms
- **null**: Pass-through transform (no changes)
- **wordstar_rtf**: WordStar to RTF conversion (future)
- **apple2_text**: Apple II text extraction (future)

## Adding New Test Cases

1. Create input file in appropriate `input/` subdirectory
2. Generate expected output using manual transformation
3. Save expected output in corresponding `expected/` subdirectory  
4. Update this inventory with new test case
5. Add parameterized test case in `TransformDataTest.java`

## File Size Guidelines

- Keep test files small (< 1KB for unit tests)
- Use larger files (< 5KB) only for integration tests
- Place large sample files in `samples/` directory
- Document any special encoding or format requirements

## Quality Checklist

- [ ] Input file is minimal but representative
- [ ] Expected output is manually verified
- [ ] File names follow naming convention
- [ ] Test case is documented in this inventory
- [ ] Parameterized test exists for the file pair
- [ ] Files are committed to version control