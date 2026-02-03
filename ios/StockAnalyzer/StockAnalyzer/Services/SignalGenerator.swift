import Foundation

struct SignalGenerator {

    static func generateAlert(symbol: String, data: [PriceData]) -> TradingAlert? {
        guard data.count > 50 else { return nil }

        let index = data.count - 1
        let indicators = StrategyIndicators.calculate(from: data)
        let closes = data.map { $0.closeDouble }
        let highs = data.map { $0.highDouble }
        let lows = data.map { $0.lowDouble }
        let volumes = data.map { $0.volumeDouble }

        var signals: [IndicatorSignal] = []

        // RSI Signal
        if let rsi = indicators.rsi14[index] {
            let signal: SignalType
            let desc: String
            if rsi < 25 { signal = .strongBuy; desc = "Deeply oversold" }
            else if rsi < 30 { signal = .buy; desc = "Oversold" }
            else if rsi > 75 { signal = .strongSell; desc = "Deeply overbought" }
            else if rsi > 70 { signal = .sell; desc = "Overbought" }
            else { signal = .hold; desc = "Neutral" }
            signals.append(IndicatorSignal(name: "RSI (14)", value: rsi, signal: signal, description: desc))
        }

        // MACD Signal
        if let macdLine = indicators.macd.macdLine[index],
           let signalLine = indicators.macd.signalLine[index] {
            let prevMacd = indicators.macd.macdLine[index - 1] ?? macdLine
            let prevSignal = indicators.macd.signalLine[index - 1] ?? signalLine
            let signal: SignalType
            let desc: String
            if prevMacd <= prevSignal && macdLine > signalLine {
                signal = .strongBuy; desc = "Bullish crossover"
            } else if prevMacd >= prevSignal && macdLine < signalLine {
                signal = .strongSell; desc = "Bearish crossover"
            } else if macdLine > signalLine {
                signal = .buy; desc = "Above signal line"
            } else {
                signal = .sell; desc = "Below signal line"
            }
            signals.append(IndicatorSignal(name: "MACD", value: macdLine, signal: signal, description: desc))
        }

        // SMA 50/200 Signal
        if let sma50 = indicators.sma50[index],
           let sma200 = indicators.sma200[index] {
            let close = closes[index]
            let signal: SignalType
            let desc: String
            if sma50 > sma200 && close > sma50 {
                signal = .strongBuy; desc = "Golden cross, price above MAs"
            } else if sma50 > sma200 {
                signal = .buy; desc = "Golden cross"
            } else if sma50 < sma200 && close < sma50 {
                signal = .strongSell; desc = "Death cross, price below MAs"
            } else if sma50 < sma200 {
                signal = .sell; desc = "Death cross"
            } else {
                signal = .hold; desc = "MAs converging"
            }
            signals.append(IndicatorSignal(name: "SMA 50/200", value: sma50, signal: signal, description: desc))
        }

        // Bollinger Bands Signal
        if let upper = indicators.bollingerBands.upper[index],
           let lower = indicators.bollingerBands.lower[index],
           let middle = indicators.bollingerBands.middle[index] {
            let close = closes[index]
            let signal: SignalType
            let desc: String
            if close <= lower {
                signal = .buy; desc = "At lower band"
            } else if close >= upper {
                signal = .sell; desc = "At upper band"
            } else if close > middle {
                signal = .hold; desc = "Above middle band"
            } else {
                signal = .hold; desc = "Below middle band"
            }
            signals.append(IndicatorSignal(name: "Bollinger Bands", value: close, signal: signal, description: desc))
        }

        // ADX Signal
        if let adxVal = indicators.adx.adx[index],
           let plusDI = indicators.adx.plusDI[index],
           let minusDI = indicators.adx.minusDI[index] {
            let signal: SignalType
            let desc: String
            if adxVal > 25 && plusDI > minusDI {
                signal = .buy; desc = "Strong uptrend (ADX: \(String(format: "%.1f", adxVal)))"
            } else if adxVal > 25 && minusDI > plusDI {
                signal = .sell; desc = "Strong downtrend (ADX: \(String(format: "%.1f", adxVal)))"
            } else {
                signal = .hold; desc = "Weak trend (ADX: \(String(format: "%.1f", adxVal)))"
            }
            signals.append(IndicatorSignal(name: "ADX", value: adxVal, signal: signal, description: desc))
        }

        // Stochastic Signal
        if let k = indicators.stochastic.k[index],
           let d = indicators.stochastic.d[index] {
            let signal: SignalType
            let desc: String
            if k < 20 && d < 20 {
                signal = .buy; desc = "Oversold (%K: \(String(format: "%.1f", k)))"
            } else if k > 80 && d > 80 {
                signal = .sell; desc = "Overbought (%K: \(String(format: "%.1f", k)))"
            } else if k > d {
                signal = .buy; desc = "%K above %D"
            } else {
                signal = .sell; desc = "%K below %D"
            }
            signals.append(IndicatorSignal(name: "Stochastic", value: k, signal: signal, description: desc))
        }

        // Williams %R Signal
        let willR = TechnicalIndicators.williamsR(highs: highs, lows: lows, closes: closes)
        if let wr = willR[index] {
            let signal: SignalType
            let desc: String
            if wr < -80 { signal = .buy; desc = "Oversold" }
            else if wr > -20 { signal = .sell; desc = "Overbought" }
            else { signal = .hold; desc = "Neutral" }
            signals.append(IndicatorSignal(name: "Williams %R", value: wr, signal: signal, description: desc))
        }

        // MFI Signal
        let mfi = TechnicalIndicators.mfi(highs: highs, lows: lows, closes: closes, volumes: volumes)
        if let mfiVal = mfi[index] {
            let signal: SignalType
            let desc: String
            if mfiVal < 20 { signal = .buy; desc = "Money flowing in" }
            else if mfiVal > 80 { signal = .sell; desc = "Money flowing out" }
            else { signal = .hold; desc = "Neutral flow" }
            signals.append(IndicatorSignal(name: "MFI", value: mfiVal, signal: signal, description: desc))
        }

        // CCI Signal
        let cci = TechnicalIndicators.cci(highs: highs, lows: lows, closes: closes)
        if let cciVal = cci[index] {
            let signal: SignalType
            let desc: String
            if cciVal < -100 { signal = .buy; desc = "Oversold" }
            else if cciVal > 100 { signal = .sell; desc = "Overbought" }
            else { signal = .hold; desc = "Neutral" }
            signals.append(IndicatorSignal(name: "CCI", value: cciVal, signal: signal, description: desc))
        }

        // ROC Signal
        let roc = TechnicalIndicators.roc(closes, period: 12)
        if let rocVal = roc[index] {
            let signal: SignalType
            let desc: String
            if rocVal > 5 { signal = .buy; desc = "Strong positive momentum" }
            else if rocVal > 0 { signal = .hold; desc = "Positive momentum" }
            else if rocVal < -5 { signal = .sell; desc = "Strong negative momentum" }
            else { signal = .hold; desc = "Negative momentum" }
            signals.append(IndicatorSignal(name: "ROC (12)", value: rocVal, signal: signal, description: desc))
        }

        // Calculate confluence
        let buyCount = Double(signals.filter { $0.signal == .buy || $0.signal == .strongBuy }.count)
        let sellCount = Double(signals.filter { $0.signal == .sell || $0.signal == .strongSell }.count)
        let totalSignals = Double(signals.count)
        let confluenceScore = totalSignals > 0 ? (buyCount - sellCount) / totalSignals : 0

        // Overall signal
        let overallSignal: SignalType
        if confluenceScore > 0.5 { overallSignal = .strongBuy }
        else if confluenceScore > 0.2 { overallSignal = .buy }
        else if confluenceScore < -0.5 { overallSignal = .strongSell }
        else if confluenceScore < -0.2 { overallSignal = .sell }
        else { overallSignal = .hold }

        return TradingAlert(
            symbol: symbol,
            date: data[index].date,
            signal: overallSignal,
            indicators: signals,
            confluenceScore: confluenceScore,
            priceAtSignal: data[index].close
        )
    }

