package com.transcript.pipeline.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transcript.pipeline.config.ConfigManager;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * API client for making requests to Claude (Anthropic), OpenAI, and Google Gemini APIs.
 * Handles authentication, retries, and error handling.
 */
public class ApiClient {

    private static final Logger logger = LoggerFactory.getLogger(ApiClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final OkHttpClient httpClient;
    private final String apiKey;
    private final String apiBase;
    private final String modelName;
    private final ModelType modelType;

    public enum ModelType {
        CLAUDE, OPENAI, GEMINI
    }

    /**
     * Create an API client for Claude
     */
    public static ApiClient createClaudeClient() {
        return new ApiClient(
                ConfigManager.get(ConfigManager.CLAUDE_API_KEY),
                ConfigManager.get(ConfigManager.CLAUDE_API_BASE),
                ConfigManager.get(ConfigManager.MODEL_CLAUDE),
                ModelType.CLAUDE
        );
    }

    /**
     * Create an API client for OpenAI
     */
    public static ApiClient createOpenAIClient() {
        return new ApiClient(
                ConfigManager.get(ConfigManager.OPENAI_API_KEY),
                ConfigManager.get(ConfigManager.OPENAI_API_BASE),
                ConfigManager.get(ConfigManager.MODEL_GPT),
                ModelType.OPENAI
        );
    }

    /**
     * Create an API client for Google Gemini
     */
    public static ApiClient createGeminiClient() {
        return new ApiClient(
                ConfigManager.get(ConfigManager.GEMINI_API_KEY),
                ConfigManager.get(ConfigManager.GEMINI_API_BASE),
                ConfigManager.get(ConfigManager.MODEL_GEMINI),
                ModelType.GEMINI
        );
    }

    /**
     * Constructor
     */
    public ApiClient(String apiKey, String apiBase, String modelName, ModelType modelType) {
        this.apiKey = apiKey;
        this.apiBase = apiBase;
        this.modelName = modelName;
        this.modelType = modelType;

        int timeoutMs = ConfigManager.getInt(ConfigManager.API_TIMEOUT, 60000);
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * Send a prompt to Claude and get a text response
     */
    public String sendPromptToClaude(String systemPrompt, String userPrompt) throws IOException, InterruptedException {
        String url = apiBase + "/messages";

        String jsonPayload = String.format(
                "{\"model\": \"%s\", \"max_tokens\": 4096, \"messages\": " +
                "[{\"role\": \"user\", \"content\": %s}]}",
                modelName,
                objectMapper.writeValueAsString(userPrompt)
        );

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            jsonPayload = String.format(
                    "{\"model\": \"%s\", \"max_tokens\": 4096, \"system\": %s, \"messages\": " +
                    "[{\"role\": \"user\", \"content\": %s}]}",
                    modelName,
                    objectMapper.writeValueAsString(systemPrompt),
                    objectMapper.writeValueAsString(userPrompt)
            );
        }

        return sendRequestWithRetry(url, jsonPayload, true);
    }

    /**
     * Send a prompt to OpenAI and get a text response
     */
    public String sendPromptToOpenAI(String systemPrompt, String userPrompt) throws IOException, InterruptedException {
        String url = apiBase + "/chat/completions";

        StringBuilder messagesJson = new StringBuilder();
        messagesJson.append("[");

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messagesJson.append("{\"role\": \"system\", \"content\": ")
                    .append(objectMapper.writeValueAsString(systemPrompt))
                    .append("}, ");
        }

        messagesJson.append("{\"role\": \"user\", \"content\": ")
                .append(objectMapper.writeValueAsString(userPrompt))
                .append("}]");

        String jsonPayload = String.format(
                "{\"model\": \"%s\", \"messages\": %s, \"temperature\": 0.7}",
                modelName,
                messagesJson.toString()
        );

        return sendRequestWithRetry(url, jsonPayload, ModelType.OPENAI);
    }

    /**
     * Send a prompt to Google Gemini and get a text response
     * Uses Gemini's native API format with X-goog-api-key authentication
     */
    public String sendPromptToGemini(String systemPrompt, String userPrompt) throws IOException, InterruptedException {
        // Gemini native API endpoint format: /v1beta/models/{model}:generateContent
        String url = apiBase + "/models/" + modelName + ":generateContent";

        // Combine system and user prompts into a single text
        String combinedPrompt = userPrompt;
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            combinedPrompt = systemPrompt + "\n\n" + userPrompt;
        }

