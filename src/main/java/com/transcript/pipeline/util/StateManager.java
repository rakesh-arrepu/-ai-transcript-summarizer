package com.transcript.pipeline.util;

import com.transcript.pipeline.config.ConfigManager;
import com.transcript.pipeline.models.PipelineState;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages pipeline state persistence for resume capability.
 * Saves and loads pipeline state to/from JSON files.
 */
public class StateManager {

    private static final Logger logger = LoggerFactory.getLogger(StateManager.java);
    private static final String STATE_FILE = ".pipeline_state.json";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Get the state file path
     */
    public static String getStateFilePath() {
        String outputDir = ConfigManager.get(ConfigManager.OUTPUT_DIR, "output");
        return outputDir + "/" + STATE_FILE;
    }

    /**
     * Check if a state file exists
     */
    public static boolean stateFileExists() {
        File stateFile = new File(getStateFilePath());
        return stateFile.exists() && stateFile.isFile();
    }

    /**
     * Save pipeline state to file
     */
    public static void saveState(PipelineState state) {
        try {
            String stateFilePath = getStateFilePath();

            // Ensure output directory exists
            File stateFile = new File(stateFilePath);
            File parentDir = stateFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            FileService.writeJsonFile(stateFilePath, state);
            logger.info("Pipeline state saved to {}", stateFilePath);
            logger.debug("State saved at {}", state.getTimestamp().format(FORMATTER));
        } catch (IOException e) {
            logger.error("Failed to save pipeline state", e);
        }
    }

    /**
     * Load pipeline state from file
     */
    public static PipelineState loadState() {
        try {
            String stateFilePath = getStateFilePath();

            if (!stateFileExists()) {
                logger.debug("No state file found at {}", stateFilePath);
                return null;
            }

            PipelineState state = FileService.readJsonFile(stateFilePath, PipelineState.class);
            logger.info("Pipeline state loaded from {}", stateFilePath);
            logger.debug("State from {}", state.getTimestamp().format(FORMATTER));
            return state;
        } catch (IOException e) {
            logger.error("Failed to load pipeline state", e);
            return null;
        }
    }

    /**
     * Delete state file
     */
    public static void deleteState() {
        try {
            String stateFilePath = getStateFilePath();
            File stateFile = new File(stateFilePath);

            if (stateFile.exists()) {
                Files.delete(stateFile.toPath());
                logger.info("Pipeline state file deleted");
            }
        } catch (IOException e) {
            logger.warn("Failed to delete state file", e);
        }
    }

    /**
     * Display state information
     */
    public static void displayState(PipelineState state) {
        if (state == null) {
            System.out.println("No saved state found.");
            return;
        }

        System.out.println();
        ConsoleColors.printSection("Saved Pipeline State");
        System.out.println();
        System.out.println(String.format("Saved at: %s",
            ConsoleColors.colorize(state.getTimestamp().format(FORMATTER), ConsoleColors.CYAN)));
        System.out.println(String.format("Overall status: %s",
            ConsoleColors.colorize(state.getOverallStatus() != null ? state.getOverallStatus() : "in_progress",
                ConsoleColors.YELLOW)));
        System.out.println();

        if (state.getLessons() != null && !state.getLessons().isEmpty()) {
            System.out.println("Files in progress:");
            for (PipelineState.LessonState lessonState : state.getLessons().values()) {
                displayLessonState(lessonState);
            }
        }
    }

    /**
     * Display lesson state details
     */
    private static void displayLessonState(PipelineState.LessonState lessonState) {
        System.out.println();
        System.out.println(ConsoleColors.colorize("  📄 " + lessonState.getFilename(), ConsoleColors.BOLD_WHITE));

        // Display stage statuses
        displayStageStatus("    Chunking", lessonState.getChunkingStatus());
        displayStageStatus("    Summarization", lessonState.getSummarizationStatus());
        displayStageStatus("    Consolidation", lessonState.getConsolidationStatus());
        displayStageStatus("    Exam Materials", lessonState.getExamMaterialsStatus());

        // Show next step
        if (lessonState.canResume()) {
            String nextStage = lessonState.getNextStage();
            System.out.println(ConsoleColors.colorize("    Next: " + nextStage, ConsoleColors.BOLD_CYAN));
        } else if (lessonState.allStagesCompleted()) {
            System.out.println(ConsoleColors.colorize("    Status: COMPLETE", ConsoleColors.BOLD_GREEN));
        }

        // Show error if any
        if (lessonState.getErrorMessage() != null) {
            System.out.println(ConsoleColors.colorize("    Error: " + lessonState.getErrorMessage(), ConsoleColors.RED));
        }
    }

