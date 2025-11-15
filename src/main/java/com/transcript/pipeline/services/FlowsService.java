package com.transcript.pipeline.services;

import com.transcript.pipeline.models.ChunkSummary;
import com.transcript.pipeline.util.FileService;
import com.transcript.pipeline.util.FlowsGenerator;
import java.io.File;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for generating flows and diagrams from pipeline data.
 *
 * This optional service creates visual representations of:
 * - Pipeline execution flow
 * - Extracted workflows from summaries
 * - Process diagrams in multiple formats (Mermaid, ASCII)
 *
 * Outputs are saved to the output/flows directory for easy viewing.
 */
public class FlowsService {

    private static final Logger logger = LoggerFactory.getLogger(FlowsService.class);
    private static final String FLOWS_DIR = "output/flows";

    /**
     * Generate flows and diagrams from chunk summaries.
     * Creates a comprehensive flows report including:
     * - Pipeline execution diagram
     * - Individual workflow flowcharts
     * - Workflow reference table
     *
     * @param summaries List of chunk summaries to analyze
     * @param outputBaseDir Base output directory
     * @return Path to generated flows report
     */
    public static String generateFlows(List<ChunkSummary> summaries, String outputBaseDir) {
        try {
            String flowsDir = outputBaseDir.isEmpty() ? FLOWS_DIR : outputBaseDir + "/flows";
            File flowsDirFile = new File(flowsDir);

            if (!flowsDirFile.exists()) {
                if (flowsDirFile.mkdirs()) {
                    logger.info("Created flows directory: {}", flowsDir);
                } else {
                    logger.warn("Failed to create flows directory: {}", flowsDir);
                    return null;
                }
            }

            // Generate complete flows report
            String reportsReport = FlowsGenerator.generateCompleteFlowsReport(summaries);
            String reportPath = flowsDir + "/flows_report.md";
            FileService.writeTextFile(reportPath, reportsReport);
            logger.info("Generated flows report: {}", reportPath);

            // Generate separate pipeline diagram
            String pipelineDiagram = generatePipelineDiagramFile(flowsDir);

            // Generate workflow summaries
            generateWorkflowSummaries(summaries, flowsDir);

            // Log summary
            int workflowCount = countWorkflows(summaries);
            logger.info("Flows generation complete: {} workflows visualized", workflowCount);

            return reportPath;

        } catch (Exception e) {
            logger.error("Error generating flows", e);
            return null;
        }
    }

    /**
     * Generate a dedicated pipeline diagram file.
     *
     * @param flowsDir Directory to save the diagram
     * @return Path to generated diagram
     */
    private static String generatePipelineDiagramFile(String flowsDir) {
        String pipelineContent = "# Pipeline Execution Flow\n\n";
        pipelineContent += FlowsGenerator.generatePipelineFlowDiagram();
        pipelineContent += "\n## ASCII Diagram\n\n";
        pipelineContent += "```\n";
        pipelineContent += FlowsGenerator.generatePipelineAsciiDiagram();
        pipelineContent += "```\n";

        String pipelinePath = flowsDir + "/pipeline_diagram.md";
        FileService.writeTextFile(pipelinePath, pipelineContent);
        logger.info("Generated pipeline diagram: {}", pipelinePath);

        return pipelinePath;
    }

