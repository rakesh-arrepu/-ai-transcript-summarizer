package com.transcript.pipeline.util;

import com.transcript.pipeline.config.ConfigManager;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to test API keys for Claude, OpenAI, and Gemini.
 * Performs simple test requests to verify authentication and connectivity.
 */
public class ApiKeyTester {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyTester.class);

    // Simple test prompts
    private static final String TEST_SYSTEM_PROMPT = "You are a helpful assistant. Respond concisely.";
    private static final String TEST_USER_PROMPT = "Say 'API key is working' if you can read this.";

    /**
     * Test all configured API keys
     * @return true if at least one API key is working, false otherwise
     */
    public static boolean testAllApiKeys() {
        logger.info("=== Testing API Keys ===");
        logger.info("");

        boolean claudeWorks = testClaudeApiKey();
        boolean openaiWorks = testOpenAIApiKey();
        boolean geminiWorks = testGeminiApiKey();

        logger.info("");
        logger.info("=== Test Summary ===");
        logger.info("Claude API: {}", claudeWorks ? "✓ WORKING" : "✗ FAILED");
        logger.info("OpenAI API: {}", openaiWorks ? "✓ WORKING" : "✗ FAILED");
        logger.info("Gemini API: {}", geminiWorks ? "✓ WORKING" : "✗ FAILED");
        logger.info("");

        boolean anyWorking = claudeWorks || openaiWorks || geminiWorks;

        if (anyWorking) {
            logger.info("✓ At least one API key is working!");

            // Validate pipeline configuration
            String summarizerModel = ConfigManager.get(ConfigManager.SUMMARIZER_MODEL, "claude").toLowerCase();
            String consolidatorModel = ConfigManager.get(ConfigManager.CONSOLIDATOR_MODEL, "gpt").toLowerCase();

            logger.info("");
            logger.info("=== Pipeline Configuration ===");
            logger.info("Summarizer Model: {}", summarizerModel);
            logger.info("Consolidator Model: {}", consolidatorModel);

            boolean pipelineReady = true;

            // Check if required models are available
            if ("claude".equals(summarizerModel) && !claudeWorks) {
                logger.error("✗ SUMMARIZER_MODEL is set to 'claude' but Claude API key is not working!");
                pipelineReady = false;
            }
            if ("gemini".equals(summarizerModel) && !geminiWorks) {
                logger.error("✗ SUMMARIZER_MODEL is set to 'gemini' but Gemini API key is not working!");
                pipelineReady = false;
            }
            if ("gpt".equals(consolidatorModel) && !openaiWorks) {
                logger.error("✗ CONSOLIDATOR_MODEL is set to 'gpt' but OpenAI API key is not working!");
                pipelineReady = false;
            }
            if ("gemini".equals(consolidatorModel) && !geminiWorks) {
                logger.error("✗ CONSOLIDATOR_MODEL is set to 'gemini' but Gemini API key is not working!");
                pipelineReady = false;
            }

            if (pipelineReady) {
                logger.info("✓ Pipeline is properly configured and ready to use!");
            } else {
                logger.error("✗ Pipeline configuration has issues. Please fix the API keys or update model selection.");
                return false;
            }
        } else {
            logger.error("✗ No API keys are working. Please check your configuration.");
        }

        return anyWorking;
    }

    /**
     * Test Claude API key
     * @return true if the API key is valid and working
     */
    public static boolean testClaudeApiKey() {
        String apiKey = ConfigManager.get(ConfigManager.CLAUDE_API_KEY);

        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("Claude API key not configured. Skipping test.");
            return false;
        }

        logger.info("Testing Claude API key...");

        try {
            ApiClient client = ApiClient.createClaudeClient();
            String response = client.sendPromptToClaude(TEST_SYSTEM_PROMPT, TEST_USER_PROMPT);

            if (response != null && !response.trim().isEmpty()) {
                logger.info("✓ Claude API key is valid and working!");
                logger.debug("Response: {}", response.substring(0, Math.min(response.length(), 100)));
                return true;
            } else {
                logger.error("✗ Claude API returned empty response");
                return false;
            }
        } catch (IOException e) {
            logger.error("✗ Claude API test failed: {}", e.getMessage());
            if (e.getMessage().contains("401")) {
                logger.error("  → Invalid API key. Please check your CLAUDE_API_KEY.");
            } else if (e.getMessage().contains("403")) {
                logger.error("  → Access forbidden. Check API key permissions.");
            } else if (e.getMessage().contains("429")) {
                logger.error("  → Rate limit exceeded. Please wait and try again.");
            } else if (e.getMessage().contains("timeout")) {
                logger.error("  → Request timed out. Check your internet connection.");
            }
            return false;
        } catch (InterruptedException e) {
            logger.error("✗ Claude API test interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Test OpenAI API key
     * @return true if the API key is valid and working
     */
    public static boolean testOpenAIApiKey() {
        String apiKey = ConfigManager.get(ConfigManager.OPENAI_API_KEY);

        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("OpenAI API key not configured. Skipping test.");
            return false;
        }

        logger.info("Testing OpenAI API key...");

        try {
            ApiClient client = ApiClient.createOpenAIClient();
            String response = client.sendPromptToOpenAI(TEST_SYSTEM_PROMPT, TEST_USER_PROMPT);

            if (response != null && !response.trim().isEmpty()) {
                logger.info("✓ OpenAI API key is valid and working!");
                logger.debug("Response: {}", response.substring(0, Math.min(response.length(), 100)));
                return true;
            } else {
                logger.error("✗ OpenAI API returned empty response");
                return false;
            }
        } catch (IOException e) {
            logger.error("✗ OpenAI API test failed: {}", e.getMessage());
            if (e.getMessage().contains("401")) {
                logger.error("  → Invalid API key. Please check your OPENAI_API_KEY.");
            } else if (e.getMessage().contains("403")) {
                logger.error("  → Access forbidden. Check API key permissions.");
            } else if (e.getMessage().contains("429")) {
                logger.error("  → Rate limit exceeded. Please wait and try again.");
            } else if (e.getMessage().contains("timeout")) {
                logger.error("  → Request timed out. Check your internet connection.");
            }
            return false;
        } catch (InterruptedException e) {
            logger.error("✗ OpenAI API test interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Test Gemini API key
     * @return true if the API key is valid and working
     */
    public static boolean testGeminiApiKey() {
        String apiKey = ConfigManager.get(ConfigManager.GEMINI_API_KEY);

        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("Gemini API key not configured. Skipping test.");
            return false;
        }

        logger.info("Testing Gemini API key...");

        try {
            ApiClient client = ApiClient.createGeminiClient();
            String response = client.sendPromptToGemini(TEST_SYSTEM_PROMPT, TEST_USER_PROMPT);

            if (response != null && !response.trim().isEmpty()) {
                logger.info("✓ Gemini API key is valid and working!");
                logger.debug("Response: {}", response.substring(0, Math.min(response.length(), 100)));
                return true;
            } else {
                logger.error("✗ Gemini API returned empty response");
                return false;
            }
        } catch (IOException e) {
            logger.error("✗ Gemini API test failed: {}", e.getMessage());
            if (e.getMessage().contains("401")) {
                logger.error("  → Invalid API key. Please check your GEMINI_API_KEY.");
            } else if (e.getMessage().contains("403")) {
                logger.error("  → Access forbidden. Check API key permissions.");
            } else if (e.getMessage().contains("429")) {
                logger.error("  → Rate limit exceeded. Please wait and try again.");
            } else if (e.getMessage().contains("timeout")) {
                logger.error("  → Request timed out. Check your internet connection.");
            }
            return false;
        } catch (InterruptedException e) {
            logger.error("✗ Gemini API test interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Main method for standalone testing
     */
    public static void main(String[] args) {
        logger.info("API Key Tester - Standalone Mode");
        logger.info("");

        boolean success = testAllApiKeys();

        System.exit(success ? 0 : 1);
    }
}
