import Foundation

struct RSIStrategy30_70: TradingStrategy {
    let name = "RSI (30/70)"
    let description = "Mean reversion: Buys when RSI drops below 30, sells when RSI rises above 70"
    let warmupPeriod = 15

    func evaluate(data: [PriceData], index: Int, indicators: StrategyIndicators) -> TradeAction {
        guard let rsi = indicators.rsi14[index] else {
            return .hold
        }

        if rsi < 30 {
            return .buy
        }

        if rsi > 70 {
            return .sell
        }

        return .hold
    }
}

struct RSIStrategy25_75: TradingStrategy {
    let name = "RSI (25/75)"
    let description = "Aggressive RSI: Buys when RSI drops below 25, sells when RSI rises above 75"
    let warmupPeriod = 15

    func evaluate(data: [PriceData], index: Int, indicators: StrategyIndicators) -> TradeAction {
        guard let rsi = indicators.rsi14[index] else {
            return .hold
        }

        if rsi < 25 {
            return .buy
        }

        if rsi > 75 {
            return .sell
        }

        return .hold
    }
}
