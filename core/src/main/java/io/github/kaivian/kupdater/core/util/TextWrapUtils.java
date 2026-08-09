package io.github.kaivian.kupdater.core.util;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility for word-wrapping formatted Minecraft text while preserving active ChatColor formatting codes.
 */
public final class TextWrapUtils {

    private TextWrapUtils() {}

    /**
     * Wraps text at word boundaries to ensure visible character length per line does not exceed maxLineLength.
     * Preserves active Minecraft ChatColor codes across line breaks.
     *
     * @param text String text to wrap (supports color codes like &a or §a and explicit \n)
     * @param maxLineLength Maximum visible characters per line (<= 0 disables wrapping)
     * @return List of formatted lines
     */
    public static List<String> wrapText(String text, int maxLineLength) {
        List<String> result = new ArrayList<>();
        if (text == null) {
            return result;
        }

        if (text.isEmpty()) {
            result.add("");
            return result;
        }

        if (maxLineLength <= 0) {
            result.add(text);
            return result;
        }

        // Support explicit newlines
        String[] rawParagraphs = text.split("\n", -1);
        for (String paragraph : rawParagraphs) {
            if (paragraph.isEmpty()) {
                result.add("");
                continue;
            }

            List<String> wrappedParagraph = wrapSingleParagraph(paragraph, maxLineLength);
            result.addAll(wrappedParagraph);
        }

        return result;
    }

    private static List<String> wrapSingleParagraph(String text, int maxLineLength) {
        List<String> lines = new ArrayList<>();
        String colorTranslated = ChatColor.translateAlternateColorCodes('&', text);
        String[] words = colorTranslated.split(" ");

        StringBuilder currentLine = new StringBuilder();
        int currentLineLength = 0;
        String activeColors = "";

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            int wordLength = stripColor(word).length();

            if (currentLineLength > 0 && currentLineLength + 1 + wordLength > maxLineLength) {
                lines.add(currentLine.toString());

                // Track active colors at the end of the previous line
                activeColors = getLastColors(currentLine.toString());

                currentLine = new StringBuilder();
                currentLine.append(activeColors).append(word);
                currentLineLength = wordLength;
            } else {
                if (currentLineLength > 0) {
                    currentLine.append(" ");
                    currentLineLength += 1;
                } else if (!activeColors.isEmpty()) {
                    currentLine.append(activeColors);
                }
                currentLine.append(word);
                currentLineLength += wordLength;
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    /**
     * Strips ChatColor section symbols from text to calculate visible string length.
     */
    public static String stripColor(String input) {
        if (input == null) return "";
        return ChatColor.stripColor(input);
    }

    /**
     * Extracts active ChatColor formatting codes at the end of a string.
     */
    public static String getLastColors(String input) {
        if (input == null) return "";
        return ChatColor.getLastColors(input);
    }
}
