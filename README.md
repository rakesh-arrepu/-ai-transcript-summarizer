# Transcript → Exam Notes Pipeline

Convert lecture transcripts into comprehensive, exam-ready study materials automatically. This tool uses AI to chunk, summarize, and consolidate transcripts into master notes, flashcards, practice questions, and quick revision sheets.

## 🎯 Features

- **Semantic Text Chunking**: Intelligently split transcripts into manageable chunks preserving topic boundaries
- **AI-Powered Summarization**: Use Claude, GPT, or Gemini API to create structured, high-quality summaries
- **Master Notes Generation**: Consolidate summaries into comprehensive study documents (Markdown)
- **Exam Materials**: Auto-generate:
  - Flashcards (CSV format for Anki/Quizlet)
  - Practice questions (MCQ, short-answer, long-form)
  - Quick revision sheets (1-page summary)
- **📊 Flows & Diagrams** *(Optional)*: Visualize multi-step processes extracted from lectures:
  - Mermaid flowcharts (GitHub-renderable diagrams)
  - ASCII art diagrams (terminal-friendly)
  - Pipeline execution flow visualization
  - Workflow reference tables
- **Multi-File Processing**: Process multiple transcripts with configurable modes:
  - **Separate Mode**: Each transcript gets its own study materials (default)
  - **Combined Mode**: Merge all transcripts into one comprehensive guide
- **Multi-Model Support**: Claude 3.5 Sonnet, GPT-4o, and Gemini 2.5 Pro
- **Cost Optimization**: Save up to 77-95% using Gemini instead of Claude+GPT
- **Quality Tracking**: Confidence levels for each summary item
- **Interactive & CLI Modes**: Use the interactive menu or command-line arguments
- **State Tracking**: Resume interrupted pipeline runs
- **Error Handling**: Automatic retries with exponential backoff for API calls

## 🚀 Quick Start (5 minutes)

