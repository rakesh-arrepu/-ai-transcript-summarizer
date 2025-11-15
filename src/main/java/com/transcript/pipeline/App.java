package com.transcript.pipeline;

import com.transcript.pipeline.config.ConfigManager;
import com.transcript.pipeline.models.BatchResult;
import com.transcript.pipeline.models.ChunkSummary;
import com.transcript.pipeline.models.PipelineState;
import com.transcript.pipeline.models.TextChunk;
import com.transcript.pipeline.services.ChunkerService;
import com.transcript.pipeline.services.ConsolidatorService;
import com.transcript.pipeline.services.FlowsService;
import com.transcript.pipeline.services.SummarizerService;
import com.transcript.pipeline.util.BatchProcessor;
import com.transcript.pipeline.util.ConsoleColors;
import com.transcript.pipeline.util.CostTracker;
import com.transcript.pipeline.util.FileService;
import com.transcript.pipeline.util.StateManager;
import com.transcript.pipeline.util.TextProcessingUtil;
import com.transcript.pipeline.util.ValidationResult;
import com.transcript.pipeline.util.ValidationService;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main CLI application for the Transcript to Exam Notes Pipeline.
 * Provides interactive and command-line interfaces for running the pipeline.
 */
public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private static final String VERSION = "1.0.0";

    private final ChunkerService chunkerService;
    private final SummarizerService summarizerService;
    private final ConsolidatorService consolidatorService;

    public App() {
        this.chunkerService = new ChunkerService();
        this.summarizerService = new SummarizerService();
        this.consolidatorService = new ConsolidatorService();
    }

    /**
     * Main entry point
     */
    public static void main(String[] args) {
        try {
            // Display banner
            ConsoleColors.printHeader("Transcript → Exam Notes Pipeline v" + VERSION);

            // Run startup validation
            System.out.println();
            ConsoleColors.printSection("Startup Validation");
            System.out.println();

            // Validate configuration
            System.out.print("Checking configuration... ");
            ValidationResult configCheck = ValidationService.validatePipelineConfiguration();
            System.out.println();
            configCheck.print();

            if (configCheck.isError()) {
                System.out.println();
                ConsoleColors.printError("Configuration validation failed. Cannot start application.");
                System.exit(1);
            }

            System.out.println();
            ConfigManager.printSummary();

            App app = new App();

            // Check for existing pipeline state (Phase 1.3 - Resume Capability)
            if (StateManager.stateFileExists() && args.length == 0) {
                PipelineState savedState = StateManager.loadState();
                StateManager.ResumeChoice choice = StateManager.promptResumeChoice(savedState);

                switch (choice) {
                    case RESUME:
                        ConsoleColors.printSuccess("Resuming from saved state...");
                        app.resumePipeline(savedState);
                        return;
                    case START_NEW:
                        ConsoleColors.printWarning("Starting new pipeline (saved state will be discarded)");
                        StateManager.deleteState();
                        break;
                    case CANCEL:
                        ConsoleColors.printInfo("Pipeline cancelled");
                        return;
                }
            }

            if (args.length == 0) {
                app.runInteractive();
            } else {
                app.runCommand(args);
            }
        } catch (Exception e) {
            logger.error("Fatal error", e);
            System.err.println("\n❌ Fatal error: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Run interactive mode
     */
    private void runInteractive() throws Exception {
        System.out.println("\n");
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  Transcript → Exam Notes Pipeline v" + VERSION + "                   ║");
        System.out.println("║  Convert lectures into study materials automatically       ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n📋 MAIN MENU");
            System.out.println("1. Run complete pipeline (chunk → summarize → consolidate)");
            System.out.println("2. Chunk transcripts only");
            System.out.println("3. Summarize chunks only");
            System.out.println("4. Consolidate to master notes");
            System.out.println("5. Generate exam materials (flashcards, practice questions)");
            System.out.println("6. Generate flows and diagrams (optional visualization)");
            System.out.println("7. Process all transcripts (batch mode)");
            System.out.println("8. View pipeline status");
            System.out.println("9. Settings");
            System.out.println("0. Exit");
            System.out.print("\n👉 Choose an option (0-9): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    runCompletePipeline(scanner);
                    break;
                case "2":
                    runChunkingOnly(scanner);
                    break;
                case "3":
                    runSummarizationOnly(scanner);
                    break;
                case "4":
                    runConsolidationOnly(scanner);
                    break;
                case "5":
                    runExamMaterials(scanner);
                    break;
                case "6":
                    generateFlows(scanner);
                    break;
                case "7":
                    runBatchProcessing(scanner);
                    break;
                case "8":
                    viewPipelineStatus();
                    break;
                case "9":
                    displaySettings();
                    break;
                case "0":
                    running = false;
                    System.out.println("\n✅ Goodbye!");
                    break;
                default:
                    System.out.println("❌ Invalid option. Please try again.");
            }
        }

        scanner.close();
    }

    /**
     * Run complete pipeline
     */
    private void runCompletePipeline(Scanner scanner) {
        try {
            System.out.print("\n📁 Enter transcript directory (default: 'transcripts'): ");
            String transcriptDir = scanner.nextLine().trim();
            if (transcriptDir.isEmpty()) {
                transcriptDir = "transcripts";
            }

            String outputDir = ConfigManager.get(ConfigManager.OUTPUT_DIR);
            String multiFileMode = ConfigManager.get(ConfigManager.MULTI_FILE_MODE, "separate").toLowerCase();

            // Get list of transcript files
            List<File> transcriptFiles = FileService.listFilesInDirectory(transcriptDir, ".txt");
            if (transcriptFiles.isEmpty()) {
                ConsoleColors.printError("No transcript files found in " + transcriptDir);
                ConsoleColors.printInfo("Place .txt transcript files in the " + transcriptDir + "/ directory");
                return;
            }

            System.out.println("\n🔍 Found " + transcriptFiles.size() + " transcript file(s)");
            System.out.println("⚙️  Multi-File Mode: " + multiFileMode.toUpperCase());
            System.out.println();

            // Validate first transcript file (as representative sample)
            List<ValidationResult> validationResults = ValidationService.runPreFlightChecks(
                transcriptFiles.get(0).getAbsolutePath()
            );

            if (!ValidationService.canProceed(validationResults)) {
                return;
            }

            // Show cost estimate
            System.out.println();
            int totalTokens = 0;
            for (File file : transcriptFiles) {
                try {
                    String content = FileService.readTextFile(file.getAbsolutePath());
                    totalTokens += TextProcessingUtil.estimateTokenCount(content);
                } catch (Exception e) {
                    logger.warn("Failed to estimate tokens for {}", file.getName(), e);
                }
            }

            String summarizerModel = ConfigManager.get(ConfigManager.SUMMARIZER_MODEL, "claude");
            String consolidatorModel = ConfigManager.get(ConfigManager.CONSOLIDATOR_MODEL, "gpt");

            CostTracker.CostEstimate estimate = CostTracker.estimateTranscriptCost(
                totalTokens, summarizerModel, consolidatorModel
            );

            System.out.println(estimate.formatEstimate());

            // Confirm before proceeding
            System.out.print("Continue with pipeline execution? (y/n): ");
            String confirm = scanner.nextLine().trim().toLowerCase();
            if (!confirm.equals("y") && !confirm.equals("yes")) {
                ConsoleColors.printWarning("Pipeline cancelled by user");
                return;
            }

            // Create output directories
            FileService.createDirectoryIfNotExists(outputDir + "/chunks");
            FileService.createDirectoryIfNotExists(outputDir + "/summaries");
            FileService.createDirectoryIfNotExists(outputDir + "/consolidated");
            FileService.createDirectoryIfNotExists(outputDir + "/exam_materials");

            System.out.println();
            ConsoleColors.printDoubleSeparator();
            ConsoleColors.printHeader("STARTING PIPELINE EXECUTION");

            long pipelineStartTime = System.currentTimeMillis();

            if ("separate".equals(multiFileMode)) {
                // Process each file separately
                runPipelineInSeparateMode(transcriptFiles, outputDir);
            } else {
                // Process all files combined
                runPipelineInCombinedMode(transcriptFiles, outputDir);
            }

            long pipelineTotalTime = System.currentTimeMillis() - pipelineStartTime;

            System.out.println();
            ConsoleColors.printDoubleSeparator();
            ConsoleColors.printSuccess("PIPELINE COMPLETE!");
            System.out.println();
            System.out.println("Total execution time: " + ConsoleColors.formatTime(pipelineTotalTime));
            System.out.println("Files processed: " + transcriptFiles.size());
            System.out.println("Output directory: " + new File(outputDir).getAbsolutePath());
            ConsoleColors.printDoubleSeparator();

        } catch (Exception e) {
            System.out.println();
            ConsoleColors.printError("Pipeline error: " + e.getMessage());
            logger.error("Pipeline error", e);
        }
    }

    /**
     * Run pipeline in SEPARATE mode - each file gets its own output
     */
    private void runPipelineInSeparateMode(List<File> transcriptFiles, String outputDir) {
        System.out.println("\n📋 Processing each file separately with individual outputs\n");

        // Initialize pipeline state for resume capability
        PipelineState pipelineState = new PipelineState();
        pipelineState.setOverallStatus("in_progress");

        for (int i = 0; i < transcriptFiles.size(); i++) {
            File file = transcriptFiles.get(i);
            System.out.println("\n" + "=".repeat(70));
            System.out.println("📄 FILE " + (i + 1) + "/" + transcriptFiles.size() + ": " + file.getName());
            System.out.println("=".repeat(70));

            // Create lesson state
            PipelineState.LessonState lessonState = new PipelineState.LessonState(file.getName());
            pipelineState.addLesson(file.getName(), lessonState);

            try {
                // Step 1: Chunking
                System.out.println("\n⏳ STEP 1: CHUNKING");
                lessonState.setChunkingStatus(PipelineState.StageStatus.IN_PROGRESS);
                StateManager.saveState(pipelineState);

                List<TextChunk> chunks = chunkerService.chunkTranscript(file.getAbsolutePath());
                String chunkOutputPath = FileService.generateChunkOutputPath(outputDir, file.getName());
                chunkerService.saveChunks(chunks, chunkOutputPath);
                System.out.println("✅ Created " + chunks.size() + " chunks");

                lessonState.setChunksPath(chunkOutputPath);
                lessonState.setChunkingStatus(PipelineState.StageStatus.COMPLETED);
                StateManager.saveState(pipelineState);

                // Step 2: Summarization
                System.out.println("\n⏳ STEP 2: SUMMARIZING");
                lessonState.setSummarizationStatus(PipelineState.StageStatus.IN_PROGRESS);
                StateManager.saveState(pipelineState);

                List<ChunkSummary> summaries = summarizerService.summarizeChunks(chunks);
                String summaryDir = FileService.generateSummaryOutputDir(outputDir, file.getName());
                FileService.createDirectoryIfNotExists(summaryDir);
                summarizerService.saveSummaries(summaries, summaryDir);
                System.out.println("✅ " + summarizerService.getSummaryStatistics(summaries));

                lessonState.setSummariesPath(summaryDir);
                lessonState.setSummarizationStatus(PipelineState.StageStatus.COMPLETED);
                StateManager.saveState(pipelineState);

                // Step 3: Consolidation
                System.out.println("\n⏳ STEP 3: CONSOLIDATING");
                lessonState.setConsolidationStatus(PipelineState.StageStatus.IN_PROGRESS);
                StateManager.saveState(pipelineState);

                String masterNotes = consolidatorService.consolidateToMasterNotes(summaries);
                String consolidatedPath = FileService.generateConsolidatedPath(outputDir, file.getName());
                consolidatorService.saveMasterNotes(masterNotes, consolidatedPath);
                System.out.println("✅ Master notes created");

                lessonState.setMasterNotesPath(consolidatedPath);
                lessonState.setConsolidationStatus(PipelineState.StageStatus.COMPLETED);
                StateManager.saveState(pipelineState);

                // Step 4: Exam Materials
                System.out.println("\n⏳ STEP 4: GENERATING EXAM MATERIALS");
                lessonState.setExamMaterialsStatus(PipelineState.StageStatus.IN_PROGRESS);
                StateManager.saveState(pipelineState);

                String examDir = FileService.generateExamMaterialsDir(outputDir, file.getName());
                FileService.createDirectoryIfNotExists(examDir);

                String flashcards = consolidatorService.generateFlashcards(masterNotes, summaries);
                consolidatorService.saveFlashcards(flashcards, examDir + "/flashcards.csv");
                System.out.println("✅ Flashcards generated");

                String practiceQuestions = consolidatorService.generatePracticeQuestions(masterNotes);
                consolidatorService.savePracticeQuestions(practiceQuestions, examDir + "/practice_questions.md");
                System.out.println("✅ Practice questions generated");

                String quickRevision = consolidatorService.generateQuickRevision(masterNotes);
                consolidatorService.saveQuickRevision(quickRevision, examDir + "/quick_revision.md");
                System.out.println("✅ Quick revision sheet generated");

                lessonState.setExamMaterialsPath(examDir);
                lessonState.setExamMaterialsStatus(PipelineState.StageStatus.COMPLETED);
                StateManager.saveState(pipelineState);

                // Optional: Generate flows and diagrams
                System.out.println("\n⏳ OPTIONAL: GENERATING FLOWS & DIAGRAMS");
                consolidatorService.generateAndSaveFlows(summaries, outputDir);
                System.out.println("✅ Flows diagrams generated");

                System.out.println("\n✨ FILE COMPLETE: " + file.getName());
                System.out.println("📁 Master notes: " + consolidatedPath);
                System.out.println("📁 Exam materials: " + examDir + "/");
                System.out.println("📊 Flows diagrams: " + outputDir + "/flows/");

            } catch (Exception e) {
                System.out.println("❌ Error processing " + file.getName() + ": " + e.getMessage());
                logger.error("Error processing file: " + file.getName(), e);

                // Mark stage as failed
                if (lessonState.getChunkingStatus() == PipelineState.StageStatus.IN_PROGRESS) {
                    lessonState.setChunkingStatus(PipelineState.StageStatus.FAILED);
                } else if (lessonState.getSummarizationStatus() == PipelineState.StageStatus.IN_PROGRESS) {
                    lessonState.setSummarizationStatus(PipelineState.StageStatus.FAILED);
                } else if (lessonState.getConsolidationStatus() == PipelineState.StageStatus.IN_PROGRESS) {
                    lessonState.setConsolidationStatus(PipelineState.StageStatus.FAILED);
                } else if (lessonState.getExamMaterialsStatus() == PipelineState.StageStatus.IN_PROGRESS) {
                    lessonState.setExamMaterialsStatus(PipelineState.StageStatus.FAILED);
                }
                lessonState.setErrorMessage(e.getMessage());
                StateManager.saveState(pipelineState);
            }
        }

        // All files processed - delete state
        pipelineState.setOverallStatus("completed");
        StateManager.deleteState();
        logger.info("Pipeline state cleared after successful completion");

        System.out.println("\n\n" + "=".repeat(70));
        System.out.println("✨ ALL FILES PROCESSED!");
        System.out.println("=".repeat(70));
        System.out.println("📁 Output directory: " + outputDir + "/");
        System.out.println("📝 Master notes: " + outputDir + "/consolidated/");
        System.out.println("📚 Exam materials: " + outputDir + "/exam_materials/");
    }

    /**
     * Run pipeline in COMBINED mode - all files merged into one output
     */
    private void runPipelineInCombinedMode(List<File> transcriptFiles, String outputDir) {
        System.out.println("\n📋 Processing all files combined into single output\n");

        try {
            // Step 1: Chunking all files
            System.out.println("\n⏳ STEP 1: CHUNKING ALL TRANSCRIPTS");
            System.out.println("═".repeat(60));

            java.util.List<TextChunk> allChunks = new java.util.ArrayList<>();
            int totalChunks = 0;

            for (File file : transcriptFiles) {
                System.out.println("\n📄 Processing: " + file.getName());
                try {
                    List<TextChunk> chunks = chunkerService.chunkTranscript(file.getAbsolutePath());
                    String outputPath = FileService.generateChunkOutputPath(outputDir, file.getName());
                    chunkerService.saveChunks(chunks, outputPath);
                    System.out.println("✅ Created " + chunks.size() + " chunks");

                    // Make chunk IDs unique by prefixing with filename
                    for (TextChunk chunk : chunks) {
                        String uniqueId = FileService.sanitizeFileName(file.getName()) + "_" + chunk.getChunkId();
                        TextChunk uniqueChunk = new TextChunk(
                            uniqueId,
                            chunk.getTitle(),
                            chunk.getText(),
                            chunk.getSourceFile(),
                            chunk.getStartLine(),
                            chunk.getEndLine()
                        );
                        allChunks.add(uniqueChunk);
                    }
                    totalChunks += chunks.size();
                } catch (Exception e) {
                    System.out.println("❌ Error chunking " + file.getName() + ": " + e.getMessage());
                }
            }

            if (allChunks.isEmpty()) {
                System.out.println("❌ No chunks created. Pipeline aborted.");
                return;
            }

            System.out.println("\n✅ Total chunks from all files: " + totalChunks);

            // Step 2: Summarization
            System.out.println("\n\n⏳ STEP 2: SUMMARIZING ALL CHUNKS");
            System.out.println("═".repeat(60));

            List<ChunkSummary> summaries = summarizerService.summarizeChunks(allChunks);
            summarizerService.saveSummaries(summaries, outputDir + "/summaries");
            System.out.println("✅ " + summarizerService.getSummaryStatistics(summaries));

            // Step 3: Consolidation
            System.out.println("\n\n⏳ STEP 3: CONSOLIDATING TO MASTER NOTES");
            System.out.println("═".repeat(60));

            String masterNotes = consolidatorService.consolidateToMasterNotes(summaries);
            consolidatorService.saveMasterNotes(masterNotes, outputDir + "/consolidated/master_notes.md");
            System.out.println("✅ Master notes created (combined from " + transcriptFiles.size() + " files)");

            // Step 4: Generate exam materials
            System.out.println("\n\n⏳ STEP 4: GENERATING EXAM MATERIALS");
            System.out.println("═".repeat(60));

            String flashcards = consolidatorService.generateFlashcards(masterNotes, summaries);
            consolidatorService.saveFlashcards(flashcards, outputDir + "/exam_materials/flashcards.csv");
            System.out.println("✅ Flashcards generated");

            String practiceQuestions = consolidatorService.generatePracticeQuestions(masterNotes);
            consolidatorService.savePracticeQuestions(practiceQuestions, outputDir + "/exam_materials/practice_questions.md");
            System.out.println("✅ Practice questions generated");

            String quickRevision = consolidatorService.generateQuickRevision(masterNotes);
            consolidatorService.saveQuickRevision(quickRevision, outputDir + "/exam_materials/quick_revision.md");
            System.out.println("✅ Quick revision sheet generated");

            // Optional: Generate flows and diagrams
            System.out.println("\n\n⏳ OPTIONAL: GENERATING FLOWS & DIAGRAMS");
            System.out.println("═".repeat(60));
            consolidatorService.generateAndSaveFlows(summaries, outputDir);
            System.out.println("✅ Flows diagrams generated");

            System.out.println("\n\n✨ PIPELINE COMPLETE!");
            System.out.println("═".repeat(60));
            System.out.println("📁 Output directory: " + outputDir + "/");
            System.out.println("📝 Master notes: " + outputDir + "/consolidated/master_notes.md");
            System.out.println("📚 Quick revision: " + outputDir + "/exam_materials/quick_revision.md");
            System.out.println("🎯 Practice questions: " + outputDir + "/exam_materials/practice_questions.md");
            System.out.println("🎓 Flashcards: " + outputDir + "/exam_materials/flashcards.csv");
            System.out.println("📊 Flows diagrams: " + outputDir + "/flows/");

        } catch (Exception e) {
            System.out.println("❌ Error in combined pipeline: " + e.getMessage());
            logger.error("Combined pipeline error", e);
        }
    }

    /**
     * Run chunking only
     */
    private void runChunkingOnly(Scanner scanner) {
        try {
            System.out.print("\n📄 Enter transcript file path: ");
            String filePath = scanner.nextLine().trim();

            if (!FileService.fileExists(filePath)) {
                System.out.println("❌ File not found: " + filePath);
                return;
            }

            System.out.println("\n⏳ Chunking transcript...");
            List<TextChunk> chunks = chunkerService.chunkTranscript(filePath);

            String outputDir = ConfigManager.get(ConfigManager.OUTPUT_DIR);
            FileService.createDirectoryIfNotExists(outputDir + "/chunks");
            String outputPath = FileService.generateChunkOutputPath(outputDir, new File(filePath).getName());
            chunkerService.saveChunks(chunks, outputPath);

            System.out.println("✅ " + chunkerService.getChunkStatistics(chunks));
            System.out.println("💾 Saved to: " + outputPath);
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            logger.error("Chunking error", e);
        }
    }

    /**
     * Run summarization only
     */
    private void runSummarizationOnly(Scanner scanner) {
        try {
            System.out.print("\n📁 Enter chunks JSON file path: ");
            String chunksFile = scanner.nextLine().trim();

            if (!FileService.fileExists(chunksFile)) {
                System.out.println("❌ File not found: " + chunksFile);
                return;
            }

            System.out.println("\n⏳ Loading chunks...");
            List<TextChunk> chunks = chunkerService.loadChunks(chunksFile);
            System.out.println("✅ Loaded " + chunks.size() + " chunks");

            System.out.println("⏳ Summarizing chunks...");
            List<ChunkSummary> summaries = summarizerService.summarizeChunks(chunks);

            String outputDir = ConfigManager.get(ConfigManager.OUTPUT_DIR);
            FileService.createDirectoryIfNotExists(outputDir + "/summaries");
            summarizerService.saveSummaries(summaries, outputDir + "/summaries");

            System.out.println("✅ " + summarizerService.getSummaryStatistics(summaries));
            System.out.println("💾 Saved to: " + outputDir + "/summaries/");
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            logger.error("Summarization error", e);
        }
    }

    /**
     * Run consolidation only
     */
    private void runConsolidationOnly(Scanner scanner) {
        try {
            System.out.print("\n📁 Enter summaries directory: ");
            String summariesDir = scanner.nextLine().trim();

            System.out.println("\n⏳ Loading summaries...");
            List<ChunkSummary> summaries = summarizerService.loadSummaries(summariesDir);
            System.out.println("✅ Loaded " + summaries.size() + " summaries");

            System.out.println("⏳ Consolidating to master notes...");
            String masterNotes = consolidatorService.consolidateToMasterNotes(summaries);

            String outputDir = ConfigManager.get(ConfigManager.OUTPUT_DIR);
            FileService.createDirectoryIfNotExists(outputDir + "/consolidated");
            consolidatorService.saveMasterNotes(masterNotes, outputDir + "/consolidated/master_notes.md");

            System.out.println("✅ Master notes created");
            System.out.println("💾 Saved to: " + outputDir + "/consolidated/master_notes.md");
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            logger.error("Consolidation error", e);
        }
    }

    /**
     * Generate exam materials
     */
    private void runExamMaterials(Scanner scanner) {
        try {
            System.out.print("\n📄 Enter master notes file path: ");
            String notesFile = scanner.nextLine().trim();

            if (!FileService.fileExists(notesFile)) {
                System.out.println("❌ File not found: " + notesFile);
                return;
            }

            String masterNotes = FileService.readTextFile(notesFile);
            String outputDir = ConfigManager.get(ConfigManager.OUTPUT_DIR);
            FileService.createDirectoryIfNotExists(outputDir + "/exam_materials");

            System.out.println("\n⏳ Generating flashcards...");
            String flashcards = consolidatorService.generateFlashcards(masterNotes, null);
            consolidatorService.saveFlashcards(flashcards, outputDir + "/exam_materials/flashcards.csv");
            System.out.println("✅ Flashcards created");

            System.out.println("⏳ Generating practice questions...");
            String practiceQuestions = consolidatorService.generatePracticeQuestions(masterNotes);
            consolidatorService.savePracticeQuestions(practiceQuestions, outputDir + "/exam_materials/practice_questions.md");
            System.out.println("✅ Practice questions created");

            System.out.println("⏳ Generating quick revision...");
            String quickRevision = consolidatorService.generateQuickRevision(masterNotes);
            consolidatorService.saveQuickRevision(quickRevision, outputDir + "/exam_materials/quick_revision.md");
            System.out.println("✅ Quick revision created");

            System.out.println("\n✨ All exam materials generated!");
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            logger.error("Exam materials error", e);
        }
    }

    /**
     * View pipeline status
     */
    private void viewPipelineStatus() {
        try {
            String outputDir = ConfigManager.get(ConfigManager.OUTPUT_DIR);
            System.out.println("\n📊 PIPELINE STATUS");
            System.out.println("═".repeat(60));

            File chunksDir = new File(outputDir + "/chunks");
            File summariesDir = new File(outputDir + "/summaries");
            File consolidatedDir = new File(outputDir + "/consolidated");
            File examMaterialsDir = new File(outputDir + "/exam_materials");

            int chunksCount = chunksDir.listFiles((dir, name) -> name.endsWith(".json")) != null ?
                    chunksDir.listFiles((dir, name) -> name.endsWith(".json")).length : 0;
            int summariesCount = summariesDir.listFiles((dir, name) -> name.endsWith(".json")) != null ?
                    summariesDir.listFiles((dir, name) -> name.endsWith(".json")).length : 0;
            int consolidatedCount = consolidatedDir.listFiles((dir, name) -> name.endsWith(".md")) != null ?
                    consolidatedDir.listFiles((dir, name) -> name.endsWith(".md")).length : 0;
            int examCount = examMaterialsDir.listFiles() != null ?
                    examMaterialsDir.listFiles().length : 0;

            System.out.println("📦 Chunk files: " + chunksCount);
            System.out.println("📝 Summary files: " + summariesCount);
            System.out.println("📚 Consolidated documents: " + consolidatedCount);
            System.out.println("🎓 Exam materials: " + examCount);
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
    }

    /**
     * Generate flows and diagrams from existing summaries
     */
    private void generateFlows(Scanner scanner) {
        try {
            System.out.print("\n📁 Enter summaries directory: ");
            String summariesDir = scanner.nextLine().trim();

            System.out.println("\n⏳ Loading summaries...");
            List<ChunkSummary> summaries = summarizerService.loadSummaries(summariesDir);
            System.out.println("✅ Loaded " + summaries.size() + " summaries");

            String outputDir = ConfigManager.get(ConfigManager.OUTPUT_DIR);
            FileService.createDirectoryIfNotExists(outputDir + "/flows");

            System.out.println("\n⏳ Generating flows and diagrams...");
            System.out.println("📊 Creating workflow visualizations...");
            System.out.println("📈 Generating flowcharts in Mermaid format...");
            System.out.println("🎨 Creating ASCII diagrams...");

            consolidatorService.generateAndSaveFlows(summaries, outputDir);

            System.out.println("\n✨ FLOWS GENERATION COMPLETE!");
            System.out.println("═".repeat(60));
            System.out.println("📊 Flows directory: " + outputDir + "/flows/");
            System.out.println("📄 Main report: " + outputDir + "/flows/flows_report.md");
            System.out.println("📋 Pipeline diagram: " + outputDir + "/flows/pipeline_diagram.md");
            System.out.println("🔍 Individual workflows: " + outputDir + "/flows/workflow_*.md");
            System.out.println("\nTip: Open the flows_report.md file to view all diagrams!");

        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            logger.error("Flows generation error", e);
        }
    }

    /**
     * Display settings
     */
    private void displaySettings() {
        System.out.println("\n⚙️  SETTINGS");
        System.out.println("═".repeat(60));
        ConfigManager.printSummary();
    }

    /**
     * Display help
     */
    private void displayHelp() {
        System.out.println("\n📖 HELP & USAGE");
        System.out.println("═".repeat(60));
        System.out.println("""
                COMMAND LINE USAGE:
                java -jar transcript-pipeline.jar [options]

                OPTIONS:
                --help                  Show this help message
                --version               Show version information
                --step <name>           Run specific pipeline step
                --input <path>          Input file path
                --output <path>         Output file path

                PIPELINE STEPS:
                - chunk                 Split transcripts into chunks
                - summarize             Summarize chunks
                - consolidate           Create master notes
                - export                Generate exam materials

                EXAMPLE:
                java -jar transcript-pipeline.jar --step chunk --input transcripts/lesson1.txt

                ENVIRONMENT SETUP:
                1. Create a .env file with:
                   CLAUDE_API_KEY=your_key_here
                   OPENAI_API_KEY=your_key_here
                2. Run: java -jar transcript-pipeline.jar

                For more info, see README.md
                """);
    }

    /**
     * Resume pipeline from saved state (Phase 1.3)
     */
    private void resumePipeline(PipelineState state) {
        try {
            System.out.println();
            ConsoleColors.printHeader("RESUMING PIPELINE");
            System.out.println();

            String outputDir = ConfigManager.get(ConfigManager.OUTPUT_DIR);

            // Resume each lesson that can be resumed
            for (PipelineState.LessonState lessonState : state.getLessons().values()) {
                if (!lessonState.canResume()) {
                    if (lessonState.allStagesCompleted()) {
                        ConsoleColors.printSuccess("Skipping completed file: " + lessonState.getFilename());
                    }
                    continue;
                }

                System.out.println();
                ConsoleColors.printSection("Resuming: " + lessonState.getFilename());
                System.out.println("Next stage: " + lessonState.getNextStage());
                System.out.println();

                // Load file
                File transcriptFile = new File("transcripts/" + lessonState.getFilename());
                if (!transcriptFile.exists()) {
                    ConsoleColors.printError("Transcript file not found: " + transcriptFile.getAbsolutePath());
                    continue;
                }

                // Resume from next stage
                resumeLessonFromStage(lessonState, transcriptFile, outputDir);

                // Save state after each file
                StateManager.saveState(state);
            }

            // Delete state when all complete
            if (state.getLessons().values().stream().allMatch(PipelineState.LessonState::allStagesCompleted)) {
                StateManager.deleteState();
                ConsoleColors.printSuccess("All files completed! State cleared.");
            }

        } catch (Exception e) {
            logger.error("Resume pipeline error", e);
            ConsoleColors.printError("Resume error: " + e.getMessage());
        }
    }

    /**
     * Resume a single lesson from a specific stage
     */
    private void resumeLessonFromStage(PipelineState.LessonState lessonState, File transcriptFile, String outputDir) {
        try {
            List<TextChunk> chunks = null;
            List<ChunkSummary> summaries = null;
            String masterNotes = null;

            String nextStage = lessonState.getNextStage();

            // Stage 1: Chunking (if needed)
            if ("chunking".equals(nextStage)) {
                System.out.println("\n⏳ STEP 1: CHUNKING");
                lessonState.setChunkingStatus(PipelineState.StageStatus.IN_PROGRESS);
                chunks = chunkerService.chunkTranscript(transcriptFile.getAbsolutePath());
                String chunkOutputPath = FileService.generateChunkOutputPath(outputDir, transcriptFile.getName());
                chunkerService.saveChunks(chunks, chunkOutputPath);
                lessonState.setChunksPath(chunkOutputPath);
                lessonState.setChunkingStatus(PipelineState.StageStatus.COMPLETED);
                System.out.println("✅ Created " + chunks.size() + " chunks");
                nextStage = "summarization";
            } else if (lessonState.getChunksPath() != null) {
                chunks = chunkerService.loadChunks(lessonState.getChunksPath());
            }

            // Stage 2: Summarization (if needed)
            if ("summarization".equals(nextStage)) {
                System.out.println("\n⏳ STEP 2: SUMMARIZING");
                lessonState.setSummarizationStatus(PipelineState.StageStatus.IN_PROGRESS);
                summaries = summarizerService.summarizeChunks(chunks);
                String summaryDir = FileService.generateSummaryOutputDir(outputDir, transcriptFile.getName());
                FileService.createDirectoryIfNotExists(summaryDir);
                summarizerService.saveSummaries(summaries, summaryDir);
                lessonState.setSummariesPath(summaryDir);
                lessonState.setSummarizationStatus(PipelineState.StageStatus.COMPLETED);
                System.out.println("✅ " + summarizerService.getSummaryStatistics(summaries));
                nextStage = "consolidation";
            } else if (lessonState.getSummariesPath() != null) {
                summaries = summarizerService.loadSummaries(lessonState.getSummariesPath());
            }

            // Stage 3: Consolidation (if needed)
            if ("consolidation".equals(nextStage)) {
                System.out.println("\n⏳ STEP 3: CONSOLIDATING");
                lessonState.setConsolidationStatus(PipelineState.StageStatus.IN_PROGRESS);
                masterNotes = consolidatorService.consolidateToMasterNotes(summaries);
                String consolidatedPath = FileService.generateConsolidatedPath(outputDir, transcriptFile.getName());
                consolidatorService.saveMasterNotes(masterNotes, consolidatedPath);
                lessonState.setMasterNotesPath(consolidatedPath);
                lessonState.setConsolidationStatus(PipelineState.StageStatus.COMPLETED);
                System.out.println("✅ Master notes created");
                nextStage = "exam_materials";
            } else if (lessonState.getMasterNotesPath() != null) {
                masterNotes = FileService.readTextFile(lessonState.getMasterNotesPath());
            }

            // Stage 4: Exam Materials (if needed)
            if ("exam_materials".equals(nextStage)) {
                System.out.println("\n⏳ STEP 4: GENERATING EXAM MATERIALS");
                lessonState.setExamMaterialsStatus(PipelineState.StageStatus.IN_PROGRESS);
                String examDir = FileService.generateExamMaterialsDir(outputDir, transcriptFile.getName());
                FileService.createDirectoryIfNotExists(examDir);

                String flashcards = consolidatorService.generateFlashcards(masterNotes, summaries);
                consolidatorService.saveFlashcards(flashcards, examDir + "/flashcards.csv");

                String practiceQuestions = consolidatorService.generatePracticeQuestions(masterNotes);
                consolidatorService.savePracticeQuestions(practiceQuestions, examDir + "/practice_questions.md");

                String quickRevision = consolidatorService.generateQuickRevision(masterNotes);
                consolidatorService.saveQuickRevision(quickRevision, examDir + "/quick_revision.md");

                lessonState.setExamMaterialsPath(examDir);
                lessonState.setExamMaterialsStatus(PipelineState.StageStatus.COMPLETED);
                System.out.println("✅ Exam materials generated");
            }

            ConsoleColors.printSuccess("File completed: " + transcriptFile.getName());

        } catch (Exception e) {
            logger.error("Error resuming lesson: " + lessonState.getFilename(), e);
            lessonState.setErrorMessage(e.getMessage());
            ConsoleColors.printError("Error: " + e.getMessage());
        }
    }

    /**
     * Run batch processing mode (Phase 1.4)
     */
    private void runBatchProcessing(Scanner scanner) {
        try {
            System.out.print("\n📁 Enter transcript directory (default: 'transcripts'): ");
            String transcriptDir = scanner.nextLine().trim();
            if (transcriptDir.isEmpty()) {
                transcriptDir = "transcripts";
            }

            // Validate directory
            File dir = new File(transcriptDir);
            if (!dir.exists() || !dir.isDirectory()) {
                ConsoleColors.printError("Directory not found: " + transcriptDir);
                return;
            }

            List<File> transcriptFiles = FileService.listFilesInDirectory(transcriptDir, ".txt");
            if (transcriptFiles.isEmpty()) {
                ConsoleColors.printError("No transcript files found in " + transcriptDir);
                ConsoleColors.printInfo("Place .txt files in the " + transcriptDir + "/ directory");
                return;
            }

            // Show summary and confirm
            System.out.println();
            ConsoleColors.printInfo("Found " + transcriptFiles.size() + " transcript files");
            System.out.println();

            System.out.print("Continue with batch processing? (y/n): ");
            String confirm = scanner.nextLine().trim().toLowerCase();
            if (!confirm.equals("y") && !confirm.equals("yes")) {
                ConsoleColors.printWarning("Batch processing cancelled");
                return;
            }

            // Run batch processing
            BatchProcessor batchProcessor = new BatchProcessor();
            BatchResult result = batchProcessor.processAllTranscripts(transcriptDir);

            // Display results
            System.out.println();
            ConsoleColors.printHeader("BATCH PROCESSING COMPLETE");
            System.out.println();
            System.out.println("Total files: " + (result.getSuccessfulFiles().size() + result.getFailedFiles().size()));
            System.out.println("Successful: " + ConsoleColors.colorize(String.valueOf(result.getSuccessfulFiles().size()), ConsoleColors.GREEN));
            System.out.println("Failed: " + ConsoleColors.colorize(String.valueOf(result.getFailedFiles().size()), ConsoleColors.RED));
            System.out.println("Success rate: " + ConsoleColors.formatPercentage(result.getSuccessRate()));
            System.out.println("Total cost: " + ConsoleColors.formatCost(result.getTotalCost()));
            System.out.println("Total time: " + ConsoleColors.formatTime(result.getTotalDurationMs()));
            System.out.println();
            ConsoleColors.printInfo("Reports saved to output directory");

        } catch (Exception e) {
            logger.error("Batch processing error", e);
            ConsoleColors.printError("Batch processing error: " + e.getMessage());
        }
    }

    /**
     * Run command-line mode
     */
    private void runCommand(String[] args) throws ParseException {
        Options options = new Options();
        options.addOption("h", "help", false, "Show help");
        options.addOption("v", "version", false, "Show version");
        options.addOption("s", "step", true, "Pipeline step to run");
        options.addOption("i", "input", true, "Input file path");
        options.addOption("o", "output", true, "Output file path");
        options.addOption("b", "batch", true, "Batch process all files in directory");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("transcript-pipeline", options);
            return;
        }

        if (cmd.hasOption("v")) {
            System.out.println("Transcript to Exam Notes Pipeline v" + VERSION);
            return;
        }

        if (cmd.hasOption("b")) {
            // Batch processing mode
            String transcriptDir = cmd.getOptionValue("b");
            BatchProcessor batchProcessor = new BatchProcessor();
            BatchResult result = batchProcessor.processAllTranscripts(transcriptDir);

            System.out.println();
            System.out.println("Batch processing complete!");
            System.out.println("Success rate: " + ConsoleColors.formatPercentage(result.getSuccessRate()));
            return;
        }

        if (cmd.hasOption("s")) {
            System.out.println("Command-line mode not fully implemented yet. Use interactive mode.");
        }
    }
}
