# Stock Downloader

A Java-based stock market analysis and backtesting platform. It fetches historical price data from Yahoo Finance and runs configurable trading strategies against it, producing detailed performance reports for both equity and options strategies.

## Features

- **Historical data download** from Yahoo Finance with automatic authentication
- **Equity backtesting** with SMA Crossover, RSI, and MACD strategies
- **Options backtesting** with Covered Call and Protective Put strategies using Black-Scholes pricing
- **Trend analysis** across stock universes (NASDAQ, Zacks lists, earnings calendars)
- **Strategy comparison** with side-by-side performance metrics and buy-and-hold benchmarks

## Requirements

- Java 21+
- Maven 3.x or Gradle 8.x

## Build

```bash
# Maven
mvn clean compile

# Gradle
gradle clean build
```

## Usage

### SPY Equity Backtest

Runs five equity trading strategies against SPY historical data:

```bash
# From a CSV file
java -cp "target/classes:lib/*" com.stockdownloader.app.SPYBacktestApp spy_data.csv

# Or download directly from Yahoo Finance (no argument needed)
java -cp "target/classes:lib/*" com.stockdownloader.app.SPYBacktestApp
```

CSV format: `Date,Open,High,Low,Close,Adj Close,Volume`

### Options Backtest

Runs six options strategies (covered calls and protective puts) against historical data:

```bash
java -cp "target/classes:lib/*" com.stockdownloader.app.OptionsBacktestApp spy_data.csv
```

### Trend Analysis

Scans stock universes for price patterns:

```bash
java -cp "target/classes:lib/*" com.stockdownloader.app.TrendAnalysisApp
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

Aggressive RSI variant with tighter thresholds, requiring stronger oversold/overbought signals before triggering.

- **BUY**: RSI crosses above 25
- **SELL**: RSI crosses below 75
- **Best for**: High-volatility environments with sharp reversals
- **Warmup**: 15 bars

#### MACD (12/26/9)

Momentum strategy using Moving Average Convergence Divergence.

- **BUY**: MACD line crosses above signal line
- **SELL**: MACD line crosses below signal line
- **Calculation**: MACD = 12-period EMA - 26-period EMA; Signal = 9-period EMA of MACD
- **Best for**: Trending markets with clear momentum shifts
- **Warmup**: 35 bars

### Options Strategies

All options strategies start with $100,000 initial capital and $0.65/contract commission. Option premiums are estimated using the Black-Scholes model with historical volatility derived from 20-day log returns.

#### Covered Call

Sells out-of-the-money calls against a long stock position to generate income. Caps upside in exchange for premium collected.

| Variant | MA Period | OTM % | DTE | Exit Threshold |
|---------|-----------|-------|-----|----------------|
| Aggressive income | 20 | 3% | 30 | 3% |
| Standard | 20 | 5% | 30 | 3% |
| Conservative | 50 | 5% | 45 | 4% |

- **OPEN**: Price crosses above the moving average (mild bullish bias)
- **CLOSE**: Price drops more than the exit threshold below the MA

#### Protective Put

Buys out-of-the-money puts to hedge a long stock position against downside risk.

| Variant | MA Period | OTM % | DTE | Momentum Lookback |
|---------|-----------|-------|-----|--------------------|
| Standard hedge | 20 | 5% | 30 | 5 bars |
| Aggressive protection | 20 | 3% | 45 | 10 bars |
| Conservative long-term | 50 | 5% | 60 | 10 bars |

- **OPEN**: Price crosses below the MA, or momentum drops below -2% over the lookback window
- **CLOSE**: Price recovers more than 2% above the MA

## Sample Output

### Equity Backtest Report

```
======================================================================
  BACKTEST REPORT: SMA Crossover (50/200)
======================================================================

  Period:              2021-01-04 to 2025-12-31
  Initial Capital:     $100000.00
  Final Capital:       $112480.35

----------------------------------------------------------------------
  PERFORMANCE METRICS
----------------------------------------------------------------------
  Total Return:        12.48%
  Buy & Hold Return:   58.27%
  Total P/L:           $12480.35
  Sharpe Ratio:        0.42
  Max Drawdown:        8.75%
  Profit Factor:       1.86

----------------------------------------------------------------------
  TRADE STATISTICS
----------------------------------------------------------------------
  Total Trades:        6
  Winning Trades:      4
  Losing Trades:       2
  Win Rate:            66.67%
  Average Win:         $4820.12
  Average Loss:        $3400.06
```

### Equity Strategy Comparison

```
==========================================================================================
  STRATEGY COMPARISON SUMMARY
==========================================================================================

  Strategy                              Return     Sharpe      MaxDD     Trades   Win Rate
------------------------------------------------------------------------------------------
  Buy & Hold (Benchmark)               58.27%        N/A        N/A          1        N/A
  SMA Crossover (50/200)               12.48%       0.42      8.75%          6     66.67%
  SMA Crossover (20/50)                18.92%       0.55     10.20%         14     57.14%
  RSI (14, 30/70)                       9.35%       0.31      7.40%         11     54.55%
  RSI (14, 25/75)                       6.20%       0.22      6.15%          7     57.14%
  MACD (12/26/9)                       15.60%       0.48     11.30%         18     50.00%
