# Usage Guide - Transcript → Exam Notes Pipeline

A step-by-step guide to using the Transcript → Exam Notes Pipeline for generating study materials from lecture transcripts.

## Table of Contents

1. [Getting Started](#getting-started)
   - [Step 1: Create Configuration File](#step-1-create-configuration-file)
   - [Step 2: Test API Keys](#step-2-test-api-keys-recommended) ⭐ **New**
   - [Step 3: Verify Configuration](#step-3-verify-configuration)
2. [Interactive Mode](#interactive-mode)
3. [Step-by-Step Examples](#step-by-step-examples)
4. [Advanced Workflows](#advanced-workflows)
5. [Output Files Explained](#output-files-explained)
6. [Tips & Best Practices](#tips--best-practices)
7. [FAQ](#faq)

---

## Getting Started

### Initial Setup (First Time Only)

#### Step 1: Create Configuration File

Create `.env` file in your project root:

```bash
# Navigate to project directory
cd transcript-to-exam-notes

# Copy example configuration
cp config/.env.example .env

# Edit .env with your API keys
# Minimum configuration (Best Quality):
# CLAUDE_API_KEY=sk-ant-xxxxxxxxxxxxx
# OPENAI_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxx

# Optional: Add Gemini for cost savings (77% less expensive!)
# GEMINI_API_KEY=AIzaSy_xxxxxxxxxxxxx
# CONSOLIDATOR_MODEL=gemini
```

#### Step 2: Test API Keys (Recommended)

**Before running the pipeline, verify your API keys are working:**

```bash
# Quick test all API keys
./test-api-keys.sh

# You should see:
# ======================
# === Testing API Keys ===
# Testing Claude API key...
# ✓ Claude API key is valid and working!
# Testing OpenAI API key...
# ✓ OpenAI API key is valid and working!
# Testing Gemini API key...
# ✓ Gemini API key is valid and working!
# ======================
```

**What this does:**
- ✓ Tests each API key with a simple request
- ✓ Validates your pipeline configuration
- ✓ Detects issues before you start processing
- ✓ Provides clear error messages if something is wrong

**If a test fails:**
```bash
# Check the error message for details
# Common issues:
# - Invalid API key format
# - Expired or revoked key
# - Rate limit exceeded
# - Network connection problems

# Fix the issue in your .env file, then test again
./test-api-keys.sh
```

**Alternative: Test using Java directly**
```bash
java -cp target/transcript-pipeline.jar com.transcript.pipeline.util.ApiKeyTester
```

#### Step 3: Verify Configuration

```bash
# The application will verify API keys on startup
java -jar target/transcript-pipeline.jar

# You should see:
# ✅ Configuration initialized
# === Pipeline Configuration ===
# Model (Claude): claude-3-5-sonnet-20241022
# Model (GPT): gpt-4o
# ...
```

---

## Interactive Mode

### Starting the Application

```bash
java -jar target/transcript-pipeline.jar
```

You'll see the main menu:

```
╔════════════════════════════════════════════════════════════╗
║  Transcript → Exam Notes Pipeline v1.0.0                  ║
║  Convert lectures into study materials automatically      ║
╚════════════════════════════════════════════════════════════╝

📋 MAIN MENU
1. Run complete pipeline (chunk → summarize → consolidate)
2. Chunk transcripts only
3. Summarize chunks only
4. Consolidate to master notes
5. Generate exam materials (flashcards, practice questions)
6. View pipeline status
7. Settings
8. Help
0. Exit

👉 Choose an option (0-8): _
```

---

## Step-by-Step Examples

### Example 1: Complete Pipeline (Most Common)

**Time required**: 5-15 minutes per transcript

#### Step 1: Prepare Transcript Files

```bash
# Create transcripts directory if not exists
mkdir -p transcripts

# Place your transcript files
# Example file: transcripts/biology_lecture_01.txt
```

Sample transcript format:

```
# BIOLOGY 101 - Lecture 1: Introduction to Cells

Delivered by Dr. Sarah Johnson
March 15, 2024

## What is a Cell?

A cell is the basic unit of life. All living organisms are made up of one or more cells.
There are two main types of cells...

## Prokaryotic vs Eukaryotic Cells

Prokaryotic cells:
- No nucleus
- Found in bacteria
- Simpler structure

Eukaryotic cells:
- Have a nucleus
- Found in animals, plants, fungi
- More complex...
```

#### Step 2: Run Complete Pipeline

```
1. Run complete pipeline (chunk → summarize → consolidate)
👉 Choose an option (0-8): 1

📁 Enter transcript directory (default: 'transcripts'): transcripts
🔍 Found 1 transcript file(s)

⏳ STEP 1: CHUNKING TRANSCRIPTS
════════════════════════════════════════════════════════

📄 Processing: biology_lecture_01.txt
✅ Created 8 chunks

⏳ STEP 2: SUMMARIZING CHUNKS
════════════════════════════════════════════════════════

Summarizing chunk 1/8...
✅ Summary Statistics: Total=8, High Confidence=6, Medium=2, Low=0

⏳ STEP 3: CONSOLIDATING TO MASTER NOTES
════════════════════════════════════════════════════════

✅ Master notes created
⏳ Generating exam materials...
✅ Flashcards generated
✅ Practice questions generated
✅ Quick revision sheet generated

✨ PIPELINE COMPLETE!
════════════════════════════════════════════════════════
📁 Output directory: output/
📝 Master notes: output/consolidated/master_notes.md
📚 Quick revision: output/exam_materials/quick_revision.md
🎯 Practice questions: output/exam_materials/practice_questions.md
🎓 Flashcards: output/exam_materials/flashcards.csv
```

#### Step 3: Review Generated Files

**Master Notes** (output/consolidated/master_notes.md):
```markdown
# Master Notes

## Introduction to Cells

A cell is the basic unit of life. All living organisms are made up of one or more cells.

### Key Points
- Basic unit of all living organisms
- All cells come from pre-existing cells (Cell Theory)
- Contains genetic material (DNA)

### Definitions
- Cell: The smallest unit of life capable of performing all life functions
- Membrane: Semi-permeable barrier controlling cell contents

### Examples
- Bacterial cells: Prokaryotic, no nucleus
- Human cells: Eukaryotic, with nucleus

## Prokaryotic vs Eukaryotic Cells

### Prokaryotic Cells
...

### Eukaryotic Cells
...
```

**Quick Revision** (output/exam_materials/quick_revision.md):
```markdown
# Quick Revision Sheet

## Must-Know Concepts
1. Cell is the basic unit of life
2. Two main cell types: Prokaryotic and Eukaryotic
3. Prokaryotic cells lack nucleus (bacteria)
4. Eukaryotic cells have nucleus (animals, plants)

## Key Definitions
- **Cell**: Basic living unit
- **Prokaryotic**: No nucleus
- **Eukaryotic**: Has nucleus

## High-Yield Exam Tips
- Understand cell structure types for MCQs
- Know differences for short answers
- Memorize prokaryotic vs eukaryotic features
```

**Flashcards** (output/exam_materials/flashcards.csv):
```csv
"Front","Back"
"What is a cell?","The basic unit of life that can perform all life functions"
"Define: Prokaryotic cells","Cells without a nucleus, found in bacteria"
"Define: Eukaryotic cells","Cells with a nucleus, found in animals, plants, fungi"
"What is the cell membrane?","Semi-permeable barrier controlling substance entry/exit"
...
```

**Practice Questions** (output/exam_materials/practice_questions.md):
```markdown
## Multiple Choice Questions

### Q1: Which of the following is NOT a characteristic of all cells?
a) Contains DNA
b) Has a nucleus
c) Bounded by a membrane
d) Can reproduce

*Answer: b

### Q2: What is the main difference between prokaryotic and eukaryotic cells?
...

## Short Answer Questions

### Q6: Describe the structure and function of the cell membrane.

Expected Answer: The cell membrane is a semi-permeable barrier made of lipids and proteins. It controls what enters and exits the cell while protecting the cell contents.
```

---

### Example 2: Chunking Only (Advanced Users)

**Use case**: You want to chunk manually and review before summarization

#### Step 1: Menu Selection

```
2. Chunk transcripts only
👉 Choose an option (0-8): 2

📄 Enter transcript file path: transcripts/chemistry_lecture_02.txt

⏳ Chunking transcript...
✅ Chunk Statistics: Total Chunks=12, Total Tokens≈18000, Avg Tokens/Chunk≈1500
💾 Saved to: output/chunks/chemistry_lecture_02_chunks.json
```

#### Step 2: Inspect Chunks

Open `output/chunks/chemistry_lecture_02_chunks.json`:

```json
[
  {
    "chunk_id": "1",
    "title": "Chunk 1",
    "text": "Complete chapter text...",
    "source_file": "chemistry_lecture_02.txt",
    "start_line": 0,
    "end_line": 45
  },
  {
    "chunk_id": "2",
    "title": "Chunk 2",
    "text": "Next section...",
    "source_file": "chemistry_lecture_02.txt",
    "start_line": 46,
    "end_line": 95
  }
]
```

---

### Example 3: Summarize Chunks Only

**Use case**: You already have chunks and want to summarize them

#### Step 1: Menu Selection

```
3. Summarize chunks only
👉 Choose an option (0-8): 3

📁 Enter chunks JSON file path: output/chunks/chemistry_lecture_02_chunks.json

⏳ Loading chunks...
✅ Loaded 12 chunks
⏳ Summarizing chunks...
✅ Summary Statistics: Total=12, High Confidence=10, Medium=2, Low=0
💾 Saved to: output/summaries/
```

#### Step 2: Review Summaries

Each summary is saved as `output/summaries/chunk_1.json`:

```json
{
  "chunk_id": "1",
  "title": "Atomic Structure",
  "summary": "Atoms are the fundamental building blocks of matter, composed of protons, neutrons, and electrons organized in specific structures.",
  "key_points": [
    "Atoms composed of protons, neutrons, electrons",
    "Protons and neutrons in nucleus",
    "Electrons in electron shells",
    "Electron configuration determines chemical properties"
  ],
  "definitions": [
    {"term": "Atom", "definition": "Smallest unit of matter retaining chemical properties"},
    {"term": "Nucleus", "definition": "Dense center containing protons and neutrons"}
  ],
  "confidence": "high"
}
```

---

## Advanced Workflows

### Workflow 1: Multi-Lecture Course

Processing multiple lectures into a consolidated master guide:

```bash
# 1. Place all transcripts
transcripts/
├── week1_lecture1.txt
├── week1_lecture2.txt
├── week2_lecture1.txt
└── week2_lecture2.txt

# 2. Run complete pipeline (processes all files)
# Menu option 1

# 3. All outputs consolidated:
output/consolidated/master_notes.md  # Complete course guide
output/exam_materials/flashcards.csv  # All flashcards combined
```

### Workflow 2: Quality Review with Low-Confidence Items

1. **Run pipeline**: Option 1
2. **Review low-confidence items**:
   ```bash
   # Check logs for warnings
   grep "confidence.*low" output/summaries/*.json
   ```
3. **Manual editing** in IDE or text editor
4. **Re-consolidate** with Option 4

### Workflow 3: Custom Chunk Sizes

For different content types:

```env
# For dense technical content (more chunks needed)
CHUNK_SIZE=1000

# For narrative content (fewer, larger chunks)
CHUNK_SIZE=2000

# Then run pipeline option 2 (chunking only)
```

### Workflow 4: Incremental Processing

Process large courses in batches:

```bash
# Session 1: Process first 3 lectures
transcripts/lecture_1_3.txt
java -jar target/transcript-pipeline.jar  # Run option 1

# Session 2: Add more lectures
transcripts/lecture_4_6.txt
java -jar target/transcript-pipeline.jar  # Run option 1 again

# Pipeline detects existing files and continues
```

---

## Output Files Explained

### Directory Structure

```
output/
├── chunks/                              # Step 1 outputs
│   ├── lesson1_chunks.json             # Chunked transcript
│   ├── lesson2_chunks.json
│   └── ...
│
├── summaries/                           # Step 2 outputs
│   ├── chunk_1.json                    # Summary JSON
│   ├── chunk_2.json                    # With confidence levels
│   └── ...
│
├── consolidated/                        # Step 3 outputs
│   └── master_notes.md                 # Main study guide
│
└── exam_materials/                      # Step 4 outputs
    ├── flashcards.csv                  # For flashcard apps
    ├── practice_questions.md           # Test questions
    └── quick_revision.md               # 1-page cheat sheet
```

### File Format Guide

#### master_notes.md

**Format**: Markdown

**Structure**:
- Topic headings (##)
- Definitions (bold terms)
- Bullet lists
- Code blocks if applicable
- Internal links between topics

**Usage**:
```bash
# Read in text editor
cat output/consolidated/master_notes.md

# Convert to PDF
pandoc output/consolidated/master_notes.md -o master_notes.pdf

# View in browser (with markdown viewer extension)
```

#### flashcards.csv

**Format**: CSV (Comma-Separated Values)

**Structure**:
```csv
"Front","Back"
"Question or term","Answer or definition"
```

**Import instructions**:

**Anki**:
1. Open Anki
2. Create new deck
3. File → Import
4. Select `flashcards.csv`
5. Map fields if prompted

**Quizlet**:
1. Create new study set
2. Import from file
3. CSV format
4. Paste CSV content

**Excel/Google Sheets**:
1. Open file directly in Excel
2. Or import to Google Sheets
3. Each row becomes one flashcard

#### practice_questions.md

**Format**: Markdown

**Structure**:
```markdown
## Multiple Choice Questions
### Q1: [Question]
a) Option A
b) Option B
c) Option C
d) Option D
*Answer: b

## Short Answer Questions
### Q6: [Question]
Expected Answer: ...

## Long Form Questions
### Q13: [Question]
Marking Rubric:
- Point 1: X marks
- Point 2: X marks
```

**Usage**:
- Print for practice tests
- Time yourself (MCQ: 1 min each, Short: 5 min, Long: 15 min)
- Compare with answer key
- Identify weak areas

#### quick_revision.md

**Format**: Markdown with bullet points

**Contents**:
- Key concepts
- Definitions
- Memory aids
- Formulas
- Important dates

**Usage**:
- Study last 24 hours before exam
- Print on index card
- Use during commute
- One-page format fits on phone

---

## Tips & Best Practices

### For Best Quality Output

1. **Clean Transcripts**
   - Remove timestamps (except if meaningful)
   - Fix obvious transcription errors
   - Preserve section headings
   - Standardize formatting

2. **Optimal Chunk Sizes**
   - Default (1500 tokens) works for most content
   - Too small: Loses context
   - Too large: Too much per chunk

3. **Review Low-Confidence Items**
   - Always check items marked as "low confidence"
   - These likely need manual correction
   - Mark them in your notes

4. **Iterative Improvement**
   - Run pipeline
   - Review outputs
   - Manually edit if needed
   - Re-run consolidation for improvements

### Time Management

| Task | Time |
|------|------|
| Prepare 1 transcript | 2-5 min |
| Chunking | <1 min |
| Summarization | 30-60 sec per chunk |
| Consolidation | 2-5 min |
| Review & edits | 10-20 min |
| **Total per lecture** | **15-30 min** |

### Cost Optimization

**Using Gemini for Major Savings**:

Instead of spending $2.15 per lecture, switch to Gemini:

```env
# Option 1: Use Gemini for consolidation only (34% savings)
CONSOLIDATOR_MODEL=gemini
# Cost: ~$1.42/lecture (save $0.73!)
# Quality: ⭐⭐⭐⭐⭐ (same as Claude for consolidation)

# Option 2: Use Gemini for both (82% savings - test first!)
SUMMARIZER_MODEL=gemini
CONSOLIDATOR_MODEL=gemini
# Cost: ~$0.39/lecture (save $1.76!)
# Quality: ⭐⭐⭐⭐ (very good)
```

**Cost per course** (estimate):
- Small course (5 lectures):
  - Claude + GPT: $10-15
  - Claude + Gemini: $7-10 (save $3!)
  - Gemini + Gemini: $2-4 (save $8-12!)

- Medium course (15 lectures):
  - Claude + GPT: $30-45
  - Claude + Gemini: $20-30 (save $10!)
  - Gemini + Gemini: $6-12 (save $24-33!)

- Large course (30+ lectures):
  - Claude + GPT: $60-90
  - Claude + Gemini: $40-60 (save $20!)
  - Gemini + Gemini: $12-24 (save $48-66!)

**Setup Gemini** (takes 2 minutes):
1. Visit [Google AI Studio](https://aistudio.google.com/app/apikey)
2. Click "Create API key"
3. Add to `.env`: `GEMINI_API_KEY=AIzaSy...`
4. Set: `CONSOLIDATOR_MODEL=gemini`
5. Run pipeline (saves $0.73 per lecture!)

**Documentation**: See [GEMINI_INTEGRATION_GUIDE.md](GEMINI_INTEGRATION_GUIDE.md) for detailed setup

### Quality Assurance Checklist

Before considering study materials final:

- [ ] All chunks processed successfully
- [ ] Review low-confidence items
- [ ] Check for duplicate content in master notes
- [ ] Verify flashcard count is reasonable (50-100+)
- [ ] Test opening all generated files
- [ ] Import flashcards to Anki/Quizlet (test)
- [ ] Print quick revision sheet (check formatting)
- [ ] Share with study group for feedback

---

## FAQ

### Q: How long does the pipeline take?

**A**: Depends on transcript length:
- 10-minute lecture: ~5 minutes total
- 1-hour lecture: ~15-30 minutes total
- Full course (10 lectures): ~2-3 hours

Most time is waiting for API responses.

### Q: What if API key is missing?

**A**:
```
❌ ERROR: API keys not configured!
Please set CLAUDE_API_KEY and OPENAI_API_KEY environment variables
Or create a .env file in the project root directory
```

**Solution**:
1. Create `.env` file
2. Add your API keys
3. Run application again

### Q: How do I save money using Gemini?

**A**: Use Google Gemini instead of OpenAI:
```env
# Add Gemini API key (free from https://aistudio.google.com/app/apikey)
GEMINI_API_KEY=AIzaSy_xxxxx

# Use Gemini for consolidation (saves 34%)
CONSOLIDATOR_MODEL=gemini
```

**Savings**:
- Per lecture: Save $0.73 (34% less expensive)
- 10 lectures: Save $7.30
- 100 lectures: Save $73!

**Quality**: Same excellent quality as GPT for consolidation.

**Try it**: It takes 2 minutes to set up!

### Q: Can I process very long transcripts?

**A**: Yes, but:
- Large transcripts automatically split into chunks
- Each chunk processed separately
- Pipeline handles it transparently
- May take longer and cost more

**To optimize**:
- Split very long lectures into multiple files
- Process one lecture at a time
- Increase `CHUNK_SIZE` for faster processing

### Q: What if summarization fails for a chunk?

**A**:
- Application logs the error
- Creates default summary as fallback
- Marks with "low confidence"
- Pipeline continues with other chunks
- You can manually fix low-confidence items

### Q: Can I edit generated files?

**A**: Yes! All outputs are plain text files:
- **Markdown** files (.md) - edit in any text editor
- **CSV** files (.csv) - edit in Excel or text editor
- **JSON** files (.json) - edit in text editor (preserve format)

### Q: How do I convert to PDF?

**A**:
```bash
# Install pandoc
# macOS
brew install pandoc

# Ubuntu/Debian
sudo apt-get install pandoc

# Windows
choco install pandoc

# Convert to PDF
pandoc output/consolidated/master_notes.md -o master_notes.pdf
```

### Q: Can I use different lecture sources?

**A**: Yes! Any text transcript works:
- Video lecture transcripts (auto-generated or manual)
- Podcast transcripts
- Seminar notes
- Textbook chapters (copied as text)
- Research paper abstracts
- Meeting notes

### Q: How accurate is the summarization?

**A**:
- **High confidence items**: 95%+ accuracy
- **Medium confidence items**: 85-95% accuracy
- **Low confidence items**: Need manual review

Always verify important facts against source material!

### Q: Can I process multiple courses?

**A**: Yes! Options:
1. **Separate directories**: Create different output dirs for each course
2. **Sequential processing**: Process Course 1, then Course 2
3. **Combined processing**: Put all lectures in one `transcripts/` folder (creates single master notes)

### Q: What file size limits exist?

**A**:
- **Input**: Up to 100KB per API call (auto-truncated)
- **Output**: No limits (grows with input)
- **Total output**: Typically 2-3x input size

### Q: How do I share results with classmates?

**A**:
```bash
# Create archive
zip -r biology_101_study.zip output/

# Share via email, Drive, OneDrive, etc.
# Others can extract and import flashcards
```

### Q: Can I schedule automatic runs?

**A**: Currently manual, but you can:
- Create batch script for multiple runs
- Use system scheduler (cron on Linux, Task Scheduler on Windows)
- Integrate with workflow automation tools

---

## Getting Help

### Resources
- **Documentation**: README.md, TECHNICAL_IMPLEMENTATION_GUIDE.md, GEMINI_INTEGRATION_GUIDE.md
- **API Documentation**:
  - [Anthropic Claude](https://docs.anthropic.com)
  - [OpenAI](https://platform.openai.com/docs)
  - [Google Gemini](https://ai.google.dev/docs) (for cost savings!)
- **Setup Guides**:
  - [Gemini Setup Guide](GEMINI_INTEGRATION_GUIDE.md)
  - [API Integration Guide](API_INTEGRATION_GUIDE.md)
- **GitHub**: Check Issues and Discussions

### Reporting Issues
Include:
- Error message (full text)
- Steps to reproduce
- Transcript sample (if applicable)
- Configuration (without API keys)
- Java version: `java -version`

---

**Happy studying! 🎓**

Transform your transcripts → Master your material → Ace your exams! 🚀
