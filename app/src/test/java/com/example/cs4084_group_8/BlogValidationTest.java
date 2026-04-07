package com.example.cs4084_group_8;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BlogValidationTest {

    @Test
    public void normalizeTitleTrimsWhitespace() {
        assertEquals("Morning Session", BlogValidation.normalizeTitle("  Morning Session  "));
    }

    @Test
    public void normalizeBodyTrimsWhitespace() {
        assertEquals("Long form climb note", BlogValidation.normalizeBody("\nLong form climb note\t"));
    }

    @Test
    public void titleValidationRejectsEmptyAndOversizedValues() {
        assertFalse(BlogValidation.isTitleValid("   "));
        assertFalse(BlogValidation.isTitleValid(repeat("a", BlogValidation.MAX_TITLE_LENGTH + 1)));
        assertTrue(BlogValidation.isTitleValid("Short title"));
    }

    @Test
    public void bodyValidationRejectsEmptyAndOversizedValues() {
        assertFalse(BlogValidation.isBodyValid(""));
        assertFalse(BlogValidation.isBodyValid(repeat("b", BlogValidation.MAX_BODY_LENGTH + 1)));
        assertTrue(BlogValidation.isBodyValid("Useful body text"));
    }

    private static String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }
}
