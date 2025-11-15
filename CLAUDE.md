# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Transcript to Exam Notes Pipeline** - An automated Java-based pipeline that converts lecture transcripts into comprehensive exam-ready study materials using AI. Supports three AI providers (Claude, GPT-4, Gemini) with flexible model selection for cost optimization.

**Key Value**: Transform raw transcript text files into structured master notes, flashcards, practice questions, and quick revision sheets automatically.

## Pipeline Architecture

The pipeline operates in 4 distinct stages. When multiple transcript files are present, the pipeline can process them in two modes (configurable via `MULTI_FILE_MODE`):
- **Separate mode**: Each file goes through all 4 stages independently
- **Combined mode**: Files are chunked separately but then merged and processed together through stages 2-4

### Stage 1: Chunking (Local)
- **Input**: Raw transcript `.txt` files from `transcripts/` directory
- **Process**: Semantic chunking using local algorithm (no API calls)
- **Implementation**: `TextProcessingUtil.semanticChunk()`
- **Output**: `output/chunks/*.json` (1000-2000 words per chunk)
- **Key Feature**: Overlap-based context preservation

### Stage 2: Summarization (AI-Powered)
- **Input**: Text chunks from stage 1
- **Process**: Claude API or Gemini API extracts structured information
- **Implementation**: `SummarizerService.summarizeChunks()`
- **Output**: `output/summaries/chunk_*.json` with:
  - Key points and definitions
  - Workflows and processes
  - Examples and analogies
  - **Confidence scoring** (high/medium/low) for quality control
- **Model Selection**: Controlled by `SUMMARIZER_MODEL` env var (claude or gemini)

### Stage 3: Consolidation (AI-Powered)
- **Input**: All chunk summaries
- **Process**: GPT-4 or Gemini consolidates into coherent document
- **Implementation**: `ConsolidatorService.consolidateSummaries()`
- **Output**: `output/consolidated/master_notes.md`
- **Key Features**: Deduplication, cross-referencing, coherent narrative
- **Model Selection**: Controlled by `CONSOLIDATOR_MODEL` env var (gpt or gemini)

### Stage 4: Exam Materials Generation (AI-Powered)
- **Input**: Master notes from stage 3
- **Process**: Same model as stage 3 generates study aids
- **Implementation**: `ConsolidatorService.generateExamMaterials()`
- **Output**:
  - `output/exam_materials/flashcards.csv`
  - `output/exam_materials/practice_questions.md` (MCQ, short answer, long-form)
  - `output/exam_materials/quick_revision.md`

## Key Package Responsibilities

### com.transcript.pipeline.config
- **ConfigManager**: Loads and validates configuration from `.env` files and environment variables. Contains all default values and configuration constants.

### com.transcript.pipeline.services
- **ChunkerService**: Semantic text chunking with local algorithm + optional API-based chunking
- **SummarizerService**: AI-powered chunk summarization using Claude or Gemini
- **ConsolidatorService**: Master notes consolidation and exam materials generation using GPT or Gemini

### com.transcript.pipeline.util
- **ApiClient**: Unified HTTP client for Claude, OpenAI, and Gemini APIs with retry logic (see API Client Architecture below)
- **FileService**: All file I/O operations (JSON serialization, text file reading, directory management)
- **TextProcessingUtil**: Text processing utilities (cleaning, token estimation, semantic chunking algorithm)
- **ApiKeyTester**: Standalone utility to validate API keys before running pipeline

### com.transcript.pipeline.models
- **TextChunk**: Data model for chunked text segments
- **ChunkSummary**: Data model for summarized chunks with nested Workflow and Definition classes
- **PipelineState**: State tracking for pipeline execution

## Build & Run Commands

### Maven Commands
```bash
mvn clean package          # Build executable JAR (required before running)
mvn clean compile          # Compile only
mvn test                   # Run unit tests
mvn clean install          # Install to local Maven repo
```

### Makefile Shortcuts
```bash
make build                 # Compile the project
make package               # Build executable JAR
make run                   # Build and run in interactive mode
make test                  # Run tests
make debug                 # Run with DEBUG logging enabled
make clean                 # Remove build artifacts
make init                  # First-time project setup
```

