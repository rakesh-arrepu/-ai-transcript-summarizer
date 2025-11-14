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

    public static class LessonState implements Serializable {
        @JsonProperty("filename")
        private String filename;

        @JsonProperty("chunks")
        private String chunksStatus; // "pending", "done"

        @JsonProperty("summaries")
        private String summariesStatus; // "pending", "in_progress", "done"

        @JsonProperty("confidence_issues")
        private List<String> confidenceIssues; // list of low-confidence chunk IDs

        @JsonProperty("summary_count")
        private Integer summaryCount;

        @JsonProperty("last_updated")
        private LocalDateTime lastUpdated;

        public LessonState() {}

        public LessonState(String filename) {
            this.filename = filename;
            this.chunksStatus = "pending";
            this.summariesStatus = "pending";
            this.lastUpdated = LocalDateTime.now();
        }

        // Getters and Setters
        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
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

        public LocalDateTime getLastUpdated() {
            return lastUpdated;
        }

        public void setLastUpdated(LocalDateTime lastUpdated) {
            this.lastUpdated = lastUpdated;
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
