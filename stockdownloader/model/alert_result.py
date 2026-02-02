from __future__ import annotations

from dataclasses import dataclass
from enum import Enum, auto
from typing import TYPE_CHECKING

from .option_type import OptionType

if TYPE_CHECKING:
    from .indicator_values import IndicatorValues


class AlertDirection(Enum):
    STRONG_BUY = auto()
    BUY = auto()
    NEUTRAL = auto()
    SELL = auto()
    STRONG_SELL = auto()


class OptionsAction(Enum):
    BUY = auto()
    SELL = auto()
    HOLD = auto()


@dataclass(frozen=True)
class OptionsRecommendation:
    type: OptionType
    action: OptionsAction
    suggested_strike: float
    suggested_dte: int
    estimated_premium: float
    target_delta: float
    rationale: str

    def __str__(self) -> str:
        if self.action == OptionsAction.HOLD:
            return f"{self.type.name}: No action recommended"
        return (f"{self.action.name} {self.type.name} ${self.suggested_strike:.2f} strike, "
                f"{self.suggested_dte}DTE, est. premium ${self.estimated_premium:.2f}, "
                f"delta {self.target_delta:.2f} - {self.rationale}")


@dataclass(frozen=True)
class AlertResult:
    symbol: str
    date: str
    current_price: float
    direction: AlertDirection
    confluence_score: float
    total_indicators: int
    bullish_indicators: list[str]
    bearish_indicators: list[str]
    call_recommendation: OptionsRecommendation
    put_recommendation: OptionsRecommendation
    support_levels: list[float]
    resistance_levels: list[float]
    indicators: IndicatorValues

    @property
    def signal_strength(self) -> str:
        return (f"{self.confluence_score * 100:.0f}% "
                f"({int(self.confluence_score * self.total_indicators)}/{self.total_indicators} indicators)")

    def __str__(self) -> str:
        lines: list[str] = []
        sep = "=" * 80
        thin = "-" * 80
        lines.append(sep)
        lines.append(f"  TRADING ALERT: {self.symbol} - {self.date}")
        lines.append(sep)
        lines.append(f"  Current Price:     ${self.current_price:.2f}")
        lines.append(f"  Signal:            {self.direction.name}")
        lines.append(f"  Confluence:        {self.signal_strength}")
        lines.append("")
        lines.append(thin)
        lines.append(f"  BULLISH INDICATORS ({len(self.bullish_indicators)})")
        lines.append(thin)
        for ind in self.bullish_indicators:
            lines.append(f"    + {ind}")
        lines.append("")
        lines.append(thin)
        lines.append(f"  BEARISH INDICATORS ({len(self.bearish_indicators)})")
        lines.append(thin)
        for ind in self.bearish_indicators:
            lines.append(f"    - {ind}")
        lines.append("")
        lines.append(thin)
        lines.append("  OPTIONS RECOMMENDATIONS")
        lines.append(thin)
        lines.append(f"    CALL: {self.call_recommendation}")
        lines.append(f"    PUT:  {self.put_recommendation}")
        lines.append("")
        lines.append(thin)
        lines.append("  SUPPORT & RESISTANCE")
        lines.append(thin)
        if self.support_levels:
            levels = "  ".join(f"${s:.2f}" for s in self.support_levels[:3])
            lines.append(f"    Support:    {levels}")
        if self.resistance_levels:
            levels = "  ".join(f"${r:.2f}" for r in self.resistance_levels[:3])
            lines.append(f"    Resistance: {levels}")
        lines.append("")
        lines.append(thin)
        lines.append("  INDICATOR SNAPSHOT")
        lines.append(thin)
        lines.append(self.indicators.summary())
        lines.append("")
        lines.append(sep)
        lines.append("  DISCLAIMER: This is for educational purposes only.")
        lines.append("  Not financial advice. Always do your own research.")
        lines.append(sep)
        return "\n".join(lines)
