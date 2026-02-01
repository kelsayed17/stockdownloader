package com.stockdownloader.analysis;

import com.stockdownloader.model.PatternResult;
import com.stockdownloader.util.BigDecimalMath;

import com.google.common.collect.HashMultimap;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Analyzes stock price movement patterns, computing frequency,
 * accuracy, and offset-by-one-day variations.
 */
public final class PatternAnalyzer {

    private static final int TOP_N = 10;

    private PatternAnalyzer() {}

    public static SortedSet<PatternResult> analyze(HashMultimap<String, String> frequency) {
        SortedSet<PatternResult> results = new TreeSet<>();

        for (String patternKey : frequency.keySet()) {
            String inner = patternKey.substring(1, patternKey.length() - 1);
            List<String> upDownList = new ArrayList<>(Arrays.asList(inner.split(", ")));

            int element = Integer.parseInt(upDownList.getFirst());
            upDownList.set(0, element == 1 ? "-1" : (element == -1 ? "1" : "0"));

            String similarKey = upDownList.toString();
            upDownList.removeFirst();
            String offsetKey = upDownList.toString();

            Set<String> patternSet = frequency.get(patternKey);
            Set<String> similarSet = frequency.get(similarKey);
            Set<String> offsetSet = frequency.get(offsetKey);

            var patternFreq = BigDecimal.valueOf(patternSet.size());
            var similarFreq = BigDecimal.valueOf(similarSet.size());
            var offsetFreq = BigDecimal.valueOf(offsetSet.size());

            BigDecimal total = BigDecimalMath.add(patternFreq, similarFreq);
            BigDecimal accuracy = BigDecimalMath.scale2(
                    BigDecimalMath.divide(BigDecimalMath.multiply(patternFreq, BigDecimal.valueOf(100)), total));

            results.add(new PatternResult(
                    patternKey, similarKey, offsetKey,
                    patternFreq, similarFreq, offsetFreq,
                    accuracy, patternSet, similarSet, offsetSet));
        }
        return results;
    }

    public static void printResults(SortedSet<PatternResult> results) {
        System.out.println("Top 10 patterns");
        System.out.printf("%-16s%-16s%-64s%s%n", "Frequency:", "Percentage:", "Pattern:", "Stocks:");

        int i = 0;
        for (PatternResult entry : results) {
            if (++i > TOP_N) break;
            System.out.printf("%-16s%-16s%-64s%s%n",
                    entry.getPatternFreq(),
                    entry.getAccuracy() + "%",
                    entry.getPattern(),
                    entry.getPatternSymbols());
        }

        System.out.println();
        System.out.println("Top 10 patterns offset by one day");
        System.out.printf("%-16s%-16s%-64s%s%n", "Frequency:", "Percentage:", "Pattern:", "Stocks:");

        int j = 0;
        for (PatternResult entry : results) {
            if (++j > TOP_N) break;
            System.out.printf("%-16s%-16s%-64s%s%n",
                    entry.getOffsetFreq(),
                    entry.getAccuracy() + "%",
                    entry.getOffset(),
                    entry.getOffsetSymbols());
        }
    }
}
