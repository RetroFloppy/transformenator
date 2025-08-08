package org.transformenator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Files;

@DisplayName("Transform Tests")
public class TransformTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should clean filename with special characters")
    public void testFixFilename() throws Exception {
        // Create a file with problematic characters
        File testFile = new File(tempDir.toFile(), "test*file?.txt");
        testFile.createNewFile();
        
        // Fix the filename
        File fixedFile = Transform.fixFilename(testFile);
        
        // Verify the file was renamed  
        assertNotEquals(testFile.getName(), fixedFile.getName());
        assertEquals("test_file_txt", fixedFile.getName());
        assertTrue(fixedFile.exists());
        assertFalse(testFile.exists());
    }

    @Test
    @DisplayName("Should not rename file with valid filename")
    public void testFixFilenameNoChange() throws Exception {
        // Create a file with valid filename
        File testFile = new File(tempDir.toFile(), "validfile.txt");
        testFile.createNewFile();
        
        // Fix the filename
        File fixedFile = Transform.fixFilename(testFile);
        
        // Verify the file was not renamed
        assertEquals(testFile.getName(), fixedFile.getName());
        assertEquals(testFile.getAbsolutePath(), fixedFile.getAbsolutePath());
        assertTrue(fixedFile.exists());
    }

    @Test
    @DisplayName("Should handle leading and trailing spaces")
    public void testFixFilenameSpaces() throws Exception {
        // Create a file with spaces
        File testFile = new File(tempDir.toFile(), " spacefile ");
        testFile.createNewFile();
        
        // Fix the filename
        File fixedFile = Transform.fixFilename(testFile);
        
        // Verify spaces were replaced with underscores
        assertEquals("_spacefile_", fixedFile.getName());
        assertTrue(fixedFile.exists());
        assertFalse(testFile.exists());
    }

    @Test
    @DisplayName("Should condition filename")
    public void testConditionFileName() {
        String input = "testfile.txt";
        String result = Transform.conditionFileName(input);
        
        // Currently just returns the input unchanged
        assertEquals(input, result);
    }

    @Test
    @DisplayName("Should handle emdash character")
    public void testFixFilenameEmdash() throws Exception {
        // Create a file with emdash character
        File testFile = new File(tempDir.toFile(), "test—file.txt");
        testFile.createNewFile();
        
        // Fix the filename
        File fixedFile = Transform.fixFilename(testFile);
        
        // Verify emdash was replaced (actual behavior removes the character)  
        assertEquals("test-filetxt", fixedFile.getName());
        assertTrue(fixedFile.exists());
        assertFalse(testFile.exists());
    }

    @Test
    @DisplayName("Should handle final period removal")
    public void testFixFilenameFinalPeriod() throws Exception {
        // Create a file with final period
        File testFile = new File(tempDir.toFile(), "testfile.");
        testFile.createNewFile();
        
        // Fix the filename
        File fixedFile = Transform.fixFilename(testFile);
        
        // Verify final period was removed
        assertEquals("testfile", fixedFile.getName());
        assertTrue(fixedFile.exists());
        assertFalse(testFile.exists());
    }
}