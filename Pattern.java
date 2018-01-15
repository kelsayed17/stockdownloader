import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.common.collect.ComparisonChain;

public class Pattern implements Comparator<Pattern> {
	private String pattern, similar, offset;
	private BigDecimal patternFreq, similarFreq, offsetFreq;
	private BigDecimal accuracy;
	private Set<String> patternSymbols, similarSymbols, offsetSymbols;

	public Pattern() {
		//frequency.removeAll(Collections.singletonMap(key, value));

		PatternCompare pc = new PatternCompare();
		SortedSet<Pattern> sortedFreq =  new TreeSet<Pattern>(pc);

		for (String pattern : frequency.keySet()) {
			String patternModified = pattern.substring(1, pattern.length() - 1);

			List<String> UpDownList = new ArrayList<String>(Arrays.asList(patternModified.split(", ")));

			int element = Integer.parseInt(UpDownList.get(0));

			if (element == 1)
				UpDownList.set(0, "-1");
			else if (element == -1)
				UpDownList.set(0, "1");

			String similar = UpDownList.toString();

			UpDownList.remove(0);

			String offset = UpDownList.toString();

			int patternSize = frequency.get(pattern).size();
			int similarSize = frequency.get(similar).size();
			int offsetSize = frequency.get(offset).size();

			BigDecimal patternFreq = new BigDecimal(patternSize);
			BigDecimal similarFreq = new BigDecimal(similarSize);
			BigDecimal offsetFreq = new BigDecimal(offsetSize);

			BigDecimal accuracy = new BigDecimal(bd.SetScaleTwo(bd.BDDivide(bd.BDMultiply(patternFreq, BigDecimal.valueOf(100)), bd.BDAdd(patternFreq, similarFreq))).toString());

			Set<String> patternSymbols = frequency.get(pattern);
			Set<String> similarSymbols = frequency.get(similar);
			Set<String> offsetSymbols = frequency.get(offset);

			Pattern trend = new Pattern();

			trend.setPattern(pattern);
			trend.setSimilar(similar);
			trend.setOffset(offset);
			trend.setPatternFreq(patternFreq);
			trend.setSimilarFreq(similarFreq);
			trend.setOffsetFreq(offsetFreq);
			trend.setAccuracy(accuracy);
			trend.setPatternSymbols(patternSymbols);
			trend.setSimilarSymbols(similarSymbols);
			trend.setOffsetSymbols(offsetSymbols);

			sortedFreq.add(trend);
		}
		print(sortedFreq);
	}


	public String getPattern() {
		return pattern;
	}
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}
	public String getSimilar() {
		return similar;
	}
	public void setSimilar(String similar) {
		this.similar = similar;
	}
	public String getOffset() {
		return offset;
	}
	public void setOffset(String offset) {
		this.offset = offset;
	}
	public BigDecimal getPatternFreq() {
		return patternFreq;
	}
	public void setPatternFreq(BigDecimal patternFreq) {
		this.patternFreq = patternFreq;
	}
	public BigDecimal getSimilarFreq() {
		return similarFreq;
	}
	public void setSimilarFreq(BigDecimal similarFreq) {
		this.similarFreq = similarFreq;
	}
	public BigDecimal getOffsetFreq() {
		return offsetFreq;
	}
	public void setOffsetFreq(BigDecimal offsetFreq) {
		this.offsetFreq = offsetFreq;
	}
	public BigDecimal getAccuracy() {
		return accuracy;
	}
	public void setAccuracy(BigDecimal accuracy) {
		this.accuracy = accuracy;
	}
	public Set<String> getPatternSymbols() {
		return patternSymbols;
	}
	public void setPatternSymbols(Set<String> patternSymbols) {
		this.patternSymbols = patternSymbols;
	}
	public Set<String> getSimilarSymbols() {
		return similarSymbols;
	}
	public void setSimilarSymbols(Set<String> similarSymbols) {
		this.similarSymbols = similarSymbols;
	}
	public Set<String> getOffsetSymbols() {
		return offsetSymbols;
	}
	public void setOffsetSymbols(Set<String> offsetSymbols) {
		this.offsetSymbols = offsetSymbols;
	}

	@Override
	public int compare(Pattern o1, Pattern o2) {
		return ComparisonChain.start()
				//.compare(i2.frequency, i1.frequency)
				//.compare(o1.getAccuracy(), o2.getAccuracy())
				.compare(o2.getPatternFreq(), o1.getPatternFreq())
				//.compare(o2.getPattern(), o1.getPattern())
				.result();
	}
	
	public void print(SortedSet<Pattern> sortedFreq) {
		// Result title
		System.out.println("Top 10 patterns");

		// Print column titles
		System.out.println("Frequency:" + "\t" + "Percentage:" + "\t" + "Pattern:" + "\t\t\t\t\t\t\t" + "Stocks:");

		int i = 1;
		// Display top 10 occurrences
		for (Pattern entry : sortedFreq) { 
			if (i > 10)
				break;

			// Output results
			//System.out.print(padRight("#" + i, 16));
			System.out.print(padRight(entry.getPatternFreq().toString(), 16));
			System.out.print(padRight(entry.getAccuracy() + "%", 16));
			System.out.print(padRight(entry.getPattern(), 64));
			System.out.println(entry.getPatternSymbols().toString());
			i++;
		}

		// Print blank line
		System.out.println();

		// Result title
		System.out.println("Top 10 patterns offset by one day");

		// Print column titles
		System.out.println("Frequency:" + "\t" + "Percentage:" + "\t" + "Pattern:" + "\t\t\t\t\t\t\t" + "Stocks:");

		int j = 1;
		// Display top 10 occurrences
		for (Pattern entry : sortedFreq) { 
			if (j > 10)
				break;

			// Output results
			//System.out.print(padRight("#" + j, 16));
			System.out.print(padRight(entry.getOffsetFreq().toString(), 16));
			System.out.print(padRight(entry.getAccuracy() + "%", 16));
			System.out.print(padRight(entry.getOffset(), 64));
			System.out.println(entry.getOffsetSymbols().toString());
			j++;
		}
	}

	public String padRight(String s, int n) {
		return String.format("%1$-" + n + "s", s);
	}

	public String padLeft(String s, int n) {
		return String.format("%1$" + n + "s", s);
	}
}
