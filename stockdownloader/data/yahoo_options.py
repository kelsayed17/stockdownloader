"""Options chain data from Yahoo Finance."""

import json
import logging
from stockdownloader.model.option_type import OptionType
from stockdownloader.model.option_contract import OptionContract
from stockdownloader.model.options_chain import OptionsChain
from stockdownloader.data.yahoo_auth import get_session, get_crumb
from stockdownloader.util.retry_executor import retry_with_backoff

logger = logging.getLogger(__name__)


def fetch_options_chain(symbol: str) -> OptionsChain:
    """Fetch the full options chain for a symbol."""
    chain = OptionsChain(symbol)

    def _fetch_expirations() -> dict:
        session = get_session()
        crumb = get_crumb()
        url = f"https://query1.finance.yahoo.com/v7/finance/options/{symbol}"
        params = {"crumb": crumb}
        resp = session.get(url, params=params, timeout=30)
        resp.raise_for_status()
        return resp.json()

    try:
        data = retry_with_backoff(_fetch_expirations, max_retries=3, initial_delay=2.0)
        result = data.get("optionChain", {}).get("result", [])
        if not result:
            return chain

        chain_data = result[0]
        chain.underlying_price = chain_data.get("quote", {}).get("regularMarketPrice", 0.0)
        expirations = chain_data.get("expirationDates", [])

        from datetime import datetime
        for exp_ts in expirations:
            exp_date = datetime.fromtimestamp(exp_ts).strftime("%Y-%m-%d")
            chain.add_expiration_date(exp_date)

        options = chain_data.get("options", [])
        for opt_data in options:
            exp_date = datetime.fromtimestamp(opt_data.get("expirationDate", 0)).strftime("%Y-%m-%d")
            for call in opt_data.get("calls", []):
                contract = _parse_contract(call, OptionType.CALL)
                if contract:
                    chain.add_call(exp_date, contract)
            for put in opt_data.get("puts", []):
                contract = _parse_contract(put, OptionType.PUT)
                if contract:
                    chain.add_put(exp_date, contract)

    except Exception as e:
        logger.error(f"Error fetching options chain for {symbol}: {e}")
    return chain


def _parse_contract(data: dict, option_type: OptionType) -> OptionContract | None:
    """Parse a single options contract from Yahoo Finance data."""
    try:
        return OptionContract(
            contract_symbol=data.get("contractSymbol", ""),
            type=option_type,
            strike=data.get("strike", 0.0),
            expiration_date=data.get("expiration", ""),
            last_price=data.get("lastPrice", 0.0),
            bid=data.get("bid", 0.0),
            ask=data.get("ask", 0.0),
            volume=data.get("volume", 0),
            open_interest=data.get("openInterest", 0),
            implied_volatility=data.get("impliedVolatility", 0.0),
            delta=0.0,
            gamma=0.0,
            theta=0.0,
            vega=0.0,
            in_the_money=data.get("inTheMoney", False),
        )
    except Exception as e:
        logger.warning(f"Error parsing contract: {e}")
        return None