    /**
     * Generate individual workflow summary files.
     * Each workflow gets its own file with detailed flowchart and steps.
     *
     * @param summaries List of chunk summaries
     * @param flowsDir Directory to save workflow files
     */
    private static void generateWorkflowSummaries(List<ChunkSummary> summaries, String flowsDir) {
        if (summaries == null) {
            return;
        }

        int workflowIndex = 0;
        for (ChunkSummary summary : summaries) {
            if (summary.getWorkflows() != null) {
                for (ChunkSummary.Workflow workflow : summary.getWorkflows()) {
                    try {
                        String workflowName = workflow.getName() != null ? workflow.getName() : "workflow_" + workflowIndex;
                        String filename = workflowName.toLowerCase()
                                .replace(" ", "_")
                                .replace("/", "_")
                                .replace("\\", "_")
                                .replaceAll("[^a-z0-9_]", "");

                        StringBuilder sb = new StringBuilder();
                        sb.append("# Workflow: ").append(workflowName).append("\n\n");
                        sb.append("**Source**: ").append(summary.getTitle()).append(" (").append(summary.getChunkId()).append(")\n\n");

                        // Add notes if available
                        if (workflow.getNotes() != null && !workflow.getNotes().isEmpty()) {
                            sb.append("**Notes**: ").append(workflow.getNotes()).append("\n\n");
                        }

                        // Step-by-step list
                        sb.append("## Steps\n\n");
                        if (workflow.getSteps() != null) {
                            for (int i = 0; i < workflow.getSteps().size(); i++) {
                                sb.append(String.format("%d. %s\n", i + 1, workflow.getSteps().get(i)));
                            }
                        }
                        sb.append("\n");

                        // Mermaid flowchart
                        sb.append("## Flowchart\n\n");
                        sb.append(FlowsGenerator.generateMermaidFlowchart(workflow, workflowIndex));
                        sb.append("\n");

                        // ASCII diagram
                        sb.append("## ASCII Representation\n\n");
                        sb.append("```\n");
                        sb.append(FlowsGenerator.generateAsciiFlowchart(workflow));
                        sb.append("```\n");

                        String filePath = flowsDir + "/" + filename + ".md";
                        FileService.writeTextFile(filePath, sb.toString());
                        logger.debug("Generated workflow file: {}", filePath);

                        workflowIndex++;
                    } catch (Exception e) {
                        logger.warn("Error generating workflow summary for: {}", workflow.getName(), e);
                    }
                }
            }
        }
    }

    /**
     * Count total workflows in summaries.
     *
     * @param summaries List of chunk summaries
     * @return Total workflow count
     */
    private static int countWorkflows(List<ChunkSummary> summaries) {
        if (summaries == null) {
            return 0;
        }

        int count = 0;
        for (ChunkSummary summary : summaries) {
            if (summary.getWorkflows() != null) {
                count += summary.getWorkflows().size();
            }
        }
        return count;
    }

    /**
     * Generate only ASCII diagrams for workflows (useful for terminal output).
     * Returns a comprehensive text representation.
     *
     * @param summaries List of chunk summaries
     * @return Text representation of all workflows
     */
    public static String generateWorkflowsAsciText(List<ChunkSummary> summaries) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n╔═══════════════════════════════════════════════════════════════════╗\n");
        sb.append("║                     WORKFLOWS ASCII DIAGRAMS                       ║\n");
        sb.append("╚═══════════════════════════════════════════════════════════════════╝\n\n");

        if (summaries == null || summaries.isEmpty()) {
            sb.append("No summaries available.\n");
            return sb.toString();
        }

        int workflowIndex = 1;
        for (ChunkSummary summary : summaries) {
            if (summary.getWorkflows() != null && !summary.getWorkflows().isEmpty()) {
                sb.append("SOURCE: ").append(summary.getTitle()).append("\n");
                sb.append("CHUNK ID: ").append(summary.getChunkId()).append("\n");
                sb.append("─".repeat(70)).append("\n\n");

                for (ChunkSummary.Workflow workflow : summary.getWorkflows()) {
                    sb.append(FlowsGenerator.generateAsciiFlowchart(workflow));
                    sb.append("\n");
                    workflowIndex++;
                }

                sb.append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Get flows directory path.
     *
     * @param outputBaseDir Base output directory
     * @return Flows directory path
     */
    public static String getFlowsDir(String outputBaseDir) {
        return outputBaseDir.isEmpty() ? FLOWS_DIR : outputBaseDir + "/flows";
    }

    /**
     * Check if flows directory exists and contains generated files.
     *
     * @param outputBaseDir Base output directory
     * @return True if flows exist
     */
    public static boolean flowsExist(String outputBaseDir) {
        File flowsDir = new File(getFlowsDir(outputBaseDir));
        return flowsDir.exists() && flowsDir.isDirectory() && flowsDir.listFiles() != null && flowsDir.listFiles().length > 0;
    }
}
