package com.transcript.pipeline.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

/**
 * Represents a semantic chunk of text extracted from a transcript.
 * A chunk is a logical unit that preserves topic boundaries.
 */
public class TextChunk implements Serializable {

    @JsonProperty("chunk_id")
    private String chunkId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("text")
    private String text;

    @JsonProperty("source_file")
    private String sourceFile;

    @JsonProperty("start_line")
    private Integer startLine;

    @JsonProperty("end_line")
    private Integer endLine;

    public TextChunk() {}

    public TextChunk(String chunkId, String title, String text) {
        this.chunkId = chunkId;
        this.title = title;
        this.text = text;
    }

    public TextChunk(String chunkId, String title, String text, String sourceFile, Integer startLine, Integer endLine) {
        this.chunkId = chunkId;
        this.title = title;
        this.text = text;
        this.sourceFile = sourceFile;
        this.startLine = startLine;
        this.endLine = endLine;
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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public Integer getStartLine() {
        return startLine;
    }

    public void setStartLine(Integer startLine) {
        this.startLine = startLine;
    }

    public Integer getEndLine() {
        return endLine;
    }

    public void setEndLine(Integer endLine) {
        this.endLine = endLine;
    }

    @Override
    public String toString() {
        return "TextChunk{" +
                "chunkId='" + chunkId + '\'' +
                ", title='" + title + '\'' +
                ", sourceFile='" + sourceFile + '\'' +
                ", textLength=" + (text != null ? text.length() : 0) +
                '}';
    }
}