### 1. Prerequisites
- **Java 17+** (LTS version recommended)
- **Maven 3.8+**
- **API Keys** (at least one required):
  - [Anthropic Claude API Key](https://console.anthropic.com)
  - [OpenAI API Key](https://platform.openai.com/api-keys)
  - [Google Gemini API Key](https://aistudio.google.com/app/apikey) ⭐ **Recommended for cost savings**

### 2. Setup

```bash
# Clone the repository
git clone <repository-url>
cd transcript-to-exam-notes

# Create .env file with your API keys
cp config/.env.example .env

# Edit .env with your keys (at least one model per step required)
# For best quality (default):
# CLAUDE_API_KEY=sk-ant-xxxxx
# OPENAI_API_KEY=sk-xxxxx

# For cost savings (recommended):
# CLAUDE_API_KEY=sk-ant-xxxxx
# GEMINI_API_KEY=AIzaSy... (from https://aistudio.google.com/app/apikey)
# CONSOLIDATOR_MODEL=gemini
```

### 3. Test API Keys (Recommended)

Before running the full pipeline, verify your API keys are working:

```bash
# Make the test script executable (first time only)
chmod +x test-api-keys.sh

# Run the API key tests
./test-api-keys.sh
```

The test script will:
- ✓ Check if your .env file exists
- ✓ Build the project if needed
- ✓ Test each configured API key (Claude, OpenAI, Gemini)
- ✓ Verify your pipeline configuration is valid
- ✓ Provide detailed error messages if any issues are found

**Alternative: Test using Java directly**
```bash
# Build the project first
mvn clean package

# Run the API key tester
java -cp target/transcript-pipeline.jar com.transcript.pipeline.util.ApiKeyTester
```

**Expected output:**
```
=== Testing API Keys ===

Testing Claude API key...
✓ Claude API key is valid and working!
Testing OpenAI API key...
✓ OpenAI API key is valid and working!
Testing Gemini API key...
✓ Gemini API key is valid and working!

=== Test Summary ===
Claude API: ✓ WORKING
OpenAI API: ✓ WORKING
Gemini API: ✓ WORKING

✓ At least one API key is working!
✓ Pipeline is properly configured and ready to use!
```

### 4. Build Project

```bash
# Build with Maven
mvn clean package

# This creates: target/transcript-pipeline.jar
```

### 5. Run Interactive Mode

```bash
java -jar target/transcript-pipeline.jar
```

Follow the menu to:
1. Place `.txt` transcript files in `transcripts/` directory
2. Choose "Run complete pipeline"
3. Let the tool generate all study materials

### 6. Outputs

Generated files appear in `output/`:

**Separate Mode** (default - `MULTI_FILE_MODE=separate`):
```
output/
├── chunks/
│   ├── Section-Fundamentals_chunks.json
│   └── Section-RAG_chunks.json
├── summaries/
│   ├── Section-Fundamentals/
│   │   ├── chunk_1.json
│   │   └── chunk_2.json
│   └── Section-RAG/
│       └── chunk_1.json
├── consolidated/
│   ├── Section-Fundamentals_master_notes.md
│   └── Section-RAG_master_notes.md
├── exam_materials/
│   ├── Section-Fundamentals/
│   │   ├── flashcards.csv
│   │   ├── practice_questions.md
│   │   └── quick_revision.md
│   └── Section-RAG/
│       ├── flashcards.csv
│       ├── practice_questions.md
│       └── quick_revision.md
└── flows/                               # 📊 NEW - Visual diagrams & flowcharts
    ├── flows_report.md
    ├── pipeline_diagram.md
    ├── workflow_photosynthesis.md
    └── workflow_cellular_respiration.md
```

**Combined Mode** (`MULTI_FILE_MODE=combined`):
```
output/
├── chunks/
│   ├── Section-Fundamentals_chunks.json
│   └── Section-RAG_chunks.json
├── summaries/
│   ├── chunk_1.json (from all files)
│   ├── chunk_2.json
│   └── ...
├── consolidated/
│   └── master_notes.md              # Combined from all files
├── exam_materials/
│   ├── flashcards.csv               # From all files
│   ├── practice_questions.md        # From all files
│   └── quick_revision.md            # From all files
└── flows/                            # 📊 NEW - Visual diagrams & flowcharts
    ├── flows_report.md
    ├── pipeline_diagram.md
    └── workflow_*.md
```

## 📚 Workflow

```
Transcripts (*.txt)
        ↓
    [CHUNKING]
        ↓
  Chunks (JSON)
        ↓
  [SUMMARIZATION]
        ↓
 Chunk Summaries (JSON)
        ↓
 [CONSOLIDATION]
        ↓
Master Notes (Markdown)
        ↓
[EXAM MATERIALS]
        ↓
Flashcards / Q&A / Quick Revision
```

## 🎮 Interactive Menu

```
📋 MAIN MENU
1. Run complete pipeline
2. Chunk transcripts only
3. Summarize chunks only
4. Consolidate to master notes
5. Generate exam materials
6. View pipeline status
7. Settings
8. Help
0. Exit
```

Choose options to run full pipeline or individual steps.

## 🛠️ Technical Stack

| Component | Technology |
|-----------|-----------|
| **Language** | Java 17+ |
| **Build Tool** | Maven 3.8+ |
| **HTTP Client** | OkHttp 4.11 |
| **JSON Processing** | Jackson 2.15 |
| **Logging** | SLF4J 2.0 |
| **CLI** | Apache Commons CLI |
| **Configuration** | .env files + Environment variables |

## 🔑 API Configuration

### Environment Variables

Create a `.env` file in the project root:

```env
# ============================================================================
# REQUIRED API KEYS (at least one per model type)
# ============================================================================

# Anthropic Claude API Key
# Get from: https://console.anthropic.com/
CLAUDE_API_KEY=sk-ant-xxxxx

# OpenAI API Key
# Get from: https://platform.openai.com/api-keys
OPENAI_API_KEY=sk-xxxxx

# Google Gemini API Key (recommended for cost savings)
# Get from: https://aistudio.google.com/app/apikey
GEMINI_API_KEY=AIzaSy_xxxxxxxxxxxxx

# ============================================================================
# API ENDPOINTS (Optional - for proxies or custom endpoints)
# ============================================================================
CLAUDE_API_BASE=https://api.anthropic.com/v1
OPENAI_API_BASE=https://api.openai.com/v1
GEMINI_API_BASE=https://generativelanguage.googleapis.com/v1beta/openai/

# ============================================================================
# PIPELINE MODEL SELECTION
# ============================================================================
# Choose which model to use for each step (default: claude + gpt)
SUMMARIZER_MODEL=claude      # or gemini
CONSOLIDATOR_MODEL=gpt       # or gemini

# ============================================================================
# PIPELINE CONFIGURATION
# ============================================================================
TRANSCRIPT_DIR=transcripts
OUTPUT_DIR=output
LOGS_DIR=logs

# Multi-file processing mode
# - separate: Each transcript gets its own study materials (default)
# - combined: Merge all transcripts into one comprehensive output
MULTI_FILE_MODE=separate

# ============================================================================
# CHUNKING PARAMETERS
# ============================================================================
CHUNK_SIZE=1500              # Estimated tokens per chunk
CHUNK_OVERLAP=200            # Overlap tokens between chunks

# ============================================================================
# API PARAMETERS
# ============================================================================
API_TIMEOUT=60000            # Timeout in milliseconds
MAX_RETRIES=3                # Retry attempts for failed requests
RETRY_BACKOFF=1000           # Initial backoff in milliseconds

# ============================================================================
# MODEL SPECIFICATIONS
# ============================================================================
MODEL_CLAUDE=claude-3-5-sonnet-20241022
MODEL_GPT=gpt-4o
MODEL_GEMINI=gemini-2.5-flash
```

## 💰 Cost Optimization with Gemini

### Model Combinations & Pricing

| Configuration | Summarizer | Consolidator | Cost/Lecture | Savings | Quality |
|---|---|---|---|---|---|
| **Default (Best Quality)** | Claude | GPT-4o | ~$2.15 | Baseline | ⭐⭐⭐⭐⭐ |
| **Cost-Optimized ⭐** | Claude | Gemini | ~$0.50 | **77%** | ⭐⭐⭐⭐ |
| **Budget Mode** | Gemini | Gemini | ~$0.10 | **95%** | ⭐⭐⭐ |

### How to Enable Gemini

**For 77% cost savings (recommended):**
```env
CONSOLIDATOR_MODEL=gemini
GEMINI_API_KEY=AIzaSy_your_key_from_https://aistudio.google.com/app/apikey
```

**For 95% cost savings:**
```env
SUMMARIZER_MODEL=gemini
CONSOLIDATOR_MODEL=gemini
GEMINI_API_KEY=AIzaSy_your_key_from_https://aistudio.google.com/app/apikey
```

### API Reference Links
- **Claude**: [console.anthropic.com](https://console.anthropic.com) | [Documentation](https://docs.anthropic.com)
- **OpenAI**: [platform.openai.com](https://platform.openai.com/api-keys) | [Documentation](https://platform.openai.com/docs)
- **Gemini**: [aistudio.google.com](https://aistudio.google.com/app/apikey) | [Documentation](https://ai.google.dev/docs)

For detailed Gemini integration guide, see [GEMINI_INTEGRATION_GUIDE.md](./GEMINI_INTEGRATION_GUIDE.md)

## 📁 Multi-File Processing

Process multiple transcript files with two configurable modes:

### Separate Mode (Default)

Each transcript file gets its own complete set of study materials.

**Use when:**

- Transcripts cover different, unrelated topics
- You want to study each subject individually
- You need separate flashcard decks per topic
- Cost is not a major concern (more API calls)

**Configuration:**
```env
MULTI_FILE_MODE=separate
```

**Example:**

```text
transcripts/
├── Section-Fundamentals.txt
├── Section-RAG.txt
└── Section-Chatbot.txt

→ Produces 3 separate master notes, 3 sets of flashcards, etc.
```

### Combined Mode

All transcript files are merged into one comprehensive output.

**Use when:**

- Transcripts are related chapters/sections of same course
- You want one unified study guide
- You want to minimize API costs (fewer calls)
- You want cross-topic connections in summaries

**Configuration:**
```env
MULTI_FILE_MODE=combined
```

**Example:**

```text
transcripts/
├── Section-Fundamentals.txt
├── Section-RAG.txt
└── Section-Chatbot.txt

→ Produces 1 master note combining all topics, 1 unified flashcard deck, etc.
```

### Switching Between Modes

Simply change the setting in your `.env` file:

```bash
# Edit .env file
nano .env

# Change MULTI_FILE_MODE=separate to MULTI_FILE_MODE=combined
# Or vice versa

# Run pipeline again
java -jar target/transcript-pipeline.jar
```

## 📖 Configuration Defaults

If not specified in `.env`:

| Parameter | Default |
|-----------|---------|
| `TRANSCRIPT_DIR` | `transcripts` |
| `OUTPUT_DIR` | `output` |
| `LOGS_DIR` | `logs` |
| `MULTI_FILE_MODE` | `separate` |
| `CHUNK_SIZE` | 1500 tokens |
| `CHUNK_OVERLAP` | 200 tokens |
| `API_TIMEOUT` | 60 seconds |
| `MAX_RETRIES` | 3 attempts |
| `RETRY_BACKOFF` | 1 second |

## 📊 Output File Formats

### Master Notes (Markdown)
```markdown
# Master Notes

## Topic 1
- Summary text
- Key points
- Definitions
- Examples

## Topic 2
...
```

### Flashcards (CSV)
```csv
"Front","Back"
"What is photosynthesis?","Process converting light energy to chemical energy"
"Define: Chlorophyll","Green pigment in plants for light absorption"
...
```

### Practice Questions (Markdown)
```markdown
## Multiple Choice
### Q1: Which of the following...
a) Option A
b) Option B
c) Option C
d) Option D

*Answer: b

## Short Answer
### Q6: Explain the process of...
Expected Answer: ...
```

## ⚙️ Advanced Configuration

### Custom Chunk Size
To adjust chunk size (in estimated tokens):

```env
CHUNK_SIZE=2000              # Larger chunks
CHUNK_OVERLAP=300            # More overlap
```

### API Retry Strategy
```env
MAX_RETRIES=5                # More retry attempts
RETRY_BACKOFF=2000           # Longer backoff (2s)
```

### Custom API Endpoints
For corporate proxies or alternative providers:

```env
CLAUDE_API_BASE=https://proxy.company.com/anthropic
OPENAI_API_BASE=https://proxy.company.com/openai
```

## 🔒 Security

### API Key Safety

- **Never commit `.env`** to version control
- `.env` is in `.gitignore` by default
- Use environment variables in production:
  ```bash
  export CLAUDE_API_KEY=sk-ant-xxxxx
  export OPENAI_API_KEY=sk-xxxxx
  java -jar target/transcript-pipeline.jar
  ```

### Best Practices
1. Use a secrets manager in production (Vault, 1Password, AWS Secrets)
2. Rotate API keys regularly
3. Monitor API usage for unusual activity
4. Use separate API keys for dev/prod environments

## 📝 Input Format

### Transcript Files
- **Location**: `transcripts/` directory
- **Format**: Plain text (`.txt`)
- **Encoding**: UTF-8
- **Size**: Up to several thousand lines per file
- **Structure**: Any format (headings optional)

Example `lesson1.txt`:
```
# Introduction to Biology

Lecture delivered by Dr. Smith on March 15, 2024

## Cell Structure

Cells are the basic units of life. There are two main types:
- Prokaryotic cells (bacteria, archaea)
- Eukaryotic cells (animals, plants, fungi)

The cell membrane controls what enters and exits the cell...

## Mitochondria

Mitochondria are often called the powerhouse of the cell because...
```

## 📤 Exporting Results

### Convert to PDF
```bash
# Install pandoc (if not installed)
brew install pandoc        # macOS
apt install pandoc         # Linux
choco install pandoc       # Windows

# Convert markdown to PDF
pandoc output/consolidated/master_notes.md -o master_notes.pdf
```

### Import Flashcards to Anki
1. Download [Anki](https://apps.ankiweb.net/)
2. Create new deck
3. File → Import → Select `flashcards.csv`
4. Configure field mapping if needed

### Share with Others
```bash
# Create zip archive of outputs
zip -r study_materials.zip output/

# Share via email, Google Drive, or cloud storage
```

## 🐛 Troubleshooting

### API Key Issues
```
❌ ERROR: API keys not configured!
```
**Solution**:
- Verify `.env` file exists in project root
- Check syntax: `KEY=value` (no spaces around `=`)
- **Run the API key tester**: `./test-api-keys.sh`
- Verify keys are valid: test in Anthropic/OpenAI/Gemini console

**Testing your API keys:**
```bash
# Quick test all API keys
./test-api-keys.sh

# Or test individually using Java
java -cp target/transcript-pipeline.jar com.transcript.pipeline.util.ApiKeyTester
```

Common error messages and solutions:
- **401 - Invalid API key**: The API key format is incorrect or expired
  - Claude keys should start with `sk-ant-`
  - OpenAI keys should start with `sk-`
  - Gemini keys should start with `AIzaSy`
- **403 - Access forbidden**: API key doesn't have required permissions
- **429 - Rate limit exceeded**: Too many requests, wait and try again
- **Timeout**: Check your internet connection or increase API_TIMEOUT in .env

### API Rate Limits
```
API request failed: 429 - Rate limit exceeded
```
**Solution**:
- Increase `RETRY_BACKOFF` value
- Add delays between API calls
- Use a free tier API or upgrade plan

### Large Transcript Issues
```
API request failed: 400 - Token limit exceeded
```
**Solution**:
- Reduce `CHUNK_SIZE` (e.g., from 1500 to 1000)
- Pipeline will truncate very long chunks automatically

### Output Permission Errors
```
IOException: Permission denied
```
**Solution**:
- Ensure `output/` directory is writable
- Check file permissions: `chmod 755 output/`
- Verify disk space availability

### Connection Timeout
```
API request failed: Read timed out
```
**Solution**:
- Increase `API_TIMEOUT` value (e.g., 120000 for 2 minutes)
- Check internet connection
- Verify API endpoint URLs are correct

## 📋 Pipeline Steps Explained

### 1. Chunking
- **Input**: Raw transcript text
- **Process**: Split into semantic chunks (1000-2000 words each)
- **Output**: `chunks/lesson1_chunks.json`
- **Time**: <1 minute per transcript

### 2. Summarization
- **Input**: Chunks from step 1
- **Process**: Claude API summarizes each chunk (key points, definitions, workflows)
- **Output**: `summaries/chunk_*.json` (one per chunk)
- **Time**: 30-60 seconds per chunk
- **Confidence Tracking**: Marks low-confidence items for review

### 3. Consolidation
- **Input**: All chunk summaries
- **Process**: GPT consolidates into master document with proper formatting
- **Output**: `consolidated/master_notes.md`
- **Time**: 2-5 minutes
- **Quality**: Deduplicates content, resolves contradictions

### 4. Exam Materials
- **Input**: Master notes
- **Process**: Generate flashcards, questions, and revision sheet
- **Output**:
  - `exam_materials/flashcards.csv`
  - `exam_materials/practice_questions.md`
  - `exam_materials/quick_revision.md`
- **Time**: 3-5 minutes

## 🎓 Use Cases

### Students
- 📚 Quickly create study guides from course lectures
- 🎯 Generate practice exams to test knowledge
- 💾 Create flashcards for memorization
- 📄 Get one-page revision sheets before exams

### Educators
- 🏫 Auto-generate supplementary materials for courses
- ✅ Create practice questions from lecture transcripts
- 📊 Generate standardized study materials
- 🔍 Review and edit AI-generated content

### Researchers
- 📖 Process seminar transcripts into structured notes
- 🔎 Extract key concepts and definitions
- 📝 Build literature review materials
- 🎓 Create exam study guides from lecture series

### Content Creators
- 🎬 Transform podcast/video transcripts to written guides
- 📚 Create eBooks from transcribed content
- 🌐 Generate SEO-friendly study materials
- 💡 Repurpose existing educational content

## 🔍 Quality Assurance

The pipeline includes built-in QA measures:

1. **Confidence Scoring**: Each summary is marked as high/medium/low confidence
2. **Manual Review**: Flag low-confidence items for verification
3. **Duplication Detection**: Automatically removes duplicate content during consolidation
4. **Validation**: Cross-references facts between chunks
5. **Format Validation**: Ensures JSON/Markdown output is well-formed

### Review Low-Confidence Items
```json
{
  "chunk_id": "3",
  "confidence": "low",    // ⚠️ Needs review
  "summary": "..."
}
```

## 🚀 Performance Tips

### For Faster Processing
1. **Reduce chunk size**: `CHUNK_SIZE=1000` (faster, less context)
2. **Fewer retries**: `MAX_RETRIES=1` (risky, but faster)
3. **Parallel chunking**: Process multiple files in different runs
4. **Use lighter models**: Consider using cheaper GPT models

### For Better Quality
1. **Larger chunks**: `CHUNK_SIZE=2000` (better context)
2. **More retries**: `MAX_RETRIES=5` (handles transient errors)
3. **Claude model**: Claude is better for summarization than GPT
4. **Manual review**: Always review flagged items

## 📊 Cost Estimation

**Approximate API costs** (as of Nov 2024):

For a 50-page transcript (≈8000 chunks):

| Step | Model | Input Tokens | Output Tokens | Est. Cost |
|------|-------|-------------|---------------|-----------|
| Chunking | Local | 0 | 0 | Free |
| Summarize | Claude | 300K | 50K | ~$1.50 |
| Consolidate | GPT-4o | 100K | 50K | ~$0.50 |
| Exam Materials | GPT-4o | 50K | 100K | ~$0.30 |
| **Total** | - | - | - | ~$2.30 |

*Prices may vary; check current API pricing.*

## 🤝 Contributing

Contributions welcome! Areas for improvement:
- [ ] Embedding-based vector search
- [ ] Web UI for interactive editing
- [ ] PDF generation integration
- [ ] Support for audio transcription
- [ ] Multi-language support
- [ ] Database for storing results

## 📄 License

MIT License - See LICENSE file for details

## 🆘 Support

- **Documentation**: See docs/ directory and guides in this README
- **Issues**: Report bugs via GitHub Issues
- **Discussion**: Ask questions in GitHub Discussions
- **API Help**:
  - [Anthropic Documentation](https://docs.anthropic.com)
  - [OpenAI Documentation](https://platform.openai.com/docs)

## 🎉 Acknowledgments

Built with:
- [Anthropic Claude API](https://www.anthropic.com/claude)
- [OpenAI GPT-4o](https://openai.com/api)
- [OkHttp](https://square.github.io/okhttp/)
- [Jackson](https://github.com/FasterXML/jackson)

---

**Made with ❤️ for students and educators**

Transform your transcripts. Master your subjects. Ace your exams. 🚀