    static func generateOptionsRecommendations(symbol: String, data: [PriceData]) -> [OptionsRecommendation] {
        guard data.count > 30 else { return [] }

        let closes = data.map { $0.closeDouble }
        let currentPrice = closes.last ?? 0
        let vol = BlackScholesModel.historicalVolatility(closes: closes)

        var recommendations: [OptionsRecommendation] = []

        // Covered Call recommendation
        let callStrike = currentPrice * 1.05 // 5% OTM
        let callPremium = BlackScholesModel.price(
            spotPrice: currentPrice,
            strikePrice: callStrike,
            timeToExpiry: 30.0 / 365.0,
            volatility: vol,
            isCall: true
        )

        recommendations.append(OptionsRecommendation(
            optionType: .coveredCall,
            strikePrice: Decimal(callStrike),
            suggestedDTE: 30,
            estimatedPremium: Decimal(callPremium),
            rationale: "5% OTM covered call with \(String(format: "%.1f%%", vol * 100)) implied volatility"
        ))

        // Another covered call
        let callStrike2 = currentPrice * 1.03
        let callPremium2 = BlackScholesModel.price(
            spotPrice: currentPrice,
            strikePrice: callStrike2,
            timeToExpiry: 45.0 / 365.0,
            volatility: vol,
            isCall: true
        )

        recommendations.append(OptionsRecommendation(
            optionType: .coveredCall,
            strikePrice: Decimal(callStrike2),
            suggestedDTE: 45,
            estimatedPremium: Decimal(callPremium2),
            rationale: "3% OTM covered call, higher premium with longer expiration"
        ))

        // Protective Put recommendation
        let putStrike = currentPrice * 0.95
        let putPremium = BlackScholesModel.price(
            spotPrice: currentPrice,
            strikePrice: putStrike,
            timeToExpiry: 30.0 / 365.0,
            volatility: vol,
            isCall: false
        )

        recommendations.append(OptionsRecommendation(
            optionType: .protectivePut,
            strikePrice: Decimal(putStrike),
            suggestedDTE: 30,
            estimatedPremium: Decimal(putPremium),
            rationale: "5% OTM protective put for downside protection"
        ))

        // Another protective put
        let putStrike2 = currentPrice * 0.97
        let putPremium2 = BlackScholesModel.price(
            spotPrice: currentPrice,
            strikePrice: putStrike2,
            timeToExpiry: 45.0 / 365.0,
            volatility: vol,
            isCall: false
        )

        recommendations.append(OptionsRecommendation(
            optionType: .protectivePut,
            strikePrice: Decimal(putStrike2),
            suggestedDTE: 45,
            estimatedPremium: Decimal(putPremium2),
            rationale: "3% OTM protective put, tighter protection"
        ))

        return recommendations
    }
}