### Running the Pipeline
```bash
# Interactive mode (recommended) - presents menu-driven interface
java -jar target/transcript-pipeline.jar

# Test API keys before running pipeline (IMPORTANT)
./test-api-keys.sh
# or
java -cp target/transcript-pipeline.jar com.transcript.pipeline.util.ApiKeyTester

# Debug mode for troubleshooting
java -Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG -jar target/transcript-pipeline.jar
```

## API Client Architecture

**Critical for understanding multi-provider support:**

### Unified Client Design
- Single `ApiClient` class handles all 3 providers via `ModelType` enum (CLAUDE, OPENAI, GEMINI)
- Factory methods: `createClaudeClient()`, `createOpenAIClient()`, `createGeminiClient()`
- Different authentication and request formats per provider

### Authentication Methods
```java
// Claude: Uses x-api-key header (NOT Bearer token)
requestBuilder.addHeader("x-api-key", apiKey)
              .addHeader("anthropic-version", "2023-06-01")

// OpenAI: Uses Bearer token
requestBuilder.addHeader("Authorization", "Bearer " + apiKey)

// Gemini: Uses X-goog-api-key header (native API, not OpenAI-compatible endpoint)
requestBuilder.addHeader("X-goog-api-key", apiKey)
```

### Retry Logic
- Automatic retry with exponential backoff
- Configurable via `MAX_RETRIES` (default: 3) and `RETRY_BACKOFF` (default: 1000ms)
- Backoff formula: `backoffTime = RETRY_BACKOFF * 2^attempt`
- Timeout controlled by `API_TIMEOUT` (default: 60000ms)

### Response Parsing
Each provider has different JSON response formats:
- **Claude**: `content[0].text`
- **OpenAI**: `choices[0].message.content`
- **Gemini**: `candidates[0].content.parts[0].text`

## Configuration Structure

### .env File Organization
- **Template**: `config/.env.example` (copy to `.env` in root)
- **Security**: `.env` is in `.gitignore` - NEVER commit API keys

### Configuration Categories
1. **API Keys** (required)
   - `CLAUDE_API_KEY`, `OPENAI_API_KEY`, `GEMINI_API_KEY`

