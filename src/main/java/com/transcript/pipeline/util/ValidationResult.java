package com.transcript.pipeline.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of a validation operation.
 * Contains status (success/warning/error) and associated messages.
 */
public class ValidationResult {

    public enum Status {
        SUCCESS,   // Validation passed completely
        WARNING,   // Validation passed but with warnings
        ERROR      // Validation failed
    }

    private Status status;
    private String message;
    private List<String> details;
    private String solution;

    public ValidationResult(Status status, String message) {
        this.status = status;
        this.message = message;
        this.details = new ArrayList<>();
    }

    public ValidationResult(Status status, String message, String solution) {
        this.status = status;
        this.message = message;
        this.solution = solution;
        this.details = new ArrayList<>();
    }

    /**
     * Create a successful validation result
     */
    public static ValidationResult success() {
        return new ValidationResult(Status.SUCCESS, "Validation passed");
    }

    public static ValidationResult success(String message) {
        return new ValidationResult(Status.SUCCESS, message);
    }

    /**
     * Create a warning validation result
     */
    public static ValidationResult warning(String message) {
        return new ValidationResult(Status.WARNING, message);
    }

    public static ValidationResult warning(String message, String solution) {
        return new ValidationResult(Status.WARNING, message, solution);
    }

    /**
     * Create an error validation result
     */
    public static ValidationResult error(String message) {
        return new ValidationResult(Status.ERROR, message);
    }

    public static ValidationResult error(String message, String solution) {
        return new ValidationResult(Status.ERROR, message, solution);
    }

    /**
     * Add a detail message
     */
    public ValidationResult addDetail(String detail) {
        this.details.add(detail);
        return this;
    }

    /**
     * Add multiple detail messages
     */
    public ValidationResult addDetails(List<String> details) {
        this.details.addAll(details);
        return this;
    }

    /**
     * Set the solution/fix message
     */
    public ValidationResult withSolution(String solution) {
        this.solution = solution;
        return this;
    }

    // Getters
    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getDetails() {
        return details;
    }

    public String getSolution() {
        return solution;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isWarning() {
        return status == Status.WARNING;
    }

    public boolean isError() {
        return status == Status.ERROR;
    }

    /**
     * Check if validation allows proceeding (success or warning)
     */
    public boolean canProceed() {
        return status == Status.SUCCESS || status == Status.WARNING;
    }

    /**
     * Print the validation result with colors
     */
    public void print() {
        switch (status) {
            case SUCCESS:
                ConsoleColors.printSuccess(message);
                break;
            case WARNING:
                ConsoleColors.printWarning(message);
                break;
            case ERROR:
                ConsoleColors.printError(message);
                break;
        }

        // Print details
        for (String detail : details) {
            System.out.println("  - " + detail);
        }

        // Print solution if present
        if (solution != null && !solution.isEmpty()) {
            System.out.println();
            System.out.println(ConsoleColors.colorize("Solution: " + solution, ConsoleColors.BOLD_CYAN));
        }
    }

    /**
     * Print the result and return whether we can proceed
     */
    public boolean printAndCheck() {
        print();
        return canProceed();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(status).append(": ").append(message);

        if (!details.isEmpty()) {
            sb.append(" [");
            sb.append(String.join(", ", details));
            sb.append("]");
        }

        if (solution != null) {
            sb.append(" | Solution: ").append(solution);
        }

        return sb.toString();
    }
}
