"""Pattern detection in price data."""

from collections import defaultdict
from stockdownloader.model.price_data import PriceData
from stockdownloader.model.pattern_result import PatternResult


def analyze_patterns(data: list[PriceData], pattern_length: int = 5) -> list[PatternResult]:
    """Analyze price patterns and find frequency of occurrence."""
    if len(data) < pattern_length + 1:
        return []

    patterns: dict[str, list[str]] = defaultdict(list)

    for i in range(len(data) - pattern_length):
        pattern_parts = []
        for j in range(pattern_length):
            change = data[i + j + 1].close - data[i + j].close
            pattern_parts.append("U" if change > 0 else "D" if change < 0 else "F")
        pattern = "".join(pattern_parts)
        outcome = "U" if data[i + pattern_length].close > data[i + pattern_length - 1].close else "D"
        patterns[pattern].append(outcome)

    results = []
    total = len(data) - pattern_length

    for pattern, outcomes in patterns.items():
        freq = len(outcomes) / total if total > 0 else 0
        up_count = outcomes.count("U")
        accuracy = up_count / len(outcomes) if outcomes else 0

        similar = pattern[::-1]
        similar_outcomes = patterns.get(similar, [])
        similar_freq = len(similar_outcomes) / total if total > 0 else 0

        offset = "".join("D" if c == "U" else "U" if c == "D" else "F" for c in pattern)
        offset_outcomes = patterns.get(offset, [])
        offset_freq = len(offset_outcomes) / total if total > 0 else 0

        results.append(PatternResult(
            pattern=pattern,
            similar=similar,
            offset=offset,
            pattern_freq=freq,
            similar_freq=similar_freq,
            offset_freq=offset_freq,
            accuracy=accuracy,
            pattern_symbols=set(),
            similar_symbols=set(),
            offset_symbols=set(),
        ))

    results.sort()
    return results
