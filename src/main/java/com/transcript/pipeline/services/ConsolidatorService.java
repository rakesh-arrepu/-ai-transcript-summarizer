package com.transcript.pipeline.services;

import com.transcript.pipeline.config.ConfigManager;
import com.transcript.pipeline.models.ChunkSummary;
import com.transcript.pipeline.util.ApiClient;
import com.transcript.pipeline.util.ConsoleColors;
import com.transcript.pipeline.util.FileService;
import com.transcript.pipeline.util.FlowsGenerator;
import com.transcript.pipeline.util.TextProcessingUtil;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for consolidating chunk summaries into exam-ready master notes.
 * Generates master notes, quick revision sheet, and practice questions.
 * Supports both OpenAI and Gemini models.
 */
public class ConsolidatorService {

    private static final Logger logger = LoggerFactory.getLogger(ConsolidatorService.class);

    private final ApiClient apiClient;
    private final String modelType;

    public ConsolidatorService() {
        String consolidatorModel = ConfigManager.get(ConfigManager.CONSOLIDATOR_MODEL, "gpt").toLowerCase().trim();
        this.modelType = consolidatorModel;

        if ("gemini".equals(consolidatorModel)) {
            this.apiClient = ApiClient.createGeminiClient();
            logger.info("Using Gemini model for consolidation");
        } else {
            this.apiClient = ApiClient.createOpenAIClient();
            logger.info("Using GPT model for consolidation");
        }
    }

