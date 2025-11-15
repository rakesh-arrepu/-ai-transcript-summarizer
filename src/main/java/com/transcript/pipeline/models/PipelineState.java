package com.transcript.pipeline.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks the state of the pipeline execution.
 * Allows resuming interrupted pipeline runs.
 */
public class PipelineState implements Serializable {

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("lessons")
    private Map<String, LessonState> lessons = new HashMap<>();

    @JsonProperty("overall_status")
    private String overallStatus; // "pending", "in_progress", "completed", "failed"

    @JsonProperty("error_log")
    private List<String> errorLog;

    public PipelineState() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Stage completion status
     */
    public enum StageStatus {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }

    public static class LessonState implements Serializable {
        @JsonProperty("filename")
        private String filename;

        @JsonProperty("chunking_status")
        private StageStatus chunkingStatus;

        @JsonProperty("summarization_status")
        private StageStatus summarizationStatus;

        @JsonProperty("consolidation_status")
        private StageStatus consolidationStatus;

        @JsonProperty("exam_materials_status")
        private StageStatus examMaterialsStatus;

        @JsonProperty("chunks")
        private String chunksStatus; // "pending", "done" - for backward compatibility

        @JsonProperty("summaries")
        private String summariesStatus; // "pending", "in_progress", "done" - for backward compatibility

        @JsonProperty("confidence_issues")
        private List<String> confidenceIssues; // list of low-confidence chunk IDs

        @JsonProperty("summary_count")
        private Integer summaryCount;

        @JsonProperty("chunks_path")
        private String chunksPath;

        @JsonProperty("summaries_path")
        private String summariesPath;

        @JsonProperty("master_notes_path")
        private String masterNotesPath;

        @JsonProperty("exam_materials_path")
        private String examMaterialsPath;

        @JsonProperty("last_updated")
        private LocalDateTime lastUpdated;

        @JsonProperty("error_message")
        private String errorMessage;

        public LessonState() {
            this.chunkingStatus = StageStatus.NOT_STARTED;
            this.summarizationStatus = StageStatus.NOT_STARTED;
            this.consolidationStatus = StageStatus.NOT_STARTED;
            this.examMaterialsStatus = StageStatus.NOT_STARTED;
        }

        public LessonState(String filename) {
            this();
            this.filename = filename;
            this.chunksStatus = "pending";
            this.summariesStatus = "pending";
            this.lastUpdated = LocalDateTime.now();
        }

        /**
         * Check if pipeline can be resumed
         */
        public boolean canResume() {
            return hasCompletedStages() && !allStagesCompleted();
        }

        /**
         * Check if any stages are completed
         */
        public boolean hasCompletedStages() {
            return chunkingStatus == StageStatus.COMPLETED ||
                   summarizationStatus == StageStatus.COMPLETED ||
                   consolidationStatus == StageStatus.COMPLETED ||
                   examMaterialsStatus == StageStatus.COMPLETED;
        }

        /**
         * Check if all stages are completed
         */
        public boolean allStagesCompleted() {
            return chunkingStatus == StageStatus.COMPLETED &&
                   summarizationStatus == StageStatus.COMPLETED &&
                   consolidationStatus == StageStatus.COMPLETED &&
                   examMaterialsStatus == StageStatus.COMPLETED;
        }

        /**
         * Get next stage to execute
         */
        public String getNextStage() {
            if (chunkingStatus != StageStatus.COMPLETED) return "chunking";
            if (summarizationStatus != StageStatus.COMPLETED) return "summarization";
            if (consolidationStatus != StageStatus.COMPLETED) return "consolidation";
            if (examMaterialsStatus != StageStatus.COMPLETED) return "exam_materials";
            return "none";
        }

        // Getters and Setters
        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public StageStatus getChunkingStatus() {
            return chunkingStatus;
        }

        public void setChunkingStatus(StageStatus chunkingStatus) {
            this.chunkingStatus = chunkingStatus;
            this.lastUpdated = LocalDateTime.now();
        }

        public StageStatus getSummarizationStatus() {
            return summarizationStatus;
        }

        public void setSummarizationStatus(StageStatus summarizationStatus) {
            this.summarizationStatus = summarizationStatus;
            this.lastUpdated = LocalDateTime.now();
        }

        public StageStatus getConsolidationStatus() {
            return consolidationStatus;
        }

        public void setConsolidationStatus(StageStatus consolidationStatus) {
            this.consolidationStatus = consolidationStatus;
            this.lastUpdated = LocalDateTime.now();
        }

        public StageStatus getExamMaterialsStatus() {
            return examMaterialsStatus;
        }

        public void setExamMaterialsStatus(StageStatus examMaterialsStatus) {
            this.examMaterialsStatus = examMaterialsStatus;
            this.lastUpdated = LocalDateTime.now();
        }

        public String getChunksStatus() {
            return chunksStatus;
        }

        public void setChunksStatus(String chunksStatus) {
            this.chunksStatus = chunksStatus;
            this.lastUpdated = LocalDateTime.now();
        }

        public String getSummariesStatus() {
            return summariesStatus;
        }

        public void setSummariesStatus(String summariesStatus) {
            this.summariesStatus = summariesStatus;
            this.lastUpdated = LocalDateTime.now();
        }

        public List<String> getConfidenceIssues() {
            return confidenceIssues;
        }

        public void setConfidenceIssues(List<String> confidenceIssues) {
            this.confidenceIssues = confidenceIssues;
        }

        public Integer getSummaryCount() {
            return summaryCount;
        }

        public void setSummaryCount(Integer summaryCount) {
            this.summaryCount = summaryCount;
        }

        public String getChunksPath() {
            return chunksPath;
        }

        public void setChunksPath(String chunksPath) {
            this.chunksPath = chunksPath;
        }

        public String getSummariesPath() {
            return summariesPath;
        }

        public void setSummariesPath(String summariesPath) {
            this.summariesPath = summariesPath;
        }

        public String getMasterNotesPath() {
            return masterNotesPath;
        }

        public void setMasterNotesPath(String masterNotesPath) {
            this.masterNotesPath = masterNotesPath;
        }

        public String getExamMaterialsPath() {
            return examMaterialsPath;
        }

        public void setExamMaterialsPath(String examMaterialsPath) {
            this.examMaterialsPath = examMaterialsPath;
        }

        public LocalDateTime getLastUpdated() {
            return lastUpdated;
        }

        public void setLastUpdated(LocalDateTime lastUpdated) {
            this.lastUpdated = lastUpdated;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    // Getters and Setters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, LessonState> getLessons() {
        return lessons;
    }

    public void setLessons(Map<String, LessonState> lessons) {
        this.lessons = lessons;
    }

    public String getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
    }

    public List<String> getErrorLog() {
        return errorLog;
    }

    public void setErrorLog(List<String> errorLog) {
        this.errorLog = errorLog;
    }

    public void addLesson(String lessonKey, LessonState state) {
        this.lessons.put(lessonKey, state);
    }

    public LessonState getLesson(String lessonKey) {
        return this.lessons.get(lessonKey);
    }
}
