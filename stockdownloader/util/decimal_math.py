"""Utility functions for common arithmetic with safe division handling."""


def safe_divide(dividend: float, divisor: float, scale: int = 10) -> float:
    if divisor == 0:
        return 0.0
    return round(dividend / divisor, scale)


def percent_change(from_val: float, to_val: float) -> float:
    if from_val == 0:
        return 0.0
    return ((to_val - from_val) / from_val) * 100


def average(*values: float) -> float:
    non_zero = [v for v in values if v != 0]
    if not non_zero:
        return 0.0
    return sum(non_zero) / len(non_zero)
