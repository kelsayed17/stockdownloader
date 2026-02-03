# Stock Downloader

A Python-based stock market analysis and backtesting platform. It fetches historical price data from Yahoo Finance and runs configurable trading strategies against it, producing detailed performance reports for both equity and options strategies.

## Features

- **Historical data download** from Yahoo Finance with automatic authentication
- **Equity backtesting** with SMA Crossover, RSI, MACD, Bollinger Band RSI, Momentum Confluence, Breakout, and Multi-Indicator strategies
- **Options backtesting** with Covered Call and Protective Put strategies using Black-Scholes pricing
- **Trend analysis** across stock universes (NASDAQ, Zacks lists, earnings calendars)
- **Signal generation** with confluence scoring across trend, momentum, volume, and volatility indicators
- **Strategy comparison** with side-by-side performance metrics and buy-and-hold benchmarks
- **20+ technical indicators**: RSI, MACD, Bollinger Bands, Ichimoku Cloud, Stochastic, Williams %R, CCI, ROC, ADX, Parabolic SAR, VWAP, OBV, MFI, Fibonacci retracement, support/resistance

## Requirements

- Python 3.12+
- pip

## Installation

```bash
# Install dependencies
pip install -r requirements.txt

# Or install as a package
pip install -e .

# Install development dependencies
pip install -e ".[dev]"
```

## Usage

### Symbol Analysis (Full Suite)

Runs all equity and options strategies, generates trading alerts with confluence scoring:

```bash
python -m stockdownloader.app.symbol_analysis_app AAPL
python -m stockdownloader.app.symbol_analysis_app SPY 2y
```

### SPY Equity Backtest

Runs nine equity trading strategies against historical data:

```bash
# Fetch from Yahoo Finance
python -m stockdownloader.app.spy_backtest_app
python -m stockdownloader.app.spy_backtest_app AAPL

# From a CSV file
python -m stockdownloader.app.spy_backtest_app --csv spy_data.csv
```

CSV format: `Date,Open,High,Low,Close,Adj Close,Volume`

### Options Backtest

Runs six options strategies (covered calls and protective puts) against historical data:

```bash
python -m stockdownloader.app.options_backtest_app
python -m stockdownloader.app.options_backtest_app --csv spy_data.csv
```

### Trend Analysis

Scans stock universes for price patterns:

```bash
python -m stockdownloader.app.trend_analysis_app
```

## Backtesting Strategies

### Equity Strategies

All equity strategies start with $100,000 initial capital and zero commission.

#### SMA Crossover (50/200)

Long-term trend-following using the classic golden cross / death cross signals.

- **BUY**: 50-day SMA crosses above 200-day SMA
- **SELL**: 50-day SMA crosses below 200-day SMA
- **Best for**: Strong, sustained trends
- **Warmup**: 200 bars

#### SMA Crossover (20/50)

Medium-term trend-following with faster signal generation.

- **BUY**: 20-day SMA crosses above 50-day SMA
- **SELL**: 20-day SMA crosses below 50-day SMA
- **Best for**: Intermediate trends, more frequent trading
- **Warmup**: 50 bars

#### RSI (14, 30/70)

Standard mean-reversion strategy using Relative Strength Index.

- **BUY**: RSI crosses above 30 (oversold recovery)
- **SELL**: RSI crosses below 70 (overbought pullback)
- **Best for**: Range-bound and choppy markets
- **Warmup**: 15 bars

#### RSI (14, 25/75)

Aggressive RSI variant with tighter thresholds.

- **BUY**: RSI crosses above 25
- **SELL**: RSI crosses below 75
- **Best for**: High-volatility environments with sharp reversals
- **Warmup**: 15 bars

#### MACD (12/26/9)

Momentum strategy using Moving Average Convergence Divergence.

- **BUY**: MACD line crosses above signal line
- **SELL**: MACD line crosses below signal line
- **Best for**: Trending markets with clear momentum shifts
- **Warmup**: 35 bars

#### Bollinger Band + RSI

Combined Bollinger Band and RSI strategy.

- **BUY**: Price touches lower band AND RSI < 30
- **SELL**: Price touches upper band AND RSI > 70

#### Momentum Confluence

Multi-signal momentum confirmation strategy using MACD, RSI, and Stochastic.

#### Breakout

Channel breakout strategy based on high/low price channels.

#### Multi-Indicator

Scores multiple indicators and requires confluence for signals.

### Options Strategies

