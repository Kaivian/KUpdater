package io.github.kaivian.kupdater.testing.unit;

import io.github.kaivian.kupdater.core.util.TextWrapUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TextWrapUtilsTest {

    @Test
    public void testWrapTextShortStringNotWrapped() {
        String input = "&e⚡ Reached max level!";
        List<String> wrapped = TextWrapUtils.wrapText(input, 35);
        assertEquals(1, wrapped.size());
        assertTrue(wrapped.get(0).contains("Reached max level!"));
    }

    @Test
    public void testWrapTextLongStringWrappedAtSpaces() {
        String input = "&e⚡ Reached max level! Upgrade with &f8 Stone&e in Crafting Table.";
        List<String> wrapped = TextWrapUtils.wrapText(input, 35);

        assertTrue(wrapped.size() >= 2);
        for (String line : wrapped) {
            assertTrue(TextWrapUtils.stripColor(line).length() <= 35);
        }
    }

    @Test
    public void testWrapTextPreservesColorAcrossLines() {
        String input = "&e⚡ Reached max level! Upgrade with &f8 Stone&e in Crafting Table.";
        List<String> wrapped = TextWrapUtils.wrapText(input, 35);

        assertNotNull(wrapped);
        assertTrue(wrapped.size() >= 2);
        // Line 2 should start with color formatting codes
        String line2 = wrapped.get(1);
        assertTrue(line2.startsWith("§") || line2.startsWith("&"));
    }

    @Test
    public void testWrapEmptyStringPreserved() {
        List<String> wrapped = TextWrapUtils.wrapText("", 35);
        assertEquals(1, wrapped.size());
        assertEquals("", wrapped.get(0));
    }
}
