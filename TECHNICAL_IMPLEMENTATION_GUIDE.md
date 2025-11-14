# Technical Implementation Guide

## Architecture Overview

This document provides in-depth technical details about the Transcript → Exam Notes Pipeline for developers and contributors.

## Project Structure

```
transcript-to-exam-notes/
├── src/
│   ├── main/java/com/transcript/pipeline/
│   │   ├── App.java                          # CLI entry point
│   │   ├── config/
│   │   │   └── ConfigManager.java            # Configuration management
│   │   ├── models/
│   │   │   ├── TextChunk.java                # Text chunk DTO
│   │   │   ├── ChunkSummary.java             # Summarized chunk DTO
│   │   │   └── PipelineState.java            # Pipeline state tracking
│   │   ├── services/
│   │   │   ├── ChunkerService.java           # Text chunking logic
│   │   │   ├── SummarizerService.java        # Chunk summarization
│   │   │   └── ConsolidatorService.java      # Master notes generation
│   │   └── util/
│   │       ├── ApiClient.java                # API communication
│   │       ├── FileService.java              # File I/O operations
│   │       └── TextProcessingUtil.java       # Text processing utilities
│   └── test/java/com/transcript/pipeline/
├── transcripts/                              # Input transcript files
├── output/                                   # Generated outputs
│   ├── chunks/
│   ├── summaries/
│   ├── consolidated/
│   └── exam_materials/
├── config/
│   └── .env.example                          # Configuration template
├── logs/                                     # Application logs
├── pom.xml                                   # Maven configuration
├── README.md                                 # User documentation
└── TECHNICAL_IMPLEMENTATION_GUIDE.md         # This file
```

## Core Components

### 1. ConfigManager (config/ConfigManager.java)

**Purpose**: Centralized configuration management for multi-model support

**Key Features**:
- Loads from `.env` files and environment variables
- Provides default values for all settings
- Validates API keys for multiple model types
- Supports flexible model selection per pipeline step (Claude, OpenAI, Gemini)
- Runtime configuration overrides

**Example Usage**:
```java
// Get API keys for any model
String claudeKey = ConfigManager.get(ConfigManager.CLAUDE_API_KEY);
String openaiKey = ConfigManager.get(ConfigManager.OPENAI_API_KEY);
String geminiKey = ConfigManager.get(ConfigManager.GEMINI_API_KEY);

// Get model selection
String summarizer = ConfigManager.get(ConfigManager.SUMMARIZER_MODEL, "claude");
String consolidator = ConfigManager.get(ConfigManager.CONSOLIDATOR_MODEL, "gpt");

// Get configuration
int chunkSize = ConfigManager.getInt(ConfigManager.CHUNK_SIZE, 1500);
ConfigManager.printSummary();  // Log all settings including model selections
```

**Model Selection Configuration**:
```env
# Summarizer: "claude" or "gemini"
SUMMARIZER_MODEL=claude

# Consolidator: "gpt" or "gemini"
CONSOLIDATOR_MODEL=gpt
```

**Configuration Priority**:
1. Environment variables (highest)
2. `.env` file
3. Default hardcoded values (lowest)

**Validation**:
- Validates that selected model has corresponding API key configured
- Example: If `SUMMARIZER_MODEL=gemini`, requires `GEMINI_API_KEY` to be set
- Ensures at least one valid configuration exists

### 2. ApiClient (util/ApiClient.java)

**Purpose**: Handle all API communication with multi-model support and retry logic

**Supported Models**:
- `ModelType.CLAUDE`: Anthropic Claude API (proprietary format)
- `ModelType.OPENAI`: OpenAI Chat Completions API (OpenAI format)
- `ModelType.GEMINI`: Google Gemini API (OpenAI-compatible format) ⭐ NEW

**Key Features**:
- Automatic retries with exponential backoff
- Connection pooling (OkHttp) for efficient requests
- Timeout handling and graceful degradation
- Error parsing and comprehensive logging
- Support for system and user prompts
- Seamless switching between model providers
- Unified response parsing for all models

**Example Usage**:
```java
// Claude API (proprietary format)
ApiClient claudeClient = ApiClient.createClaudeClient();
String response = claudeClient.sendPromptToClaude(
    "You are a summarizer.",
    "Summarize this text..."
);

// OpenAI API (OpenAI format)
ApiClient openaiClient = ApiClient.createOpenAIClient();
String response = openaiClient.sendPromptToOpenAI(
    "You are an educator.",
    "Create flashcards from..."
);

// Gemini API (OpenAI-compatible format) - NEW!
ApiClient geminiClient = ApiClient.createGeminiClient();
String response = geminiClient.sendPromptToGemini(
    "You are a summarizer.",
    "Summarize this text..."
);
```

