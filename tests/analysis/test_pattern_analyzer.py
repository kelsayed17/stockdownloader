"""Tests for pattern_analyzer module."""

from collections import defaultdict
from decimal import Decimal, ROUND_HALF_UP

from stockdownloader.analysis.pattern_analyzer import analyze


def test_analyze_empty_frequency_map():
    frequency = defaultdict(set)
    results = analyze(frequency)
    assert len(results) == 0


def test_analyze_with_single_pattern():
    frequency = defaultdict(set)
    frequency["[1, -1, 1]"].add("AAPL")
    frequency["[1, -1, 1]"].add("GOOG")
    frequency["[-1, -1, 1]"].add("MSFT")
    frequency["[-1, 1]"].add("TSLA")

    results = analyze(frequency)
    assert len(results) > 0

    first = results[0]
    assert first.pattern is not None
    assert first.similar is not None
    assert first.offset is not None
    assert first.pattern_freq > Decimal("0")


def test_results_are_sorted_by_frequency_descending():
    frequency = defaultdict(set)
    # Pattern with freq 3
    frequency["[1, 1]"].add("A")
    frequency["[1, 1]"].add("B")
    frequency["[1, 1]"].add("C")
    # Inverse pattern
    frequency["[-1, 1]"].add("D")
    # Offset
    frequency["[1]"].add("E")

    # Pattern with freq 1
    frequency["[1, -1]"].add("F")
    frequency["[-1, -1]"].add("G")
    frequency["[-1]"].add("H")

    results = analyze(frequency)
    prev = None
    for r in results:
        if prev is not None:
            assert prev >= r.pattern_freq, "Results should be sorted by frequency descending"
        prev = r.pattern_freq


def test_accuracy_calculation():
    frequency = defaultdict(set)
    # Pattern: 3 occurrences
    frequency["[1, -1]"].add("A")
    frequency["[1, -1]"].add("B")
    frequency["[1, -1]"].add("C")
    # Similar (inverse first element): 1 occurrence
    frequency["[-1, -1]"].add("D")
    # Offset
    frequency["[-1]"].add("E")

    results = analyze(frequency)

    for r in results:
        if r.pattern == "[1, -1]":
            # accuracy = 3/(3+1) * 100 = 75
            assert Decimal("75").compare(
                r.accuracy.quantize(Decimal("1"), rounding=ROUND_HALF_UP)
            ) == 0
