package com.transcript.pipeline.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transcript.pipeline.models.TextChunk;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for file operations including reading, writing, and parsing.
 */
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Read a text file as a single string
     */
    public static String readTextFile(String filePath) throws IOException {
        logger.info("Reading file: {}", filePath);
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }

    /**
     * Write a string to a file
     */
    public static void writeTextFile(String filePath, String content) throws IOException {
        logger.info("Writing file: {}", filePath);
        Files.createDirectories(Paths.get(filePath).getParent());
        Files.write(Paths.get(filePath), content.getBytes());
    }

    /**
     * Read a JSON file and deserialize to an object
     */
    public static <T> T readJsonFile(String filePath, Class<T> clazz) throws IOException {
        logger.info("Reading JSON file: {}", filePath);
        String content = readTextFile(filePath);
        return objectMapper.readValue(content, clazz);
    }

    /**
     * Read a JSON file as a list
     */
    public static <T> List<T> readJsonFileAsList(String filePath, Class<T> clazz) throws IOException {
        logger.info("Reading JSON array file: {}", filePath);
        String content = readTextFile(filePath);
        return objectMapper.readValue(content, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
    }

    /**
     * Write an object as JSON to a file
     */
    public static void writeJsonFile(String filePath, Object object) throws IOException {
        logger.info("Writing JSON file: {}", filePath);
        String jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        writeTextFile(filePath, jsonContent);
    }

    /**
     * Write a list as JSON array to a file
     */
    public static void writeJsonArrayFile(String filePath, List<?> list) throws IOException {
        logger.info("Writing JSON array file: {} with {} items", filePath, list.size());
        String jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(list);
        writeTextFile(filePath, jsonContent);
    }

    /**
     * List all files in a directory with a specific extension
     */
    public static List<File> listFilesInDirectory(String directoryPath, String extension) {
        List<File> files = new ArrayList<>();
        File directory = new File(directoryPath);

        if (!directory.isDirectory()) {
            logger.warn("Directory not found: {}", directoryPath);
            return files;
        }

        File[] fileArray = directory.listFiles((dir, name) -> name.endsWith(extension));
        if (fileArray != null) {
            files.addAll(Arrays.asList(fileArray));
            logger.info("Found {} files with extension {} in {}", files.size(), extension, directoryPath);
        }

        return files;
    }

    /**
     * Check if a file exists
     */
    public static boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    /**
     * Create a directory if it doesn't exist
     */
    public static void createDirectoryIfNotExists(String directoryPath) throws IOException {
        Files.createDirectories(Paths.get(directoryPath));
        logger.info("Directory created or already exists: {}", directoryPath);
    }

    /**
     * Generate an output file path based on step and input file
     */
    public static String generateOutputPath(String baseOutputDir, String step, String inputFileName, String extension) {
        String fileNameWithoutExt = inputFileName.replaceAll("\\.txt$", "").replaceAll("\\.md$", "");
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        return baseOutputDir + "/" + step + "/" + fileNameWithoutExt + "_" + step + extension;
    }

    /**
     * Generate chunk output file path
     */
    public static String generateChunkOutputPath(String baseOutputDir, String lessonFileName) {
        String fileNameWithoutExt = lessonFileName.replaceAll("\\.txt$", "");
        return baseOutputDir + "/chunks/" + fileNameWithoutExt + "_chunks.json";
    }

    /**
     * Generate summary output file path
     */
    public static String generateSummaryOutputPath(String baseOutputDir, String chunkId) {
        return baseOutputDir + "/summaries/chunk_" + chunkId + ".json";
    }

    /**
     * Count lines in a file
     */
    public static int countLines(String filePath) throws IOException {
        return (int) Files.lines(Paths.get(filePath)).count();
    }

    /**
     * Read file as lines
     */
    public static List<String> readFileAsLines(String filePath) throws IOException {
        logger.info("Reading file as lines: {}", filePath);
        return Files.readAllLines(Paths.get(filePath));
    }

    /**
     * Append content to a file
     */
    public static void appendToFile(String filePath, String content) throws IOException {
        logger.info("Appending to file: {}", filePath);
        Files.createDirectories(Paths.get(filePath).getParent());
        Files.write(Paths.get(filePath), content.getBytes(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
    }

    /**
     * Delete a file
     */
    public static void deleteFile(String filePath) throws IOException {
        logger.info("Deleting file: {}", filePath);
        Files.deleteIfExists(Paths.get(filePath));
    }

    /**
     * Sanitize filename by removing extension and special characters
     */
    public static String sanitizeFileName(String fileName) {
        // Remove .txt extension
        String sanitized = fileName.replaceAll("\\.txt$", "");
        // Remove or replace special characters that might cause issues
        sanitized = sanitized.replaceAll("[<>:\"/\\\\|?*]", "_");
        return sanitized;
    }

    /**
     * Generate file-specific summary output directory path
     * Example: output/summaries/Section- Fundamentals of Large Language Models/
     */
    public static String generateSummaryOutputDir(String baseOutputDir, String fileName) {
        String sanitized = sanitizeFileName(fileName);
        return baseOutputDir + "/summaries/" + sanitized;
    }

    /**
     * Generate file-specific consolidated master notes path
     * Example: output/consolidated/Section- Fundamentals of Large Language Models_master_notes.md
     */
    public static String generateConsolidatedPath(String baseOutputDir, String fileName) {
        String sanitized = sanitizeFileName(fileName);
        return baseOutputDir + "/consolidated/" + sanitized + "_master_notes.md";
    }

    /**
     * Generate file-specific exam materials directory path
     * Example: output/exam_materials/Section- Fundamentals of Large Language Models/
     */
    public static String generateExamMaterialsDir(String baseOutputDir, String fileName) {
        String sanitized = sanitizeFileName(fileName);
        return baseOutputDir + "/exam_materials/" + sanitized;
    }
}
