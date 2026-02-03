import Foundation

struct SMACrossover50_200Strategy: TradingStrategy {
    let name = "SMA Crossover (50/200)"
    let description = "Golden Cross / Death Cross: Buys when 50-day SMA crosses above 200-day SMA"
    let warmupPeriod = 200

    func evaluate(data: [PriceData], index: Int, indicators: StrategyIndicators) -> TradeAction {
        guard index >= 1,
              let sma50 = indicators.sma50[index],
              let sma200 = indicators.sma200[index],
              let prevSma50 = indicators.sma50[index - 1],
              let prevSma200 = indicators.sma200[index - 1] else {
            return .hold
        }

        if prevSma50 <= prevSma200 && sma50 > sma200 {
            return .buy
        }

        if prevSma50 >= prevSma200 && sma50 < sma200 {
            return .sell
        }

        return .hold
    }
}

struct SMACrossover20_50Strategy: TradingStrategy {
    let name = "SMA Crossover (20/50)"
    let description = "Medium-term trend following: Buys when 20-day SMA crosses above 50-day SMA"
    let warmupPeriod = 50

    func evaluate(data: [PriceData], index: Int, indicators: StrategyIndicators) -> TradeAction {
        guard index >= 1,
              let sma20 = indicators.sma20[index],
              let sma50 = indicators.sma50[index],
              let prevSma20 = indicators.sma20[index - 1],
              let prevSma50 = indicators.sma50[index - 1] else {
            return .hold
        }

        if prevSma20 <= prevSma50 && sma20 > sma50 {
            return .buy
        }

        if prevSma20 >= prevSma50 && sma20 < sma50 {
            return .sell
        }

        return .hold
    }
}