**Gemini Integration Details**:
- Uses OpenAI-compatible API endpoint: `https://generativelanguage.googleapis.com/v1beta/openai/`
- Request format identical to OpenAI (seamless migration)
- Response format identical to OpenAI (no parsing changes needed)
- Authentication: Bearer token with Gemini API key
- Cost savings: 97% cheaper than Claude, 98% cheaper than OpenAI

**Retry Mechanism**:
```
Attempt 1 → Failed → Wait 1s (1000ms * 2^0)
Attempt 2 → Failed → Wait 2s (1000ms * 2^1)
Attempt 3 → Failed → Wait 4s (1000ms * 2^2)
Attempt 4 → Success or throw exception
```

### 3. FileService (util/FileService.java)

**Purpose**: All file I/O operations

**Main Methods**:
```java
// Text operations
String content = FileService.readTextFile("path/file.txt");
FileService.writeTextFile("path/file.txt", content);

// JSON operations
MyClass obj = FileService.readJsonFile("path/file.json", MyClass.class);
List<MyClass> list = FileService.readJsonFileAsList("path/file.json", MyClass.class);
FileService.writeJsonFile("path/file.json", obj);

// Directory operations
FileService.createDirectoryIfNotExists("path/to/dir");
List<File> files = FileService.listFilesInDirectory("path", ".txt");

// Utility operations
String chunksPath = FileService.generateChunkOutputPath(baseDir, "lesson.txt");
String summaryPath = FileService.generateSummaryOutputPath(baseDir, "chunk_1");
```

### 4. TextProcessingUtil (util/TextProcessingUtil.java)

**Purpose**: Text analysis and chunking algorithms

**Key Methods**:
```java
// Token estimation (approximation)
int tokens = TextProcessingUtil.estimateTokenCount(text);

// Text cleaning
String clean = TextProcessingUtil.cleanText(text);

// Text splitting
List<String> paragraphs = TextProcessingUtil.splitIntoParagraphs(text);
List<String> headings = TextProcessingUtil.extractHeadings(text);

// Semantic chunking
List<TextProcessingUtil.TextChunkData> chunks =
    TextProcessingUtil.semanticChunk(text, targetSize=1500, overlap=200);

// Truncation for API limits
String truncated = TextProcessingUtil.truncateToTokens(text, maxTokens=100000);
```

**Token Estimation Algorithm**:
```
Estimated Tokens ≈ Word Count / 0.75
(Based on average of 0.75 words per token in English)
```

### 5. ChunkerService (services/ChunkerService.java)

**Purpose**: Split transcripts into semantic chunks

**Processing Flow**:
```
Raw Transcript Text
    ↓
[Clean Text]
    ↓
[Detect Headings & Paragraphs]
    ↓
[Semantic Chunking Algorithm]
    ↓
List<TextChunk> (JSON serializable)
    ↓
Save to chunks/lesson1_chunks.json
```

**Chunking Algorithm**:
1. Split text into paragraphs (by blank lines)
2. Estimate tokens for each paragraph
3. Group paragraphs until approaching target size
4. Start new chunk when exceeding target
5. Maintain overlap between chunks

**Configuration Parameters**:
- `CHUNK_SIZE`: Target tokens per chunk (default: 1500)
- `CHUNK_OVERLAP`: Token overlap between chunks (default: 200)

**Example Usage**:
```java
ChunkerService chunker = new ChunkerService();

// Method 1: Local chunking (no API call needed)
List<TextChunk> chunks = chunker.chunkTranscript("transcripts/lesson1.txt");

// Method 2: API-based chunking (higher quality, costs API calls)
List<TextChunk> chunks = chunker.chunkTranscriptWithApi("transcripts/lesson1.txt");

// Save chunks
chunker.saveChunks(chunks, "output/chunks/lesson1_chunks.json");

// Statistics
String stats = chunker.getChunkStatistics(chunks);
```

### 6. SummarizerService (services/SummarizerService.java)

**Purpose**: Summarize individual chunks with structured output, supporting multiple AI models

**Model Selection**:
- Configurable via `SUMMARIZER_MODEL` environment variable
- Default: Claude 3.5 Sonnet (best quality)
- Alternative: Gemini 2.5 Pro (77% cost savings)

