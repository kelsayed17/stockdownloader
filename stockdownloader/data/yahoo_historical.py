"""Historical data fetching from Yahoo Finance."""

import logging
from stockdownloader.model.historical_data import HistoricalData
from stockdownloader.data import yahoo_data_client

logger = logging.getLogger(__name__)


def fetch_historical_data(symbol: str) -> HistoricalData:
    """Fetch and compute historical data for a symbol."""
    hd = HistoricalData(symbol)
    try:
        data = yahoo_data_client.fetch_price_data(symbol, data_range="1y", interval="1d")
        if not data:
            hd.error = True
            return hd

        hd.historical_prices = [f"{d.date},{d.close}" for d in data]

        quarter_size = min(63, len(data))
        recent = data[-quarter_size:]
        hd.highest_price_this_qtr = max(d.high for d in recent)
        hd.lowest_price_this_qtr = min(d.low for d in recent)

        if len(data) > quarter_size:
            prev = data[-quarter_size * 2:-quarter_size] if len(data) > quarter_size * 2 else data[:-quarter_size]
            if prev:
                hd.highest_price_last_qtr = max(d.high for d in prev)
                hd.lowest_price_last_qtr = min(d.low for d in prev)
    except Exception as e:
        logger.error(f"Error fetching historical data for {symbol}: {e}")
        hd.error = True
    return hd
