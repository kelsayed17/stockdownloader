"""Loads price data from CSV files in Yahoo Finance format."""

import csv
from pathlib import Path
from stockdownloader.model.price_data import PriceData


def load_from_file(file_path: str) -> list[PriceData]:
    """Load price data from a CSV file. Expected columns: Date, Open, High, Low, Close, Adj Close, Volume."""
    data = []
    path = Path(file_path)
    with path.open(encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            try:
                pd = PriceData(
                    date=row["Date"].strip(),
                    open=float(row["Open"]),
                    high=float(row["High"]),
                    low=float(row["Low"]),
                    close=float(row["Close"]),
                    adj_close=float(row.get("Adj Close", row["Close"])),
                    volume=int(float(row["Volume"])),
                )
                data.append(pd)
            except (ValueError, KeyError):
                continue
    return data


def load_from_string(csv_content: str) -> list[PriceData]:
    """Load price data from a CSV string."""
    data = []
    lines = csv_content.strip().split("\n")
    if not lines:
        return data
    reader = csv.DictReader(lines)
    for row in reader:
        try:
            pd = PriceData(
                date=row["Date"].strip(),
                open=float(row["Open"]),
                high=float(row["High"]),
                low=float(row["Low"]),
                close=float(row["Close"]),
                adj_close=float(row.get("Adj Close", row["Close"])),
                volume=int(float(row["Volume"])),
            )
            data.append(pd)
        except (ValueError, KeyError):
            continue
    return data