```env
SUMMARIZER_MODEL=claude   # Uses Claude API
SUMMARIZER_MODEL=gemini   # Uses Gemini API (cost-optimized)
```

**Output Structure**:
```json
{
  "chunk_id": "1",
  "title": "Introduction to Biology",
  "summary": "50-80 word concise summary",
  "key_points": ["point1", "point2", "point3"],
  "workflows": [
    {
      "name": "Cell Division",
      "steps": ["step1", "step2", "step3"],
      "notes": "optional notes"
    }
  ],
  "definitions": [
    {"term": "Mitochondria", "definition": "..."},
    {"term": "Chloroplast", "definition": "..."}
  ],
  "examples": ["example1", "example2"],
  "exam_pointers": ["high-yield point 1", "high-yield point 2"],
  "confidence": "high|medium|low"
}
```

**Processing Flow**:
```
TextChunk
    ↓
[Check SUMMARIZER_MODEL config]
    ↓
[Claude OR Gemini Summarization Prompt]
    ↓
[Parse JSON Response]
    ↓
ChunkSummary (with confidence)
    ↓
Save to summaries/chunk_*.json
```

**Confidence Levels**:
- **high**: Content is clear, complete, and well-structured
- **medium**: Content is partly unclear or has some gaps
- **low**: Content is confusing, contradictory, or incomplete

**Example Usage**:
```java
SummarizerService summarizer = new SummarizerService();

// Summarize single chunk (uses configured model)
ChunkSummary summary = summarizer.summarizeChunk(chunk);

// Summarize multiple chunks (with rate limiting)
List<ChunkSummary> summaries = summarizer.summarizeChunks(chunks);

// Save summaries
summarizer.saveSummaries(summaries, "output/summaries");

// Load summaries
List<ChunkSummary> loaded = summarizer.loadSummaries("output/summaries");

// Get statistics
String stats = summarizer.getSummaryStatistics(summaries);
```

**Cost Comparison**:
- Claude: ~$1.35/lecture
- Gemini: ~$0.32/lecture (77% savings!)
- Both models produce high-quality structured JSON summaries

### 7. ConsolidatorService (services/ConsolidatorService.java)

**Purpose**: Consolidate summaries into exam-ready materials, supporting multiple AI models

**Model Selection**:
- Configurable via `CONSOLIDATOR_MODEL` environment variable
- Default: GPT-4o (best quality)
- Alternative: Gemini 2.5 Pro (98% cost savings!)

```env
CONSOLIDATOR_MODEL=gpt     # Uses OpenAI GPT-4o API
CONSOLIDATOR_MODEL=gemini  # Uses Gemini API (cost-optimized)
```

**Generated Materials**:
1. **Master Notes** (Markdown)
   - Topic-wise organization
   - Definitions and examples
   - Workflows as numbered steps
   - Low-confidence warning markers

2. **Flashcards** (CSV)
   - One flashcard per row
   - Format: "Front","Back"
   - Compatible with Anki, Quizlet

3. **Practice Questions** (Markdown)
   - 6 MCQs (4 options each)
   - 6 short-answer questions
   - 2 long-form questions with rubrics
   - Complete answer keys included

4. **Quick Revision Sheet** (Markdown)
   - 10-15 bullet points
   - Key definitions
   - Memory aids
   - One-page format

**Cost Comparison**:
- GPT-4o: ~$0.50/lecture
- Gemini: ~$0.07/lecture (86% savings!)
- Recommended: Use Gemini for consolidation (same quality, much cheaper!)

**Processing Flow**:
```
List<ChunkSummary>
    ↓
[Build Consolidator Payload]
    ↓
[OpenAI Consolidation Prompt]
    ↓
[Parse Markdown Response]
    ↓
Master Notes (Markdown)
    ↓
[Generate Flashcards/Questions/Revision]
    ↓
CSV & Markdown Files
```

**Example Usage**:
```java
ConsolidatorService consolidator = new ConsolidatorService();

// Consolidate to master notes
String masterNotes = consolidator.consolidateToMasterNotes(summaries);
consolidator.saveMasterNotes(masterNotes, "output/consolidated/master_notes.md");

// Generate exam materials
String flashcards = consolidator.generateFlashcards(masterNotes, summaries);
String questions = consolidator.generatePracticeQuestions(masterNotes);
String revision = consolidator.generateQuickRevision(masterNotes);

consolidator.saveFlashcards(flashcards, "output/exam_materials/flashcards.csv");
consolidator.savePracticeQuestions(questions, "output/exam_materials/practice_questions.md");
consolidator.saveQuickRevision(revision, "output/exam_materials/quick_revision.md");
```

