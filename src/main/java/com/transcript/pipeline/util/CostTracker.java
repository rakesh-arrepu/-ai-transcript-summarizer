package com.transcript.pipeline.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for tracking and estimating API costs.
 * Provides cost estimation based on token counts and model pricing.
 */
public class CostTracker {

    // Pricing per 1M tokens (as of November 2024)
    // Source: Anthropic, OpenAI, and Google pricing pages
    private static final Map<String, Double> PRICING = new HashMap<>();

    static {
        // Claude pricing (per 1M tokens)
        PRICING.put("claude-input", 0.003);   // $3 per 1M input tokens
        PRICING.put("claude-output", 0.015);  // $15 per 1M output tokens

        // OpenAI GPT-4o pricing (per 1M tokens)
        PRICING.put("gpt-input", 0.0025);     // $2.50 per 1M input tokens
        PRICING.put("gpt-output", 0.010);     // $10 per 1M output tokens

        // Google Gemini pricing (per 1M tokens)
        PRICING.put("gemini-input", 0.00015); // $0.15 per 1M input tokens
        PRICING.put("gemini-output", 0.0006); // $0.60 per 1M output tokens
    }

    private double totalCost = 0.0;
    private int totalInputTokens = 0;
    private int totalOutputTokens = 0;
    private Map<String, Double> costByStage = new HashMap<>();

    /**
     * Estimate cost for a given number of tokens and model
     */
    public static double estimateCost(int tokens, String model, boolean isOutput) {
        String key = model.toLowerCase() + "-" + (isOutput ? "output" : "input");
        Double pricePerMillion = PRICING.get(key);

        if (pricePerMillion == null) {
            // Default to Claude pricing if model not found
            pricePerMillion = isOutput ? 0.015 : 0.003;
        }

        return (tokens / 1_000_000.0) * pricePerMillion;
    }

    /**
     * Estimate total cost for processing a transcript
     */
    public static CostEstimate estimateTranscriptCost(
        int transcriptTokens,
        String summarizerModel,
        String consolidatorModel
    ) {
        CostEstimate estimate = new CostEstimate();

        // Stage 1: Chunking (local, no cost)
        estimate.chunkingCost = 0.0;

        // Stage 2: Summarization
        // Assume each chunk (1500 tokens) produces ~300 tokens of summary
        int numChunks = (int) Math.ceil(transcriptTokens / 1500.0);
        int summarizerInputTokens = transcriptTokens + (numChunks * 200); // Include system prompts
        int summarizerOutputTokens = numChunks * 300;

        estimate.summarizationCost = estimateCost(summarizerInputTokens, summarizerModel, false)
            + estimateCost(summarizerOutputTokens, summarizerModel, true);

        // Stage 3: Consolidation
        // Input = all summaries (~300 tokens per chunk)
        // Output = master notes (~2000 tokens for typical lecture)
        int consolidatorInputTokens = (numChunks * 300) + 500; // Include prompt
        int consolidatorOutputTokens = 2000;

        estimate.consolidationCost = estimateCost(consolidatorInputTokens, consolidatorModel, false)
            + estimateCost(consolidatorOutputTokens, consolidatorModel, true);

        // Stage 4: Exam Materials
        // Flashcards: ~3000 tokens output
        // Practice questions: ~2000 tokens output
        // Quick revision: ~800 tokens output
        int examMaterialsInputTokens = 2000 * 3; // Master notes repeated 3 times
        int examMaterialsOutputTokens = 3000 + 2000 + 800;

        estimate.examMaterialsCost = estimateCost(examMaterialsInputTokens, consolidatorModel, false)
            + estimateCost(examMaterialsOutputTokens, consolidatorModel, true);

        estimate.totalCost = estimate.chunkingCost + estimate.summarizationCost
            + estimate.consolidationCost + estimate.examMaterialsCost;

        estimate.numChunks = numChunks;
        estimate.summarizerModel = summarizerModel;
        estimate.consolidatorModel = consolidatorModel;

        return estimate;
    }

    /**
     * Track actual cost for an API call
     */
    public void recordCost(String stage, int inputTokens, int outputTokens, String model) {
        double cost = estimateCost(inputTokens, model, false)
            + estimateCost(outputTokens, model, true);

        totalCost += cost;
        totalInputTokens += inputTokens;
        totalOutputTokens += outputTokens;

        costByStage.merge(stage, cost, Double::sum);
    }

    /**
     * Get total cost so far
     */
    public double getTotalCost() {
        return totalCost;
    }

    /**
     * Get cost breakdown by stage
     */
    public Map<String, Double> getCostByStage() {
        return new HashMap<>(costByStage);
    }

