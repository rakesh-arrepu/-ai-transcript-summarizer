package com.transcript.pipeline.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of a batch processing operation.
 * Tracks successful and failed files, timing, and costs.
 */
public class BatchResult implements Serializable {

    @JsonProperty("batch_start_time")
    private LocalDateTime batchStartTime;

    @JsonProperty("batch_end_time")
    private LocalDateTime batchEndTime;

    @JsonProperty("total_files")
    private int totalFiles;

    @JsonProperty("successful_files")
    private List<FileResult> successfulFiles;

    @JsonProperty("failed_files")
    private List<FileResult> failedFiles;

    @JsonProperty("total_cost")
    private double totalCost;

    @JsonProperty("total_duration_ms")
    private long totalDurationMs;

    public BatchResult() {
        this.successfulFiles = new ArrayList<>();
        this.failedFiles = new ArrayList<>();
        this.batchStartTime = LocalDateTime.now();
        this.totalCost = 0.0;
    }

    /**
     * Result for individual file
     */
    public static class FileResult implements Serializable {
        @JsonProperty("filename")
        private String filename;

        @JsonProperty("status")
        private String status; // "success" or "failed"

        @JsonProperty("duration_ms")
        private long durationMs;

        @JsonProperty("cost")
        private double cost;

        @JsonProperty("error_message")
        private String errorMessage;

        @JsonProperty("chunks_created")
        private Integer chunksCreated;

        @JsonProperty("summaries_created")
        private Integer summariesCreated;

        @JsonProperty("outputs_generated")
        private List<String> outputsGenerated;

        public FileResult(String filename) {
            this.filename = filename;
            this.outputsGenerated = new ArrayList<>();
        }

        // Getters and setters
        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public void setDurationMs(long durationMs) {
            this.durationMs = durationMs;
        }

        public double getCost() {
            return cost;
        }

        public void setCost(double cost) {
            this.cost = cost;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public Integer getChunksCreated() {
            return chunksCreated;
        }

        public void setChunksCreated(Integer chunksCreated) {
            this.chunksCreated = chunksCreated;
        }

        public Integer getSummariesCreated() {
            return summariesCreated;
        }

        public void setSummariesCreated(Integer summariesCreated) {
            this.summariesCreated = summariesCreated;
        }

        public List<String> getOutputsGenerated() {
            return outputsGenerated;
        }

        public void setOutputsGenerated(List<String> outputsGenerated) {
            this.outputsGenerated = outputsGenerated;
        }

        public void addOutput(String output) {
            this.outputsGenerated.add(output);
        }
    }

    /**
     * Add a successful file result
     */
    public void addSuccess(File file, FileResult result) {
        result.setStatus("success");
        successfulFiles.add(result);
        totalCost += result.getCost();
    }

    /**
     * Add a failed file result
     */
    public void addFailure(File file, Exception e) {
        FileResult result = new FileResult(file.getName());
        result.setStatus("failed");
        result.setErrorMessage(e.getMessage());
        failedFiles.add(result);
    }

    /**
     * Mark batch as complete
     */
    public void complete() {
        this.batchEndTime = LocalDateTime.now();
        this.totalFiles = successfulFiles.size() + failedFiles.size();
        this.totalDurationMs = java.time.Duration.between(batchStartTime, batchEndTime).toMillis();
    }

    /**
     * Get success rate
     */
    public double getSuccessRate() {
        if (totalFiles == 0) return 0.0;
        return (successfulFiles.size() * 100.0) / totalFiles;
    }

    /**
     * Generate a summary report
     */
    public String generateSummaryReport() {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        sb.append("\n");
        sb.append("═══════════════════════════════════════════════\n");
        sb.append("           BATCH PROCESSING REPORT\n");
        sb.append("═══════════════════════════════════════════════\n");
        sb.append("\n");

        sb.append(String.format("Started:  %s\n", batchStartTime.format(formatter)));
        sb.append(String.format("Ended:    %s\n", batchEndTime.format(formatter)));
        sb.append(String.format("Duration: %s\n", formatDuration(totalDurationMs)));
        sb.append("\n");

        sb.append(String.format("Total Files:      %d\n", totalFiles));
        sb.append(String.format("Successful:       %d (%.1f%%)\n",
            successfulFiles.size(), getSuccessRate()));
        sb.append(String.format("Failed:           %d\n", failedFiles.size()));
        sb.append("\n");

        sb.append(String.format("Total Cost:       $%.4f\n", totalCost));
        if (!successfulFiles.isEmpty()) {
            sb.append(String.format("Average Cost/File: $%.4f\n",
                totalCost / successfulFiles.size()));
        }
        sb.append("\n");

        if (!successfulFiles.isEmpty()) {
            sb.append("Successful Files:\n");
            for (FileResult result : successfulFiles) {
                sb.append(String.format("  ✓ %-30s %8s  $%.4f\n",
                    result.getFilename(),
                    formatDuration(result.getDurationMs()),
                    result.getCost()));
            }
            sb.append("\n");
        }

        if (!failedFiles.isEmpty()) {
            sb.append("Failed Files:\n");
            for (FileResult result : failedFiles) {
                sb.append(String.format("  ✗ %-30s %s\n",
                    result.getFilename(),
                    result.getErrorMessage() != null ? result.getErrorMessage() : "Unknown error"));
            }
            sb.append("\n");
        }

        sb.append("═══════════════════════════════════════════════\n");

        return sb.toString();
    }

    /**
     * Generate CSV report
     */
    public String generateCsvReport() {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("Filename,Status,Duration(ms),Cost($),Chunks,Summaries,Error\n");

        // Successful files
        for (FileResult result : successfulFiles) {
            sb.append(String.format("\"%s\",%s,%d,%.4f,%d,%d,\n",
                result.getFilename(),
                result.getStatus(),
                result.getDurationMs(),
                result.getCost(),
                result.getChunksCreated() != null ? result.getChunksCreated() : 0,
                result.getSummariesCreated() != null ? result.getSummariesCreated() : 0));
        }

        // Failed files
        for (FileResult result : failedFiles) {
            sb.append(String.format("\"%s\",%s,0,0.0,0,0,\"%s\"\n",
                result.getFilename(),
                result.getStatus(),
                result.getErrorMessage() != null ? result.getErrorMessage().replace("\"", "\"\"") : ""));
        }

        return sb.toString();
    }

    /**
     * Format duration in human-readable format
     */
    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    // Getters and setters
    public LocalDateTime getBatchStartTime() {
        return batchStartTime;
    }

    public void setBatchStartTime(LocalDateTime batchStartTime) {
        this.batchStartTime = batchStartTime;
    }

    public LocalDateTime getBatchEndTime() {
        return batchEndTime;
    }

    public void setBatchEndTime(LocalDateTime batchEndTime) {
        this.batchEndTime = batchEndTime;
    }

    public int getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }

    public List<FileResult> getSuccessfulFiles() {
        return successfulFiles;
    }

    public void setSuccessfulFiles(List<FileResult> successfulFiles) {
        this.successfulFiles = successfulFiles;
    }

    public List<FileResult> getFailedFiles() {
        return failedFiles;
    }

    public void setFailedFiles(List<FileResult> failedFiles) {
        this.failedFiles = failedFiles;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public long getTotalDurationMs() {
        return totalDurationMs;
    }

    public void setTotalDurationMs(long totalDurationMs) {
        this.totalDurationMs = totalDurationMs;
    }
}
