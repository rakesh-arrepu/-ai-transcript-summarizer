package com.transcript.pipeline.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;

/**
 * Represents a summarized chunk with extracted key information.
 * This is the output of the chunk summarization stage.
 */
public class ChunkSummary implements Serializable {

    @JsonProperty("chunk_id")
    private String chunkId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("key_points")
    private List<String> keyPoints;

    @JsonProperty("workflows")
    private List<Workflow> workflows;

    @JsonProperty("definitions")
    private List<Definition> definitions;

    @JsonProperty("examples")
    private List<String> examples;

    @JsonProperty("exam_pointers")
    private List<String> examPointers;

    @JsonProperty("confidence")
    private String confidence; // "high", "medium", "low"

    public ChunkSummary() {}

    public ChunkSummary(String chunkId, String title, String summary, String confidence) {
        this.chunkId = chunkId;
        this.title = title;
        this.summary = summary;
        this.confidence = confidence;
    }

    // Nested classes
    public static class Workflow implements Serializable {
        @JsonProperty("name")
        private String name;

        @JsonProperty("steps")
        private List<String> steps;

        @JsonProperty("notes")
        private String notes;

        public Workflow() {}

        public Workflow(String name, List<String> steps) {
            this.name = name;
            this.steps = steps;
        }

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getSteps() {
            return steps;
        }

        public void setSteps(List<String> steps) {
            this.steps = steps;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    public static class Definition implements Serializable {
        @JsonProperty("term")
        private String term;

        @JsonProperty("definition")
        private String definition;

        public Definition() {}

        public Definition(String term, String definition) {
            this.term = term;
            this.definition = definition;
        }

        // Getters and Setters
        public String getTerm() {
            return term;
        }

        public void setTerm(String term) {
            this.term = term;
        }

        public String getDefinition() {
            return definition;
        }

        public void setDefinition(String definition) {
            this.definition = definition;
        }
    }

    // Getters and Setters
    public String getChunkId() {
        return chunkId;
    }

    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getKeyPoints() {
        return keyPoints;
    }

    public void setKeyPoints(List<String> keyPoints) {
        this.keyPoints = keyPoints;
    }

    public List<Workflow> getWorkflows() {
        return workflows;
    }

    public void setWorkflows(List<Workflow> workflows) {
        this.workflows = workflows;
    }

    public List<Definition> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(List<Definition> definitions) {
        this.definitions = definitions;
    }

    public List<String> getExamples() {
        return examples;
    }

    public void setExamples(List<String> examples) {
        this.examples = examples;
    }

    public List<String> getExamPointers() {
        return examPointers;
    }

    public void setExamPointers(List<String> examPointers) {
        this.examPointers = examPointers;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    @Override
    public String toString() {
        return "ChunkSummary{" +
                "chunkId='" + chunkId + '\'' +
                ", title='" + title + '\'' +
                ", confidence='" + confidence + '\'' +
                '}';
    }
}