------------------------------------------------------------------------------------------

  Best performing strategy: SMA Crossover (20/50)
  Return: 18.92% | Sharpe: 0.55 | Max Drawdown: 10.20%
  >> Underperformed Buy & Hold by 39.35 percentage points

  DISCLAIMER: This is for educational purposes only.
  Past performance does not guarantee future results.
  Always do your own research before trading.
```

### Options Backtest Report

```
================================================================================
  OPTIONS BACKTEST REPORT: Covered Call (MA20, 5% OTM, 30DTE)
================================================================================

  Period:              2021-01-04 to 2025-12-31
  Initial Capital:     $100000.00
  Final Capital:       $104250.80

--------------------------------------------------------------------------------
  PERFORMANCE METRICS
--------------------------------------------------------------------------------
  Total Return:        4.25%
  Total P/L:           $4250.80
  Sharpe Ratio:        0.35
  Max Drawdown:        5.10%
  Profit Factor:       1.52

--------------------------------------------------------------------------------
  TRADE STATISTICS
--------------------------------------------------------------------------------
  Total Trades:        22
  Winning Trades:      15
  Losing Trades:       7
  Win Rate:            68.18%
  Average Win:         $520.40
  Average Loss:        $380.15

--------------------------------------------------------------------------------
  OPTIONS-SPECIFIC METRICS
--------------------------------------------------------------------------------
  Avg Premium/Trade:   $485.30
  Total Volume:        22 contracts
```

### Options Strategy Comparison

```
====================================================================================================
  OPTIONS STRATEGY COMPARISON
====================================================================================================

  Strategy                                   Return     Sharpe      MaxDD     Trades    WinRate    Volume
----------------------------------------------------------------------------------------------------
  Covered Call (MA20, 3% OTM, 30DTE)          5.80%       0.40      4.90%         25     72.00%        25
  Covered Call (MA20, 5% OTM, 30DTE)          4.25%       0.35      5.10%         22     68.18%        22
  Covered Call (MA50, 5% OTM, 45DTE)          3.10%       0.28      4.20%         12     66.67%        12
  Protective Put (MA20, 5% OTM, 30DTE)       -2.40%      -0.18      3.80%         18     44.44%        18
  Protective Put (MA20, 3% OTM, 45DTE)       -3.60%      -0.25      4.50%         15     40.00%        15
  Protective Put (MA50, 5% OTM, 60DTE)       -1.80%      -0.12      3.20%          8     37.50%         8
----------------------------------------------------------------------------------------------------

  Best performing strategy: Covered Call (MA20, 3% OTM, 30DTE)
  Return: 5.80% | Sharpe: 0.40 | Max Drawdown: 4.90%

  DISCLAIMER: This is for educational purposes only.
  Options trading involves significant risk. Past performance
  does not guarantee future results.
```

> **Note**: The numbers above are illustrative examples showing the report format. Actual results depend on the date range and market conditions of the input data.

## Project Structure

```
src/main/java/com/stockdownloader/
  app/                          # Entry points
    SPYBacktestApp.java           # Equity backtest runner
    OptionsBacktestApp.java       # Options backtest runner
    TrendAnalysisApp.java         # Stock universe pattern scanner
  strategy/                     # Trading strategies
    TradingStrategy.java          # Equity strategy interface
    OptionsStrategy.java          # Options strategy interface
    SMACrossoverStrategy.java     # SMA golden/death cross
    RSIStrategy.java              # RSI overbought/oversold
    MACDStrategy.java             # MACD signal line crossover
    CoveredCallStrategy.java      # Sell OTM calls for income
    ProtectivePutStrategy.java    # Buy OTM puts for hedging
  backtest/                     # Backtesting engines
    BacktestEngine.java           # Equity simulation engine
    OptionsBacktestEngine.java    # Options simulation engine
    BacktestResult.java           # Equity result metrics
    OptionsBacktestResult.java    # Options result metrics
    BacktestReportFormatter.java  # Equity report output
    OptionsBacktestReportFormatter.java
  model/                        # Data models
    PriceData.java                # OHLCV price record
    Trade.java                    # Equity trade tracking
    OptionsTrade.java             # Options trade tracking
    OptionContract.java           # Option with greeks
    OptionsChain.java             # Full chain by expiration
    UnifiedMarketData.java        # Consolidated symbol data
  data/                         # Data fetching
    YahooQuoteClient.java         # Yahoo Finance API client
    YahooAuthHelper.java          # Yahoo authentication
    CsvPriceDataLoader.java       # CSV parser
  util/                         # Utilities
    MovingAverageCalculator.java  # SMA and EMA calculation
    BlackScholesCalculator.java   # Option pricing and greeks
```

## Tests

```bash
# Maven
mvn test

# Gradle
gradle test
```

Integration tests cover the full pipeline: CSV loading, strategy signal generation, backtest execution, options pricing, and report formatting.

## Disclaimer

This software is for **educational purposes only**. It is not financial advice. Past backtest performance does not guarantee future results. Options trading involves significant risk of loss. Always do your own research before making investment decisions.
