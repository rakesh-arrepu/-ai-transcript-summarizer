package com.transcript.pipeline.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages configuration from environment variables and .env files.
 * Provides centralized access to all pipeline configuration.
 */
public class ConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);

    // API Configuration
    public static final String CLAUDE_API_KEY = "CLAUDE_API_KEY";
    public static final String OPENAI_API_KEY = "OPENAI_API_KEY";
    public static final String GEMINI_API_KEY = "GEMINI_API_KEY";
    public static final String MODEL_CLAUDE = "MODEL_CLAUDE";
    public static final String MODEL_GPT = "MODEL_GPT";
    public static final String MODEL_GEMINI = "MODEL_GEMINI";

    // API Endpoints
    public static final String CLAUDE_API_BASE = "CLAUDE_API_BASE";
    public static final String OPENAI_API_BASE = "OPENAI_API_BASE";
    public static final String GEMINI_API_BASE = "GEMINI_API_BASE";

    // Model Selection for Pipeline
    public static final String SUMMARIZER_MODEL = "SUMMARIZER_MODEL";  // claude|gemini
    public static final String CONSOLIDATOR_MODEL = "CONSOLIDATOR_MODEL";  // gpt|gemini

    // Pipeline Configuration
    public static final String TRANSCRIPT_DIR = "TRANSCRIPT_DIR";
    public static final String OUTPUT_DIR = "OUTPUT_DIR";
    public static final String LOGS_DIR = "LOGS_DIR";
    public static final String MULTI_FILE_MODE = "MULTI_FILE_MODE";  // separate|combined
    public static final String CHUNK_SIZE = "CHUNK_SIZE";
    public static final String CHUNK_OVERLAP = "CHUNK_OVERLAP";

    // API Configuration Parameters
    public static final String API_TIMEOUT = "API_TIMEOUT";
    public static final String MAX_RETRIES = "MAX_RETRIES";
    public static final String RETRY_BACKOFF = "RETRY_BACKOFF";

    private static final Map<String, String> config = new HashMap<>();
    private static boolean initialized = false;

    static {
        initializeConfig();
    }

    /**
     * Initialize configuration from environment and .env file
     */
    private static void initializeConfig() {
        if (initialized) {
            return;
        }

        try {
            // Load from .env file if it exists
            loadFromEnvFile();
        } catch (IOException e) {
            logger.warn("Could not load .env file, falling back to environment variables", e);
        }

        // Load from environment variables (overrides .env)
        loadFromEnvironment();

        // Set defaults for missing values
        setDefaults();

        initialized = true;
        logger.info("Configuration initialized");
    }

    /**
     * Load configuration from .env file
     */
    private static void loadFromEnvFile() throws IOException {
        String envPath = ".env";
        if (Files.exists(Paths.get(envPath))) {
            Files.lines(Paths.get(envPath))
                    .filter(line -> !line.trim().isEmpty() && !line.trim().startsWith("#"))
                    .forEach(line -> {
                        String[] parts = line.split("=", 2);
                        if (parts.length == 2) {
                            config.put(parts[0].trim(), parts[1].trim());
                        }
                    });
            logger.info("Loaded configuration from .env file");
        }
    }

    /**
     * Load configuration from environment variables
     */
    private static void loadFromEnvironment() {
        config.putIfAbsent(CLAUDE_API_KEY, System.getenv(CLAUDE_API_KEY) != null ? System.getenv(CLAUDE_API_KEY) : "");
        config.putIfAbsent(OPENAI_API_KEY, System.getenv(OPENAI_API_KEY) != null ? System.getenv(OPENAI_API_KEY) : "");
        config.putIfAbsent(GEMINI_API_KEY, System.getenv(GEMINI_API_KEY) != null ? System.getenv(GEMINI_API_KEY) : "");
        config.putIfAbsent(MODEL_CLAUDE, System.getenv(MODEL_CLAUDE) != null ? System.getenv(MODEL_CLAUDE) : "");
        config.putIfAbsent(MODEL_GPT, System.getenv(MODEL_GPT) != null ? System.getenv(MODEL_GPT) : "");
        config.putIfAbsent(MODEL_GEMINI, System.getenv(MODEL_GEMINI) != null ? System.getenv(MODEL_GEMINI) : "");
        config.putIfAbsent(CLAUDE_API_BASE, System.getenv(CLAUDE_API_BASE) != null ? System.getenv(CLAUDE_API_BASE) : "");
        config.putIfAbsent(OPENAI_API_BASE, System.getenv(OPENAI_API_BASE) != null ? System.getenv(OPENAI_API_BASE) : "");
        config.putIfAbsent(GEMINI_API_BASE, System.getenv(GEMINI_API_BASE) != null ? System.getenv(GEMINI_API_BASE) : "");
        config.putIfAbsent(SUMMARIZER_MODEL, System.getenv(SUMMARIZER_MODEL) != null ? System.getenv(SUMMARIZER_MODEL) : "");
        config.putIfAbsent(CONSOLIDATOR_MODEL, System.getenv(CONSOLIDATOR_MODEL) != null ? System.getenv(CONSOLIDATOR_MODEL) : "");
        config.putIfAbsent(TRANSCRIPT_DIR, System.getenv(TRANSCRIPT_DIR) != null ? System.getenv(TRANSCRIPT_DIR) : "");
        config.putIfAbsent(OUTPUT_DIR, System.getenv(OUTPUT_DIR) != null ? System.getenv(OUTPUT_DIR) : "");
        config.putIfAbsent(LOGS_DIR, System.getenv(LOGS_DIR) != null ? System.getenv(LOGS_DIR) : "");
        config.putIfAbsent(MULTI_FILE_MODE, System.getenv(MULTI_FILE_MODE) != null ? System.getenv(MULTI_FILE_MODE) : "");
    }

    /**
     * Set default values for missing configuration
     */
    private static void setDefaults() {
        config.putIfAbsent(MODEL_CLAUDE, "claude-3-5-sonnet-20241022");
        config.putIfAbsent(MODEL_GPT, "gpt-4o");
        config.putIfAbsent(MODEL_GEMINI, "gemini-2.5-flash");
        config.putIfAbsent(CLAUDE_API_BASE, "https://api.anthropic.com/v1");
        config.putIfAbsent(OPENAI_API_BASE, "https://api.openai.com/v1");
        config.putIfAbsent(GEMINI_API_BASE, "https://generativelanguage.googleapis.com/v1beta/openai/");
        config.putIfAbsent(SUMMARIZER_MODEL, "claude");  // claude or gemini
        config.putIfAbsent(CONSOLIDATOR_MODEL, "gpt");   // gpt or gemini
        config.putIfAbsent(TRANSCRIPT_DIR, "transcripts");
        config.putIfAbsent(OUTPUT_DIR, "output");
        config.putIfAbsent(LOGS_DIR, "logs");
        config.putIfAbsent(MULTI_FILE_MODE, "separate");  // separate or combined
        config.putIfAbsent(CHUNK_SIZE, "1500");
        config.putIfAbsent(CHUNK_OVERLAP, "200");
        config.putIfAbsent(API_TIMEOUT, "60000"); // 60 seconds in milliseconds
        config.putIfAbsent(MAX_RETRIES, "3");
        config.putIfAbsent(RETRY_BACKOFF, "1000"); // 1 second in milliseconds
    }

    /**
     * Get a configuration value
     */
    public static String get(String key) {
        return config.getOrDefault(key, "");
    }

    /**
     * Get a configuration value with a default fallback
     */
    public static String get(String key, String defaultValue) {
        return config.getOrDefault(key, defaultValue);
    }

    /**
     * Get an integer configuration value
     */
    public static int getInt(String key) {
        try {
            return Integer.parseInt(config.getOrDefault(key, "0"));
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer value for {}", key);
            return 0;
        }
    }

    /**
     * Get an integer configuration value with a default fallback
     */
    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(config.getOrDefault(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer value for {}, using default: {}", key, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Validate that all required API keys are set
     * At least one summarizer model and one consolidator model must be configured
     */
    public static boolean validateApiKeys() {
        String claudeKey = get(CLAUDE_API_KEY).trim();
        String openaiKey = get(OPENAI_API_KEY).trim();
        String geminiKey = get(GEMINI_API_KEY).trim();
        String summarizer = get(SUMMARIZER_MODEL, "claude").toLowerCase().trim();
        String consolidator = get(CONSOLIDATOR_MODEL, "gpt").toLowerCase().trim();

        // Check summarizer model has required API key
        if ("claude".equals(summarizer) && claudeKey.isEmpty()) {
            logger.error("SUMMARIZER_MODEL is 'claude' but CLAUDE_API_KEY not configured");
            return false;
        }

        if ("gemini".equals(summarizer) && geminiKey.isEmpty()) {
            logger.error("SUMMARIZER_MODEL is 'gemini' but GEMINI_API_KEY not configured");
            return false;
        }

        // Check consolidator model has required API key
        if ("gpt".equals(consolidator) && openaiKey.isEmpty()) {
            logger.error("CONSOLIDATOR_MODEL is 'gpt' but OPENAI_API_KEY not configured");
            return false;
        }

        if ("gemini".equals(consolidator) && geminiKey.isEmpty()) {
            logger.error("CONSOLIDATOR_MODEL is 'gemini' but GEMINI_API_KEY not configured");
            return false;
        }

        // At least one valid key must be configured
        if (claudeKey.isEmpty() && openaiKey.isEmpty() && geminiKey.isEmpty()) {
            logger.error("No API keys configured. Please set at least one of: CLAUDE_API_KEY, OPENAI_API_KEY, or GEMINI_API_KEY");
            return false;
        }

        return true;
    }

    /**
     * Print configuration summary (without sensitive data)
     */
    public static void printSummary() {
        logger.info("=== Pipeline Configuration ===");
        logger.info("Models:");
        logger.info("  Model (Claude): {}", get(MODEL_CLAUDE));
        logger.info("  Model (GPT): {}", get(MODEL_GPT));
        logger.info("  Model (Gemini): {}", get(MODEL_GEMINI));
        logger.info("");
        logger.info("Pipeline Selection:");
        logger.info("  Summarizer Model: {}", get(SUMMARIZER_MODEL, "claude"));
        logger.info("  Consolidator Model: {}", get(CONSOLIDATOR_MODEL, "gpt"));
        logger.info("");
        logger.info("Directories:");
        logger.info("  Transcript Directory: {}", get(TRANSCRIPT_DIR));
        logger.info("  Output Directory: {}", get(OUTPUT_DIR));
        logger.info("  Logs Directory: {}", get(LOGS_DIR));
        logger.info("");
        logger.info("Processing Parameters:");
        logger.info("  Multi-File Mode: {}", get(MULTI_FILE_MODE, "separate"));
        logger.info("  Chunk Size (tokens): {}", get(CHUNK_SIZE));
        logger.info("  Chunk Overlap (tokens): {}", get(CHUNK_OVERLAP));
        logger.info("  API Timeout (ms): {}", get(API_TIMEOUT));
        logger.info("  Max Retries: {}", get(MAX_RETRIES));
        logger.info("==============================");
    }
}
