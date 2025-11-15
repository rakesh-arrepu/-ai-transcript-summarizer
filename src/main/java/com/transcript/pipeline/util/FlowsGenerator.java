package com.transcript.pipeline.util;

import com.transcript.pipeline.models.ChunkSummary;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for generating visual flows and diagrams from pipeline data.
 * Supports multiple output formats: Mermaid, ASCII art, and text-based representations.
 *
 * This optional feature helps visualize multi-step processes, workflows, and pipeline execution.
 */
public class FlowsGenerator {

    private static final Logger logger = LoggerFactory.getLogger(FlowsGenerator.class);

    private FlowsGenerator() {
        // Utility class - no instantiation
    }

    /**
     * Generate a Mermaid flowchart from workflow data.
     * Mermaid is a JavaScript-based diagramming and charting tool that renders Markdown.
     *
     * @param workflow The workflow to visualize
     * @param workflowIndex Index of the workflow (for unique IDs)
     * @return Mermaid flowchart syntax
     */
    public static String generateMermaidFlowchart(ChunkSummary.Workflow workflow, int workflowIndex) {
        StringBuilder sb = new StringBuilder();
        sb.append("```mermaid\n");
        sb.append("flowchart TD\n");

        if (workflow == null || workflow.getSteps() == null || workflow.getSteps().isEmpty()) {
            sb.append("    A[No steps defined]\n");
            sb.append("```\n");
            return sb.toString();
        }

        String workflowName = workflow.getName() != null ? workflow.getName() : "Workflow_" + workflowIndex;
        String startId = "START_" + workflowIndex;
        String endId = "END_" + workflowIndex;

        // Start node
        sb.append("    ").append(startId).append("[🚀 Start: ").append(sanitizeMermaidText(workflowName)).append("]\n");

        // Step nodes with sequential connections
        List<String> steps = workflow.getSteps();
        String previousId = startId;

        for (int i = 0; i < steps.size(); i++) {
            String stepId = "STEP_" + workflowIndex + "_" + i;
            String stepText = sanitizeMermaidText(steps.get(i));

            // Determine icon based on step position
            String icon = i == steps.size() - 1 ? "✓" : "→";
            sb.append("    ").append(stepId).append("[").append(icon).append(" ").append(stepText).append("]\n");
            sb.append("    ").append(previousId).append(" --> ").append(stepId).append("\n");

            previousId = stepId;
        }

        // End node
        sb.append("    ").append(endId).append("[✅ Complete]\n");
        sb.append("    ").append(previousId).append(" --> ").append(endId).append("\n");

        // Add notes if available
        if (workflow.getNotes() != null && !workflow.getNotes().isEmpty()) {
            String notesId = "NOTE_" + workflowIndex;
            sb.append("    ").append(notesId).append("[📝 Note: ").append(sanitizeMermaidText(workflow.getNotes())).append("]\n");
            sb.append("    ").append(previousId).append(" -.->|additional info| ").append(notesId).append("\n");
        }

        sb.append("```\n");
        return sb.toString();
    }

    /**
     * Generate ASCII art representation of a workflow (for terminal display).
     *
     * @param workflow The workflow to visualize
     * @return ASCII art representation
     */
    public static String generateAsciiFlowchart(ChunkSummary.Workflow workflow) {
        StringBuilder sb = new StringBuilder();

        if (workflow == null || workflow.getSteps() == null || workflow.getSteps().isEmpty()) {
            return "┌─────────────────┐\n│ No steps defined │\n└─────────────────┘\n";
        }

        String workflowName = workflow.getName() != null ? workflow.getName() : "Workflow";

        // Header
        sb.append("┌─────────────────────────────────────────────────────┐\n");
        sb.append("│ ").append(centerText(workflowName, 45)).append(" │\n");
        sb.append("└─────────────────────────────────────────────────────┘\n");
        sb.append("                          ↓\n");

        // Steps
        List<String> steps = workflow.getSteps();
        for (int i = 0; i < steps.size(); i++) {
            String step = truncateText(steps.get(i), 47);
            sb.append("    ┌─────────────────────────────────────────────┐\n");
            sb.append("    │ STEP ").append(String.format("%2d", i + 1)).append(": ").append(padRight(step, 35)).append(" │\n");
            sb.append("    └─────────────────────────────────────────────┘\n");

            if (i < steps.size() - 1) {
                sb.append("                          ↓\n");
            }
        }

        // Notes if available
        if (workflow.getNotes() != null && !workflow.getNotes().isEmpty()) {
            sb.append("\n    📝 Note: ").append(workflow.getNotes()).append("\n");
        }

        sb.append("\n    ✅ Workflow Complete\n\n");
        return sb.toString();
    }

