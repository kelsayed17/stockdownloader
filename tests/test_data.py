"""Tests for data loading and clients."""

import pytest
from pathlib import Path
from stockdownloader.data.csv_loader import load_from_file, load_from_string

TEST_CSV = Path(__file__).parent / "data" / "test-price-data.csv"


class TestCsvLoader:
    def test_load_from_file(self):
        data = load_from_file(str(TEST_CSV))
        assert len(data) > 200
        assert data[0].date == "2023-01-03"

    def test_data_integrity(self):
        data = load_from_file(str(TEST_CSV))
        for bar in data:
            assert bar.high >= bar.low
            assert bar.high >= bar.open
            assert bar.high >= bar.close
            assert bar.low <= bar.open
            assert bar.low <= bar.close
            assert bar.volume >= 0

    def test_load_nonexistent_raises(self):
        with pytest.raises(FileNotFoundError):
            load_from_file("/nonexistent/file.csv")

    def test_load_from_string(self):
        csv = "Date,Open,High,Low,Close,Adj Close,Volume\n2023-01-03,100,105,95,102,101.5,1000000"
        data = load_from_string(csv)
        assert len(data) == 1
        assert data[0].close == 102.0

    def test_load_skips_invalid_rows(self):
        csv = "Date,Open,High,Low,Close,Adj Close,Volume\n2023-01-03,100,105,95,102,101.5,1000000\nbad,data,here"
        data = load_from_string(csv)
        assert len(data) == 1

    def test_empty_string(self):
        data = load_from_string("")
        assert data == []

    def test_header_only(self):
        data = load_from_string("Date,Open,High,Low,Close,Adj Close,Volume")
        assert data == []
