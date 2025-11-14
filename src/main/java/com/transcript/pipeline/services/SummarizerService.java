package com.transcript.pipeline.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transcript.pipeline.config.ConfigManager;
import com.transcript.pipeline.models.ChunkSummary;
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
 * Service for summarizing individual text chunks.
 * Creates structured summaries with key points, workflows, definitions, etc.
 * Supports both Claude and Gemini models.
 */
public class SummarizerService {

    private static final Logger logger = LoggerFactory.getLogger(SummarizerService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ApiClient apiClient;
    private final String modelType;

    public SummarizerService() {
        String summarizerModel = ConfigManager.get(ConfigManager.SUMMARIZER_MODEL, "claude").toLowerCase().trim();
        this.modelType = summarizerModel;

        if ("gemini".equals(summarizerModel)) {
            this.apiClient = ApiClient.createGeminiClient();
            logger.info("Using Gemini model for summarization");
        } else {
            this.apiClient = ApiClient.createClaudeClient();
            logger.info("Using Claude model for summarization");
        }
    }

    /**
     * Summarize a single chunk and return structured summary
     */
    public ChunkSummary summarizeChunk(TextChunk chunk) throws IOException, InterruptedException {
        logger.info("Summarizing chunk: {} - {} (using {})", chunk.getChunkId(), chunk.getTitle(), modelType);

        String systemPrompt = getChunkSummarizerPrompt();
        String userPrompt = String.format(
                "Chunk ID: %s\nChunk Title: %s\n\nChunk Text:\n%s",
                chunk.getChunkId(),
                chunk.getTitle(),
                chunk.getText()
        );

        // Truncate if too long for API
        userPrompt = TextProcessingUtil.truncateToTokens(userPrompt, 100000);

        try {
            String response;
            if ("gemini".equals(modelType)) {
                response = apiClient.sendPromptToGemini(systemPrompt, userPrompt);
            } else {
                response = apiClient.sendPromptToClaude(systemPrompt, userPrompt);
            }
            return parseChunkSummaryResponse(response, chunk.getChunkId());
        } catch (IOException e) {
            logger.error("Failed to summarize chunk {}", chunk.getChunkId(), e);
            // Return a minimal summary on failure
            return createDefaultSummary(chunk.getChunkId(), chunk.getTitle(), chunk.getText());
        }
    }

    /**
     * Summarize multiple chunks
     */
    public List<ChunkSummary> summarizeChunks(List<TextChunk> chunks) throws IOException, InterruptedException {
        List<ChunkSummary> summaries = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            TextChunk chunk = chunks.get(i);
            logger.info("Summarizing chunk {}/{}", i + 1, chunks.size());

            try {
                ChunkSummary summary = summarizeChunk(chunk);
                summaries.add(summary);
            } catch (Exception e) {
                logger.error("Failed to summarize chunk {}, using default summary", chunk.getChunkId(), e);
                summaries.add(createDefaultSummary(chunk.getChunkId(), chunk.getTitle(), chunk.getText()));
            }

            // Rate limiting - wait between API calls
            if (i < chunks.size() - 1) {
                Thread.sleep(1000); // 1 second between calls
            }
        }

        logger.info("Summarized {} chunks", summaries.size());
        return summaries;
    }

    /**
     * Parse Claude API response into ChunkSummary
     */
    private ChunkSummary parseChunkSummaryResponse(String response, String chunkId) throws IOException {
        try {
            // Extract JSON from response
            String jsonStr = extractJsonFromResponse(response);
            ChunkSummary summary = objectMapper.readValue(jsonStr, ChunkSummary.class);

            // Ensure chunk ID is set
            if (summary.getChunkId() == null || summary.getChunkId().isEmpty()) {
                summary.setChunkId(chunkId);
            }

            logger.debug("Parsed summary for chunk {}: {}", chunkId, summary.getConfidence());
            return summary;
        } catch (Exception e) {
            logger.warn("Failed to parse summary JSON, creating default summary for chunk {}", chunkId);
            throw new IOException("Failed to parse summary response", e);
        }
    }