    /**
     * Get total tokens processed
     */
    public int getTotalTokens() {
        return totalInputTokens + totalOutputTokens;
    }

    /**
     * Generate a cost summary report
     */
    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append(ConsoleColors.colorize("═══════════════════════════════════════════════", ConsoleColors.BOLD_CYAN)).append("\n");
        sb.append(ConsoleColors.colorize("           COST SUMMARY REPORT", ConsoleColors.BOLD_CYAN)).append("\n");
        sb.append(ConsoleColors.colorize("═══════════════════════════════════════════════", ConsoleColors.BOLD_CYAN)).append("\n");
        sb.append("\n");

        sb.append(String.format("Total Input Tokens:  %s\n",
            ConsoleColors.colorize(String.format("%,d", totalInputTokens), ConsoleColors.CYAN)));
        sb.append(String.format("Total Output Tokens: %s\n",
            ConsoleColors.colorize(String.format("%,d", totalOutputTokens), ConsoleColors.CYAN)));
        sb.append(String.format("Total Tokens:        %s\n",
            ConsoleColors.colorize(String.format("%,d", getTotalTokens()), ConsoleColors.BOLD_CYAN)));
        sb.append("\n");

        sb.append(ConsoleColors.colorize("Cost by Stage:", ConsoleColors.BOLD_WHITE)).append("\n");
        for (Map.Entry<String, Double> entry : costByStage.entrySet()) {
            sb.append(String.format("  %-20s %s\n",
                entry.getKey() + ":",
                ConsoleColors.formatCost(entry.getValue())));
        }

        sb.append("\n");
        sb.append(String.format("TOTAL COST: %s\n",
            ConsoleColors.colorize(String.format("$%.4f", totalCost), ConsoleColors.BOLD_GREEN)));
        sb.append("\n");

        return sb.toString();
    }

    /**
     * Cost estimate result
     */
    public static class CostEstimate {
        public double chunkingCost;
        public double summarizationCost;
        public double consolidationCost;
        public double examMaterialsCost;
        public double totalCost;
        public int numChunks;
        public String summarizerModel;
        public String consolidatorModel;

        public String formatEstimate() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n");
            sb.append(ConsoleColors.colorize("═══════════════════════════════════════════════", ConsoleColors.BOLD_YELLOW)).append("\n");
            sb.append(ConsoleColors.colorize("           COST ESTIMATE", ConsoleColors.BOLD_YELLOW)).append("\n");
            sb.append(ConsoleColors.colorize("═══════════════════════════════════════════════", ConsoleColors.BOLD_YELLOW)).append("\n");
            sb.append("\n");

            sb.append(String.format("Configuration:\n"));
            sb.append(String.format("  Summarizer:   %s\n",
                ConsoleColors.colorize(summarizerModel.toUpperCase(), ConsoleColors.CYAN)));
            sb.append(String.format("  Consolidator: %s\n",
                ConsoleColors.colorize(consolidatorModel.toUpperCase(), ConsoleColors.CYAN)));
            sb.append(String.format("  Chunks:       %s\n",
                ConsoleColors.colorize(String.valueOf(numChunks), ConsoleColors.CYAN)));
            sb.append("\n");

            sb.append(String.format("Estimated Costs:\n"));
            sb.append(String.format("  Chunking:        %s (local)\n",
                ConsoleColors.formatCost(chunkingCost)));
            sb.append(String.format("  Summarization:   %s\n",
                ConsoleColors.formatCost(summarizationCost)));
            sb.append(String.format("  Consolidation:   %s\n",
                ConsoleColors.formatCost(consolidationCost)));
            sb.append(String.format("  Exam Materials:  %s\n",
                ConsoleColors.formatCost(examMaterialsCost)));
            sb.append("\n");

            String costColor = totalCost > 5.0 ? ConsoleColors.BOLD_RED :
                             (totalCost > 2.0 ? ConsoleColors.BOLD_YELLOW : ConsoleColors.BOLD_GREEN);
            sb.append(String.format("ESTIMATED TOTAL: %s\n",
                ConsoleColors.colorize(String.format("$%.4f", totalCost), costColor)));
            sb.append("\n");

            // Add cost-saving tip if using expensive models
            if (summarizerModel.equals("claude") && consolidatorModel.equals("gpt")) {
                sb.append(ConsoleColors.colorize("💡 TIP: Switch CONSOLIDATOR_MODEL=gemini to save ~77% ($" +
                    String.format("%.2f", totalCost * 0.77) + " → $" +
                    String.format("%.2f", totalCost * 0.23) + ")", ConsoleColors.YELLOW)).append("\n");
                sb.append("\n");
            }

            return sb.toString();
        }
    }
}
