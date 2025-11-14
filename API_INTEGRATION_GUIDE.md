# API Integration Guide

Complete guide to integrating and working with the Anthropic Claude and OpenAI APIs in this project.

## Table of Contents

1. [API Overview](#api-overview)
2. [Getting Started](#getting-started)
3. [Anthropic Claude API](#anthropic-claude-api)
4. [OpenAI API](#openai-api)
5. [Request/Response Examples](#requestresponse-examples)
6. [Error Handling](#error-handling)
7. [Rate Limiting & Quotas](#rate-limiting--quotas)
8. [Cost Management](#cost-management)
9. [Troubleshooting](#troubleshooting)

---

## API Overview

The pipeline uses two AI models for different tasks:

| Task | Model | Provider | Reason |
|------|-------|----------|--------|
| **Chunking** | Local algorithm | N/A | Free, fast, preserves structure |
| **Summarization** | Claude 3.5 Sonnet | Anthropic | Better for detailed analysis |
| **Consolidation** | GPT-4o | OpenAI | Excellent at synthesis & formatting |
| **Exam Materials** | GPT-4o | OpenAI | Good at structured question generation |

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

#### OpenAI

1. Visit [platform.openai.com](https://platform.openai.com/)
2. Sign up or log in
3. Navigate to API Keys
4. Create new secret key
5. Copy the key (format: `sk-xxxxx`)

**Cost**: Pay-as-you-go, requires credit card

### Step 2: Configure in Project

Create `.env` file:

```env
CLAUDE_API_KEY=sk-ant-xxxxxxxxxxxxx
OPENAI_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxx
```

### Step 3: Verify Keys

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

### Monitoring Usage

**Claude**:
```bash
# Check usage at console.anthropic.com
# View billing and quota
```

**OpenAI**:
```bash
# Check usage at platform.openai.com/account/usage
# View billing and quota
# Set usage limits for cost control
```

### Cost Estimation

**Per lecture (50 minutes, ~8000 tokens)**:

| Step | Tokens | Cost |
|------|--------|------|
| Chunking | 0 | Free |
| Summarization | 300K input, 50K output | ~$1.35 |
| Consolidation | 100K input, 50K output | ~$0.50 |
| Exam Materials | 50K input, 100K output | ~$0.30 |
| **Total** | - | **~$2.15** |

**Course cost** (10 lectures): ~$20-30

---

## Cost Management

### Strategies to Reduce Costs

#### 1. Use Local Chunking
```env
USE_API_CHUNKING=false  # Default - uses local algorithm (free)
```

#### 2. Optimize Chunk Size
```env
# Larger chunks = fewer summaries = lower cost
CHUNK_SIZE=2000         # Default: 1500

# Trade-off: Less context per chunk
```

#### 3. Use Cheaper Models (if acceptable)
```env
# For chunking (hypothetically)
MODEL_CLAUDE=claude-3-5-haiku-20241022  # 60% cheaper

# For consolidation
MODEL_GPT=gpt-3.5-turbo  # 90% cheaper (but lower quality)
```

#### 4. Batch Similar Content
```bash
# Process related lectures together
# Enables summary reuse in consolidation
```

#### 5. Set Usage Limits

**OpenAI**:
```
Go to https://platform.openai.com/account/billing/limits
Set: Hard limit = $20/month
Soft limit = $15/month (notification)
```

**Anthropic**:
```
Monitor at console.anthropic.com
Consider setting budget alerts
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
- [Anthropic Claude API Docs](https://docs.anthropic.com/)
- [OpenAI API Docs](https://platform.openai.com/docs/)

### Useful Links
- [Claude Console](https://console.anthropic.com/)
- [OpenAI Platform](https://platform.openai.com/)
- [API Status Pages](https://status.anthropic.com/, https://status.openai.com/)

### Support
- Anthropic Support: support@anthropic.com
- OpenAI Support: help@openai.com
- GitHub Issues: Report bugs and request features

---

**Last Updated**: November 2024
**API Versions**: Claude 3.5, GPT-4o
