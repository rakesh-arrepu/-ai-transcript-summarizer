package com.transcript.pipeline.util;

import com.transcript.pipeline.config.ConfigManager;
import com.transcript.pipeline.models.BatchResult;
import com.transcript.pipeline.models.ChunkSummary;
import com.transcript.pipeline.models.TextChunk;
import com.transcript.pipeline.services.ChunkerService;
import com.transcript.pipeline.services.ConsolidatorService;
import com.transcript.pipeline.services.SummarizerService;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles batch processing of multiple transcript files.
 * Processes all transcripts automatically with error recovery.
 */
public class BatchProcessor {

    private static final Logger logger = LoggerFactory.getLogger(BatchProcessor.class);

    private final ChunkerService chunkerService;
    private final SummarizerService summarizerService;
    private final ConsolidatorService consolidatorService;
    private final String outputDir;

    public BatchProcessor() {
        this.chunkerService = new ChunkerService();
        this.summarizerService = new SummarizerService();
        this.consolidatorService = new ConsolidatorService();
        this.outputDir = ConfigManager.get(ConfigManager.OUTPUT_DIR, "output");
    }

    /**
     * Process all transcripts in a directory
     */
    public BatchResult processAllTranscripts(String transcriptDir) {
        BatchResult batchResult = new BatchResult();

        ConsoleColors.printHeader("BATCH PROCESSING MODE");
        System.out.println();

        try {
            // Get all transcript files
            List<File> transcriptFiles = FileService.listFilesInDirectory(transcriptDir, ".txt");

            if (transcriptFiles.isEmpty()) {
                ConsoleColors.printError("No transcript files found in " + transcriptDir);
                ConsoleColors.printInfo("Place .txt files in the " + transcriptDir + "/ directory");
                batchResult.complete();
                return batchResult;
            }

            System.out.println(String.format("Found %s transcript files",
                ConsoleColors.colorize(String.valueOf(transcriptFiles.size()), ConsoleColors.BOLD_CYAN)));
            System.out.println();

            // Create output directories
            FileService.createDirectoryIfNotExists(outputDir + "/chunks");
            FileService.createDirectoryIfNotExists(outputDir + "/summaries");
            FileService.createDirectoryIfNotExists(outputDir + "/consolidated");
            FileService.createDirectoryIfNotExists(outputDir + "/exam_materials");

            // Process each file
            for (int i = 0; i < transcriptFiles.size(); i++) {
                File file = transcriptFiles.get(i);

                ConsoleColors.printDoubleSeparator();
                System.out.println(String.format("Processing file %s/%s: %s",
                    ConsoleColors.colorize(String.valueOf(i + 1), ConsoleColors.BOLD_GREEN),
                    ConsoleColors.colorize(String.valueOf(transcriptFiles.size()), ConsoleColors.CYAN),
                    ConsoleColors.colorize(file.getName(), ConsoleColors.BOLD_WHITE)));
                ConsoleColors.printDoubleSeparator();
                System.out.println();

                long fileStartTime = System.currentTimeMillis();
                BatchResult.FileResult fileResult = new BatchResult.FileResult(file.getName());

                try {
                    // Process single file through entire pipeline
                    processSingleFile(file, fileResult);

                    // Calculate duration and mark as success
                    long fileDuration = System.currentTimeMillis() - fileStartTime;
                    fileResult.setDurationMs(fileDuration);

                    batchResult.addSuccess(file, fileResult);

                    System.out.println();
                    ConsoleColors.printSuccess(String.format("Completed %s in %s",
                        file.getName(),
                        ConsoleColors.formatTime(fileDuration)));

                } catch (Exception e) {
                    logger.error("Failed to process file: {}", file.getName(), e);
                    batchResult.addFailure(file, e);

                    System.out.println();
                    ConsoleColors.printError(String.format("Failed to process %s: %s",
                        file.getName(), e.getMessage()));
                    ConsoleColors.printWarning("Continuing with next file...");
                }

                System.out.println();
            }

            // Mark batch as complete
            batchResult.complete();

            // Display summary
            displayBatchSummary(batchResult);

            // Save reports
            saveBatchReports(batchResult);

        } catch (Exception e) {
            logger.error("Batch processing failed", e);
            ConsoleColors.printError("Batch processing encountered an error: " + e.getMessage());
            batchResult.complete();
        }

        return batchResult;
    }

