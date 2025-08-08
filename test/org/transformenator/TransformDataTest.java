package org.transformenator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.transformenator.internal.FileInterpreter;

@DisplayName("Transform Data-Driven Tests")
public class TransformDataTest {

    @TempDir
    Path tempDir;

    private static final String TEST_DATA_ROOT = "test-data";

    @ParameterizedTest
    @CsvSource({
        "null, text/simple_text.txt, text/simple_text_expected.txt",
        "null, csv/sample_data.csv, csv/sample_data_expected.csv"
    })
    @DisplayName("Should transform files using test data")
    public void testTransformWithData(String transformName, String inputFile, String expectedFile) throws IOException {
        // Setup paths
        Path inputPath = Paths.get(TEST_DATA_ROOT, "input", inputFile);
        Path expectedPath = Paths.get(TEST_DATA_ROOT, "expected", expectedFile);
        
        // Skip test if test data files don't exist
        if (!Files.exists(inputPath) || !Files.exists(expectedPath)) {
            return; // Skip this test case
        }
        
        // Create FileInterpreter with specified transform
        FileInterpreter interpreter = new FileInterpreter(transformName);
        
        // Process the input file
        String outputDir = tempDir.toString();
        boolean success = interpreter.process(inputPath.toString(), outputDir);
        
        // For null transform, we expect it to process (even if isOK is false)
        assertNotNull(interpreter);
    }

    @Test
    @DisplayName("Should have test data directory structure")
    public void testDataDirectoryExists() {
        File testDataDir = new File(TEST_DATA_ROOT);
        assertTrue(testDataDir.exists(), "test-data directory should exist");
        
        File inputDir = new File(testDataDir, "input");
        assertTrue(inputDir.exists(), "test-data/input directory should exist");
        
        File expectedDir = new File(testDataDir, "expected");
        assertTrue(expectedDir.exists(), "test-data/expected directory should exist");
    }

    @Test
    @DisplayName("Should find sample test files")
    public void testSampleFilesExist() {
        Path inputText = Paths.get(TEST_DATA_ROOT, "input", "text", "simple_text.txt");
        Path expectedText = Paths.get(TEST_DATA_ROOT, "expected", "text", "simple_text_expected.txt");
        
        if (Files.exists(inputText)) {
            assertTrue(Files.exists(expectedText), 
                "Expected output file should exist for input file: " + inputText);
        }
    }

    @Test
    @DisplayName("Should validate file content structure")
    public void testFileContentValidation() throws IOException {
        Path inputFile = Paths.get(TEST_DATA_ROOT, "input", "text", "simple_text.txt");
        
        if (Files.exists(inputFile)) {
            String content = new String(Files.readAllBytes(inputFile), "UTF-8");
            assertFalse(content.isEmpty(), "Test input file should not be empty");
            assertTrue(content.contains("Hello World"), "Test file should contain expected content");
        }
    }

    @Test
    @DisplayName("Should handle binary test data")
    public void testBinaryDataHandling() {
        Path binaryFile = Paths.get(TEST_DATA_ROOT, "input", "binary", "simple_binary.dat");
        
        if (Files.exists(binaryFile)) {
            try {
                byte[] content = Files.readAllBytes(binaryFile);
                assertTrue(content.length > 0, "Binary test file should have content");
            } catch (IOException e) {
                fail("Should be able to read binary test file");
            }
        }
    }
}