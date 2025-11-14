package com.transcript.pipeline.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for text processing operations.
 * Includes chunking, token estimation, and text cleaning.
 */
public class TextProcessingUtil {

    private static final Logger logger = LoggerFactory.getLogger(TextProcessingUtil.class);

    // Pattern for detecting headings (Markdown style)
    private static final Pattern HEADING_PATTERN = Pattern.compile("^#+\\s+(.+)$", Pattern.MULTILINE);

    // Pattern for paragraph breaks
    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("\\n\\s*\\n");

    // Average words per token (approximation for English)
    private static final double WORDS_PER_TOKEN = 0.75;

    /**
     * Estimate token count for a given text
     */
    public static int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int wordCount = text.trim().split("\\s+").length;
        return (int) Math.ceil(wordCount / WORDS_PER_TOKEN);
    }

    /**
     * Split text into paragraphs
     */
    public static List<String> splitIntoParagraphs(String text) {
        List<String> paragraphs = new ArrayList<>();
        String[] parts = PARAGRAPH_PATTERN.split(text);

        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                paragraphs.add(trimmed);
            }
        }

        return paragraphs;
    }

    /**
     * Extract headings from text
     */
    public static List<String> extractHeadings(String text) {
        List<String> headings = new ArrayList<>();
        Matcher matcher = HEADING_PATTERN.matcher(text);

        while (matcher.find()) {
            headings.add(matcher.group(1));
        }

        return headings;
    }

    /**
     * Perform semantic chunking with heading awareness
     */
    public static List<TextChunkData> semanticChunk(String text, int targetChunkSize, int overlap) {
        List<TextChunkData> chunks = new ArrayList<>();
        int chunkId = 1;

        List<String> paragraphs = splitIntoParagraphs(text);
        List<TextChunkData> currentChunk = new ArrayList<>();
        int currentSize = 0;

        for (String paragraph : paragraphs) {
            int paragraphSize = estimateTokenCount(paragraph);

            // Start new chunk if current exceeds target size
            if (currentSize > 0 && currentSize + paragraphSize > targetChunkSize) {
                chunks.addAll(finalizeChunk(currentChunk, chunkId, targetChunkSize));
                chunkId++;

                // Keep last few paragraphs for overlap
                currentChunk = new ArrayList<>();
                currentSize = 0;
            }

            currentChunk.add(new TextChunkData(paragraph, paragraphSize));
            currentSize += paragraphSize;
        }

        // Add remaining content
        if (!currentChunk.isEmpty()) {
            chunks.addAll(finalizeChunk(currentChunk, chunkId, targetChunkSize));
        }

        logger.info("Created {} chunks from text", chunks.size());
        return chunks;
    }

    /**
     * Finalize a chunk and return it
     */
    private static List<TextChunkData> finalizeChunk(List<TextChunkData> paragraphs, int chunkId, int targetSize) {
        List<TextChunkData> result = new ArrayList<>();

        StringBuilder chunkText = new StringBuilder();
        int chunkSize = 0;

        for (TextChunkData para : paragraphs) {
            chunkText.append(para.getText()).append("\n\n");
            chunkSize += para.getTokenCount();
        }

        String finalText = chunkText.toString().trim();
        result.add(new TextChunkData(
                "Chunk " + chunkId,
                finalText,
                chunkId,
                chunkSize
        ));

        return result;
    }

    /**
     * Clean text by removing extra whitespace and normalizing line breaks
     */
    public static String cleanText(String text) {
        if (text == null) {
            return "";
        }

        // Normalize line breaks
        text = text.replaceAll("\\r\\n", "\n");

        // Remove multiple consecutive blank lines
        text = text.replaceAll("\\n\\s*\\n\\s*\\n+", "\n\n");

        // Trim leading and trailing whitespace
        return text.trim();
    }

    /**
     * Truncate text to maximum tokens
     */
    public static String truncateToTokens(String text, int maxTokens) {
        int estimatedTokens = estimateTokenCount(text);

        if (estimatedTokens <= maxTokens) {
            return text;
        }

        // Rough estimate: remove last 20% of words for safety margin
        int targetWords = (int) (text.split("\\s+").length * 0.8);
        String[] words = text.split("\\s+");

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < Math.min(words.length, targetWords); i++) {
            if (i > 0) {
                result.append(" ");
            }
            result.append(words[i]);
        }

        return result.toString();
    }

    /**
     * Inner class to hold chunk data during processing
     */
    public static class TextChunkData {
        private String text;
        private int tokenCount;
        private String title;
        private int chunkId;

        public TextChunkData(String text, int tokenCount) {
            this.text = text;
            this.tokenCount = tokenCount;
        }

        public TextChunkData(String title, String text, int chunkId, int tokenCount) {
            this.title = title;
            this.text = text;
            this.chunkId = chunkId;
            this.tokenCount = tokenCount;
        }

        // Getters
        public String getText() {
            return text;
        }

        public int getTokenCount() {
            return tokenCount;
        }

        public String getTitle() {
            return title;
        }

        public int getChunkId() {
            return chunkId;
        }
    }
}
