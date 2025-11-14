# API Integration Guide

Complete guide to integrating and working with the Anthropic Claude, OpenAI, and Google Gemini APIs in this project.

## Table of Contents

1. [API Overview](#api-overview)
2. [Getting Started](#getting-started)
   - [Step 1: Create API Accounts](#step-1-create-api-accounts)
   - [Step 2: Configure in Project](#step-2-configure-in-project)
   - [Step 3: Test API Keys](#step-3-test-api-keys) ⭐ **New**
3. [Anthropic Claude API](#anthropic-claude-api)
4. [OpenAI API](#openai-api)
5. [Google Gemini API](#google-gemini-api)
6. [Request/Response Examples](#requestresponse-examples)
7. [Error Handling](#error-handling)
8. [Rate Limiting & Quotas](#rate-limiting--quotas)
9. [Cost Management](#cost-management)
10. [Troubleshooting](#troubleshooting)

---

## API Overview

The pipeline supports three AI models for different tasks, offering flexible configuration:

| Task | Default Model | Alternative Models | Provider |
|------|---------------|-------------------|----------|
| **Chunking** | Local algorithm | N/A | N/A (Free) |
| **Summarization** | Claude 3.5 Sonnet | Gemini 2.5 Pro | Anthropic / Google |
| **Consolidation** | GPT-4o | Gemini 2.5 Pro | OpenAI / Google |
| **Exam Materials** | GPT-4o | Gemini 2.5 Pro | OpenAI / Google |

**Model Selection**: Configure via environment variables:
```env
SUMMARIZER_MODEL=claude    # "claude" or "gemini"
CONSOLIDATOR_MODEL=gpt     # "gpt" or "gemini"
```

---

## Getting Started

### Step 1: Create API Accounts

#### Anthropic Claude

1. Visit [console.anthropic.com](https://console.anthropic.com/)
2. Sign up or log in
3. Navigate to API Keys section
4. Create new API key
5. Copy the key (format: `sk-ant-xxxxx`)

**Cost**: Pay-as-you-go, no minimum
**Docs**: [Anthropic Documentation](https://docs.anthropic.com/)

#### OpenAI

1. Visit [platform.openai.com](https://platform.openai.com/)
2. Sign up or log in
3. Navigate to API Keys
4. Create new secret key
5. Copy the key (format: `sk-xxxxx`)

**Cost**: Pay-as-you-go, requires credit card
**Docs**: [OpenAI Documentation](https://platform.openai.com/docs/)

#### Google Gemini (Optional - Recommended for Cost Savings)

1. Visit [Google AI Studio](https://aistudio.google.com/app/apikey)
2. Sign in with Google Account
3. Click "Create API key"
4. Copy the key (format: `AIzaSy...`)
5. Keep it safe (don't commit to git)

**Cost**: Free tier with 60 req/min, 1M tokens/day; Pay-as-you-go for higher usage
**Docs**: [Gemini API Documentation](https://ai.google.dev/docs)
**Cost Savings**: 77-95% compared to Claude + GPT

### Step 2: Configure in Project

Create `.env` file:

```env
# Required
CLAUDE_API_KEY=sk-ant-xxxxxxxxxxxxx
OPENAI_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxx

# Optional (for cost savings)
GEMINI_API_KEY=AIzaSy_xxxxxxxxxxxxx

# Optional model selection (defaults: claude + gpt)
SUMMARIZER_MODEL=claude    # or gemini
CONSOLIDATOR_MODEL=gpt     # or gemini
```

### Step 3: Test API Keys

**Before running the pipeline, test your API keys to ensure they work correctly:**

#### Option 1: Using the Test Script (Recommended)

```bash
# Make the script executable (first time only)
chmod +x test-api-keys.sh

# Run the test
./test-api-keys.sh
```

**What it does:**
- ✓ Checks if .env file exists
- ✓ Builds the project if needed
- ✓ Tests each API key with a simple request
- ✓ Validates pipeline configuration
- ✓ Provides detailed error messages

**Expected output:**
```
========================================
API Key Tester
========================================

✓ Found .env file

Running API key tests...

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

=== Pipeline Configuration ===
Summarizer Model: claude
Consolidator Model: gpt
✓ Pipeline is properly configured and ready to use!

========================================
✓ API Key Test Completed Successfully
========================================
```

#### Option 2: Using Java Directly

```bash
# Build the project first
mvn clean package

# Run the API key tester
java -cp target/transcript-pipeline.jar com.transcript.pipeline.util.ApiKeyTester
```

#### Interpreting Test Results

**All tests passed ✓**
- Your API keys are valid and working
- You can proceed with running the pipeline
- The selected models (SUMMARIZER_MODEL and CONSOLIDATOR_MODEL) are properly configured

**Some tests failed ✗**
- Check the error messages for specific issues
- Common errors:
  - **401 Unauthorized**: Invalid API key format or expired key
  - **403 Forbidden**: API key lacks required permissions
  - **429 Rate Limit**: Too many requests, wait and retry
  - **Timeout**: Network issues or slow connection

**Example: API key not configured**
```
Testing Claude API key...
⚠ Claude API key not configured. Skipping test.
```
**Solution**: Add the API key to your `.env` file

**Example: Invalid API key**
```
Testing OpenAI API key...
✗ OpenAI API test failed: API request failed: 401 - Invalid API key
  → Invalid API key. Please check your OPENAI_API_KEY.
```
**Solution**: Verify the API key is correct in `.env`

**Example: Configuration mismatch**
```
✗ SUMMARIZER_MODEL is set to 'claude' but Claude API key is not working!
```
**Solution**: Either fix the Claude API key or change SUMMARIZER_MODEL to 'gemini'

### Step 4: Verify Keys (Alternative Method)

```bash
java -jar target/transcript-pipeline.jar

# Should show:
# ✅ Configuration initialized
# === Pipeline Configuration ===
```

---

## Anthropic Claude API

### Authentication

```java
String apiKey = System.getenv("CLAUDE_API_KEY");
request.addHeader("Authorization", "Bearer " + apiKey);
request.addHeader("anthropic-version", "2023-06-01");
```

### Available Models

| Model | Capabilities | Cost (Input/Output) | Latency |
|-------|--------------|-------------------|---------|
| **claude-3-5-sonnet-20241022** | Best all-around | $3/$15 per 1M tokens | ~1-3 sec |
| **claude-opus-4-1** | Most capable | $15/$60 per 1M tokens | ~3-5 sec |
| **claude-3-5-haiku-20241022** | Fastest/cheapest | $0.80/$4 per 1M tokens | <1 sec |

**Recommended**: `claude-3-5-sonnet-20241022` (best balance)

### Request Format

```json
{
  "model": "claude-3-5-sonnet-20241022",
  "max_tokens": 4096,
  "system": "You are a helpful assistant...",
  "messages": [
    {
      "role": "user",
      "content": "Summarize this text..."
    }
  ]
}
```

### Response Format

```json
{
  "id": "msg_xxxxx",
  "type": "message",
  "role": "assistant",
  "content": [
    {
      "type": "text",
      "text": "Response content here..."
    }
  ],
  "model": "claude-3-5-sonnet-20241022",
  "stop_reason": "end_turn",
  "usage": {
    "input_tokens": 250,
    "output_tokens": 150
  }
}
```

### Java Integration

```java
// Create client
ApiClient claudeClient = ApiClient.createClaudeClient();

// Send request
String response = claudeClient.sendPromptToClaude(
    "You are a summarizer.",
    "Summarize this transcript..."
);

// Response is already extracted as text
System.out.println(response);
```

### Max Tokens by Model

| Model | Context Window | Recommended Max Output |
|-------|-----------------|------------------------|
| Claude Sonnet | 200K | 4096 |
| Claude Opus | 200K | 4096 |
| Claude Haiku | 200K | 2048 |

---

## OpenAI API

### Authentication

```java
String apiKey = System.getenv("OPENAI_API_KEY");
request.addHeader("Authorization", "Bearer " + apiKey);
request.addHeader("Content-Type", "application/json");
```

### Available Models

| Model | Capabilities | Cost (Input/Output) | Latency |
|-------|--------------|-------------------|---------|
| **gpt-4o** | Best all-around | $5/$15 per 1M tokens | ~1-2 sec |
| **gpt-4-turbo** | More capable | $10/$30 per 1M tokens | ~2-3 sec |
| **gpt-3.5-turbo** | Cheaper/faster | $0.50/$1.50 per 1M tokens | <1 sec |

**Recommended**: `gpt-4o` (best for consolidation)

### Request Format

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
      "content": "Create flashcards from..."
    }
  ],
  "temperature": 0.7,
  "max_tokens": 2000
}
```

### Response Format

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
        "content": "Response content here..."
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 250,
    "completion_tokens": 150,
    "total_tokens": 400
  }
}
```

### Java Integration

```java
// Create client
ApiClient openaiClient = ApiClient.createOpenAIClient();

// Send request
String response = openaiClient.sendPromptToOpenAI(
    "You are an educator.",
    "Create flashcards from..."
);

// Response is already extracted as text
System.out.println(response);
```

### Max Tokens by Model

| Model | Context Window | Recommended Max Output |
|-------|-----------------|------------------------|
| GPT-4o | 128K | 2048-4096 |
| GPT-4 Turbo | 128K | 2048-4096 |
| GPT-3.5 Turbo | 16K | 1024-2048 |

---

## Google Gemini API

### Authentication

```java
String apiKey = System.getenv("GEMINI_API_KEY");
// Uses OpenAI-compatible format
request.addHeader("Authorization", "Bearer " + apiKey);
request.addHeader("Content-Type", "application/json");
```

**Key Format**: `AIzaSy...` (from [Google AI Studio](https://aistudio.google.com/app/apikey))

### Available Models

| Model | Capabilities | Cost (Input/Output) | Latency | Notes |
|-------|--------------|-------------------|---------|-------|
| **gemini-2.5-flash** | Best all-around | $0.075/$0.30 per 1M tokens | ~1-2 sec | Latest, recommended |
| **gemini-2.0-pro** | Capable | $0.075/$0.30 per 1M tokens | ~1-2 sec | Previous version |
| **gemini-1.5-pro** | Good | $0.075/$0.30 per 1M tokens | ~1-2 sec | Older version |

**Recommended**: `gemini-2.5-flash` (latest, best performance)
**Free Tier**: 60 requests/minute, 1M tokens/day
**Documentation**: [Gemini Models](https://ai.google.dev/models)

### Request Format (OpenAI-Compatible)

```json
{
  "model": "gemini-2.5-flash",
  "messages": [
    {
      "role": "system",
      "content": "You are a helpful assistant..."
    },
    {
      "role": "user",
      "content": "Summarize this text..."
    }
  ],
  "temperature": 0.7,
  "max_tokens": 2000
}
```

### Response Format

```json
{
  "choices": [
    {
      "message": {
        "role": "assistant",
        "content": "Response content here..."
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 250,
    "completion_tokens": 150,
    "total_tokens": 400
  }
}
```

**Note**: Gemini uses OpenAI-compatible API format for seamless integration.

### Java Integration

```java
// Create client
ApiClient geminiClient = ApiClient.createGeminiClient();

// Send request (uses OpenAI-compatible format)
String response = geminiClient.sendPromptToGemini(
    "You are a summarizer.",
    "Summarize this transcript..."
);

// Response is already extracted as text
System.out.println(response);
```

### Max Tokens by Model

| Model | Context Window | Recommended Max Output |
|-------|-----------------|------------------------|
| Gemini 2.5 Pro | 1M | 2048-4096 |
| Gemini 2.0 Pro | 1M | 2048-4096 |
| Gemini 1.5 Pro | 1M | 2048-4096 |

### Key Differences from Claude/OpenAI

| Aspect | Gemini | Claude | OpenAI |
|--------|--------|--------|--------|
| **API Format** | OpenAI-compatible | Proprietary | OpenAI standard |
| **Cost** | Lowest | Medium | Medium-High |
| **Speed** | Fast | Medium | Medium |
| **Context Window** | 1M tokens | 200K tokens | 128K tokens |
| **Quality** | Very Good | Excellent | Excellent |

---

## Request/Response Examples

### Example 1: Summarizing a Chunk (Claude)

**Request**:
```java
String systemPrompt = """
    You are an expert summarizer. For the chunk below produce JSON with fields:
    {"chunk_id": "...", "summary": "...", "confidence": "high|medium|low"}
    Output only the JSON.
    """;

String userPrompt = """
    Chunk ID: 1
    Chunk Title: Cell Biology

    Chunk Text:
    Cells are the basic units of life...
    """;

String response = claudeClient.sendPromptToClaude(systemPrompt, userPrompt);
```

**Response**:
```json
{
  "chunk_id": "1",
  "title": "Cell Biology",
  "summary": "Cells are fundamental units of life with two main types: prokaryotic (simple, no nucleus) and eukaryotic (complex, with nucleus). Each cell type has specific structures enabling different functions.",
  "key_points": ["Basic unit of life", "Two main types", "Structural variations"],
  "confidence": "high"
}
```

### Example 2: Generating Flashcards (OpenAI)

**Request**:
```java
String systemPrompt = """
    Create flashcards in CSV format.
    Output only CSV: "Front","Back"
    """;

String userPrompt = """
    Create flashcards from:
    Photosynthesis is the process by which plants...
    """;

String response = openaiClient.sendPromptToOpenAI(systemPrompt, userPrompt);
```

**Response**:
```csv
"Front","Back"
"What is photosynthesis?","Process converting light energy to chemical energy in plants"
"Define: Chlorophyll","Green pigment absorbing light in plant cells"
"What are the products of photosynthesis?","Glucose and oxygen"
```

### Example 3: Consolidating to Master Notes (OpenAI)

**Request**:
```java
String systemPrompt = """
    Consolidate chunk summaries into exam-ready Markdown.
    Include Master Notes, Quick Revision, and Practice Questions.
    """;

String userPrompt = """
    Chunk 1: Summary of photosynthesis...
    Chunk 2: Summary of cellular respiration...
    Chunk 3: Summary of metabolism...
    """;

String response = openaiClient.sendPromptToOpenAI(systemPrompt, userPrompt);
```

**Response**:
```markdown
# Master Notes

## Photosynthesis
Light energy converts to chemical energy...

### Key Points
- Light-dependent reactions
- Light-independent reactions
- Occurs in chloroplasts

## Cellular Respiration
...

## Quick Revision
- Photosynthesis: Light → Energy
- Respiration: Energy → Work
...

## Practice Questions
...
```

### Example 4: Summarizing with Gemini (Cost-Optimized)

**Request**:
```java
// Use Gemini for cost savings (77% less than Claude)
ApiClient geminiClient = ApiClient.createGeminiClient();

String systemPrompt = """
    You are an expert summarizer. For the chunk below produce JSON with fields:
    {"chunk_id": "...", "summary": "...", "confidence": "high|medium|low"}
    Output only the JSON.
    """;

String userPrompt = """
    Chunk ID: 1
    Chunk Title: Cell Biology

    Chunk Text:
    Cells are the basic units of life...
    """;

String response = geminiClient.sendPromptToGemini(systemPrompt, userPrompt);
```

**Response** (same format as Claude):
```json
{
  "chunk_id": "1",
  "title": "Cell Biology",
  "summary": "Cells are fundamental units of life with two main types: prokaryotic and eukaryotic. Each cell type has specific structures enabling different functions.",
  "key_points": ["Basic unit of life", "Two main types", "Structural variations"],
  "confidence": "high"
}
```

**Cost Comparison**:
- Claude: ~$1.50 per chunk summarization
- Gemini: ~$0.35 per chunk summarization (77% savings!)
- Budget: Use Gemini for both (95% savings total)

---

## Error Handling

### Common Errors & Solutions

#### 1. 401 Unauthorized

```
Error: API request failed: 401 - {"error": {"message": "Unauthorized", "type": "invalid_request_error"}}
```

**Causes**:
- Invalid API key
- Expired API key
- Wrong API key for model

**Solution**:
```bash
# Check API key is valid
echo $CLAUDE_API_KEY

# Regenerate key from console
# Update .env file
# Restart application
```

#### 2. 429 Rate Limited

```
Error: API request failed: 429 - {"error": {"message": "Rate limit exceeded"}}
```

**Causes**:
- Too many requests per minute
- Quota exceeded for billing period
- Burst of requests

**Solution**:
```env
# Increase backoff time
RETRY_BACKOFF=5000  # Wait longer between retries

# Or reduce request frequency
# Wait longer between chunk processing
```

#### 3. 500 Server Error

```
Error: API request failed: 500 - Internal Server Error
```

**Causes**:
- API service issue
- Temporary outage
- Bug in API

**Solution**:
- Retry automatically (application does this)
- Check API status page
- Wait a few minutes and try again

#### 4. Malformed Response

```
Error: Unexpected API response format
```

**Causes**:
- API returned unexpected format
- JSON parsing failed
- Empty response

**Solution**:
```java
// Application creates fallback summary
// Check logs for details
// May be marked as "low confidence"
```

#### 5. Gemini-Specific Errors

**Issue**: "GEMINI_API_KEY not configured"
```
Error: GEMINI_API_KEY not configured but SUMMARIZER_MODEL is set to 'gemini'
```

**Solution**:
- Visit [Google AI Studio](https://aistudio.google.com/app/apikey)
- Create API key (format: `AIzaSy...`)
- Add to `.env` file: `GEMINI_API_KEY=AIzaSy_xxxxx`
- Restart application

**Issue**: Gemini quota exceeded
```
Error: API request failed: 429 - Rate limit exceeded
```

**Solution**:
- Free tier: 60 requests/minute, 1M tokens/day
- Wait for quota reset (daily at UTC midnight)
- Or upgrade to paid plan for higher limits
- Or increase `RETRY_BACKOFF` in .env for longer waits

### Retry Logic

```java
// Automatic exponential backoff:
Attempt 1: Immediate
Attempt 2: Wait 1s (1000ms * 2^0)
Attempt 3: Wait 2s (1000ms * 2^1)
Attempt 4: Wait 4s (1000ms * 2^2)
Total: Up to 7 seconds before giving up
```

**Configuration**:
```env
MAX_RETRIES=3              # Number of retries
RETRY_BACKOFF=1000         # Initial backoff in ms
```

---

## Rate Limiting & Quotas

### Claude API Limits

**Free Trial**:
- $5 credit
- Expires after 3 months
- No strict rate limits (reasonable use)

**Paid Plan**:
- Pay-as-you-go pricing
- Rate limits by plan tier
- Standard: 10 req/sec, 10K req/day

**Documentation**: [Claude Rate Limits](https://docs.anthropic.com/en/docs/deploy/rate-limits)

### OpenAI API Limits

**Free Trial**:
- $18 credit (3 months)
- Rate limit: 3 requests/minute, 40K tokens/minute
- Model limit: GPT-3.5 Turbo only

**Paid Plan**:
- Pay-as-you-go
- Rate limits based on account age/spend
- Tier 1: 3,500 req/minute
- Tier 2+: Higher limits

**Documentation**: [OpenAI Rate Limits](https://platform.openai.com/docs/guides/rate-limits)

### Google Gemini API Limits

**Free Tier** (Recommended for Testing):
- 60 requests per minute
- 1M free tokens per day
- No credit card required
- Perfect for: Students, small projects, testing

**Paid Plan**:
- Pay-as-you-go: $0.075/1M input tokens, $0.30/1M output tokens
- No strict rate limits (scales with usage)
- Volume discounts available

**Documentation**: [Gemini Rate Limits](https://ai.google.dev/docs/ratelimit)

**Cost Comparison** (per 1M tokens):
- Claude Input: $3.00
- Claude Output: $15.00
- OpenAI Input: $5.00
- OpenAI Output: $15.00
- **Gemini Input: $0.075** ⭐ (97% cheaper!)
- **Gemini Output: $0.30** ⭐ (98% cheaper!)

### Monitoring Usage

**Claude**:
```bash
# Check usage at https://console.anthropic.com
# View billing and quota
```

**OpenAI**:
```bash
# Check usage at https://platform.openai.com/account/usage
# View billing and quota
# Set usage limits for cost control
```

**Google Gemini**:
```bash
# Check usage at https://aistudio.google.com/app/apikey
# View free tier remaining quota
# Monitor daily usage
```

### Cost Estimation

**Per lecture (50 minutes, ~8000 tokens)**:

| Configuration | Summarization Cost | Consolidation Cost | Total | Savings |
|---|---|---|---|---|
| **Claude + GPT (Default)** | ~$1.35 | ~$0.50 | **~$2.15** | Baseline |
| **Claude + Gemini** ⭐ | ~$1.35 | ~$0.07 | **~$1.42** | **34% savings** |
| **Gemini + Gemini** | ~$0.32 | ~$0.07 | **~$0.39** | **82% savings** |

**Course cost** (10 lectures):
- Claude + GPT: ~$20-30
- Claude + Gemini: ~$14-20 (save $6-10!)
- Gemini + Gemini: ~$4-8 (save $16-22!)

---

## Cost Management

### Strategies to Reduce Costs

#### 1. Use Local Chunking
```env
USE_API_CHUNKING=false  # Default - uses local algorithm (free)
```

#### 2. Switch to Gemini (Best Savings!) ⭐
```env
# 34% savings: Replace consolidation with Gemini
CONSOLIDATOR_MODEL=gemini

# 82% savings: Use Gemini for both steps (test quality first)
SUMMARIZER_MODEL=gemini
CONSOLIDATOR_MODEL=gemini
```

**Cost Impact**:
- Default (Claude + GPT): ~$2.15/lecture
- With Gemini consolidation: ~$1.42/lecture (save $0.73!)
- All Gemini: ~$0.39/lecture (save $1.76!)

#### 3. Optimize Chunk Size
```env
# Larger chunks = fewer summaries = lower cost
CHUNK_SIZE=2000         # Default: 1500
CHUNK_OVERLAP=300       # Increase if needed

# Trade-off: Less context per chunk
```

#### 4. Use Cheaper Models (if acceptable)
```env
# For summarization
MODEL_CLAUDE=claude-3-5-haiku-20241022  # 60% cheaper than Sonnet

# For consolidation (when not using Gemini)
MODEL_GPT=gpt-3.5-turbo  # 90% cheaper than GPT-4o
```

#### 5. Batch Similar Content
```bash
# Process related lectures together
# Enables summary reuse in consolidation
```

#### 6. Set Usage Limits

**Claude** (Anthropic):
```
Visit: https://console.anthropic.com
Monitor billing and set budget alerts
```

**OpenAI**:
```
Go to: https://platform.openai.com/account/billing/limits
Set: Hard limit = $20/month
Soft limit = $15/month (notification)
```

**Gemini** (Google):
```
Visit: https://aistudio.google.com/app/apikey
Free tier: 1M tokens/day (automatic reset)
Paid tier: Set billing alerts in Google Cloud Console
```

### Cost Tracking

Log all API usage:

```
DEBUG: API Request - Claude Summarization
  - Input tokens: 250
  - Output tokens: 150
  - Cost: ~$0.01

DEBUG: API Request - GPT Consolidation
  - Input tokens: 5000
  - Output tokens: 2000
  - Cost: ~$0.08
```

---

## Troubleshooting

### Issue: "API Key Not Configured"

**Message**:
```
❌ ERROR: API keys not configured!
Please set CLAUDE_API_KEY and OPENAI_API_KEY environment variables
```

**Checklist**:
- [ ] `.env` file exists in project root
- [ ] `.env` has correct format: `KEY=value`
- [ ] API keys are correct (check console)
- [ ] No spaces around `=` in `.env`
- [ ] Restart application after editing `.env`

**Test**:
```bash
# Verify key format
echo $CLAUDE_API_KEY  # Should show sk-ant-xxxxx

# Or in .env
cat .env | grep CLAUDE_API_KEY
```

### Issue: Timeout Errors

**Message**:
```
Error: API request failed: Read timed out
Retrying in 1000 ms...
```

**Causes**:
- Slow network connection
- Large request payload
- API server slow
- Local resource constraints

**Solutions**:
```env
# Increase timeout (default: 60s)
API_TIMEOUT=120000  # 2 minutes

# Or reduce request size
CHUNK_SIZE=1000     # Smaller chunks
```

### Issue: Low Quality Summaries

**Symptoms**:
- Low confidence items
- Incomplete summaries
- Hallucinated content

**Solutions**:
```env
# Use better model
MODEL_CLAUDE=claude-opus-4-1  # More capable

# Or use larger chunks for better context
CHUNK_SIZE=2000

# Or use API-based chunking
USE_API_CHUNKING=true  # Better quality (costs more)
```

**Manual Fix**:
1. Review low-confidence items in JSON
2. Edit directly in text editor
3. Re-run consolidation (Option 4 in menu)

### Issue: API Quota Exceeded

**Message**:
```
Error: API request failed: 429 - Rate limit exceeded
```

**Quick Fix**:
```env
# Increase wait time between calls
RETRY_BACKOFF=10000  # 10 seconds

# Or wait and retry later
```

**Long-term Fix**:
- Monitor usage at provider console
- Upgrade to paid plan for higher limits
- Use cheaper models
- Reduce request frequency

### Issue: Output Files Are Empty

**Cause**:
- API returned empty response
- JSON parsing failed
- Fallback mechanisms triggered

**Solution**:
1. Check application logs
2. Manually review outputs
3. Re-run problematic step
4. Check API status

### Issue: Out of Memory

**Message**:
```
Exception in thread "main" java.lang.OutOfMemoryError
```

**Solution**:
```bash
# Increase Java heap size
java -Xmx4g -jar target/transcript-pipeline.jar

# Or reduce chunk size in .env
CHUNK_SIZE=1000
```

---

## Advanced Topics

### Custom API Endpoints (Proxies)

```env
# For corporate proxy
CLAUDE_API_BASE=https://proxy.company.com/anthropic
OPENAI_API_BASE=https://proxy.company.com/openai

# Ensure proxy supports the APIs
# May need authentication credentials (not yet supported)
```

### Custom Prompts

Edit prompts in service classes:
- `ChunkerService.java` - Chunking prompt
- `SummarizerService.java` - Summarization prompt
- `ConsolidatorService.java` - Consolidation prompt

### Async Processing (Future)

Currently sequential. For scaling:
```java
// ExecutorService for parallel API calls
ExecutorService executor = Executors.newFixedThreadPool(3);

// Process chunks in parallel
chunks.parallelStream()
    .forEach(chunk -> summarizeChunk(chunk));
```

---

## Resources

### Official Documentation

**Anthropic Claude**:
- [Claude API Documentation](https://docs.anthropic.com/)
- [Rate Limits & Quotas](https://docs.anthropic.com/en/docs/deploy/rate-limits)
- [Models & Pricing](https://docs.anthropic.com/en/docs/about/models)

**OpenAI**:
- [OpenAI API Docs](https://platform.openai.com/docs/)
- [Rate Limits](https://platform.openai.com/docs/guides/rate-limits)
- [Pricing Calculator](https://openai.com/pricing)

**Google Gemini** (New!):
- [Gemini API Documentation](https://ai.google.dev/docs)
- [Available Models](https://ai.google.dev/models)
- [Rate Limits & Quotas](https://ai.google.dev/docs/ratelimit)
- [Pricing](https://ai.google.dev/pricing)

### API Consoles & Management

**Anthropic**:
- [Console (API Keys, Usage)](https://console.anthropic.com/)
- [Status Page](https://status.anthropic.com/)

**OpenAI**:
- [Platform (API Keys, Usage, Billing)](https://platform.openai.com/)
- [Usage Dashboard](https://platform.openai.com/account/usage/overview)
- [Billing & Limits](https://platform.openai.com/account/billing/limits)
- [Status Page](https://status.openai.com/)

**Google Gemini**:
- [AI Studio (API Keys, Free Tier)](https://aistudio.google.com/app/apikey)
- [Google Cloud Console (Billing)](https://console.cloud.google.com/)
- [Status Page](https://status.cloud.google.com/)

### Comparison & Articles

- [Gemini vs Claude vs GPT Comparison](https://ai.google.dev/docs)
- [Cost Analysis: Claude vs OpenAI vs Gemini](GEMINI_INTEGRATION_GUIDE.md)
- [Model Selection Guide](README.md#-Multi-Model-Support)

### Support

- **Anthropic**: [support@anthropic.com](mailto:support@anthropic.com)
- **OpenAI**: [help@openai.com](mailto:help@openai.com)
- **Google**: [Gemini Support](https://support.google.com/googleplay/)
- **Project GitHub**: Report bugs and request features in GitHub Issues

---

**Last Updated**: November 2024
**API Versions**: Claude 3.5 Sonnet, GPT-4o, Gemini 2.5 Pro
