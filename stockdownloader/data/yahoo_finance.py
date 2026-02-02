"""General Yahoo Finance API client for financial data."""

import json
import logging
from stockdownloader.model.financial_data import FinancialData
from stockdownloader.data.yahoo_auth import get_session, get_crumb
from stockdownloader.util.retry_executor import retry_with_backoff

logger = logging.getLogger(__name__)


def fetch_financial_data(symbol: str) -> FinancialData:
    """Fetch fundamental financial data for a symbol."""
    def _fetch() -> FinancialData:
        session = get_session()
        crumb = get_crumb()
        url = f"https://query1.finance.yahoo.com/v10/finance/quoteSummary/{symbol}"
        params = {
            "modules": "incomeStatementHistory,incomeStatementHistoryQuarterly,defaultKeyStatistics",
            "crumb": crumb,
        }
        resp = session.get(url, params=params, timeout=30)
        resp.raise_for_status()
        return _parse_financials(resp.json())

    try:
        return retry_with_backoff(_fetch, max_retries=3, initial_delay=2.0)
    except Exception as e:
        logger.error(f"Failed to fetch financial data for {symbol}: {e}")
        fd = FinancialData()
        fd.error = True
        return fd


def _parse_financials(data: dict) -> FinancialData:
    """Parse Yahoo Finance financial summary response."""
    fd = FinancialData()
    try:
        summary = data.get("quoteSummary", {}).get("result", [{}])[0]
        quarterly = summary.get("incomeStatementHistoryQuarterly", {}).get("incomeStatementHistory", [])

        for i, stmt in enumerate(quarterly[:5]):
            if i < 6:
                fd.revenue[i] = stmt.get("totalRevenue", {}).get("raw", 0)
                fd.fiscal_quarters[i] = stmt.get("endDate", {}).get("fmt", "")

        stats = summary.get("defaultKeyStatistics", {})
        shares = stats.get("sharesOutstanding", {}).get("raw", 0)
        for i in range(6):
            fd.diluted_shares[i] = shares

        fd.compute_revenue_per_share()
    except Exception as e:
        logger.error(f"Error parsing financials: {e}")
        fd.incomplete = True
    return fd
