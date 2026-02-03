import Foundation

struct BreakoutStrategy: TradingStrategy {
    let name = "Breakout Strategy"
    let description = "Bollinger Band squeeze detection with volume and ATR confirmation"
    let warmupPeriod = 30

    func evaluate(data: [PriceData], index: Int, indicators: StrategyIndicators) -> TradeAction {
        guard index >= 5,
              let bandwidth = indicators.bollingerBands.bandwidth[index],
              let upperBand = indicators.bollingerBands.upper[index],
              let lowerBand = indicators.bollingerBands.lower[index],
              let atrVal = indicators.atr[index] else {
            return .hold
        }

        let close = indicators.closes[index]
        let volume = indicators.volumes[index]

        // Calculate average volume over last 20 bars
        let lookback = min(20, index)
        let avgVolume = indicators.volumes[(index - lookback)..<index].reduce(0, +) / Double(lookback)

        // Check for squeeze (low bandwidth)
        let prevBandwidths = (max(0, index - 10)..<index).compactMap { indicators.bollingerBands.bandwidth[$0] }
        let avgBandwidth = prevBandwidths.isEmpty ? bandwidth : prevBandwidths.reduce(0, +) / Double(prevBandwidths.count)

        let isSqueeze = bandwidth < avgBandwidth * 0.75

        // Breakout above upper band with volume confirmation
        if close > upperBand && volume > avgVolume * 1.5 && !isSqueeze {
            return .buy
        }

        // ATR-based stop loss: sell if price drops more than 2x ATR below recent high
        if index >= 10 {
            let recentHigh = indicators.highs[(index - 10)...index].max() ?? close
            if close < recentHigh - 2.0 * atrVal {
                return .sell
            }
        }

        // Break below lower band
        if close < lowerBand && volume > avgVolume * 1.2 {
            return .sell
        }

        return .hold
    }
}