    /**
     * Generate pipeline execution flow diagram showing all 4 stages.
     *
     * @return Mermaid diagram of the complete pipeline
     */
    public static String generatePipelineFlowDiagram() {
        StringBuilder sb = new StringBuilder();
        sb.append("```mermaid\n");
        sb.append("flowchart LR\n");
        sb.append("    A[📄 Input<br/>Transcripts] -->|Local Processing| B[🔪 Stage 1<br/>Chunking]\n");
        sb.append("    B -->|Semantic Chunks| C[🤖 Stage 2<br/>Summarization<br/>Claude API]\n");
        sb.append("    C -->|Chunk Summaries| D[📚 Stage 3<br/>Consolidation<br/>GPT/Gemini]\n");
        sb.append("    D -->|Master Notes| E[📋 Stage 4<br/>Exam Materials<br/>GPT/Gemini]\n");
        sb.append("    E -->|Flashcards| F[📖 Output<br/>Study Materials]\n");
        sb.append("    E -->|Questions| F\n");
        sb.append("    E -->|Revision| F\n");
        sb.append("\n");
        sb.append("    style A fill:#e1f5ff\n");
        sb.append("    style B fill:#fff3e0\n");
        sb.append("    style C fill:#f3e5f5\n");
        sb.append("    style D fill:#e8f5e9\n");
        sb.append("    style E fill:#fce4ec\n");
        sb.append("    style F fill:#e0f2f1\n");
        sb.append("```\n");
        return sb.toString();
    }

    /**
     * Generate ASCII art pipeline diagram.
     *
     * @return ASCII representation of the pipeline
     */
    public static String generatePipelineAsciiDiagram() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔════════════════════════════════════════════════════════════════════╗\n");
        sb.append("║           TRANSCRIPT → EXAM NOTES PIPELINE FLOW                    ║\n");
        sb.append("╚════════════════════════════════════════════════════════════════════╝\n\n");

        sb.append("    ┌──────────────┐\n");
        sb.append("    │ 📄 Transcripts│\n");
        sb.append("    └──────┬───────┘\n");
        sb.append("           │\n");
        sb.append("           ↓ (No API calls)\n");
        sb.append("    ┌──────────────────────┐\n");
        sb.append("    │ 🔪 STAGE 1: CHUNKING  │\n");
        sb.append("    │ Semantic text split   │\n");
        sb.append("    └──────┬───────────────┘\n");
        sb.append("           │\n");
        sb.append("           ↓ (Cost: Minimal)\n");
        sb.append("    ┌──────────────────────────┐\n");
        sb.append("    │ 🤖 STAGE 2: SUMMARIZE     │\n");
        sb.append("    │ Claude API per-chunk     │\n");
        sb.append("    │ Extract: workflows,      │\n");
        sb.append("    │ definitions, examples    │\n");
        sb.append("    └──────┬───────────────────┘\n");
        sb.append("           │\n");
        sb.append("           ↓ (Cost: High)\n");
        sb.append("    ┌──────────────────────────┐\n");
        sb.append("    │ 📚 STAGE 3: CONSOLIDATE   │\n");
        sb.append("    │ GPT-4o or Gemini         │\n");
        sb.append("    │ Dedup, synthesize        │\n");
        sb.append("    └──────┬───────────────────┘\n");
        sb.append("           │\n");
        sb.append("           ↓ (Cost: High)\n");
        sb.append("    ┌──────────────────────────┐\n");
        sb.append("    │ 📋 STAGE 4: EXAM MATERIALS│\n");
        sb.append("    │ Generate:                │\n");
        sb.append("    │ • Flashcards (CSV)       │\n");
        sb.append("    │ • Practice Questions     │\n");
        sb.append("    │ • Quick Revision Sheet   │\n");
        sb.append("    └──────┬───────────────────┘\n");
        sb.append("           │\n");
        sb.append("           ↓\n");
        sb.append("    ┌──────────────────────────┐\n");
        sb.append("    │ ✅ OUTPUT: Study Materials │\n");
        sb.append("    │ Exam-ready notes         │\n");
        sb.append("    └──────────────────────────┘\n\n");

