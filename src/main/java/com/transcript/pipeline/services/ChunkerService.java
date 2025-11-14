package com.transcript.pipeline.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transcript.pipeline.config.ConfigManager;
import com.transcript.pipeline.models.TextChunk;
import com.transcript.pipeline.util.ApiClient;
import com.transcript.pipeline.util.FileService;
import com.transcript.pipeline.util.TextProcessingUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for splitting transcript files into semantic chunks.
 * Uses heading-aware chunking or model-based detection.
 */
public class ChunkerService {

    private static final Logger logger = LoggerFactory.getLogger(ChunkerService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ApiClient apiClient;
    private final int chunkSize;
    private final int chunkOverlap;

    public ChunkerService() {
        this.apiClient = ApiClient.createClaudeClient();
        this.chunkSize = ConfigManager.getInt(ConfigManager.CHUNK_SIZE, 1500);
        this.chunkOverlap = ConfigManager.getInt(ConfigManager.CHUNK_OVERLAP, 200);
    }

    /**
     * Chunk a transcript file using semantic chunking strategy
     */
    public List<TextChunk> chunkTranscript(String filePath) throws IOException, InterruptedException {
        logger.info("Chunking transcript: {}", filePath);

        String fileContent = FileService.readTextFile(filePath);
        String fileName = new java.io.File(filePath).getName();

        // Clean text first
        fileContent = TextProcessingUtil.cleanText(fileContent);

        // Use local semantic chunking (preferred, no API call needed)
        return performLocalChunking(fileContent, fileName);
    }

    /**
     * Perform local semantic chunking without API call
     */
    private List<TextChunk> performLocalChunking(String text, String sourceFile) {
        List<TextChunk> chunks = new ArrayList<>();

        // Split using local algorithm
        List<TextProcessingUtil.TextChunkData> chunkData = TextProcessingUtil.semanticChunk(text, chunkSize, chunkOverlap);

        for (TextProcessingUtil.TextChunkData data : chunkData) {
            TextChunk chunk = new TextChunk(
                    String.valueOf(data.getChunkId()),
                    data.getTitle(),
                    data.getText(),
                    sourceFile,
                    0, // start_line
                    0  // end_line
            );
            chunks.add(chunk);
        }

        logger.info("Performed local chunking: created {} chunks", chunks.size());
        return chunks;
    }

    /**
     * Perform chunking using Claude API (optional, for improved quality)
     * Uses the chunker prompt from documentation
     */
    public List<TextChunk> chunkTranscriptWithApi(String filePath) throws IOException, InterruptedException {
        logger.info("Chunking transcript with API: {}", filePath);

        String fileContent = FileService.readTextFile(filePath);
        String fileName = new java.io.File(filePath).getName();
        fileContent = TextProcessingUtil.cleanText(fileContent);

        String systemPrompt = """
                You are a transcript chunker. Split the following file into logical topic-level chunks.
                Output a JSON array of objects with this exact format:
                [{"chunk_id": "1", "title": "Topic Name", "text": "chunk content here"}, ...]

                Rules:
                - Preserve paragraph breaks
                - If headings are present, use them as chunk titles
                - Otherwise, detect topic shifts based on content
                - Each chunk should be 1000-2000 words
                - Do NOT summarize or modify the text
                """;

        String userPrompt = "Chunk this transcript:\n\n" + fileContent;

        // Truncate if too long
        userPrompt = TextProcessingUtil.truncateToTokens(userPrompt, 100000);

        try {
            String response = apiClient.sendPromptToClaude(systemPrompt, userPrompt);
            return parseChunkResponse(response, fileName);
        } catch (IOException e) {
            logger.warn("API chunking failed, falling back to local chunking", e);
            return performLocalChunking(fileContent, fileName);
        }
    }

    /**
     * Parse Claude API response into TextChunk objects
     */
    private List<TextChunk> parseChunkResponse(String response, String sourceFile) throws IOException {
        List<TextChunk> chunks = new ArrayList<>();

        try {
            // Extract JSON array from response
            String jsonStr = extractJsonFromResponse(response);
            List<TextChunk> parsedChunks = objectMapper.readValue(
                    jsonStr,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, TextChunk.class)
            );

            for (TextChunk chunk : parsedChunks) {
                chunk.setSourceFile(sourceFile);
            }

            logger.info("Parsed {} chunks from API response", parsedChunks.size());
            return parsedChunks;
        } catch (Exception e) {
            logger.error("Failed to parse chunk response", e);
            throw new IOException("Failed to parse API response", e);
        }
    }

    /**
     * Extract JSON array from API response text
     */
    private String extractJsonFromResponse(String response) {
        int startIdx = response.indexOf('[');
        int endIdx = response.lastIndexOf(']');

        if (startIdx == -1 || endIdx == -1) {
            throw new IllegalArgumentException("No JSON array found in response");
        }

        return response.substring(startIdx, endIdx + 1);
    }

    /**
     * Save chunks to file
     */
    public void saveChunks(List<TextChunk> chunks, String outputPath) throws IOException {
        FileService.writeJsonArrayFile(outputPath, chunks);
        logger.info("Saved {} chunks to {}", chunks.size(), outputPath);
    }

    /**
     * Load chunks from file
     */
    public List<TextChunk> loadChunks(String inputPath) throws IOException {
        List<TextChunk> chunks = FileService.readJsonFileAsList(inputPath, TextChunk.class);
        logger.info("Loaded {} chunks from {}", chunks.size(), inputPath);
        return chunks;
    }

    /**
     * Get chunk statistics
     */
    public String getChunkStatistics(List<TextChunk> chunks) {
        int totalTokens = chunks.stream()
                .mapToInt(c -> TextProcessingUtil.estimateTokenCount(c.getText()))
                .sum();
        int avgTokensPerChunk = totalTokens / Math.max(chunks.size(), 1);

        return String.format(
                "Chunk Statistics: Total Chunks=%d, Total Tokens≈%d, Avg Tokens/Chunk≈%d",
                chunks.size(), totalTokens, avgTokensPerChunk
        );
    }
}
