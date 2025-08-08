package org.transformenator.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UnsignedByte Tests")
public class UnsignedByteTest {

    @Test
    @DisplayName("Should convert byte to unsigned int correctly")
    public void testIntValue() {
        // Test positive byte value
        assertEquals(100, UnsignedByte.intValue((byte) 100));
        
        // Test zero
        assertEquals(0, UnsignedByte.intValue((byte) 0));
        
        // Test negative byte value (should become positive)
        assertEquals(200, UnsignedByte.intValue((byte) -56)); // -56 + 256 = 200
        
        // Test maximum positive byte
        assertEquals(127, UnsignedByte.intValue((byte) 127));
        
        // Test minimum negative byte (becomes 128)
        assertEquals(128, UnsignedByte.intValue((byte) -128));
        
        // Test -1 (becomes 255)
        assertEquals(255, UnsignedByte.intValue((byte) -1));
    }

    @Test
    @DisplayName("Should handle edge cases")
    public void testEdgeCases() {
        // Test that all possible byte values produce valid unsigned results
        for (int i = -128; i <= 127; i++) {
            int result = UnsignedByte.intValue((byte) i);
            assertTrue(result >= 0 && result <= 255, 
                "Result " + result + " for byte " + i + " should be between 0-255");
        }
    }

    @Test
    @DisplayName("Should maintain consistency")
    public void testConsistency() {
        // Test that the same byte value always produces the same result
        byte testByte = (byte) -100;
        int result1 = UnsignedByte.intValue(testByte);
        int result2 = UnsignedByte.intValue(testByte);
        
        assertEquals(result1, result2);
        assertEquals(156, result1); // -100 + 256 = 156
    }
}