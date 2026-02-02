"""Shared test fixtures."""

import pytest
import math
from pathlib import Path
from stockdownloader.model.price_data import PriceData
from stockdownloader.data.csv_loader import load_from_file


TEST_DATA_DIR = Path(__file__).parent / "data"
TEST_CSV_PATH = TEST_DATA_DIR / "test-price-data.csv"


@pytest.fixture(scope="session")
def spy_data() -> list[PriceData]:
    """Load SPY test data from CSV (272 trading days)."""
    data = load_from_file(str(TEST_CSV_PATH))
    assert len(data) > 200, f"Expected 200+ bars, got {len(data)}"
    return data


@pytest.fixture
def synthetic_data() -> list[PriceData]:
    """Generate 300 days of synthetic price data with mild uptrend."""
    data = []
    price = 100.0
    for i in range(300):
        noise = math.sin(i * 0.1) * 2 + (i * 0.01)
        close = price + noise
        high = close + abs(math.sin(i * 0.3)) * 1.5 + 0.5
        low = close - abs(math.cos(i * 0.3)) * 1.5 - 0.5
        open_p = close + math.sin(i * 0.2) * 0.5
        volume = int(1_000_000 + math.sin(i * 0.05) * 500_000)
        data.append(PriceData(
            date=f"2023-{(i // 30 + 1):02d}-{(i % 28 + 1):02d}",
            open=round(open_p, 2),
            high=round(high, 2),
            low=round(low, 2),
            close=round(close, 2),
            adj_close=round(close, 2),
            volume=max(volume, 100000),
        ))
    return data


@pytest.fixture
def flat_data() -> list[PriceData]:
    """Generate 100 bars of flat price data at $100."""
    return [
        PriceData(
            date=f"2023-01-{(i % 28 + 1):02d}",
            open=100.0, high=100.5, low=99.5, close=100.0,
            adj_close=100.0, volume=1_000_000,
        )
        for i in range(100)
    ]