All options strategies start with $100,000 initial capital and $0.65/contract commission. Option premiums are estimated using the Black-Scholes model with historical volatility derived from 20-day log returns.

#### Covered Call

Sells out-of-the-money calls against a long stock position to generate income.

| Variant | MA Period | OTM % | DTE | Exit Threshold |
|---------|-----------|-------|-----|----------------|
| Aggressive income | 20 | 3% | 30 | 3% |
| Standard | 20 | 5% | 30 | 3% |
| Conservative | 50 | 5% | 45 | 4% |

#### Protective Put

Buys out-of-the-money puts to hedge a long stock position against downside risk.

| Variant | MA Period | OTM % | DTE | Momentum Lookback |
|---------|-----------|-------|-----|--------------------|
| Standard hedge | 20 | 5% | 30 | 5 bars |
| Aggressive protection | 20 | 3% | 45 | 10 bars |
| Conservative long-term | 50 | 5% | 60 | 10 bars |

## Project Structure

```
stockdownloader/
  app/                              # Entry points
    spy_backtest_app.py               # Equity backtest runner
    options_backtest_app.py           # Options backtest runner
    trend_analysis_app.py             # Stock universe pattern scanner
    symbol_analysis_app.py            # Full symbol analysis with alerts
  strategy/                         # Trading strategies
    trading_strategy.py               # Equity strategy ABC
    options_strategy.py               # Options strategy ABC
    sma_crossover_strategy.py         # SMA golden/death cross
    rsi_strategy.py                   # RSI overbought/oversold
    macd_strategy.py                  # MACD signal line crossover
    bollinger_band_rsi_strategy.py    # Bollinger Band + RSI
    momentum_confluence_strategy.py   # Multi-momentum confluence
    breakout_strategy.py              # Channel breakout
    multi_indicator_strategy.py       # Multi-indicator scoring
    covered_call_strategy.py          # Sell OTM calls for income
    protective_put_strategy.py        # Buy OTM puts for hedging
  backtest/                         # Backtesting engines
    backtest_engine.py                # Equity simulation engine
    options_backtest_engine.py        # Options simulation engine
    backtest_result.py                # Equity result metrics
    options_backtest_result.py        # Options result metrics
    backtest_report_formatter.py      # Equity report output
    options_backtest_report_formatter.py
  model/                            # Data models
    price_data.py                     # OHLCV price dataclass
    trade.py                          # Equity trade tracking
    options_trade.py                  # Options trade tracking
    option_contract.py                # Option with greeks
    options_chain.py                  # Full chain by expiration
    unified_market_data.py            # Consolidated symbol data
    indicator_values.py               # Technical indicator snapshot
    alert_result.py                   # Trading alert with recommendations
  data/                             # Data fetching
    yahoo_data_client.py              # Yahoo Finance API client
    yahoo_auth_helper.py              # Yahoo authentication
    csv_price_data_loader.py          # CSV parser
    stock_list_downloader.py          # Stock list downloads
    morningstar_client.py             # Morningstar financial data
  util/                             # Utilities
    moving_average_calculator.py      # SMA and EMA calculation
    black_scholes_calculator.py       # Option pricing and greeks
    technical_indicators.py           # 20+ technical indicators
    big_decimal_math.py               # Decimal arithmetic helpers
    date_helper.py                    # Market date calculations
    file_helper.py                    # File I/O utilities
    retry_executor.py                 # Retry logic
  analysis/                         # Analysis tools
    signal_generator.py               # Trading alerts with confluence
    formula_calculator.py             # Stock valuation formulas
    pattern_analyzer.py               # Price pattern analysis
tests/                              # Test suite (pytest)
  model/                              # Model unit tests
  util/                               # Utility unit tests
  strategy/                           # Strategy unit tests
  backtest/                           # Backtest unit tests
  data/                               # Data layer tests
  analysis/                           # Analysis unit tests
  integration/                        # Integration tests
  e2e/                                # End-to-end tests
```

## Tests

```bash
# Run all tests
pytest

# Run with coverage
pytest --cov=stockdownloader

# Run specific test module
pytest tests/model/
pytest tests/strategy/
pytest tests/backtest/

# Run integration tests
pytest tests/integration/

# Run end-to-end tests
pytest tests/e2e/
```

## Disclaimer

This software is for **educational purposes only**. It is not financial advice. Past backtest performance does not guarantee future results. Options trading involves significant risk of loss. Always do your own research before making investment decisions.
