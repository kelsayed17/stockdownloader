import Foundation

struct MomentumConfluenceStrategy: TradingStrategy {
    let name = "Momentum Confluence"
    let description = "Multi-indicator: Combines MACD, ADX, EMA, and OBV for high-confidence signals"
    let warmupPeriod = 50

    func evaluate(data: [PriceData], index: Int, indicators: StrategyIndicators) -> TradeAction {
        guard index >= 5,
              let macdLine = indicators.macd.macdLine[index],
              let signalLine = indicators.macd.signalLine[index],
              let adxVal = indicators.adx.adx[index],
              let plusDI = indicators.adx.plusDI[index],
              let minusDI = indicators.adx.minusDI[index],
              let ema12 = indicators.ema12[index],
              let ema26 = indicators.ema26[index] else {
            return .hold
        }

        let close = indicators.closes[index]

        // OBV trend (compare current vs 5-bar ago)
        let obvTrending = indicators.obv[index] > indicators.obv[index - 5]

        // Count buy signals
        var buySignals = 0
        var sellSignals = 0

        // MACD above signal = bullish
        if macdLine > signalLine { buySignals += 1 } else { sellSignals += 1 }

        // ADX trending with direction
        if adxVal > 20 {
            if plusDI > minusDI { buySignals += 1 } else { sellSignals += 1 }
        }

        // Price above EMA = bullish
        if close > ema12 && ema12 > ema26 { buySignals += 1 }
        if close < ema12 && ema12 < ema26 { sellSignals += 1 }

        // OBV confirming
        if obvTrending { buySignals += 1 } else { sellSignals += 1 }

        // Need at least 3 out of 4 for confluence
        if buySignals >= 3 {
            return .buy
        }

        if sellSignals >= 3 {
            return .sell
        }

        return .hold
    }
}