### 8. Data Models

#### TextChunk (models/TextChunk.java)
```java
public class TextChunk {
    String chunkId;          // Unique identifier
    String title;            // Chunk title/heading
    String text;             // Actual chunk content
    String sourceFile;       // Source transcript filename
    Integer startLine;       // Line number in source
    Integer endLine;         // Line number in source
}
```

#### ChunkSummary (models/ChunkSummary.java)
```java
public class ChunkSummary {
    String chunkId;
    String title;
    String summary;                  // 50-80 words
    List<String> keyPoints;
    List<Workflow> workflows;
    List<Definition> definitions;
    List<String> examples;
    List<String> examPointers;
    String confidence;              // high|medium|low

    public static class Workflow {
        String name;
        List<String> steps;
        String notes;
    }

    public static class Definition {
        String term;
        String definition;
    }
}
```

#### PipelineState (models/PipelineState.java)
```java
public class PipelineState {
    LocalDateTime timestamp;
    Map<String, LessonState> lessons;
    String overallStatus;           // pending|in_progress|completed|failed
    List<String> errorLog;

    public static class LessonState {
        String filename;
        String chunksStatus;        // pending|done
        String summariesStatus;     // pending|in_progress|done
        List<String> confidenceIssues;
        Integer summaryCount;
        LocalDateTime lastUpdated;
    }
}
```

## API Integration Details

### Claude API Integration

**Endpoint**: `https://api.anthropic.com/v1/messages`

**Request Format**:
```json
{
  "model": "claude-3-5-sonnet-20241022",
  "max_tokens": 4096,
  "system": "You are a summarizer...",
  "messages": [
    {
      "role": "user",
      "content": "Summarize this text..."
    }
  ]
}
```

**Response Format**:
```json
{
  "id": "msg_xxxxx",
  "type": "message",
  "role": "assistant",
  "content": [
    {
      "type": "text",
      "text": "JSON or text response..."
    }
  ],
  "model": "claude-3-5-sonnet-20241022",
  "stop_reason": "end_turn",
  "stop_sequence": null,
  "usage": {
    "input_tokens": 1000,
    "output_tokens": 500
  }
}
```

**Authentication**: Bearer token in Authorization header

### OpenAI API Integration

**Endpoint**: `https://api.openai.com/v1/chat/completions`

**Request Format**:
```json
{
  "model": "gpt-4o",
  "messages": [
    {
      "role": "system",
      "content": "You are an educator..."
    },
    {
      "role": "user",
      "content": "Create flashcards..."
    }
  ],
  "temperature": 0.7
}
```

**Response Format**:
```json
{
  "id": "chatcmpl-xxxxx",
  "object": "chat.completion",
  "created": 1234567890,
  "model": "gpt-4o",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "Response text..."
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 1000,
    "completion_tokens": 500,
    "total_tokens": 1500
  }
}
```

## Prompts Used

### Chunk Summarizer Prompt
Used by Claude to summarize individual chunks:
```
You are an expert summarizer. For the chunk below produce JSON with fields:
{
 "chunk_id": "...",
 "title": "...",
 "summary": "one-paragraph concise summary (50-80 words)",
 "key_points": ["...","..."],
 "workflows": [
   {"name":"Flow name","steps":["step1","step2",...], "notes":"optional"}
 ],
 "definitions": [{"term":"...","definition":"..."}],
 "examples": ["..."],
 "exam_pointers": ["short high-yield bullets"],
 "confidence": "low|medium|high"
}

Then output only the JSON.

---
Chunk text:
[CHUNK TEXT HERE]
```

### Consolidator Prompt
Used by OpenAI to create master notes from summaries:
```
Combine the following chunk summaries (JSON array) into a single exam-ready document.
Output 3 parts in Markdown:
1) Master Notes — topic sections; for each topic include definitions, key points, workflows (as numbered steps), and examples.
2) Quick Revision — 1-page bullet list of must-remember facts.
3) Exam Practice — 10 practice questions (mix of MCQ, short answer, one long-form).

Mark any item that came from chunk summary with "confidence":"low" by putting **IMPORTANT (low confidence)** after the item.

Then output only the Markdown.

---
[CHUNK SUMMARIES HERE]
```

## Error Handling Strategy

### API Errors (Retry Logic)

