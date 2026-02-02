from dataclasses import dataclass


@dataclass(frozen=True)
class PatternResult:
    pattern: str
    similar: str
    offset: str
    pattern_freq: float
    similar_freq: float
    offset_freq: float
    accuracy: float
    pattern_symbols: set[str]
    similar_symbols: set[str]
    offset_symbols: set[str]

    def __lt__(self, other: 'PatternResult') -> bool:
        return other.pattern_freq < self.pattern_freq
