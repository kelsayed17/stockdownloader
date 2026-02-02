"""Downloads stock lists and earnings calendars."""

import logging
import requests
from bs4 import BeautifulSoup
from stockdownloader.util.retry_executor import retry_with_backoff

logger = logging.getLogger(__name__)


def download_nasdaq_symbols() -> list[str]:
    """Download list of NASDAQ traded symbols."""
    def _fetch() -> list[str]:
        url = "https://api.nasdaq.com/api/screener/stocks"
        params = {"tableonly": "true", "limit": "10000", "exchange": "nasdaq"}
        headers = {"User-Agent": "Mozilla/5.0"}
        resp = requests.get(url, params=params, headers=headers, timeout=30)
        resp.raise_for_status()
        data = resp.json()
        rows = data.get("data", {}).get("table", {}).get("rows", [])
        return [row.get("symbol", "").strip() for row in rows if row.get("symbol")]

    try:
        return retry_with_backoff(_fetch, max_retries=3, initial_delay=2.0)
    except Exception as e:
        logger.error(f"Error downloading NASDAQ symbols: {e}")
        return []


def download_earnings_calendar() -> list[str]:
    """Download symbols from upcoming earnings calendar."""
    def _fetch() -> list[str]:
        url = "https://finance.yahoo.com/calendar/earnings"
        headers = {"User-Agent": "Mozilla/5.0"}
        resp = requests.get(url, headers=headers, timeout=30)
        resp.raise_for_status()
        soup = BeautifulSoup(resp.text, "html.parser")
        symbols = []
        for link in soup.find_all("a", {"data-test": "quoteLink"}):
            symbol = link.get_text(strip=True)
            if symbol:
                symbols.append(symbol)
        return symbols

    try:
        return retry_with_backoff(_fetch, max_retries=3, initial_delay=2.0)
    except Exception as e:
        logger.error(f"Error downloading earnings calendar: {e}")
        return []


def download_sp500_symbols() -> list[str]:
    """Download S&P 500 component symbols from Wikipedia."""
    def _fetch() -> list[str]:
        url = "https://en.wikipedia.org/wiki/List_of_S%26P_500_companies"
        resp = requests.get(url, timeout=30, headers={"User-Agent": "Mozilla/5.0"})
        resp.raise_for_status()
        soup = BeautifulSoup(resp.text, "html.parser")
        table = soup.find("table", {"id": "constituents"})
        symbols = []
        if table:
            for row in table.find_all("tr")[1:]:
                cells = row.find_all("td")
                if cells:
                    symbol = cells[0].get_text(strip=True)
                    if symbol:
                        symbols.append(symbol)
        return symbols

    try:
        return retry_with_backoff(_fetch, max_retries=3, initial_delay=2.0)
    except Exception as e:
        logger.error(f"Error downloading S&P 500 symbols: {e}")
        return []