    /**
     * Process a single file through the complete pipeline
     */
    private void processSingleFile(File file, BatchResult.FileResult fileResult)
            throws IOException, InterruptedException {

        String fileName = file.getName();

        // Step 1: Chunking
        System.out.println();
        ConsoleColors.printSection("Stage 1: Chunking");
        List<TextChunk> chunks = chunkerService.chunkTranscript(file.getAbsolutePath());
        String chunkOutputPath = FileService.generateChunkOutputPath(outputDir, fileName);
        chunkerService.saveChunks(chunks, chunkOutputPath);
        fileResult.setChunksCreated(chunks.size());
        fileResult.addOutput(chunkOutputPath);
        System.out.println();

        // Step 2: Summarization
        System.out.println();
        ConsoleColors.printSection("Stage 2: Summarization");
        List<ChunkSummary> summaries = summarizerService.summarizeChunks(chunks);
        String summaryDir = FileService.generateSummaryOutputDir(outputDir, fileName);
        FileService.createDirectoryIfNotExists(summaryDir);
        summarizerService.saveSummaries(summaries, summaryDir);
        fileResult.setSummariesCreated(summaries.size());
        fileResult.addOutput(summaryDir);
        System.out.println();

        // Step 3: Consolidation
        System.out.println();
        ConsoleColors.printSection("Stage 3: Consolidation");
        String masterNotes = consolidatorService.consolidateToMasterNotes(summaries);
        String consolidatedPath = FileService.generateConsolidatedPath(outputDir, fileName);
        consolidatorService.saveMasterNotes(masterNotes, consolidatedPath);
        fileResult.addOutput(consolidatedPath);
        System.out.println();

        // Step 4: Exam Materials
        System.out.println();
        ConsoleColors.printSection("Stage 4: Exam Materials");
        String examDir = FileService.generateExamMaterialsDir(outputDir, fileName);
        FileService.createDirectoryIfNotExists(examDir);

        String flashcards = consolidatorService.generateFlashcards(masterNotes, summaries);
        consolidatorService.saveFlashcards(flashcards, examDir + "/flashcards.csv");
        fileResult.addOutput(examDir + "/flashcards.csv");

        String practiceQuestions = consolidatorService.generatePracticeQuestions(masterNotes);
        consolidatorService.savePracticeQuestions(practiceQuestions, examDir + "/practice_questions.md");
        fileResult.addOutput(examDir + "/practice_questions.md");

        String quickRevision = consolidatorService.generateQuickRevision(masterNotes);
        consolidatorService.saveQuickRevision(quickRevision, examDir + "/quick_revision.md");
        fileResult.addOutput(examDir + "/quick_revision.md");

        // Optional: Estimate cost (simplified)
        int totalTokens = TextProcessingUtil.estimateTokenCount(FileService.readTextFile(file.getAbsolutePath()));
        String summarizerModel = ConfigManager.get(ConfigManager.SUMMARIZER_MODEL, "claude");
        String consolidatorModel = ConfigManager.get(ConfigManager.CONSOLIDATOR_MODEL, "gpt");
        CostTracker.CostEstimate estimate = CostTracker.estimateTranscriptCost(
            totalTokens, summarizerModel, consolidatorModel
        );
        fileResult.setCost(estimate.totalCost);
    }

    /**
     * Display batch processing summary
     */
    private void displayBatchSummary(BatchResult batchResult) {
        System.out.println();
        System.out.println(batchResult.generateSummaryReport());
    }

    /**
     * Save batch reports to files
     */
    private void saveBatchReports(BatchResult batchResult) {
        try {
            // Save JSON report
            String jsonReportPath = outputDir + "/batch_report.json";
            FileService.writeJsonFile(jsonReportPath, batchResult);
            logger.info("Batch JSON report saved to {}", jsonReportPath);

            // Save CSV report
            String csvReportPath = outputDir + "/batch_report.csv";
            String csvContent = batchResult.generateCsvReport();
            java.nio.file.Files.write(
                java.nio.file.Paths.get(csvReportPath),
                csvContent.getBytes()
            );
            logger.info("Batch CSV report saved to {}", csvReportPath);

            System.out.println();
            ConsoleColors.printInfo("Reports saved:");
            System.out.println("  JSON: " + jsonReportPath);
            System.out.println("  CSV:  " + csvReportPath);

        } catch (IOException e) {
            logger.error("Failed to save batch reports", e);
            ConsoleColors.printWarning("Failed to save batch reports: " + e.getMessage());
        }
    }
}