        return sb.toString();
    }

    /**
     * Generate a table showing workflow dependencies and relationships.
     *
     * @param summaries List of chunk summaries
     * @return Text table of workflows
     */
    public static String generateWorkflowTable(List<ChunkSummary> summaries) {
        StringBuilder sb = new StringBuilder();

        if (summaries == null || summaries.isEmpty()) {
            return "No chunk summaries available.\n";
        }

        // Collect all workflows
        List<String> allWorkflows = new ArrayList<>();
        for (ChunkSummary summary : summaries) {
            if (summary.getWorkflows() != null) {
                for (ChunkSummary.Workflow w : summary.getWorkflows()) {
                    if (w.getName() != null) {
                        allWorkflows.add(w.getName());
                    }
                }
            }
        }

        if (allWorkflows.isEmpty()) {
            return "No workflows found in summaries.\n";
        }

        // Create table
        sb.append("╔═══════════════════════════════════════════════════════════════╗\n");
        sb.append("║                   WORKFLOWS REFERENCE TABLE                   ║\n");
        sb.append("╠═══════════════════════════════════════════════════════════════╣\n");
        sb.append("║ Index │ Workflow Name                        │ Steps │ Source ║\n");
        sb.append("╠═══════╪══════════════════════════════════════╪═══════╪════════╣\n");

        int index = 1;
        for (ChunkSummary summary : summaries) {
            if (summary.getWorkflows() != null) {
                for (ChunkSummary.Workflow w : summary.getWorkflows()) {
                    String workflowName = truncateText(w.getName() != null ? w.getName() : "Unknown", 36);
                    int stepCount = w.getSteps() != null ? w.getSteps().size() : 0;
                    String source = summary.getChunkId();

                    sb.append(String.format("║ %5d │ %-36s │ %5d │ %-6s ║\n", index, workflowName, stepCount, source));
                    index++;
                }
            }
        }

        sb.append("╚═══════╧══════════════════════════════════════╧═══════╧════════╝\n");

        return sb.toString();
    }

    /**
     * Generate a complete flows report combining all diagrams.
     *
     * @param summaries List of chunk summaries
     * @return Complete flows report in markdown
     */
    public static String generateCompleteFlowsReport(List<ChunkSummary> summaries) {
        StringBuilder sb = new StringBuilder();

        sb.append("# 📊 Flows & Diagrams Report\n\n");
        sb.append("_This report visualizes the pipeline execution flow and extracted workflows from your lecture materials._\n\n");

        // Pipeline diagram
        sb.append("## Pipeline Execution Flow\n\n");
        sb.append(generatePipelineFlowDiagram());
        sb.append("\n");

        // ASCII pipeline
        sb.append("## Pipeline ASCII Diagram\n");
        sb.append(generatePipelineAsciiDiagram());
        sb.append("\n");

        // Workflows reference
        sb.append("## Extracted Workflows\n\n");
        sb.append(generateWorkflowTable(summaries));
        sb.append("\n");

        // Individual workflow flowcharts
        int workflowIndex = 0;
        if (summaries != null) {
            for (ChunkSummary summary : summaries) {
                if (summary.getWorkflows() != null && !summary.getWorkflows().isEmpty()) {
                    for (ChunkSummary.Workflow workflow : summary.getWorkflows()) {
                        sb.append("## Workflow: ").append(workflow.getName()).append("\n\n");
                        sb.append("**Source**: ").append(summary.getChunkId()).append("\n");
                        if (workflow.getNotes() != null && !workflow.getNotes().isEmpty()) {
                            sb.append("**Notes**: ").append(workflow.getNotes()).append("\n");
                        }
                        sb.append("\n");

                        // Mermaid diagram
                        sb.append(generateMermaidFlowchart(workflow, workflowIndex));
                        sb.append("\n");

                        // ASCII diagram
                        sb.append("### ASCII Representation\n\n");
                        sb.append("```\n");
                        sb.append(generateAsciiFlowchart(workflow));
                        sb.append("```\n\n");

                        workflowIndex++;
                    }
                }
            }
        }

        sb.append("---\n");
        sb.append("_Generated by Transcript Pipeline Flows Feature_\n");

        return sb.toString();
    }

    /**
     * Sanitize text for Mermaid diagram (remove special characters).
     */
    private static String sanitizeMermaidText(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\"", "'")
                .replace("[", "(")
                .replace("]", ")")
                .replace("{", "(")
                .replace("}", ")")
                .replace("\n", " ");
    }

    /**
     * Truncate text to specified length.
     */
    private static String truncateText(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * Pad text to the right with spaces.
     */
    private static String padRight(String text, int length) {
        return String.format("%-" + length + "s", text);
    }

    /**
     * Center text within specified width.
     */
    private static String centerText(String text, int width) {
        if (text == null) {
            return "";
        }
        if (text.length() >= width) {
            return text.substring(0, width);
        }
        int padding = (width - text.length()) / 2;
        return String.format("%" + (padding + text.length()) + "s", text);
    }
}
