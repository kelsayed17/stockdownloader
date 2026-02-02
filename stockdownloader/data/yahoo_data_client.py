"""Fetches OHLCV data from Yahoo Finance v8 API."""

import json
import logging
from stockdownloader.model.price_data import PriceData
from stockdownloader.data.yahoo_auth import get_session, get_crumb
from stockdownloader.util.retry_executor import retry_with_backoff

logger = logging.getLogger(__name__)

VALID_RANGES = ("1mo", "3mo", "6mo", "1y", "2y", "5y", "10y", "max")
VALID_INTERVALS = ("1d", "1wk", "1mo")


def fetch_price_data(symbol: str, data_range: str = "2y", interval: str = "1d") -> list[PriceData]:
    """Fetch OHLCV price data from Yahoo Finance."""
    if data_range not in VALID_RANGES:
        raise ValueError(f"Invalid range: {data_range}. Must be one of {VALID_RANGES}")
    if interval not in VALID_INTERVALS:
        raise ValueError(f"Invalid interval: {interval}. Must be one of {VALID_INTERVALS}")

    def _fetch() -> list[PriceData]:
        session = get_session()
        crumb = get_crumb()
        url = f"https://query1.finance.yahoo.com/v8/finance/chart/{symbol}"
        params = {
            "range": data_range,
            "interval": interval,
            "includeAdjustedClose": "true",
            "crumb": crumb,
        }
        resp = session.get(url, params=params, timeout=30)
        resp.raise_for_status()
        return _parse_chart_response(resp.json())

    return retry_with_backoff(_fetch, max_retries=3, initial_delay=2.0,
                              exceptions=(Exception,))


def _parse_chart_response(data: dict) -> list[PriceData]:
    """Parse Yahoo Finance v8 chart API response into PriceData list."""
    result = []
    try:
        chart = data["chart"]["result"][0]
        timestamps = chart["timestamp"]
        quote = chart["indicators"]["quote"][0]
        adj_close_data = chart["indicators"].get("adjclose", [{}])
        adj_closes = adj_close_data[0].get("adjclose", []) if adj_close_data else []

        for i, ts in enumerate(timestamps):
            try:
                from datetime import datetime
                date_str = datetime.fromtimestamp(ts).strftime("%Y-%m-%d")
                o = quote["open"][i]
                h = quote["high"][i]
                l = quote["low"][i]
                c = quote["close"][i]
                v = quote["volume"][i]

                if any(x is None for x in (o, h, l, c, v)):
                    continue

                ac = adj_closes[i] if i < len(adj_closes) and adj_closes[i] is not None else c

                result.append(PriceData(
                    date=date_str,
                    open=float(o),
                    high=float(h),
                    low=float(l),
                    close=float(c),
                    adj_close=float(ac),
                    volume=int(v),
                ))
            except (IndexError, TypeError, ValueError):
                continue
    except (KeyError, IndexError) as e:
        logger.error(f"Error parsing chart response: {e}")
    return result
