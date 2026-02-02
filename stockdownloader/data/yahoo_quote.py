"""Real-time quote data from Yahoo Finance."""

import json
import logging
from stockdownloader.model.quote_data import QuoteData
from stockdownloader.data.yahoo_auth import get_session, get_crumb
from stockdownloader.util.retry_executor import retry_with_backoff

logger = logging.getLogger(__name__)


def fetch_quote(symbol: str) -> QuoteData:
    """Fetch real-time quote data for a symbol."""
    def _fetch() -> QuoteData:
        session = get_session()
        crumb = get_crumb()
        url = f"https://query1.finance.yahoo.com/v7/finance/quote"
        params = {"symbols": symbol, "crumb": crumb}
        resp = session.get(url, params=params, timeout=30)
        resp.raise_for_status()
        return _parse_quote_response(resp.json())

    try:
        return retry_with_backoff(_fetch, max_retries=3, initial_delay=2.0)
    except Exception as e:
        logger.error(f"Failed to fetch quote for {symbol}: {e}")
        qd = QuoteData()
        qd.error = True
        return qd


def _parse_quote_response(data: dict) -> QuoteData:
    """Parse Yahoo Finance quote response."""
    qd = QuoteData()
    try:
        results = data.get("quoteResponse", {}).get("result", [])
        if not results:
            qd.error = True
            return qd
        r = results[0]
        qd.last_trade_price_only = r.get("regularMarketPrice", 0.0)
        qd.previous_close = r.get("regularMarketPreviousClose", 0.0)
        qd.open = r.get("regularMarketOpen", 0.0)
        qd.days_high = r.get("regularMarketDayHigh", 0.0)
        qd.days_low = r.get("regularMarketDayLow", 0.0)
        qd.volume = r.get("regularMarketVolume", 0.0)
        qd.year_high = r.get("fiftyTwoWeekHigh", 0.0)
        qd.year_low = r.get("fiftyTwoWeekLow", 0.0)
        qd.fifty_day_moving_average = r.get("fiftyDayAverage", 0.0)
        qd.two_hundred_day_moving_average = r.get("twoHundredDayAverage", 0.0)
        qd.market_capitalization = r.get("marketCap", 0)
        qd.trailing_annual_dividend_yield = r.get("trailingAnnualDividendYield", 0.0) or 0.0
        qd.diluted_eps = r.get("epsTrailingTwelveMonths", 0.0) or 0.0
        qd.price_sales = r.get("priceToSalesTrailing12Months", 0.0) or 0.0
    except Exception as e:
        logger.error(f"Error parsing quote: {e}")
        qd.error = True
    return qd