    /**
     * Extract JSON object from API response text
     */
    private String extractJsonFromResponse(String response) {
        int startIdx = response.indexOf('{');
        int endIdx = response.lastIndexOf('}');

        if (startIdx == -1 || endIdx == -1) {
            throw new IllegalArgumentException("No JSON object found in response: " + response.substring(0, Math.min(200, response.length())));
        }

        return response.substring(startIdx, endIdx + 1);
    }

    /**
     * Create a default summary when API fails
     */
    private ChunkSummary createDefaultSummary(String chunkId, String title, String text) {
        logger.warn("Creating default summary for chunk {}", chunkId);

        ChunkSummary summary = new ChunkSummary(chunkId, title, text, "low");

        // Extract first 100 words as summary
        String[] words = text.split("\\s+");
        StringBuilder summaryText = new StringBuilder();
        for (int i = 0; i < Math.min(50, words.length); i++) {
            summaryText.append(words[i]).append(" ");
        }
        summary.setSummary(summaryText.toString().trim() + "...");

        // Set confidence as low since this is a fallback
        summary.setConfidence("low");

        return summary;
    }

    /**
     * Save summaries to file
     */
    public void saveSummary(ChunkSummary summary, String outputPath) throws IOException {
        FileService.writeJsonFile(outputPath, summary);
        logger.debug("Saved summary for chunk {} to {}", summary.getChunkId(), outputPath);
    }

    /**
     * Save multiple summaries to separate files
     */
    public void saveSummaries(List<ChunkSummary> summaries, String outputDir) throws IOException {
        for (ChunkSummary summary : summaries) {
            String filePath = outputDir + "/chunk_" + summary.getChunkId() + ".json";
            saveSummary(summary, filePath);
        }
        logger.info("Saved {} summaries to {}", summaries.size(), outputDir);
    }

    /**
     * Load summaries from files
     */
    public List<ChunkSummary> loadSummaries(String outputDir) throws IOException {
        List<ChunkSummary> summaries = new ArrayList<>();
        List<java.io.File> files = FileService.listFilesInDirectory(outputDir, ".json");

        for (java.io.File file : files) {
            try {
                ChunkSummary summary = FileService.readJsonFile(file.getAbsolutePath(), ChunkSummary.class);
                summaries.add(summary);
            } catch (IOException e) {
                logger.warn("Failed to load summary from {}", file.getAbsolutePath(), e);
            }
        }

        logger.info("Loaded {} summaries from {}", summaries.size(), outputDir);
        return summaries;
    }

    /**
     * Get statistics on summaries including confidence levels
     */
    public String getSummaryStatistics(List<ChunkSummary> summaries) {
        long highConfidence = summaries.stream().filter(s -> "high".equals(s.getConfidence())).count();
        long mediumConfidence = summaries.stream().filter(s -> "medium".equals(s.getConfidence())).count();
        long lowConfidence = summaries.stream().filter(s -> "low".equals(s.getConfidence())).count();

        return String.format(
                "Summary Statistics: Total=%d, High Confidence=%d, Medium=%d, Low=%d",
                summaries.size(), highConfidence, mediumConfidence, lowConfidence
        );
    }

    /**
     * Get the chunk summarizer prompt
     */
    private String getChunkSummarizerPrompt() {
        return """
                You are an expert academic summarizer. For the chunk below, produce a JSON response with these exact fields:
                {
                  "chunk_id": "string",
                  "title": "string",
                  "summary": "one-paragraph concise summary (50-80 words)",
                  "key_points": ["point1", "point2", ...],
                  "workflows": [
                    {"name": "workflow name", "steps": ["step1", "step2", ...], "notes": "optional"}
                  ],
                  "definitions": [
                    {"term": "term", "definition": "definition text"}
                  ],
                  "examples": ["example1", "example2", ...],
                  "exam_pointers": ["high-yield point 1", "high-yield point 2", ...],
                  "confidence": "high|medium|low"
                }

                Guidelines:
                - Confidence should be "high" if content is clear and complete, "medium" if partly unclear, "low" if confusing
                - Key points: 3-5 main takeaways
                - Workflows: document any step-by-step processes mentioned
                - Definitions: extract 2-4 important terms
                - Examples: list 2-3 concrete examples from the text
                - Exam pointers: 3-4 points students should memorize
                - IMPORTANT: Output only the JSON, no other text
                """;
    }
}
