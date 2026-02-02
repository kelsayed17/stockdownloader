"""Black-Scholes option pricing model for synthetic option price estimation."""

import math
from stockdownloader.model.option_type import OptionType


def price(option_type: OptionType, spot: float, strike: float,
          time_to_expiry: float, risk_free_rate: float, volatility: float) -> float:
    """Calculate theoretical option price using Black-Scholes formula."""
    if time_to_expiry <= 0 or volatility <= 0:
        return intrinsic_value(option_type, spot, strike)

    sqrt_t = math.sqrt(time_to_expiry)
    d1 = (math.log(spot / strike) + (risk_free_rate + volatility ** 2 / 2.0) * time_to_expiry) / (volatility * sqrt_t)
    d2 = d1 - volatility * sqrt_t

    if option_type == OptionType.CALL:
        result = spot * _cnd(d1) - strike * math.exp(-risk_free_rate * time_to_expiry) * _cnd(d2)
    else:
        result = strike * math.exp(-risk_free_rate * time_to_expiry) * _cnd(-d2) - spot * _cnd(-d1)

    return round(max(result, 0), 4)


def delta(option_type: OptionType, spot: float, strike: float,
          time_to_expiry: float, risk_free_rate: float, volatility: float) -> float:
    """Calculate delta."""
    if time_to_expiry <= 0 or volatility <= 0:
        if option_type == OptionType.CALL:
            return 1.0 if spot > strike else 0.0
        else:
            return -1.0 if spot < strike else 0.0

    sqrt_t = math.sqrt(time_to_expiry)
    d1 = (math.log(spot / strike) + (risk_free_rate + volatility ** 2 / 2.0) * time_to_expiry) / (volatility * sqrt_t)

    if option_type == OptionType.CALL:
        return round(_cnd(d1), 6)
    else:
        return round(_cnd(d1) - 1.0, 6)


def theta(option_type: OptionType, spot: float, strike: float,
          time_to_expiry: float, risk_free_rate: float, volatility: float) -> float:
    """Calculate theta (time decay per day)."""
    if time_to_expiry <= 0 or volatility <= 0:
        return 0.0

    sqrt_t = math.sqrt(time_to_expiry)
    d1 = (math.log(spot / strike) + (risk_free_rate + volatility ** 2 / 2.0) * time_to_expiry) / (volatility * sqrt_t)
    d2 = d1 - volatility * sqrt_t

    nd1 = math.exp(-d1 * d1 / 2.0) / math.sqrt(2 * math.pi)

    if option_type == OptionType.CALL:
        daily_theta = (-spot * nd1 * volatility / (2 * sqrt_t) - risk_free_rate * strike * math.exp(-risk_free_rate * time_to_expiry) * _cnd(d2)) / 365.0
    else:
        daily_theta = (-spot * nd1 * volatility / (2 * sqrt_t) + risk_free_rate * strike * math.exp(-risk_free_rate * time_to_expiry) * _cnd(-d2)) / 365.0

    return round(daily_theta, 6)


def estimate_volatility(close_prices: list[float], lookback: int = 20) -> float:
    """Estimate implied volatility from historical price data using log returns."""
    if not close_prices or len(close_prices) < 2:
        return 0.20
    start = max(0, len(close_prices) - lookback)
    count = len(close_prices) - start - 1
    if count < 1:
        return 0.20

    log_returns = []
    for i in range(count):
        prev = close_prices[start + i]
        curr = close_prices[start + i + 1]
        if prev > 0:
            log_returns.append(math.log(curr / prev))

    if not log_returns:
        return 0.20

    mean = sum(log_returns) / len(log_returns)
    sum_sq_diff = sum((lr - mean) ** 2 for lr in log_returns)
    daily_vol = math.sqrt(sum_sq_diff / len(log_returns))
    annual_vol = daily_vol * math.sqrt(252)
    return round(max(annual_vol, 0.01), 6)


def intrinsic_value(option_type: OptionType, spot: float, strike: float) -> float:
    """Intrinsic value of an option."""
    if option_type == OptionType.CALL:
        return round(max(spot - strike, 0), 4)
    else:
        return round(max(strike - spot, 0), 4)


def _cnd(x: float) -> float:
    """Cumulative standard normal distribution (Abramowitz and Stegun approximation)."""
    a1 = 0.254829592
    a2 = -0.284496736
    a3 = 1.421413741
    a4 = -1.453152027
    a5 = 1.061405429
    p = 0.3275911

    sign = -1 if x < 0 else 1
    x = abs(x) / math.sqrt(2)

    t = 1.0 / (1.0 + p * x)
    y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * math.exp(-x * x)

    return 0.5 * (1.0 + sign * y)
