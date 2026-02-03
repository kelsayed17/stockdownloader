import Foundation

class BacktestEngine {
    let initialCapital: Decimal
    let commissionPerTrade: Decimal

    init(initialCapital: Decimal = 100_000, commissionPerTrade: Decimal = 0) {
        self.initialCapital = initialCapital
        self.commissionPerTrade = commissionPerTrade
    }

    func run(strategy: TradingStrategy, data: [PriceData]) -> BacktestResult {
        guard data.count > strategy.warmupPeriod else {
            return BacktestResult(
                strategyName: strategy.name,
                initialCapital: initialCapital,
                finalCapital: initialCapital,
                trades: [],
                equityCurve: []
            )
        }

        let indicators = StrategyIndicators.calculate(from: data)

        var capital = initialCapital
        var trades: [Trade] = []
        var currentTrade: Trade?
        var equityCurve: [(String, Decimal)] = []

        for i in strategy.warmupPeriod..<data.count {
            let action = strategy.evaluate(data: data, index: i, indicators: indicators)
            let price = data[i].close

            switch action {
            case .buy:
                if currentTrade == nil {
                    let shares = Int(NSDecimalNumber(decimal: (capital - commissionPerTrade) / price).intValue)
                    if shares > 0 {
                        currentTrade = Trade(
                            direction: .long,
                            entryDate: data[i].date,
                            entryPrice: price,
                            shares: shares
                        )
                        capital -= price * Decimal(shares) + commissionPerTrade
                    }
                }

            case .sell:
                if var trade = currentTrade {
                    trade.close(exitDate: data[i].date, exitPrice: price)
                    capital += price * Decimal(trade.shares) - commissionPerTrade
                    trades.append(trade)
                    currentTrade = nil
                }

            case .hold:
                break
            }

            // Calculate equity
            var equity = capital
            if let trade = currentTrade {
                equity += price * Decimal(trade.shares)
            }
            equityCurve.append((data[i].date, equity))
        }

        // Close any open position at the end
        if var trade = currentTrade {
            let lastPrice = data.last!.close
            trade.close(exitDate: data.last!.date, exitPrice: lastPrice)
            capital += lastPrice * Decimal(trade.shares) - commissionPerTrade
            trades.append(trade)
        }

        let finalCapital = capital
        if let lastDate = data.last?.date {
            if equityCurve.last?.0 != lastDate {
                equityCurve.append((lastDate, finalCapital))
            }
        }

        return BacktestResult(
            strategyName: strategy.name,
            initialCapital: initialCapital,
            finalCapital: finalCapital,
            trades: trades,
            equityCurve: equityCurve
        )
    }

    func runAllStrategies(data: [PriceData]) -> [BacktestResult] {
        return StrategyRegistry.allEquityStrategies.map { strategy in
            run(strategy: strategy, data: data)
        }
    }

    func buyAndHoldResult(data: [PriceData]) -> BacktestResult {
        guard let first = data.first, let last = data.last else {
            return BacktestResult(
                strategyName: "Buy & Hold",
                initialCapital: initialCapital,
                finalCapital: initialCapital,
                trades: [],
                equityCurve: []
            )
        }

        let shares = Int(NSDecimalNumber(decimal: initialCapital / first.close).intValue)
        let cost = first.close * Decimal(shares)
        let revenue = last.close * Decimal(shares)
        let remainder = initialCapital - cost

        var trade = Trade(
            direction: .long,
            entryDate: first.date,
            entryPrice: first.close,
            shares: shares
        )
        trade.close(exitDate: last.date, exitPrice: last.close)

        let equityCurve = data.map { price -> (String, Decimal) in
            let equity = price.close * Decimal(shares) + remainder
            return (price.date, equity)
        }

        return BacktestResult(
            strategyName: "Buy & Hold",
            initialCapital: initialCapital,
            finalCapital: revenue + remainder,
            trades: [trade],
            equityCurve: equityCurve
        )
    }
}
