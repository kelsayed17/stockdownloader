import Foundation

struct MultiIndicatorStrategy: TradingStrategy {
    let name = "Multi-Indicator Strategy"
    let description = "Advanced confluence: Combines RSI, MACD, Bollinger Bands, Stochastic, and ADX"
    let warmupPeriod = 50

    func evaluate(data: [PriceData], index: Int, indicators: StrategyIndicators) -> TradeAction {
        guard index >= 2,
              let rsi = indicators.rsi14[index],
              let macdLine = indicators.macd.macdLine[index],
              let signalLine = indicators.macd.signalLine[index],
              let prevMacdLine = indicators.macd.macdLine[index - 1],
              let prevSignalLine = indicators.macd.signalLine[index - 1],
              let lowerBand = indicators.bollingerBands.lower[index],
              let upperBand = indicators.bollingerBands.upper[index],
              let stochK = indicators.stochastic.k[index],
              let adxVal = indicators.adx.adx[index] else {
            return .hold
        }

        let close = indicators.closes[index]

        var bullishScore = 0
        var bearishScore = 0

        // RSI
        if rsi < 35 { bullishScore += 2 }
        else if rsi < 45 { bullishScore += 1 }
        else if rsi > 65 { bearishScore += 2 }
        else if rsi > 55 { bearishScore += 1 }

        // MACD crossover
        if prevMacdLine <= prevSignalLine && macdLine > signalLine {
            bullishScore += 2
        } else if prevMacdLine >= prevSignalLine && macdLine < signalLine {
            bearishScore += 2
        } else if macdLine > signalLine {
            bullishScore += 1
        } else {
            bearishScore += 1
        }

        // Bollinger Bands
        if close <= lowerBand { bullishScore += 2 }
        if close >= upperBand { bearishScore += 2 }

        // Stochastic
        if stochK < 20 { bullishScore += 1 }
        if stochK > 80 { bearishScore += 1 }

        // ADX (trend strength multiplier)
        let trendMultiplier = adxVal > 25 ? 1.5 : 1.0

        let adjustedBull = Double(bullishScore) * trendMultiplier
        let adjustedBear = Double(bearishScore) * trendMultiplier

        // Need strong confluence
        if adjustedBull >= 5 && adjustedBull > adjustedBear * 1.5 {
            return .buy
        }

        if adjustedBear >= 5 && adjustedBear > adjustedBull * 1.5 {
            return .sell
        }

        return .hold
    }
}

// MARK: - Strategy Registry

struct StrategyRegistry {
    static let allEquityStrategies: [TradingStrategy] = [
        SMACrossover50_200Strategy(),
        SMACrossover20_50Strategy(),
        RSIStrategy30_70(),
        RSIStrategy25_75(),
        MACDStrategy(),
        BollingerBandRSIStrategy(),
        MomentumConfluenceStrategy(),
        BreakoutStrategy(),
        MultiIndicatorStrategy()
    ]

    static let allOptionsConfigs: [OptionsStrategyConfig] = [
        // Covered Calls
        OptionsStrategyConfig(name: "Covered Call (MA20, 3% OTM, 30 DTE)", optionType: .coveredCall, maPeriod: 20, otmPercent: 0.03, dte: 30, exitThreshold: 0.03),
        OptionsStrategyConfig(name: "Covered Call (MA20, 5% OTM, 30 DTE)", optionType: .coveredCall, maPeriod: 20, otmPercent: 0.05, dte: 30, exitThreshold: 0.03),
        OptionsStrategyConfig(name: "Covered Call (MA50, 5% OTM, 45 DTE)", optionType: .coveredCall, maPeriod: 50, otmPercent: 0.05, dte: 45, exitThreshold: 0.04),
        // Protective Puts
        OptionsStrategyConfig(name: "Protective Put (MA20, 5% OTM, 30 DTE)", optionType: .protectivePut, maPeriod: 20, otmPercent: 0.05, dte: 30, exitThreshold: 0.05),
        OptionsStrategyConfig(name: "Protective Put (MA20, 3% OTM, 45 DTE)", optionType: .protectivePut, maPeriod: 20, otmPercent: 0.03, dte: 45, exitThreshold: 0.05),
        OptionsStrategyConfig(name: "Protective Put (MA50, 5% OTM, 60 DTE)", optionType: .protectivePut, maPeriod: 50, otmPercent: 0.05, dte: 60, exitThreshold: 0.05),
    ]
}

struct OptionsStrategyConfig: Identifiable {
    let id = UUID()
    let name: String
    let optionType: OptionType
    let maPeriod: Int
    let otmPercent: Double
    let dte: Int
    let exitThreshold: Double
}
