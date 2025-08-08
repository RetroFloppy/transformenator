package org.transformenator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Helper class for managing test data files and providing utilities
 * for data-driven testing with transform operations.
 */
public class TestDataHelper {
    
    public static final String TEST_DATA_ROOT = "test-data";
    public static final String INPUT_DIR = "input";
    public static final String EXPECTED_DIR = "expected";
    public static final String TRANSFORMS_DIR = "transforms";
    public static final String SAMPLES_DIR = "samples";

    /**
     * Get all available test case combinations from test data directory
     */
    public static List<TestCase> getAllTestCases() throws IOException {
        List<TestCase> testCases = new ArrayList<>();
        Path inputRoot = Paths.get(TEST_DATA_ROOT, INPUT_DIR);
        
        if (!Files.exists(inputRoot)) {
            return testCases;
        }
        
        try (Stream<Path> paths = Files.walk(inputRoot)) {
            paths.filter(Files::isRegularFile)
                 .forEach(inputFile -> {
                     try {
                         String relativePath = inputRoot.relativize(inputFile).toString();
                         String expectedPath = findExpectedFile(relativePath);
                         if (expectedPath != null) {
                             testCases.add(new TestCase("null", relativePath, expectedPath));
                         }
                     } catch (Exception e) {
                         // Skip files that cause issues
                     }
                 });
        }
        
        return testCases;
    }

    /**
     * Find the corresponding expected output file for an input file
     */
    private static String findExpectedFile(String inputRelativePath) {
        // Convert input path to expected path
        // Example: "text/simple_text.txt" -> "text/simple_text_expected.txt"
        
        Path inputPath = Paths.get(inputRelativePath);
        String fileName = inputPath.getFileName().toString();
        String parentDir = inputPath.getParent() != null ? inputPath.getParent().toString() : "";
        
        // Remove extension and add _expected
        int lastDot = fileName.lastIndexOf('.');
        String nameWithoutExt = lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
        String extension = lastDot > 0 ? fileName.substring(lastDot) : "";
        
        String expectedFileName = nameWithoutExt + "_expected" + extension;
        String expectedRelativePath = parentDir.isEmpty() ? expectedFileName : parentDir + "/" + expectedFileName;
        
        Path expectedPath = Paths.get(TEST_DATA_ROOT, EXPECTED_DIR, expectedRelativePath);
        return Files.exists(expectedPath) ? expectedRelativePath : null;
    }

    /**
     * Read file content as string, handling both text and binary files
     */
    public static String readFileContent(String relativePath, boolean isExpected) throws IOException {
        String baseDir = isExpected ? EXPECTED_DIR : INPUT_DIR;
        Path filePath = Paths.get(TEST_DATA_ROOT, baseDir, relativePath);
        
        if (Files.exists(filePath)) {
            // For text files, read as string
            if (relativePath.endsWith(".txt") || relativePath.endsWith(".rtf") || 
                relativePath.endsWith(".html") || relativePath.endsWith(".csv")) {
                return new String(Files.readAllBytes(filePath), "UTF-8");
            } else {
                // For binary files, convert to hex representation
                byte[] bytes = Files.readAllBytes(filePath);
                StringBuilder hex = new StringBuilder();
                for (byte b : bytes) {
                    hex.append(String.format("%02x", b & 0xFF));
                }
                return hex.toString();
            }
        }
        
        throw new IOException("File not found: " + filePath);
    }

    /**
     * Get the absolute path for a test data file
     */
    public static String getAbsolutePath(String relativePath, boolean isExpected) {
        String baseDir = isExpected ? EXPECTED_DIR : INPUT_DIR;
        return Paths.get(TEST_DATA_ROOT, baseDir, relativePath).toAbsolutePath().toString();
    }

    /**
     * Check if test data directories exist and are properly structured
     */
    public static boolean validateTestDataStructure() {
        File testDataDir = new File(TEST_DATA_ROOT);
        if (!testDataDir.exists()) return false;
        
        String[] requiredDirs = {INPUT_DIR, EXPECTED_DIR, TRANSFORMS_DIR, SAMPLES_DIR};
        for (String dir : requiredDirs) {
            if (!new File(testDataDir, dir).exists()) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Data class representing a test case with input, expected output, and transform
     */
    public static class TestCase {
        public final String transformName;
        public final String inputFile;
        public final String expectedFile;
        
        public TestCase(String transformName, String inputFile, String expectedFile) {
            this.transformName = transformName;
            this.inputFile = inputFile;
            this.expectedFile = expectedFile;
        }
        
        @Override
        public String toString() {
            return String.format("TestCase{transform='%s', input='%s', expected='%s'}", 
                transformName, inputFile, expectedFile);
        }
    }
}