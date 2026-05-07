package com.example.aiproject.util;

import com.example.aiproject.model.ScanMetrics;
import com.example.aiproject.model.ScanMetricsResponse;

import java.util.List;

/**
 * Derives dashboard trend metrics from real per-file LLM scan results.
 *
 * Each metric is computed across all scanned files and expressed as a
 * 30-point list so the frontend SparkLine charts have data to render.
 * The list is ordered from oldest file processed (index 0) to newest
 * (index 29), giving a meaningful "trend over the scan run" view.
 */
public final class MetricsCalculator {

    private MetricsCalculator() {}

    /**
     * Overall AI Compatibility Score (0–100).
     * Source: ScanMetrics.aiCompatibilityScore (LLM returns a numeric string, e.g. "72").
     * Aggregation: cumulative rolling average across files as they are processed.
     */
    public static List<Integer> overallCompatibilityScores(List<ScanMetricsResponse> results) {
        List<Integer> scores = results.stream()
                .map(r -> parseIntSafe(r.getResponse().getAiCompatibilityScore()))
                .filter(v -> v >= 0)
                .toList();

        if (scores.isEmpty()) return defaultSeries(70);
        return rollingAverage(padOrTrim(scores, 30));
    }

    /**
     * Cyclomatic Complexity — mapped from LLM label to numeric value.
     * LOW=1, MODERATE=5, HIGH=10, VERY_HIGH=20, unknown=5.
     * Aggregation: rolling average to show trend.
     */
    public static List<Integer> cyclomaticComplexityScores(List<ScanMetricsResponse> results) {
        List<Integer> scores = results.stream()
                .map(r -> complexityToInt(r.getResponse().getCyclomaticComplexity()))
                .toList();

        if (scores.isEmpty()) return defaultSeries(5);
        return rollingAverage(padOrTrim(scores, 30));
    }

    /**
     * Checkstyle issue count — total issues per file across the scan.
     * Aggregation: cumulative running total so the chart shows growth.
     */
    public static List<Integer> checkStyleIssueCounts(List<ScanMetricsResponse> results) {
        List<Integer> perFile = results.stream()
                .map(r -> r.getResponse().getIssues() != null ? r.getResponse().getIssues().length : 0)
                .toList();

        if (perFile.isEmpty()) return defaultSeries(0);
        return cumulativeSum(padOrTrim(perFile, 30));
    }

    /**
     * Incompatible file count — files where AI compatibility score < 60.
     * Aggregation: cumulative running total.
     */
    public static List<Integer> incompatibleFileCounts(List<ScanMetricsResponse> results) {
        List<Integer> perFile = results.stream()
                .map(r -> {
                    int score = parseIntSafe(r.getResponse().getAiCompatibilityScore());
                    return (score >= 0 && score < 60) ? 1 : 0;
                })
                .toList();

        if (perFile.isEmpty()) return defaultSeries(0);
        return cumulativeSum(padOrTrim(perFile, 30));
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    private static int parseIntSafe(String s) {
        if (s == null || s.isBlank()) return -1;
        try {
            // LLM sometimes returns "72/100" or "72%" — strip non-numeric suffix
            String cleaned = s.trim().replaceAll("[^0-9].*", "");
            return cleaned.isEmpty() ? -1 : Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static int complexityToInt(String label) {
        if (label == null) return 5;
        return switch (label.trim().toUpperCase()) {
            case "LOW"       -> 2;
            case "MODERATE"  -> 5;
            case "HIGH"      -> 10;
            case "VERY_HIGH", "VERY HIGH" -> 20;
            default          -> 5;
        };
    }

    /** Pad with first value or trim to exactly n elements. */
    private static List<Integer> padOrTrim(List<Integer> list, int n) {
        if (list.size() >= n) return list.subList(0, n);
        int pad = list.isEmpty() ? 0 : list.get(0);
        java.util.List<Integer> result = new java.util.ArrayList<>(list);
        while (result.size() < n) result.add(0, pad);
        return result;
    }

    /** Rolling average: index i = mean of elements [0..i]. */
    private static List<Integer> rollingAverage(List<Integer> list) {
        java.util.List<Integer> out = new java.util.ArrayList<>();
        int sum = 0;
        for (int i = 0; i < list.size(); i++) {
            sum += list.get(i);
            out.add(sum / (i + 1));
        }
        return out;
    }

    /** Cumulative sum: index i = sum of elements [0..i]. */
    private static List<Integer> cumulativeSum(List<Integer> list) {
        java.util.List<Integer> out = new java.util.ArrayList<>();
        int sum = 0;
        for (int v : list) { sum += v; out.add(sum); }
        return out;
    }

    /** Fallback flat series when no scan data available yet. */
    private static List<Integer> defaultSeries(int value) {
        return java.util.Collections.nCopies(30, value);
    }
}
