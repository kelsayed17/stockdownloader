import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashMultimap;

public class Pattern implements Comparable<Pattern> {

    private String pattern;
    private String similar;
    private String offset;
    private BigDecimal patternFreq;
    private BigDecimal similarFreq;
    private BigDecimal offsetFreq;
    private BigDecimal accuracy;
    private Set<String> patternSymbols;
    private Set<String> similarSymbols;
    private Set<String> offsetSymbols;

    public Pattern() {
        // Default constructor for use as data holder
    }

    public static void analyzeAndPrint(HashMultimap<String, String> frequency, BDCalculator bd) {
        SortedSet<Pattern> sortedFreq = new TreeSet<>();

        for (String patternKey : frequency.keySet()) {
            String patternModified = patternKey.substring(1, patternKey.length() - 1);
            List<String> upDownList = new ArrayList<>(Arrays.asList(patternModified.split(", ")));

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

            var accuracy = new BigDecimal(bd.SetScaleTwo(
                    bd.BDDivide(bd.BDMultiply(patternFreq, BigDecimal.valueOf(100)),
                            bd.BDAdd(patternFreq, similarFreq))).toString());

            var trend = new Pattern();
            trend.setPattern(patternKey);
            trend.setSimilar(similarKey);
            trend.setOffset(offsetKey);
            trend.setPatternFreq(patternFreq);
            trend.setSimilarFreq(similarFreq);
            trend.setOffsetFreq(offsetFreq);
            trend.setAccuracy(accuracy);
            trend.setPatternSymbols(patternSet);
            trend.setSimilarSymbols(similarSet);
            trend.setOffsetSymbols(offsetSet);

            sortedFreq.add(trend);
        }
        printResults(sortedFreq);
    }

    private static void printResults(SortedSet<Pattern> sortedFreq) {
        System.out.println("Top 10 patterns");
        System.out.printf("%-16s%-16s%-64s%s%n", "Frequency:", "Percentage:", "Pattern:", "Stocks:");

        int i = 0;
        for (Pattern entry : sortedFreq) {
            if (++i > 10) break;
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
        for (Pattern entry : sortedFreq) {
            if (++j > 10) break;
            System.out.printf("%-16s%-16s%-64s%s%n",
                    entry.getOffsetFreq(),
                    entry.getAccuracy() + "%",
                    entry.getOffset(),
                    entry.getOffsetSymbols());
        }
    }

    @Override
    public int compareTo(Pattern other) {
        return ComparisonChain.start()
                .compare(other.getPatternFreq(), this.getPatternFreq())
                .result();
    }

    // Getters and setters
    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }
    public String getSimilar() { return similar; }
    public void setSimilar(String similar) { this.similar = similar; }
    public String getOffset() { return offset; }
    public void setOffset(String offset) { this.offset = offset; }
    public BigDecimal getPatternFreq() { return patternFreq; }
    public void setPatternFreq(BigDecimal patternFreq) { this.patternFreq = patternFreq; }
    public BigDecimal getSimilarFreq() { return similarFreq; }
    public void setSimilarFreq(BigDecimal similarFreq) { this.similarFreq = similarFreq; }
    public BigDecimal getOffsetFreq() { return offsetFreq; }
    public void setOffsetFreq(BigDecimal offsetFreq) { this.offsetFreq = offsetFreq; }
    public BigDecimal getAccuracy() { return accuracy; }
    public void setAccuracy(BigDecimal accuracy) { this.accuracy = accuracy; }
    public Set<String> getPatternSymbols() { return patternSymbols; }
    public void setPatternSymbols(Set<String> patternSymbols) { this.patternSymbols = patternSymbols; }
    public Set<String> getSimilarSymbols() { return similarSymbols; }
    public void setSimilarSymbols(Set<String> similarSymbols) { this.similarSymbols = similarSymbols; }
    public Set<String> getOffsetSymbols() { return offsetSymbols; }
    public void setOffsetSymbols(Set<String> offsetSymbols) { this.offsetSymbols = offsetSymbols; }
}