    /**
     * Display individual stage status
     */
    private static void displayStageStatus(String stageName, PipelineState.StageStatus status) {
        String statusText;
        String color;

        if (status == null) {
            status = PipelineState.StageStatus.NOT_STARTED;
        }

        switch (status) {
            case COMPLETED:
                statusText = "✓ COMPLETED";
                color = ConsoleColors.GREEN;
                break;
            case IN_PROGRESS:
                statusText = "⏳ IN PROGRESS";
                color = ConsoleColors.YELLOW;
                break;
            case FAILED:
                statusText = "✗ FAILED";
                color = ConsoleColors.RED;
                break;
            case NOT_STARTED:
            default:
                statusText = "○ NOT STARTED";
                color = ConsoleColors.WHITE;
                break;
        }

        System.out.println(String.format("%-25s %s",
            stageName + ":",
            ConsoleColors.colorize(statusText, color)));
    }

    /**
     * Prompt user to resume or restart
     */
    public static ResumeChoice promptResumeChoice(PipelineState state) {
        if (state == null || state.getLessons() == null || state.getLessons().isEmpty()) {
            return ResumeChoice.START_NEW;
        }

        // Check if any lessons can be resumed
        boolean canResume = state.getLessons().values().stream()
            .anyMatch(PipelineState.LessonState::canResume);

        if (!canResume) {
            return ResumeChoice.START_NEW;
        }

        displayState(state);
        System.out.println();
        ConsoleColors.printSection("Resume Options");
        System.out.println();
        System.out.println("1. Resume from where you left off");
        System.out.println("2. Start new pipeline (discard saved state)");
        System.out.println("3. Cancel");
        System.out.println();
        System.out.print("Choose an option (1-3): ");

        java.util.Scanner scanner = new java.util.Scanner(System.in);
        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1":
                return ResumeChoice.RESUME;
            case "2":
                return ResumeChoice.START_NEW;
            case "3":
            default:
                return ResumeChoice.CANCEL;
        }
    }

    /**
     * Clean up partial outputs for failed stages
     */
    public static void cleanupPartialOutputs(PipelineState.LessonState lessonState) {
        logger.info("Cleaning up partial outputs for {}", lessonState.getFilename());

        // Clean up based on failed stages
        if (lessonState.getChunkingStatus() == PipelineState.StageStatus.FAILED &&
            lessonState.getChunksPath() != null) {
            deleteFileIfExists(lessonState.getChunksPath());
        }

        if (lessonState.getSummarizationStatus() == PipelineState.StageStatus.FAILED &&
            lessonState.getSummariesPath() != null) {
            deleteDirectoryIfExists(lessonState.getSummariesPath());
        }

        if (lessonState.getConsolidationStatus() == PipelineState.StageStatus.FAILED &&
            lessonState.getMasterNotesPath() != null) {
            deleteFileIfExists(lessonState.getMasterNotesPath());
        }

        if (lessonState.getExamMaterialsStatus() == PipelineState.StageStatus.FAILED &&
            lessonState.getExamMaterialsPath() != null) {
            deleteDirectoryIfExists(lessonState.getExamMaterialsPath());
        }
    }

    /**
     * Delete file if it exists
     */
    private static void deleteFileIfExists(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                logger.info("Deleted partial file: {}", filePath);
            }
        } catch (IOException e) {
            logger.warn("Failed to delete file: {}", filePath, e);
        }
    }

    /**
     * Delete directory if it exists
     */
    private static void deleteDirectoryIfExists(String dirPath) {
        try {
            Path path = Paths.get(dirPath);
            if (Files.exists(path) && Files.isDirectory(path)) {
                Files.walk(path)
                    .sorted((a, b) -> b.compareTo(a)) // Reverse order to delete files before directories
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            logger.warn("Failed to delete: {}", p, e);
                        }
                    });
                logger.info("Deleted partial directory: {}", dirPath);
            }
        } catch (IOException e) {
            logger.warn("Failed to delete directory: {}", dirPath, e);
        }
    }

    /**
     * Resume choice enum
     */
    public enum ResumeChoice {
        RESUME,
        START_NEW,
        CANCEL
    }
}
