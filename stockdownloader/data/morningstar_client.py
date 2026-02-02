"""Integration with Morningstar data."""

import logging
import requests
from bs4 import BeautifulSoup
from stockdownloader.model.financial_data import FinancialData
from stockdownloader.util.retry_executor import retry_with_backoff

logger = logging.getLogger(__name__)


def fetch_financial_data(symbol: str) -> FinancialData:
    """Fetch financial data from Morningstar."""
    fd = FinancialData()

    def _fetch() -> str:
        url = f"https://financials.morningstar.com/ajax/ReportProcess4HtmlAjax.html"
        params = {"t": symbol, "reportType": "is", "period": "3"}
        resp = requests.get(url, params=params, timeout=30,
                          headers={"User-Agent": "Mozilla/5.0"})
        resp.raise_for_status()
        return resp.text

    try:
        html = retry_with_backoff(_fetch, max_retries=3, initial_delay=2.0)
        if html:
            _parse_morningstar_html(html, fd)
    except Exception as e:
        logger.error(f"Error fetching Morningstar data for {symbol}: {e}")
        fd.error = True
    return fd


def _parse_morningstar_html(html: str, fd: FinancialData) -> None:
    """Parse Morningstar financial HTML."""
    try:
        soup = BeautifulSoup(html, "html.parser")
        rows = soup.find_all("tr")
        for row in rows:
            header = row.find("th")
            if header:
                label = header.get_text(strip=True).lower()
                cells = row.find_all("td")
                if "revenue" in label and cells:
                    for i, cell in enumerate(cells[:5]):
                        try:
                            val = cell.get_text(strip=True).replace(",", "")
                            if val and val != "\u2014":
                                fd.revenue[i] = int(float(val) * 1_000_000)
                        except ValueError:
                            continue
        fd.compute_revenue_per_share()
    except Exception as e:
        logger.error(f"Error parsing Morningstar HTML: {e}")
        fd.incomplete = True
