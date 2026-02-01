package com.stockdownloader.model;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Holds the result of a pattern frequency analysis including the pattern,
 * its inverse, offset, frequencies, and associated symbols.
 */
public record PatternResult(
        String pattern,
        String similar,
        String offset,
        BigDecimal patternFreq,
        BigDecimal similarFreq,
        BigDecimal offsetFreq,
        BigDecimal accuracy,
        Set<String> patternSymbols,
        Set<String> similarSymbols,
        Set<String> offsetSymbols) implements Comparable<PatternResult> {

    @Override
    public int compareTo(PatternResult other) {
        return other.patternFreq.compareTo(this.patternFreq);
    }

    // Preserve getter-style accessors for backward compatibility with callers
    public String getPattern() { return pattern; }
    public String getSimilar() { return similar; }
    public String getOffset() { return offset; }
    public BigDecimal getPatternFreq() { return patternFreq; }
    public BigDecimal getSimilarFreq() { return similarFreq; }
    public BigDecimal getOffsetFreq() { return offsetFreq; }
    public BigDecimal getAccuracy() { return accuracy; }
    public Set<String> getPatternSymbols() { return patternSymbols; }
    public Set<String> getSimilarSymbols() { return similarSymbols; }
    public Set<String> getOffsetSymbols() { return offsetSymbols; }
}
