package com.transcript.pipeline.util;

/**
 * Utility class for colored console output using ANSI escape codes.
 * Provides methods for printing colored text and common formatting patterns.
 */
public class ConsoleColors {

    // ANSI Color Codes
    public static final String RESET = "\033[0m";

    // Regular Colors
    public static final String BLACK = "\033[0;30m";
    public static final String RED = "\033[0;31m";
    public static final String GREEN = "\033[0;32m";
    public static final String YELLOW = "\033[0;33m";
    public static final String BLUE = "\033[0;34m";
    public static final String PURPLE = "\033[0;35m";
    public static final String CYAN = "\033[0;36m";
    public static final String WHITE = "\033[0;37m";

    // Bold Colors
    public static final String BOLD_BLACK = "\033[1;30m";
    public static final String BOLD_RED = "\033[1;31m";
    public static final String BOLD_GREEN = "\033[1;32m";
    public static final String BOLD_YELLOW = "\033[1;33m";
    public static final String BOLD_BLUE = "\033[1;34m";
    public static final String BOLD_PURPLE = "\033[1;35m";
    public static final String BOLD_CYAN = "\033[1;36m";
    public static final String BOLD_WHITE = "\033[1;37m";

    // Background Colors
    public static final String BG_BLACK = "\033[40m";
    public static final String BG_RED = "\033[41m";
    public static final String BG_GREEN = "\033[42m";
    public static final String BG_YELLOW = "\033[43m";
    public static final String BG_BLUE = "\033[44m";
    public static final String BG_PURPLE = "\033[45m";
    public static final String BG_CYAN = "\033[46m";
    public static final String BG_WHITE = "\033[47m";

    // Check if colors should be disabled (for CI/CD environments)
    private static final boolean COLORS_ENABLED = System.console() != null
        && !System.getenv().getOrDefault("NO_COLOR", "").equals("1")
        && !System.getenv().getOrDefault("TERM", "").equals("dumb");

    /**
     * Print success message in green
     */
    public static void printSuccess(String message) {
        System.out.println(colorize("✓ " + message, GREEN));
    }

    /**
     * Print error message in red
     */
    public static void printError(String message) {
        System.out.println(colorize("✗ " + message, RED));
    }

    /**
     * Print warning message in yellow
     */
    public static void printWarning(String message) {
        System.out.println(colorize("⚠ " + message, YELLOW));
    }

    /**
     * Print info message in blue
     */
    public static void printInfo(String message) {
        System.out.println(colorize("ℹ " + message, BLUE));
    }

    /**
     * Print header message in bold cyan
     */
    public static void printHeader(String message) {
        System.out.println();
        System.out.println(colorize("═══════════════════════════════════════════════", BOLD_CYAN));
        System.out.println(colorize(message, BOLD_CYAN));
        System.out.println(colorize("═══════════════════════════════════════════════", BOLD_CYAN));
        System.out.println();
    }

    /**
     * Print section header
     */
    public static void printSection(String message) {
        System.out.println();
        System.out.println(colorize("───── " + message + " ─────", BOLD_WHITE));
    }

    /**
     * Print highlighted text in purple
     */
    public static void printHighlight(String message) {
        System.out.println(colorize("★ " + message, PURPLE));
    }

    /**
     * Colorize text with given color code
     */
    public static String colorize(String text, String colorCode) {
        if (!COLORS_ENABLED) {
            return text;
        }
        return colorCode + text + RESET;
    }

    /**
     * Format a percentage value
     */
    public static String formatPercentage(double percentage) {
        String color;
        if (percentage >= 75) {
            color = GREEN;
        } else if (percentage >= 50) {
            color = YELLOW;
        } else {
            color = RED;
        }
        return colorize(String.format("%.1f%%", percentage), color);
    }

    /**
     * Format a cost value
     */
    public static String formatCost(double cost) {
        String color = cost > 5.0 ? RED : (cost > 2.0 ? YELLOW : GREEN);
        return colorize(String.format("$%.2f", cost), color);
    }

    /**
     * Format elapsed time
     */
    public static String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    /**
     * Print a progress summary
     */
    public static void printProgressSummary(String stage, int current, int total, double costSoFar) {
        double percentage = (current * 100.0) / total;
        System.out.println(String.format(
            "%s [%s/%s] %s - Cost: %s",
            colorize(stage, BOLD_BLUE),
            colorize(String.valueOf(current), GREEN),
            colorize(String.valueOf(total), CYAN),
            formatPercentage(percentage),
            formatCost(costSoFar)
        ));
    }

    /**
     * Print a separator line
     */
    public static void printSeparator() {
        System.out.println(colorize("─────────────────────────────────────────────────────", CYAN));
    }

    /**
     * Print a double separator line
     */
    public static void printDoubleSeparator() {
        System.out.println(colorize("═════════════════════════════════════════════════════", BOLD_CYAN));
    }

    /**
     * Check if colors are enabled
     */
    public static boolean areColorsEnabled() {
        return COLORS_ENABLED;
    }
}