2. **API Endpoints** (optional, customizable for proxies)
   - `CLAUDE_API_BASE` (default: https://api.anthropic.com/v1)
   - `OPENAI_API_BASE` (default: https://api.openai.com/v1)
   - `GEMINI_API_BASE` (default: https://generativelanguage.googleapis.com/v1beta)

3. **Model Selection**
   - `MODEL_CLAUDE` (e.g., claude-3-5-sonnet-20241022)
   - `MODEL_GPT` (e.g., gpt-4o)
   - `MODEL_GEMINI` (e.g., gemini-2.0-flash)

4. **Pipeline Model Assignments** (cost optimization)
   - `SUMMARIZER_MODEL` (claude or gemini, default: claude)
   - `CONSOLIDATOR_MODEL` (gpt or gemini, default: gpt)
   - Mix and match for cost savings: claude+gemini saves ~77%, gemini+gemini saves ~95%

5. **Chunking Parameters**
   - `CHUNK_SIZE` (default: 1500 tokens)
   - `CHUNK_OVERLAP` (default: 200 tokens, ~13% overlap)

6. **API Behavior**
   - `API_TIMEOUT` (default: 60000ms)
   - `MAX_RETRIES` (default: 3)
   - `RETRY_BACKOFF` (default: 1000ms)

7. **Directory Paths**
   - `TRANSCRIPT_DIR` (default: transcripts)
   - `OUTPUT_DIR` (default: output)
   - `LOGS_DIR` (default: logs)
   - `MULTI_FILE_MODE` (separate|combined, default: separate)

### Default Values
All defaults are defined in `ConfigManager` constants and used as fallbacks if not specified in `.env`.

### Multi-File Processing Modes

**MULTI_FILE_MODE Configuration:**

- **separate** (default): Process each transcript file individually
  - Each file gets its own master notes, flashcards, and exam materials
  - Output structure: `output/consolidated/filename_master_notes.md` and `output/exam_materials/filename/`
  - Use when: Transcripts cover unrelated topics
  - Cost: Higher (N files × cost per file)

- **combined**: Merge all transcript files into one output
  - All files combined into single master notes and exam materials
  - Output structure: `output/consolidated/master_notes.md` and `output/exam_materials/`
  - Use when: Transcripts are related chapters of same course
  - Cost: Lower (processed as one large file)

## Important Technical Details

### Multi-Model Flexibility
- **Purpose**: Cost optimization and provider failover
- **Implementation**: `SUMMARIZER_MODEL` and `CONSOLIDATOR_MODEL` can be set independently
- **Cost Comparison** (per lecture):
  - Claude + GPT: ~$2.15 (best quality)
  - Claude + Gemini: ~$0.50 (77% savings)
  - Gemini + Gemini: ~$0.10 (95% savings, requires testing)

### Confidence-Based Quality Control
- All summaries include confidence levels (high/medium/low) in `ChunkSummary` model
- Flags content that may need human review
- Implemented by AI models during summarization stage

### Local-First Chunking
- Stage 1 uses custom semantic chunking algorithm in `TextProcessingUtil`
- **No API calls required** for chunking - reduces costs and latency
- Algorithm uses sentence boundaries and paragraph breaks for semantic coherence

### Error Handling Strategy
1. API failures trigger automatic retries with exponential backoff
2. Summarization failures fall back to default/empty summaries
3. Consolidation failures generate basic concatenated notes
4. All errors logged via SLF4J (configured in `.env`)

### Maven Shade Plugin
- Creates fat JAR with all dependencies bundled
- Main class configured in manifest: `com.transcript.pipeline.App`
- Output: `target/transcript-pipeline.jar`
- No need for classpath configuration when running

### JSON Processing
- Uses Jackson for all serialization/deserialization
- Custom `@JsonProperty` annotations for snake_case compatibility
- Structured models with nested classes (e.g., `Workflow`, `Definition` within `ChunkSummary`)

## Development Workflow

### Initial Setup
1. Copy `config/.env.example` to `.env` in repository root
2. Add API keys for desired providers (at least one required)
3. Run `./test-api-keys.sh` to validate configuration (IMPORTANT - catches auth issues early)
4. Build the project: `mvn clean package` or `make package`

### Running Pipeline
1. Place transcript `.txt` files in `transcripts/` directory
2. Run interactive mode: `make run` or `java -jar target/transcript-pipeline.jar`
3. Select option from menu:
   - Option 1: Run full pipeline (all 4 stages)
   - Option 2: Chunk only
   - Option 3: Summarize only (requires existing chunks)
   - Option 4: Consolidate only (requires existing summaries)
   - Option 5: Generate exam materials only (requires master notes)
4. Review outputs in `output/` directory structure

### Debugging
- Enable DEBUG logging: `make debug` or set `org.slf4j.simpleLogger.defaultLogLevel=DEBUG` in `.env`
- Check logs in `logs/` directory
- Inspect intermediate JSON files in `output/chunks/` and `output/summaries/`
- If API calls fail, verify with `./test-api-keys.sh`

### Common Issues
- **API authentication errors**: Run `./test-api-keys.sh` to diagnose
- **Claude API using wrong header**: Ensure `ApiClient` uses `x-api-key` header, not `Authorization: Bearer`
- **Gemini API errors**: Verify using native API endpoint (v1beta), not OpenAI-compatible endpoint
- **Rate limiting**: Increase `RETRY_BACKOFF` or reduce `CHUNK_SIZE`
- **Timeout errors**: Increase `API_TIMEOUT` or check network connectivity

## Unique Aspects of This Codebase

1. **Hybrid Architecture**: Combines local processing (chunking) with AI processing (summarization, consolidation) for cost efficiency

2. **Three-Provider Support**: Not just multi-model, but multi-provider with different authentication schemes and API formats

3. **Exam-Focused Output**: Specifically designed for educational use - generates flashcards, MCQs, and practice questions, not just summaries

4. **Interactive + CLI Modes**: Menu-driven interface for ease of use, with CLI argument support (partially implemented)

5. **Cost-Conscious Design**: Built-in support for mixing expensive and cheap models per pipeline stage

6. **No External .env Library**: Custom configuration parser (java-dotenv dependency commented out in pom.xml)

7. **Dedicated API Testing Utility**: `ApiKeyTester.java` validates all configured providers before pipeline execution
