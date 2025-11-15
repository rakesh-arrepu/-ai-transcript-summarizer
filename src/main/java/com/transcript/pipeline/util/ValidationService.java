package com.transcript.pipeline.util;

import com.transcript.pipeline.config.ConfigManager;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Comprehensive validation service for pre-flight checks.
 * Validates files, API keys, environment, and system resources.
 */
public class ValidationService {

    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);

    // File size limits
    private static final long MAX_FILE_SIZE = 50_000_000; // 50 MB
    private static final long WARN_FILE_SIZE = 10_000_000; // 10 MB
    private static final long MIN_FILE_SIZE = 100; // 100 bytes

    // Disk space requirements
    private static final long MIN_DISK_SPACE = 100_000_000; // 100 MB
    private static final long WARN_DISK_SPACE = 500_000_000; // 500 MB

    /**
     * Validate a transcript file before processing
     */
    public static ValidationResult validateTranscriptFile(String filePath) {
        logger.debug("Validating transcript file: {}", filePath);

        File file = new File(filePath);

        // Check if file exists
        if (!file.exists()) {
            return ValidationResult.error(
                "File not found: " + file.getName(),
                "Ensure the file path is correct and the file exists in the transcripts/ directory"
            );
        }

        // Check if it's a file (not a directory)
        if (!file.isFile()) {
            return ValidationResult.error(
                "Path is a directory, not a file: " + file.getName(),
                "Specify a .txt file, not a directory"
            );
        }

        // Check if file is readable
        if (!file.canRead()) {
            return ValidationResult.error(
                "File is not readable: " + file.getName(),
                "Check file permissions with: chmod 644 " + file.getAbsolutePath()
            );
        }

        // Check file size
        long fileSize = file.length();

        if (fileSize < MIN_FILE_SIZE) {
            return ValidationResult.error(
                "File is too small (" + fileSize + " bytes): " + file.getName(),
                "Ensure the file contains actual transcript content (minimum 100 bytes)"
            );
        }

        if (fileSize > MAX_FILE_SIZE) {
            return ValidationResult.error(
                "File is too large (" + formatFileSize(fileSize) + "): " + file.getName(),
                "Split the file into smaller chunks (maximum 50 MB per file)"
            );
        }

        ValidationResult result = ValidationResult.success("File validation passed: " + file.getName());

        if (fileSize > WARN_FILE_SIZE) {
            result = ValidationResult.warning(
                "Large file detected (" + formatFileSize(fileSize) + "): " + file.getName(),
                "This will take longer and cost more. Consider splitting into smaller files."
            );
        }

        // Check file extension
        if (!filePath.toLowerCase().endsWith(".txt")) {
            result.addDetail("File extension is not .txt (found: " + getFileExtension(filePath) + ")");
            result.addDetail("The file will still be processed as plain text");
        }

        // Check file encoding
        try {
            Charset encoding = detectEncoding(file);
            if (!encoding.equals(StandardCharsets.UTF_8)) {
                result.addDetail("File encoding is " + encoding + " (recommended: UTF-8)");
                result.addDetail("Non-UTF-8 files may have character encoding issues");
            }
        } catch (IOException e) {
            logger.warn("Failed to detect file encoding", e);
        }

        // Check if file is empty
        try {
            String content = Files.readString(file.toPath()).trim();
            if (content.isEmpty()) {
                return ValidationResult.error(
                    "File is empty: " + file.getName(),
                    "Add transcript content to the file before processing"
                );
            }

            // Check if file has reasonable content
            int wordCount = content.split("\\s+").length;
            if (wordCount < 50) {
                result.addDetail("File has very few words (" + wordCount + ")");
                result.addDetail("Minimum 50 words recommended for meaningful processing");
            }

        } catch (IOException e) {
            logger.warn("Failed to read file content for validation", e);
        }

        return result;
    }

    /**
     * Validate API key format
     */
    public static ValidationResult validateApiKey(String keyName, String apiKey, String provider) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return ValidationResult.error(
                keyName + " is not configured",
                "Set " + keyName + " in your .env file or environment variables"
            );
        }

        apiKey = apiKey.trim();

        // Validate format based on provider
        switch (provider.toLowerCase()) {
            case "claude":
                if (!apiKey.startsWith("sk-ant-")) {
                    return ValidationResult.error(
                        "Invalid Claude API key format",
                        "Claude API keys should start with 'sk-ant-'. Get a valid key from https://console.anthropic.com"
                    );
                }
                if (apiKey.length() < 50) {
                    return ValidationResult.warning(
                        "Claude API key seems too short",
                        "Verify your API key is complete and not truncated"
                    );
                }
                break;

            case "openai":
            case "gpt":
                if (!apiKey.startsWith("sk-")) {
                    return ValidationResult.error(
                        "Invalid OpenAI API key format",
                        "OpenAI API keys should start with 'sk-'. Get a valid key from https://platform.openai.com/api-keys"
                    );
                }
                if (apiKey.length() < 40) {
                    return ValidationResult.warning(
                        "OpenAI API key seems too short",
                        "Verify your API key is complete and not truncated"
                    );
                }
                break;

            case "gemini":
                if (!apiKey.startsWith("AIzaSy")) {
                    return ValidationResult.error(
                        "Invalid Gemini API key format",
                        "Gemini API keys should start with 'AIzaSy'. Get a valid key from https://aistudio.google.com/app/apikey"
                    );
                }
                if (apiKey.length() < 30) {
                    return ValidationResult.warning(
                        "Gemini API key seems too short",
                        "Verify your API key is complete and not truncated"
                    );
                }
                break;

            default:
                logger.warn("Unknown provider for API key validation: {}", provider);
        }

        return ValidationResult.success(keyName + " format is valid");
    }

    /**
     * Validate pipeline configuration
     */
    public static ValidationResult validatePipelineConfiguration() {
        logger.debug("Validating pipeline configuration");

        ValidationResult result = ValidationResult.success("Configuration validation passed");

        // Get configured models
        String summarizerModel = ConfigManager.get(ConfigManager.SUMMARIZER_MODEL, "claude");
        String consolidatorModel = ConfigManager.get(ConfigManager.CONSOLIDATOR_MODEL, "gpt");

        result.addDetail("Summarizer model: " + summarizerModel.toUpperCase());
        result.addDetail("Consolidator model: " + consolidatorModel.toUpperCase());

        // Check if required API keys are present for selected models
        List<String> missingKeys = new ArrayList<>();

        if ("claude".equalsIgnoreCase(summarizerModel)) {
            String claudeKey = ConfigManager.get(ConfigManager.CLAUDE_API_KEY);
            if (claudeKey == null || claudeKey.trim().isEmpty()) {
                missingKeys.add("CLAUDE_API_KEY (required for SUMMARIZER_MODEL=claude)");
            }
        } else if ("gemini".equalsIgnoreCase(summarizerModel)) {
            String geminiKey = ConfigManager.get(ConfigManager.GEMINI_API_KEY);
            if (geminiKey == null || geminiKey.trim().isEmpty()) {
                missingKeys.add("GEMINI_API_KEY (required for SUMMARIZER_MODEL=gemini)");
            }
        }

        if ("gpt".equalsIgnoreCase(consolidatorModel)) {
            String openaiKey = ConfigManager.get(ConfigManager.OPENAI_API_KEY);
            if (openaiKey == null || openaiKey.trim().isEmpty()) {
                missingKeys.add("OPENAI_API_KEY (required for CONSOLIDATOR_MODEL=gpt)");
            }
        } else if ("gemini".equalsIgnoreCase(consolidatorModel)) {
            String geminiKey = ConfigManager.get(ConfigManager.GEMINI_API_KEY);
            if (geminiKey == null || geminiKey.trim().isEmpty()) {
                missingKeys.add("GEMINI_API_KEY (required for CONSOLIDATOR_MODEL=gemini)");
            }
        }

        if (!missingKeys.isEmpty()) {
            result = ValidationResult.error(
                "Required API keys are missing",
                "Add the following to your .env file:\n  " + String.join("\n  ", missingKeys)
            );
        }

        // Validate chunk size
        int chunkSize = ConfigManager.getInt(ConfigManager.CHUNK_SIZE, 1500);
        if (chunkSize < 500 || chunkSize > 5000) {
            result.addDetail("CHUNK_SIZE is outside recommended range (500-5000): " + chunkSize);
        }

        return result;
    }

    /**
     * Validate disk space
     */
    public static ValidationResult validateDiskSpace(String outputDir) {
        File dir = new File(outputDir);

        // Create directory if it doesn't exist
        if (!dir.exists()) {
            dir.mkdirs();
        }

        long freeSpace = dir.getUsableSpace();

        if (freeSpace < MIN_DISK_SPACE) {
            return ValidationResult.error(
                "Insufficient disk space (" + formatFileSize(freeSpace) + " available)",
                "Free up at least 100 MB of disk space before running the pipeline"
            );
        }

        if (freeSpace < WARN_DISK_SPACE) {
            return ValidationResult.warning(
                "Low disk space (" + formatFileSize(freeSpace) + " available)",
                "Consider freeing up more space to avoid issues during processing"
            );
        }

        return ValidationResult.success("Sufficient disk space available (" + formatFileSize(freeSpace) + ")");
    }

    /**
     * Validate output directory
     */
    public static ValidationResult validateOutputDirectory(String outputDir) {
        File dir = new File(outputDir);

        // Check if directory exists
        if (!dir.exists()) {
            // Try to create it
            if (dir.mkdirs()) {
                return ValidationResult.success("Output directory created: " + outputDir);
            } else {
                return ValidationResult.error(
                    "Cannot create output directory: " + outputDir,
                    "Check parent directory permissions or specify a different OUTPUT_DIR in .env"
                );
            }
        }

        // Check if it's actually a directory
        if (!dir.isDirectory()) {
            return ValidationResult.error(
                "Output path exists but is not a directory: " + outputDir,
                "Remove the file or specify a different OUTPUT_DIR in .env"
            );
        }

        // Check if we can write to it
        if (!dir.canWrite()) {
            return ValidationResult.error(
                "Output directory is not writable: " + outputDir,
                "Fix permissions with: chmod 755 " + dir.getAbsolutePath()
            );
        }

        return ValidationResult.success("Output directory is ready: " + outputDir);
    }

    /**
     * Run all pre-flight checks
     */
    public static List<ValidationResult> runPreFlightChecks(String transcriptPath) {
        List<ValidationResult> results = new ArrayList<>();

        ConsoleColors.printHeader("PRE-FLIGHT CHECKS");

        // 1. Validate transcript file
        System.out.print("Checking transcript file... ");
        ValidationResult fileCheck = validateTranscriptFile(transcriptPath);
        System.out.println();
        fileCheck.print();
        results.add(fileCheck);

        System.out.println();

        // 2. Validate configuration
        System.out.print("Checking pipeline configuration... ");
        ValidationResult configCheck = validatePipelineConfiguration();
        System.out.println();
        configCheck.print();
        results.add(configCheck);

        System.out.println();

        // 3. Validate API keys
        String summarizerModel = ConfigManager.get(ConfigManager.SUMMARIZER_MODEL, "claude");
        String consolidatorModel = ConfigManager.get(ConfigManager.CONSOLIDATOR_MODEL, "gpt");

        if ("claude".equalsIgnoreCase(summarizerModel)) {
            System.out.print("Checking Claude API key... ");
            ValidationResult claudeCheck = validateApiKey(
                "CLAUDE_API_KEY",
                ConfigManager.get(ConfigManager.CLAUDE_API_KEY),
                "claude"
            );
            System.out.println();
            claudeCheck.print();
            results.add(claudeCheck);
            System.out.println();
        }

        if ("gpt".equalsIgnoreCase(consolidatorModel)) {
            System.out.print("Checking OpenAI API key... ");
            ValidationResult openaiCheck = validateApiKey(
                "OPENAI_API_KEY",
                ConfigManager.get(ConfigManager.OPENAI_API_KEY),
                "openai"
            );
            System.out.println();
            openaiCheck.print();
            results.add(openaiCheck);
            System.out.println();
        }

        if ("gemini".equalsIgnoreCase(summarizerModel) || "gemini".equalsIgnoreCase(consolidatorModel)) {
            System.out.print("Checking Gemini API key... ");
            ValidationResult geminiCheck = validateApiKey(
                "GEMINI_API_KEY",
                ConfigManager.get(ConfigManager.GEMINI_API_KEY),
                "gemini"
            );
            System.out.println();
            geminiCheck.print();
            results.add(geminiCheck);
            System.out.println();
        }

        // 4. Validate output directory
        String outputDir = ConfigManager.get(ConfigManager.OUTPUT_DIR, "output");
        System.out.print("Checking output directory... ");
        ValidationResult outputCheck = validateOutputDirectory(outputDir);
        System.out.println();
        outputCheck.print();
        results.add(outputCheck);

        System.out.println();

        // 5. Validate disk space
        System.out.print("Checking disk space... ");
        ValidationResult diskCheck = validateDiskSpace(outputDir);
        System.out.println();
        diskCheck.print();
        results.add(diskCheck);

        System.out.println();

        return results;
    }

    /**
     * Check if all validation results allow proceeding
     */
    public static boolean canProceed(List<ValidationResult> results) {
        boolean hasErrors = results.stream().anyMatch(ValidationResult::isError);

        if (hasErrors) {
            ConsoleColors.printSeparator();
            ConsoleColors.printError("Pre-flight checks failed! Cannot proceed.");
            ConsoleColors.printInfo("Fix the errors above and try again.");
            return false;
        }

        boolean hasWarnings = results.stream().anyMatch(ValidationResult::isWarning);

        if (hasWarnings) {
            ConsoleColors.printSeparator();
            ConsoleColors.printWarning("Some checks returned warnings.");
            ConsoleColors.printInfo("You can proceed, but results may be affected.");
            return true;
        }

        ConsoleColors.printSeparator();
        ConsoleColors.printSuccess("All pre-flight checks passed! Ready to proceed.");
        return true;
    }

    // Utility methods

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " bytes";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    private static String getFileExtension(String filePath) {
        int lastDot = filePath.lastIndexOf('.');
        if (lastDot == -1) {
            return "none";
        }
        return filePath.substring(lastDot);
    }

    private static Charset detectEncoding(File file) throws IOException {
        // Simple encoding detection - checks for UTF-8 BOM or assumes UTF-8
        byte[] bom = new byte[3];
        try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
            int read = fis.read(bom);
            if (read >= 3 && bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) {
                return StandardCharsets.UTF_8;
            }
        }
        // Try to read as UTF-8
        try {
            Files.readString(file.toPath(), StandardCharsets.UTF_8);
            return StandardCharsets.UTF_8;
        } catch (Exception e) {
            return Charset.defaultCharset();
        }
    }
}
