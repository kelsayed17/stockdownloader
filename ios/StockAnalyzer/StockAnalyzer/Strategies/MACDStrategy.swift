import Foundation

struct MACDStrategy: TradingStrategy {
    let name = "MACD (12/26/9)"
    let description = "Momentum: Buys on MACD signal line crossover above, sells on crossover below"
    let warmupPeriod = 35

    func evaluate(data: [PriceData], index: Int, indicators: StrategyIndicators) -> TradeAction {
        guard index >= 1,
              let macdLine = indicators.macd.macdLine[index],
              let signalLine = indicators.macd.signalLine[index],
              let prevMacdLine = indicators.macd.macdLine[index - 1],
              let prevSignalLine = indicators.macd.signalLine[index - 1] else {
            return .hold
        }

        if prevMacdLine <= prevSignalLine && macdLine > signalLine {
            return .buy
        }

        if prevMacdLine >= prevSignalLine && macdLine < signalLine {
            return .sell
        }

        return .hold
    }
}
