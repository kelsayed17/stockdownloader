package com.stockdownloader.model;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Holds the result of a pattern frequency analysis including the pattern,
 * its inverse, offset, frequencies, and associated symbols.
 */
public final class PatternResult implements Comparable<PatternResult> {

    private final String pattern;
    private final String similar;
    private final String offset;
    private final BigDecimal patternFreq;
    private final BigDecimal similarFreq;
    private final BigDecimal offsetFreq;
    private final BigDecimal accuracy;
    private final Set<String> patternSymbols;
    private final Set<String> similarSymbols;
    private final Set<String> offsetSymbols;

    public PatternResult(String pattern, String similar, String offset,
                         BigDecimal patternFreq, BigDecimal similarFreq, BigDecimal offsetFreq,
                         BigDecimal accuracy,
                         Set<String> patternSymbols, Set<String> similarSymbols, Set<String> offsetSymbols) {
        this.pattern = pattern;
        this.similar = similar;
        this.offset = offset;
        this.patternFreq = patternFreq;
        this.similarFreq = similarFreq;
        this.offsetFreq = offsetFreq;
        this.accuracy = accuracy;
        this.patternSymbols = patternSymbols;
        this.similarSymbols = similarSymbols;
        this.offsetSymbols = offsetSymbols;
    }

    @Override
    public int compareTo(PatternResult other) {
        return other.patternFreq.compareTo(this.patternFreq);
    }

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
