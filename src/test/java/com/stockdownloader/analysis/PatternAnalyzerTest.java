package com.stockdownloader.analysis;

import com.stockdownloader.model.PatternResult;
import com.google.common.collect.HashMultimap;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.SortedSet;

import static org.junit.jupiter.api.Assertions.*;

class PatternAnalyzerTest {

    @Test
    void analyzeEmptyFrequencyMap() {
        HashMultimap<String, String> frequency = HashMultimap.create();
        SortedSet<PatternResult> results = PatternAnalyzer.analyze(frequency);
        assertTrue(results.isEmpty());
    }

    @Test
    void analyzeWithSinglePattern() {
        HashMultimap<String, String> frequency = HashMultimap.create();
        frequency.put("[1, -1, 1]", "AAPL");
        frequency.put("[1, -1, 1]", "GOOG");
        frequency.put("[-1, -1, 1]", "MSFT");
        frequency.put("[-1, 1]", "TSLA");

        SortedSet<PatternResult> results = PatternAnalyzer.analyze(frequency);
        assertFalse(results.isEmpty());

        PatternResult first = results.first();
        assertNotNull(first.getPattern());
        assertNotNull(first.getSimilar());
        assertNotNull(first.getOffset());
        assertTrue(first.getPatternFreq().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void resultsAreSortedByFrequencyDescending() {
        HashMultimap<String, String> frequency = HashMultimap.create();
        // Pattern with freq 3
        frequency.put("[1, 1]", "A");
        frequency.put("[1, 1]", "B");
        frequency.put("[1, 1]", "C");
        // Inverse pattern
        frequency.put("[-1, 1]", "D");
        // Offset
        frequency.put("[1]", "E");

        // Pattern with freq 1
        frequency.put("[1, -1]", "F");
        frequency.put("[-1, -1]", "G");
        frequency.put("[-1]", "H");

        SortedSet<PatternResult> results = PatternAnalyzer.analyze(frequency);
        BigDecimal prev = null;
        for (PatternResult r : results) {
            if (prev != null) {
                assertTrue(prev.compareTo(r.getPatternFreq()) >= 0,
                        "Results should be sorted by frequency descending");
            }
            prev = r.getPatternFreq();
        }
    }

    @Test
    void accuracyCalculation() {
        HashMultimap<String, String> frequency = HashMultimap.create();
        // Pattern: 3 occurrences
        frequency.put("[1, -1]", "A");
        frequency.put("[1, -1]", "B");
        frequency.put("[1, -1]", "C");
        // Similar (inverse first element): 1 occurrence
        frequency.put("[-1, -1]", "D");
        // Offset
        frequency.put("[-1]", "E");

        SortedSet<PatternResult> results = PatternAnalyzer.analyze(frequency);

        for (PatternResult r : results) {
            if (r.getPattern().equals("[1, -1]")) {
                // accuracy = 3/(3+1) * 100 = 75
                assertEquals(0, new BigDecimal("75").compareTo(
                        r.getAccuracy().setScale(0, java.math.RoundingMode.HALF_UP)));
            }
        }
    }
}