        // Gemini native API uses "contents" with "parts" structure
        String jsonPayload = String.format(
                "{\"contents\": [{\"parts\": [{\"text\": %s}]}]}",
                objectMapper.writeValueAsString(combinedPrompt)
        );

        return sendRequestWithRetry(url, jsonPayload, ModelType.GEMINI);
    }

    /**
     * Send a request with automatic retry on failure
     */
    private String sendRequestWithRetry(String url, String jsonPayload, ModelType type)
            throws IOException, InterruptedException {
        int maxRetries = ConfigManager.getInt(ConfigManager.MAX_RETRIES, 3);
        long retryBackoffMs = ConfigManager.getInt(ConfigManager.RETRY_BACKOFF, 1000);

        IOException lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return sendRequest(url, jsonPayload, type);
            } catch (IOException e) {
                lastException = e;
                logger.warn("API request failed (attempt {}/{}): {}", attempt + 1, maxRetries + 1, e.getMessage());

                if (attempt < maxRetries) {
                    long backoffTime = retryBackoffMs * (long) Math.pow(2, attempt);
                    logger.info("Retrying in {} ms", backoffTime);
                    Thread.sleep(backoffTime);
                }
            }
        }

        logger.error("API request failed after {} retries", maxRetries + 1);
        throw lastException;
    }

    /**
     * Overloaded version for backwards compatibility
     */
    private String sendRequestWithRetry(String url, String jsonPayload, boolean isClaude)
            throws IOException, InterruptedException {
        ModelType type = isClaude ? ModelType.CLAUDE : ModelType.OPENAI;
        return sendRequestWithRetry(url, jsonPayload, type);
    }

    /**
     * Send an HTTP request to the API
     */
    private String sendRequest(String url, String jsonPayload, ModelType type) throws IOException {
        RequestBody body = RequestBody.create(jsonPayload, MediaType.get("application/json"));

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(body);

        if (type == ModelType.CLAUDE) {
            requestBuilder.addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("content-type", "application/json");
        } else if (type == ModelType.OPENAI) {
            requestBuilder.addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json");
        } else if (type == ModelType.GEMINI) {
            // Gemini uses X-goog-api-key header for authentication
            requestBuilder.addHeader("X-goog-api-key", apiKey)
                    .addHeader("Content-Type", "application/json");
        }

        Request request = requestBuilder.build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                logger.error("API request failed with status {}: {}", response.code(), errorBody);
                throw new IOException("API request failed: " + response.code() + " - " + errorBody);
            }

            String responseBody = response.body().string();
            return parseResponse(responseBody, type);
        }
    }

    /**
     * Overloaded version for backwards compatibility
     */
    private String sendRequest(String url, String jsonPayload, boolean isClaude) throws IOException {
        ModelType type = isClaude ? ModelType.CLAUDE : ModelType.OPENAI;
        return sendRequest(url, jsonPayload, type);
    }

    /**
     * Parse API response and extract text content
     */
    private String parseResponse(String responseBody, ModelType type) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);

        if (type == ModelType.CLAUDE) {
            // Claude API response format: content[0].text
            if (root.has("content") && root.get("content").isArray() && root.get("content").size() > 0) {
                return root.get("content").get(0).get("text").asText();
            }
        } else if (type == ModelType.OPENAI) {
            // OpenAI response format: choices[0].message.content
            if (root.has("choices") && root.get("choices").isArray() && root.get("choices").size() > 0) {
                return root.get("choices").get(0).get("message").get("content").asText();
            }
        } else if (type == ModelType.GEMINI) {
            // Gemini native API response format: candidates[0].content.parts[0].text
            if (root.has("candidates") && root.get("candidates").isArray() && root.get("candidates").size() > 0) {
                JsonNode candidate = root.get("candidates").get(0);
                if (candidate.has("content")) {
                    JsonNode content = candidate.get("content");
                    if (content.has("parts") && content.get("parts").isArray() && content.get("parts").size() > 0) {
                        return content.get("parts").get(0).get("text").asText();
                    }
                }
            }
        }

        throw new IOException("Unexpected API response format for " + type + ": " + responseBody);
    }

    /**
     * Overloaded version for backwards compatibility
     */
    private String parseResponse(String responseBody, boolean isClaude) throws IOException {
        ModelType type = isClaude ? ModelType.CLAUDE : ModelType.OPENAI;
        return parseResponse(responseBody, type);
    }

    /**
     * Get the model name
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * Get the model type
     */
    public ModelType getModelType() {
        return modelType;
    }
}
