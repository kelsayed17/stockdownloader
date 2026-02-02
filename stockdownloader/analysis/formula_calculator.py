"""Advanced financial calculations."""

import math


def compound_annual_growth_rate(begin_value: float, end_value: float, years: float) -> float:
    """Calculate CAGR."""
    if begin_value <= 0 or years <= 0:
        return 0.0
    return (math.pow(end_value / begin_value, 1.0 / years) - 1.0) * 100


def risk_adjusted_return(total_return: float, max_drawdown: float) -> float:
    """Calculate return / max drawdown ratio."""
    if max_drawdown == 0:
        return 0.0
    return total_return / max_drawdown


def kelly_criterion(win_rate: float, avg_win: float, avg_loss: float) -> float:
    """Calculate Kelly Criterion for optimal position sizing."""
    if avg_loss == 0 or win_rate <= 0 or win_rate >= 1:
        return 0.0
    b = avg_win / abs(avg_loss)
    return (win_rate * b - (1 - win_rate)) / b


def expected_value(win_rate: float, avg_win: float, avg_loss: float) -> float:
    """Calculate expected value per trade."""
    return win_rate * avg_win + (1 - win_rate) * avg_loss
