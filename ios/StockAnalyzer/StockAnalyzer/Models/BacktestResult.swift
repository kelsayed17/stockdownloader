import Foundation

struct BacktestResult: Identifiable {
    let id = UUID()
    let strategyName: String
    let initialCapital: Decimal
    let finalCapital: Decimal
    let trades: [Trade]
    let equityCurve: [(String, Decimal)]

    var totalReturn: Decimal {
        guard initialCapital != 0 else { return 0 }
        return ((finalCapital - initialCapital) / initialCapital) * 100
    }

    var totalTrades: Int {
        trades.filter { $0.status == .closed }.count
    }

    var winningTrades: Int {
        trades.filter { $0.status == .closed && ($0.profitLoss ?? 0) > 0 }.count
    }

    var losingTrades: Int {
        trades.filter { $0.status == .closed && ($0.profitLoss ?? 0) <= 0 }.count
    }

    var winRate: Decimal {
        guard totalTrades > 0 else { return 0 }
        return (Decimal(winningTrades) / Decimal(totalTrades)) * 100
    }

    var totalProfit: Decimal {
        trades.filter { $0.status == .closed && ($0.profitLoss ?? 0) > 0 }
            .compactMap { $0.profitLoss }
            .reduce(0, +)
    }

    var totalLoss: Decimal {
        trades.filter { $0.status == .closed && ($0.profitLoss ?? 0) <= 0 }
            .compactMap { $0.profitLoss }
            .reduce(0, +)
    }

    var profitFactor: Decimal {
        let loss = abs(NSDecimalNumber(decimal: totalLoss).decimalValue)
        guard loss != 0 else { return totalProfit > 0 ? 999 : 0 }
        return totalProfit / loss
    }

    var averageWin: Decimal {
        guard winningTrades > 0 else { return 0 }
        return totalProfit / Decimal(winningTrades)
    }

    var averageLoss: Decimal {
        guard losingTrades > 0 else { return 0 }
        return totalLoss / Decimal(losingTrades)
    }

    var maxDrawdown: Decimal {
        guard !equityCurve.isEmpty else { return 0 }
        var peak: Decimal = equityCurve[0].1
        var maxDD: Decimal = 0

        for (_, equity) in equityCurve {
            if equity > peak {
                peak = equity
            }
            let drawdown = peak > 0 ? ((peak - equity) / peak) * 100 : 0
            if drawdown > maxDD {
                maxDD = drawdown
            }
        }
        return maxDD
    }

    var sharpeRatio: Double {
        let returns = equityCurve.enumerated().compactMap { index, item -> Double? in
            guard index > 0 else { return nil }
            let prev = NSDecimalNumber(decimal: equityCurve[index - 1].1).doubleValue
            let curr = NSDecimalNumber(decimal: item.1).doubleValue
            guard prev > 0 else { return nil }
            return (curr - prev) / prev
        }

        guard returns.count > 1 else { return 0 }

        let mean = returns.reduce(0, +) / Double(returns.count)
        let variance = returns.map { ($0 - mean) * ($0 - mean) }.reduce(0, +) / Double(returns.count - 1)
        let stdDev = sqrt(variance)

        guard stdDev > 0 else { return 0 }
        let annualized = (mean / stdDev) * sqrt(252.0)
        return annualized
    }
}

struct OptionsBacktestResult: Identifiable {
    let id = UUID()
    let strategyName: String
    let optionType: OptionType
    let initialCapital: Decimal
    let finalCapital: Decimal
    let trades: [OptionsTrade]

    var totalReturn: Decimal {
        guard initialCapital != 0 else { return 0 }
        return ((finalCapital - initialCapital) / initialCapital) * 100
    }

    var totalTrades: Int {
        trades.filter { $0.status == .closed }.count
    }

    var winRate: Decimal {
        let closed = trades.filter { $0.status == .closed }
        guard !closed.isEmpty else { return 0 }
        let wins = closed.filter { ($0.profitLoss ?? 0) > 0 }.count
        return (Decimal(wins) / Decimal(closed.count)) * 100
    }

    var totalPremiumCollected: Decimal {
        trades.compactMap { $0.profitLoss }.filter { $0 > 0 }.reduce(0, +)
    }

    var totalPremiumPaid: Decimal {
        trades.compactMap { $0.profitLoss }.filter { $0 < 0 }.reduce(0, +)
    }

    var netPremium: Decimal {
        trades.compactMap { $0.profitLoss }.reduce(0, +)
    }

    var averageDaysHeld: Double {
        let held = trades.compactMap { $0.daysHeld }
        guard !held.isEmpty else { return 0 }
        return Double(held.reduce(0, +)) / Double(held.count)
    }
}
