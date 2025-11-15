package com.transcript.pipeline;

import com.transcript.pipeline.config.ConfigManager;
import com.transcript.pipeline.models.ChunkSummary;
import com.transcript.pipeline.models.TextChunk;
import com.transcript.pipeline.services.ChunkerService;
import com.transcript.pipeline.services.ConsolidatorService;
import com.transcript.pipeline.services.SummarizerService;
import com.transcript.pipeline.util.FileService;
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
            // Validate API keys first
            if (!ConfigManager.validateApiKeys()) {
                System.err.println("\n❌ ERROR: API keys not configured!");
                System.err.println("Please set CLAUDE_API_KEY and OPENAI_API_KEY environment variables");
                System.err.println("Or create a .env file in the project root directory");
                System.exit(1);
            }

            ConfigManager.printSummary();
            App app = new App();

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
            System.out.println("6. View pipeline status");
            System.out.println("7. Settings");
            System.out.println("8. Help");
            System.out.println("0. Exit");
            System.out.print("\n👉 Choose an option (0-8): ");

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
                    viewPipelineStatus();
                    break;
                case "7":
                    displaySettings();
                    break;
                case "8":
                    displayHelp();
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

            // Create output directories
            FileService.createDirectoryIfNotExists(outputDir + "/chunks");
            FileService.createDirectoryIfNotExists(outputDir + "/summaries");
            FileService.createDirectoryIfNotExists(outputDir + "/consolidated");
            FileService.createDirectoryIfNotExists(outputDir + "/exam_materials");

            // Get list of transcript files
            List<File> transcriptFiles = FileService.listFilesInDirectory(transcriptDir, ".txt");
            if (transcriptFiles.isEmpty()) {
                System.out.println("❌ No transcript files found in " + transcriptDir);
                return;
            }

            System.out.println("\n🔍 Found " + transcriptFiles.size() + " transcript file(s)");
            System.out.println("⚙️  Multi-File Mode: " + multiFileMode.toUpperCase());

            if ("separate".equals(multiFileMode)) {
                // Process each file separately
                runPipelineInSeparateMode(transcriptFiles, outputDir);
            } else {
                // Process all files combined
                runPipelineInCombinedMode(transcriptFiles, outputDir);
            }

        } catch (Exception e) {
            System.out.println("❌ Pipeline error: " + e.getMessage());
            logger.error("Pipeline error", e);
        }
    }

    /**
     * Run pipeline in SEPARATE mode - each file gets its own output
     */
    private void runPipelineInSeparateMode(List<File> transcriptFiles, String outputDir) {
        System.out.println("\n📋 Processing each file separately with individual outputs\n");

        for (int i = 0; i < transcriptFiles.size(); i++) {
            File file = transcriptFiles.get(i);
            System.out.println("\n" + "=".repeat(70));
            System.out.println("📄 FILE " + (i + 1) + "/" + transcriptFiles.size() + ": " + file.getName());
            System.out.println("=".repeat(70));

            try {
                // Step 1: Chunking
                System.out.println("\n⏳ STEP 1: CHUNKING");
                List<TextChunk> chunks = chunkerService.chunkTranscript(file.getAbsolutePath());
                String chunkOutputPath = FileService.generateChunkOutputPath(outputDir, file.getName());
                chunkerService.saveChunks(chunks, chunkOutputPath);
                System.out.println("✅ Created " + chunks.size() + " chunks");

                // Step 2: Summarization
                System.out.println("\n⏳ STEP 2: SUMMARIZING");
                List<ChunkSummary> summaries = summarizerService.summarizeChunks(chunks);
                String summaryDir = FileService.generateSummaryOutputDir(outputDir, file.getName());
                FileService.createDirectoryIfNotExists(summaryDir);
                summarizerService.saveSummaries(summaries, summaryDir);
                System.out.println("✅ " + summarizerService.getSummaryStatistics(summaries));

                // Step 3: Consolidation
                System.out.println("\n⏳ STEP 3: CONSOLIDATING");
                String masterNotes = consolidatorService.consolidateToMasterNotes(summaries);
                String consolidatedPath = FileService.generateConsolidatedPath(outputDir, file.getName());
                consolidatorService.saveMasterNotes(masterNotes, consolidatedPath);
                System.out.println("✅ Master notes created");

                // Step 4: Exam Materials
                System.out.println("\n⏳ STEP 4: GENERATING EXAM MATERIALS");
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

                System.out.println("\n✨ FILE COMPLETE: " + file.getName());
                System.out.println("📁 Master notes: " + consolidatedPath);
                System.out.println("📁 Exam materials: " + examDir + "/");

            } catch (Exception e) {
                System.out.println("❌ Error processing " + file.getName() + ": " + e.getMessage());
                logger.error("Error processing file: " + file.getName(), e);
            }
        }

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

            System.out.println("\n\n✨ PIPELINE COMPLETE!");
            System.out.println("═".repeat(60));
            System.out.println("📁 Output directory: " + outputDir + "/");
            System.out.println("📝 Master notes: " + outputDir + "/consolidated/master_notes.md");
            System.out.println("📚 Quick revision: " + outputDir + "/exam_materials/quick_revision.md");
            System.out.println("🎯 Practice questions: " + outputDir + "/exam_materials/practice_questions.md");
            System.out.println("🎓 Flashcards: " + outputDir + "/exam_materials/flashcards.csv");

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
     * Run command-line mode
     */
    private void runCommand(String[] args) throws ParseException {
        Options options = new Options();
        options.addOption("h", "help", false, "Show help");
        options.addOption("v", "version", false, "Show version");
        options.addOption("s", "step", true, "Pipeline step to run");
        options.addOption("i", "input", true, "Input file path");
        options.addOption("o", "output", true, "Output file path");

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

        if (cmd.hasOption("s")) {
            System.out.println("Command-line mode not fully implemented yet. Use interactive mode.");
        }
    }
}
