```markdown
# Quick Revision Guide: Large Language Models (LLMs)

## Must-Know Concepts
- LLMs compute **probabilities** over vocabulary; "large" refers to parameter count.
- **Architectures**:
  - **Encoder**: Converts text to vectors (e.g., BERT).
  - **Decoder**: Generates text sequences (e.g., GPT-4).
  - **Encoder-Decoder**: Combines both for tasks like translation.
- **Prompting**: Influences model outputs by altering input structures.
- **In-context Learning**:
  - **Zero-shot**: No examples provided.
  - **K-shot**: Includes k examples for guidance.
- **Advanced Prompting Techniques**:
  - **Chain-of-Thought**: Breaks problems into smaller steps.
  - **Least-to-Most**: Solves simpler tasks first.
  - **Concept-Based**: Uses first principles for problem-solving.
- **Prompt Injection**: Manipulating inputs for unintended outputs.
- **Training Methods**:
  - **Fine-tuning**: Adjusts all model parameters.
  - **LoRA**: Modifies only specific parameters for efficiency.
- **Soft Prompting**: Adds trainable parameters to input cues.
- **Decoding Strategies**:
  - **Greedy Decoding**: Chooses highest probability word.
  - **Nucleus Sampling**: Samples from a portion of the output distribution.
  - **Beam Search**: Generates multiple sequences, selects the best.
- **Hallucination**: Text not grounded in data, often factually incorrect.
- **Mitigation**: **Retrieval-Augmented Generation (RAG)** uses external context to reduce hallucination.
- **Limitations**: Code models fix bugs only 15% of the time; poor performance on structured tasks.
- **Multi-modal Models**: Handle multiple data types (text, images, audio).
- **Language Agents**: Execute sequential actions in decision-making tasks.

## Key Definitions
- **Hallucination**: Generating content not based on training data.
- **Prompt Engineering**: Refining input structures for better outputs.
- **Soft Prompting**: Adding adjustable parameters to model prompts.

## Important Formulas/Processes
- **Training**:
  - **Fine-tuning** = Adjusting all parameters.
  - **LoRA** = Adjusting only a subset of parameters.

## High-Yield Exam Tips
- Focus on understanding the differences between **encoder** and **decoder** architectures.
- Memorize key prompting techniques and their uses.
- Be familiar with decoding strategies and their implications on output quality.
- Understand the concept of hallucination and the mitigation strategies available.
- Practice multiple-choice and short-answer questions to solidify knowledge.
```