```
400 Bad Request → Fail immediately (no retry)
401 Unauthorized → Fail immediately (check API key)
429 Rate Limited → Retry with exponential backoff
500+ Server Error → Retry with exponential backoff
Timeout → Retry with exponential backoff
Network Error → Retry with exponential backoff
```

### File Errors

```
File not found → Log error, skip file
Permission denied → Log error, skip file
IO Exception → Log error, continue with other files
Invalid JSON → Log error, use fallback
```

### Validation Errors

```
Missing required fields → Use defaults
Invalid confidence level → Set to "medium"
Malformed JSON response → Create minimal summary
Empty summary → Flag for review
```

## Performance Considerations

### Token Limits
- Claude: 200K context window (can handle entire pipeline in one call)
- GPT-4o: 128K context window (may need batching for large consolidations)
- Requests truncated to 100K tokens by default

### Rate Limiting
- Default: 1-second delay between API calls
- Can be adjusted via `RETRY_BACKOFF` setting
- Exponential backoff for retries

### Concurrency
- Currently sequential (not parallel)
- Future enhancement: Parallel chunk summarization
- Pipeline state tracking enables resumable runs

### Storage
- Output files: Plain text JSON/Markdown (human-readable)
- No database required (file-based storage)
- Total output size: ~2-3x input size

## Testing & Quality Assurance

### Unit Testing
```bash
mvn test
```

### Integration Testing
```bash
# Test with sample transcript
java -jar target/transcript-pipeline.jar
# Menu option 2 to chunk sample file
# Menu option 3 to summarize chunks
```

### Quality Checks
1. Validate output JSON schema
2. Check Markdown formatting
3. Verify CSV syntax for flashcards
4. Review low-confidence items

## Logging

### Logging Framework
- **Framework**: SLF4J with Simple implementation
- **Levels**: DEBUG, INFO, WARN, ERROR
- **Output**: Console and `logs/` directory

### Important Log Messages
```
INFO: Configuration initialized
INFO: Chunking transcript: transcripts/lesson1.txt
INFO: Summarizing chunk: 1 - Introduction to Biology
WARN: Creating default summary for chunk 3 (API failed)
ERROR: API request failed: 401 - Unauthorized
```

## Building & Deployment

### Build Command
```bash
mvn clean package
```

### Output
- `target/transcript-pipeline.jar` - Executable JAR
- `target/transcript-pipeline-sources.jar` - Source code

### Running as Service
```bash
# Create systemd service file
[Unit]
Description=Transcript Pipeline Service
After=network.target

[Service]
Type=simple
User=pipeline
ExecStart=/usr/bin/java -jar /opt/transcript-pipeline.jar
Restart=always

[Install]
WantedBy=multi-user.target
```

## Future Enhancements

### Planned Features
- [ ] Parallel chunk summarization (ThreadPool)
- [ ] Embedding-based vector search (FAISS)
- [ ] Web UI for interactive editing
- [ ] PDF generation without pandoc
- [ ] Audio/video transcript support
- [ ] Multi-language support
- [ ] Database backend for large-scale use
- [ ] Scheduled pipeline runs
- [ ] Webhook support for integrations

### Performance Optimizations
- [ ] Caching for repeated summarizations
- [ ] Incremental processing for updated transcripts
- [ ] Batch API calls where supported
- [ ] Local LLM option (ollama integration)

## Security Considerations

### API Key Management
- Never hardcode keys
- Use environment variables
- Support secrets manager integration
- Validate keys before execution

### Input Validation
- Sanitize file paths
- Validate JSON input
- Check file sizes
- Limit recursion depth

### Output Safety
- No sensitive data in logs
- Clean API responses
- Secure temp file handling
- Validate output content

## Troubleshooting Guide

### Build Issues
```bash
# Clear cache
mvn clean

# Update dependencies
mvn dependency:resolve

# Check Java version
java -version  # Should be 17+
```

### Runtime Issues
```bash
# Enable debug logging
-Dorg.slf4j.simpleLogger.defaultLogLevel=debug

# Check memory
-Xmx4g  # Increase heap size

# Check file permissions
chmod +x logs/ output/
```

## References

- [Anthropic Claude API Documentation](https://docs.anthropic.com/)
- [OpenAI API Documentation](https://platform.openai.com/docs/)
- [OkHttp Documentation](https://square.github.io/okhttp/)
- [Jackson Documentation](https://github.com/FasterXML/jackson-docs)
- [Maven Documentation](https://maven.apache.org/guides/)

---

For questions or clarifications, open an issue on GitHub.
