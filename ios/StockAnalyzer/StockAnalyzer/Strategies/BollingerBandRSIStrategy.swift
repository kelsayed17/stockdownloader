import Foundation

struct BollingerBandRSIStrategy: TradingStrategy {
    let name = "Bollinger Band + RSI"
    let description = "Mean reversion with ADX trend filter: Buys at lower band with low RSI in non-trending market"
    let warmupPeriod = 30

    func evaluate(data: [PriceData], index: Int, indicators: StrategyIndicators) -> TradeAction {
        guard let rsi = indicators.rsi14[index],
              let lowerBand = indicators.bollingerBands.lower[index],
              let upperBand = indicators.bollingerBands.upper[index],
              let adxVal = indicators.adx.adx[index] else {
            return .hold
        }

        let close = indicators.closes[index]
        let isTrending = adxVal > 25

        // In non-trending market, use mean reversion
        if !isTrending {
            if close <= lowerBand && rsi < 35 {
                return .buy
            }
            if close >= upperBand && rsi > 65 {
                return .sell
            }
        } else {
            // In trending market, trade with the trend
            if let plusDI = indicators.adx.plusDI[index],
               let minusDI = indicators.adx.minusDI[index] {
                if plusDI > minusDI && close <= lowerBand && rsi < 40 {
                    return .buy
                }
                if minusDI > plusDI && close >= upperBand && rsi > 60 {
                    return .sell
                }
            }
        }

        return .hold
    }
}