    /**
     * Consolidate chunk summaries into master notes
     */
    public String consolidateToMasterNotes(List<ChunkSummary> summaries) throws IOException, InterruptedException {
        ConsoleColors.printSection("Consolidation Stage");
        System.out.println(String.format("Model: %s | Input: %d summaries",
            ConsoleColors.colorize(modelType.toUpperCase(), ConsoleColors.BOLD_CYAN),
            summaries.size()));
        System.out.println();

        logger.info("Consolidating {} chunk summaries into master notes (using {})", summaries.size(), modelType);

        System.out.print("Building consolidation payload... ");
        String systemPrompt = getConsolidatorPrompt();
        String userPrompt = buildConsolidatorPayload(summaries);
        ConsoleColors.printSuccess("Done");

        System.out.print("Generating master notes (this may take 2-5 minutes)... ");
        long startTime = System.currentTimeMillis();

        try {
            String response;
            if ("gemini".equals(modelType)) {
                response = apiClient.sendPromptToGemini(systemPrompt, userPrompt);
            } else {
                response = apiClient.sendPromptToOpenAI(systemPrompt, userPrompt);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println();
            ConsoleColors.printSuccess(String.format("Master notes generated in %s",
                ConsoleColors.formatTime(elapsed)));

            return response;
        } catch (IOException e) {
            System.out.println();
            ConsoleColors.printError("Consolidation failed, creating fallback master notes");
            logger.error("Failed to consolidate, creating fallback master notes", e);
            return createFallbackMasterNotes(summaries);
        }
    }

    /**
     * Generate flashcards from master notes
     */
    public String generateFlashcards(String masterNotes, List<ChunkSummary> summaries) throws IOException, InterruptedException {
        ConsoleColors.printSection("Exam Materials: Flashcards");
        System.out.print("Generating flashcards (50-100 cards)... ");

        logger.info("Generating flashcards from master notes");
        long startTime = System.currentTimeMillis();

        String systemPrompt = """
                You are an expert educator creating flashcard content.
                Create exactly 50-100 flashcards in CSV format.
                Format: "Front","Back"
                Front = question or term
                Back = concise answer (1-2 sentences max)
                Output ONLY CSV content, no explanations.""";

        String userPrompt = "Create flashcards from this content:\n\n" + masterNotes;
        userPrompt = TextProcessingUtil.truncateToTokens(userPrompt, 50000);

        try {
            String result;
            if ("gemini".equals(modelType)) {
                result = apiClient.sendPromptToGemini(systemPrompt, userPrompt);
            } else {
                result = apiClient.sendPromptToOpenAI(systemPrompt, userPrompt);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println();
            ConsoleColors.printSuccess(String.format("Flashcards generated in %s",
                ConsoleColors.formatTime(elapsed)));

            return result;
        } catch (IOException e) {
            System.out.println();
            ConsoleColors.printWarning("Flashcard generation failed, creating default flashcards");
            logger.warn("Flashcard generation failed, creating default flashcards", e);
            return createDefaultFlashcards(summaries);
        }
    }

    /**
     * Generate practice questions from master notes
     */
    public String generatePracticeQuestions(String masterNotes) throws IOException, InterruptedException {
        ConsoleColors.printSection("Exam Materials: Practice Questions");
        System.out.print("Generating practice questions (MCQ + Short Answer + Long Form)... ");

        logger.info("Generating practice questions");
        long startTime = System.currentTimeMillis();

        String systemPrompt = """
                You are an expert exam question designer.
                Generate exactly:
                - 6 multiple choice questions (with 4 options each, mark correct answer with *)
                - 6 short answer questions
                - 2 long-form questions with marking rubrics

                Format each section clearly with headers.
                Include answer keys and expected answer points.
                Output in Markdown format.""";

        String userPrompt = "Generate practice questions based on this content:\n\n" + masterNotes;
        userPrompt = TextProcessingUtil.truncateToTokens(userPrompt, 50000);

        try {
            String result;
            if ("gemini".equals(modelType)) {
                result = apiClient.sendPromptToGemini(systemPrompt, userPrompt);
            } else {
                result = apiClient.sendPromptToOpenAI(systemPrompt, userPrompt);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println();
            ConsoleColors.printSuccess(String.format("Practice questions generated in %s",
                ConsoleColors.formatTime(elapsed)));

            return result;
        } catch (IOException e) {
            System.out.println();
            ConsoleColors.printWarning("Practice question generation failed");
            logger.warn("Practice question generation failed", e);
            return createDefaultPracticeQuestions();
        }
    }

    /**
     * Generate quick revision sheet
     */
    public String generateQuickRevision(String masterNotes) throws IOException, InterruptedException {
        ConsoleColors.printSection("Exam Materials: Quick Revision");
        System.out.print("Generating quick revision sheet (1-page summary)... ");

        logger.info("Generating quick revision sheet");
        long startTime = System.currentTimeMillis();

        String systemPrompt = """
                You are an expert study guide creator.
                Create a concise 1-page quick revision guide.
                Include:
                - 10-15 bullet points of must-know concepts
                - Key definitions
                - Important formulas or processes
                - High-yield exam tips

                Make it scannable and easy to memorize.
                Output in Markdown bullet-point format.""";

        String userPrompt = "Create a quick revision sheet from this content:\n\n" + masterNotes;
        userPrompt = TextProcessingUtil.truncateToTokens(userPrompt, 50000);

        try {
            String result;
            if ("gemini".equals(modelType)) {
                result = apiClient.sendPromptToGemini(systemPrompt, userPrompt);
            } else {
                result = apiClient.sendPromptToOpenAI(systemPrompt, userPrompt);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println();
            ConsoleColors.printSuccess(String.format("Quick revision generated in %s",
                ConsoleColors.formatTime(elapsed)));

            return result;
        } catch (IOException e) {
            System.out.println();
            ConsoleColors.printWarning("Quick revision generation failed");

            logger.warn("Quick revision generation failed", e);
            return createDefaultQuickRevision();
        }
    }

    /**
     * Save master notes to file
     */
    public void saveMasterNotes(String content, String outputPath) throws IOException {
        FileService.writeTextFile(outputPath, content);
        logger.info("Saved master notes to {}", outputPath);
    }

    /**
     * Save flashcards to file
     */
    public void saveFlashcards(String content, String outputPath) throws IOException {
        FileService.writeTextFile(outputPath, content);
        logger.info("Saved flashcards to {}", outputPath);
    }

    /**
     * Save practice questions to file
     */
    public void savePracticeQuestions(String content, String outputPath) throws IOException {
        FileService.writeTextFile(outputPath, content);
        logger.info("Saved practice questions to {}", outputPath);
    }

    /**
     * Save quick revision sheet to file
     */
    public void saveQuickRevision(String content, String outputPath) throws IOException {
        FileService.writeTextFile(outputPath, content);
        logger.info("Saved quick revision sheet to {}", outputPath);
    }

    /**
     * Build payload for consolidator API call
     */
    private String buildConsolidatorPayload(List<ChunkSummary> summaries) {
        StringBuilder payload = new StringBuilder();
        payload.append("CHUNK SUMMARIES:\n\n");

        for (ChunkSummary summary : summaries) {
            payload.append("--- Chunk ").append(summary.getChunkId()).append(": ").append(summary.getTitle()).append(" ---\n");
            payload.append("Summary: ").append(summary.getSummary()).append("\n");
            payload.append("Confidence: ").append(summary.getConfidence()).append("\n");

            if (summary.getKeyPoints() != null && !summary.getKeyPoints().isEmpty()) {
                payload.append("Key Points: ").append(String.join(", ", summary.getKeyPoints())).append("\n");
            }

            if (summary.getDefinitions() != null && !summary.getDefinitions().isEmpty()) {
                payload.append("Definitions:\n");
                for (ChunkSummary.Definition def : summary.getDefinitions()) {
                    payload.append("  - ").append(def.getTerm()).append(": ").append(def.getDefinition()).append("\n");
                }
            }

            payload.append("\n");
        }

        payload.append("\nPlease consolidate these summaries into a comprehensive Markdown document with three sections:\n");
        payload.append("1. Master Notes (detailed topic-wise breakdown)\n");
        payload.append("2. Quick Revision (1-page summary)\n");
        payload.append("3. Practice Questions\n");

        return payload.toString();
    }

    /**
     * Create fallback master notes when API fails
     */
    private String createFallbackMasterNotes(List<ChunkSummary> summaries) {
        StringBuilder notes = new StringBuilder();
        notes.append("# Master Notes\n\n");

        for (ChunkSummary summary : summaries) {
            notes.append("## ").append(summary.getTitle()).append("\n\n");
            notes.append(summary.getSummary()).append("\n\n");

            if (summary.getKeyPoints() != null && !summary.getKeyPoints().isEmpty()) {
                notes.append("### Key Points\n");
                for (String point : summary.getKeyPoints()) {
                    notes.append("- ").append(point).append("\n");
                }
                notes.append("\n");
            }

            if (summary.getDefinitions() != null && !summary.getDefinitions().isEmpty()) {
                notes.append("### Definitions\n");
                for (ChunkSummary.Definition def : summary.getDefinitions()) {
                    notes.append("- **").append(def.getTerm()).append("**: ").append(def.getDefinition()).append("\n");
                }
                notes.append("\n");
            }
        }

        return notes.toString();
    }

    /**
     * Create default flashcards from summaries
     */
    private String createDefaultFlashcards(List<ChunkSummary> summaries) {
        StringBuilder csv = new StringBuilder();
        csv.append("\"Front\",\"Back\"\n");

        for (ChunkSummary summary : summaries) {
            // Add title as flashcard
            csv.append("\"What is ").append(summary.getTitle()).append("?\",\"")
                    .append(summary.getSummary().replace("\"", "\\\"")).append("\"\n");

            // Add key points as flashcards
            if (summary.getKeyPoints() != null) {
                for (String point : summary.getKeyPoints()) {
                    csv.append("\"Key point from ").append(summary.getTitle()).append("\",\"")
                            .append(point.replace("\"", "\\\"")).append("\"\n");
                }
            }

            // Add definitions as flashcards
            if (summary.getDefinitions() != null) {
                for (ChunkSummary.Definition def : summary.getDefinitions()) {
                    csv.append("\"Define: ").append(def.getTerm()).append("\",\"")
                            .append(def.getDefinition().replace("\"", "\\\"")).append("\"\n");
                }
            }
        }

        return csv.toString();
    }

    /**
     * Create default practice questions
     */
    private String createDefaultPracticeQuestions() {
        return """
                # Practice Questions

                ## Multiple Choice Questions

                ### Question 1
                Which of the following is correct?
                a) Option A
                b) Option B
                c) Option C
                d) Option D

                **Answer**: To be filled in

                ### Question 2-6
                [Additional MCQ questions...]

                ## Short Answer Questions

                ### Question 1
                Short answer question here?

                **Expected Answer**: [Expected response]

                ### Question 2-6
                [Additional short answer questions...]

                ## Long Form Questions

                ### Question 1
                [Long-form question with marking rubric]

                **Expected Answer and Marking Rubric**:
                - Point 1: [X marks]
                - Point 2: [X marks]

                ### Question 2
                [Second long-form question]

                """;
    }

    /**
     * Create default quick revision sheet
     */
    private String createDefaultQuickRevision() {
        return """
                # Quick Revision Sheet

                ## Must-Know Concepts
                - Concept 1: Brief explanation
                - Concept 2: Brief explanation
                - Concept 3: Brief explanation

                ## Key Definitions
                - Term 1: Definition
                - Term 2: Definition

                ## High-Yield Exam Tips
                - Focus on frequently tested topics
                - Understand not just memorize
                - Practice past exam questions

                """;
    }

    /**
     * Generate flows and diagrams from summaries (optional feature).
     * Creates visual representations of workflows and pipeline execution.
     */
    public void generateAndSaveFlows(List<ChunkSummary> summaries, String outputBaseDir) {
        try {
            logger.info("Generating flows from {} summaries", summaries.size());
            FlowsService.generateFlows(summaries, outputBaseDir);
            logger.info("✓ Flows generation complete");
        } catch (Exception e) {
            logger.warn("Error generating flows", e);
        }
    }

    /**
     * Get the consolidator system prompt
     */
    private String getConsolidatorPrompt() {
        return """
                You are an expert educational content creator specializing in exam preparation.
                Your task is to consolidate lecture chunk summaries into comprehensive, exam-ready study materials.

                Create THREE sections in Markdown format:

                ## 1. Master Notes
                - Organize by topic/theme from the chunks
                - Include all definitions and key concepts
                - Add workflows as numbered steps
                - Include concrete examples
                - Cross-reference related topics
                - Mark sections with LOW confidence as "⚠️ REVIEW THIS"

                ## 2. Quick Revision (1 page maximum)
                - 10-15 bullet points of must-remember facts
                - High-yield exam concepts
                - Concise definitions
                - Memory aids or mnemonics if helpful

                ## 3. Practice Questions
                - 6 multiple choice (mark correct answer with *)
                - 6 short answer
                - 2 long form with marking rubrics
                - Include answer keys

                Format output as clear Markdown with headers.
                Ensure high-confidence items get priority in Master Notes.
                """;
    }
}
