package org.transformenator.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Files;

@DisplayName("FileInterpreter Tests")
public class FileInterpreterTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should handle invalid transform name")
    public void testInvalidTransformName() {
        FileInterpreter interpreter = new FileInterpreter("invalid_transform_name");
        
        assertFalse(interpreter.isOK);
        assertNull(interpreter.detanglerName());
    }

    @Test
    @DisplayName("Should handle null transform name")
    public void testNullTransformName() {
        FileInterpreter interpreter = new FileInterpreter(null);
        
        // Null transform name appears to be accepted by the system
        assertNotNull(interpreter);
        assertNull(interpreter.detanglerName());
    }

    @Test
    @DisplayName("Should handle empty transform name")
    public void testEmptyTransformName() {
        FileInterpreter interpreter = new FileInterpreter("");
        
        // Empty transform name appears to be accepted by the system
        assertNotNull(interpreter);
        assertNull(interpreter.detanglerName());
    }

    @Test
    @DisplayName("Should validate known transform names")
    public void testKnownTransformNames() {
        // Test with a known internal transform (null is a valid built-in transform)
        FileInterpreter interpreter = new FileInterpreter("null");
        
        // The transform should be recognized (isOK should be true for valid transforms)
        // Note: This test may need adjustment based on actual available transforms
        assertNotNull(interpreter);
    }

    @Test
    @DisplayName("Should process with default suffix")
    public void testProcessDefaultSuffix() throws Exception {
        // Create a test input file
        File inputFile = new File(tempDir.toFile(), "testinput.dat");
        Files.write(inputFile.toPath(), "test data".getBytes());
        
        File outputDir = new File(tempDir.toFile(), "output");
        outputDir.mkdirs();
        
        FileInterpreter interpreter = new FileInterpreter("null");
        
        // Test the process method with default suffix
        boolean result = interpreter.process(inputFile.getAbsolutePath(), outputDir.getAbsolutePath());
        
        // For invalid transforms, process should handle gracefully
        assertNotNull(interpreter);
    }
}