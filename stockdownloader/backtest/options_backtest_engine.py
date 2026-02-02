"""Options backtesting simulation engine using Black-Scholes pricing."""

import math
from stockdownloader.model.price_data import PriceData
from stockdownloader.model.option_type import OptionType
from stockdownloader.model.options_trade import OptionsTrade, OptionsDirection, OptionsTradeStatus
from stockdownloader.strategy.options_strategy import OptionsStrategy, OptionsSignal
from stockdownloader.backtest.options_backtest_result import OptionsBacktestResult
from stockdownloader.util import black_scholes


class OptionsBacktestEngine:
    CONTRACT_MULTIPLIER = 100

    def __init__(self, initial_capital: float = 100_000.0, commission_per_contract: float = 0.65,
                 option_type: OptionType = OptionType.CALL, direction: OptionsDirection = OptionsDirection.SELL,
                 risk_free_rate: float = 0.05):
        self.initial_capital = initial_capital
        self.commission_per_contract = commission_per_contract
        self.option_type = option_type
        self.direction = direction
        self.risk_free_rate = risk_free_rate

    def run(self, strategy: OptionsStrategy, data: list[PriceData]) -> OptionsBacktestResult:
        if not data:
            raise ValueError("data must not be empty")

        result = OptionsBacktestResult(strategy.get_name(), self.initial_capital)
        cash = self.initial_capital
        current_trade: OptionsTrade | None = None
        equity_curve: list[float] = []

        result.start_date = data[0].date
        result.end_date = data[-1].date

        vol_lookback = 20

        for i, bar in enumerate(data):
            signal = strategy.evaluate(data, i)

            equity = cash
            if current_trade and current_trade.status == OptionsTradeStatus.OPEN:
                close_prices = [data[j].close for j in range(max(0, i - vol_lookback), i + 1)]
                vol = black_scholes.estimate_volatility(close_prices, vol_lookback)
                dte = strategy.get_days_to_expiry()
                time_to_expiry = max(dte / 365.0, 1 / 365.0)
                current_premium = black_scholes.price(
                    self.option_type, bar.close, current_trade.strike,
                    time_to_expiry, self.risk_free_rate, vol
                )
                position_value = current_premium * current_trade.contracts * self.CONTRACT_MULTIPLIER
                if self.direction == OptionsDirection.SELL:
                    equity = cash + current_trade.total_entry_cost() - position_value
                else:
                    equity = cash + position_value
            equity_curve.append(equity)

            if signal == OptionsSignal.OPEN and current_trade is None:
                close_prices = [data[j].close for j in range(max(0, i - vol_lookback), i + 1)]
                vol = black_scholes.estimate_volatility(close_prices, vol_lookback)
                strike = strategy.get_strike_price(data, i)
                dte = strategy.get_days_to_expiry()
                time_to_expiry = dte / 365.0

                premium = black_scholes.price(
                    self.option_type, bar.close, strike,
                    time_to_expiry, self.risk_free_rate, vol
                )

                if premium > 0:
                    contracts = max(1, int(bar.close * 100 / (self.initial_capital * 0.1)))
                    contracts = min(contracts, 10)
                    commission = self.commission_per_contract * contracts

                    current_trade = OptionsTrade(
                        option_type=self.option_type,
                        direction=self.direction,
                        strike=strike,
                        expiration_date=bar.date,
                        entry_date=bar.date,
                        entry_premium=premium,
                        contracts=contracts,
                        entry_volume=bar.volume,
                    )

                    if self.direction == OptionsDirection.BUY:
                        cash -= premium * contracts * self.CONTRACT_MULTIPLIER + commission
                    else:
                        cash += premium * contracts * self.CONTRACT_MULTIPLIER - commission

            elif signal == OptionsSignal.CLOSE and current_trade and current_trade.status == OptionsTradeStatus.OPEN:
                close_prices = [data[j].close for j in range(max(0, i - vol_lookback), i + 1)]
                vol = black_scholes.estimate_volatility(close_prices, vol_lookback)
                time_to_expiry = max(strategy.get_days_to_expiry() / 365.0, 1 / 365.0)

                exit_premium = black_scholes.price(
                    self.option_type, bar.close, current_trade.strike,
                    time_to_expiry, self.risk_free_rate, vol
                )

                commission = self.commission_per_contract * current_trade.contracts
                current_trade.close(bar.date, exit_premium)

                if self.direction == OptionsDirection.BUY:
                    cash += exit_premium * current_trade.contracts * self.CONTRACT_MULTIPLIER - commission
                else:
                    cash -= exit_premium * current_trade.contracts * self.CONTRACT_MULTIPLIER + commission

                result.add_trade(current_trade)
                current_trade = None

        # Close any open position at end
        if current_trade and current_trade.status == OptionsTradeStatus.OPEN:
            last_bar = data[-1]
            close_prices = [d.close for d in data[-vol_lookback:]]
            vol = black_scholes.estimate_volatility(close_prices, vol_lookback)
            exit_premium = black_scholes.price(
                self.option_type, last_bar.close, current_trade.strike,
                1 / 365.0, self.risk_free_rate, vol
            )
            commission = self.commission_per_contract * current_trade.contracts
            current_trade.close(last_bar.date, exit_premium)

            if self.direction == OptionsDirection.BUY:
                cash += exit_premium * current_trade.contracts * self.CONTRACT_MULTIPLIER - commission
            else:
                cash -= exit_premium * current_trade.contracts * self.CONTRACT_MULTIPLIER + commission

            result.add_trade(current_trade)

        result.final_capital = cash
        result.equity_curve = equity_curve
        return result
