import Foundation

struct TechnicalIndicators {

    // MARK: - Simple Moving Average (SMA)

    static func sma(_ data: [Double], period: Int) -> [Double?] {
        guard period > 0, data.count >= period else {
            return Array(repeating: nil, count: data.count)
        }

        var result: [Double?] = Array(repeating: nil, count: data.count)
        var windowSum = data[0..<period].reduce(0, +)
        result[period - 1] = windowSum / Double(period)

        for i in period..<data.count {
            windowSum += data[i] - data[i - period]
            result[i] = windowSum / Double(period)
        }

        return result
    }

    // MARK: - Exponential Moving Average (EMA)

    static func ema(_ data: [Double], period: Int) -> [Double?] {
        guard period > 0, data.count >= period else {
            return Array(repeating: nil, count: data.count)
        }

        var result: [Double?] = Array(repeating: nil, count: data.count)
        let multiplier = 2.0 / Double(period + 1)

        let firstSMA = data[0..<period].reduce(0, +) / Double(period)
        result[period - 1] = firstSMA

        for i in period..<data.count {
            let prev = result[i - 1]!
            result[i] = (data[i] - prev) * multiplier + prev
        }

        return result
    }

    // MARK: - RSI (Relative Strength Index)

    static func rsi(_ data: [Double], period: Int = 14) -> [Double?] {
        guard data.count > period else {
            return Array(repeating: nil, count: data.count)
        }

        var result: [Double?] = Array(repeating: nil, count: data.count)
        var gains: [Double] = []
        var losses: [Double] = []

        for i in 1...period {
            let change = data[i] - data[i - 1]
            gains.append(max(change, 0))
            losses.append(max(-change, 0))
        }

        var avgGain = gains.reduce(0, +) / Double(period)
        var avgLoss = losses.reduce(0, +) / Double(period)

        if avgLoss == 0 {
            result[period] = 100
        } else {
            let rs = avgGain / avgLoss
            result[period] = 100 - (100 / (1 + rs))
        }

        for i in (period + 1)..<data.count {
            let change = data[i] - data[i - 1]
            let gain = max(change, 0)
            let loss = max(-change, 0)

            avgGain = (avgGain * Double(period - 1) + gain) / Double(period)
            avgLoss = (avgLoss * Double(period - 1) + loss) / Double(period)

            if avgLoss == 0 {
                result[i] = 100
            } else {
                let rs = avgGain / avgLoss
                result[i] = 100 - (100 / (1 + rs))
            }
        }

        return result
    }

    // MARK: - MACD

    struct MACDResult {
        let macdLine: [Double?]
        let signalLine: [Double?]
        let histogram: [Double?]
    }

    static func macd(
        _ data: [Double],
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9
    ) -> MACDResult {
        let fastEMA = ema(data, period: fastPeriod)
        let slowEMA = ema(data, period: slowPeriod)

        var macdLine: [Double?] = Array(repeating: nil, count: data.count)
        var macdValues: [Double] = []

        for i in 0..<data.count {
            if let fast = fastEMA[i], let slow = slowEMA[i] {
                let val = fast - slow
                macdLine[i] = val
                macdValues.append(val)
            }
        }

        let signalEMA = ema(macdValues, period: signalPeriod)

        var signalLine: [Double?] = Array(repeating: nil, count: data.count)
        var histogram: [Double?] = Array(repeating: nil, count: data.count)

        let macdStartIndex = slowPeriod - 1
        for (j, signalVal) in signalEMA.enumerated() {
            let i = macdStartIndex + j
            if i < data.count {
                signalLine[i] = signalVal
                if let m = macdLine[i], let s = signalVal {
                    histogram[i] = m - s
                }
            }
        }

        return MACDResult(macdLine: macdLine, signalLine: signalLine, histogram: histogram)
    }

    // MARK: - Bollinger Bands

    struct BollingerBands {
        let upper: [Double?]
        let middle: [Double?]
        let lower: [Double?]
        let bandwidth: [Double?]
    }

    static func bollingerBands(_ data: [Double], period: Int = 20, stdDevMultiplier: Double = 2.0) -> BollingerBands {
        let middle = sma(data, period: period)
        var upper: [Double?] = Array(repeating: nil, count: data.count)
        var lower: [Double?] = Array(repeating: nil, count: data.count)
        var bandwidth: [Double?] = Array(repeating: nil, count: data.count)

        for i in (period - 1)..<data.count {
            guard let mid = middle[i] else { continue }
            let slice = Array(data[(i - period + 1)...i])
            let mean = slice.reduce(0, +) / Double(slice.count)
            let variance = slice.map { ($0 - mean) * ($0 - mean) }.reduce(0, +) / Double(slice.count)
            let stdDev = sqrt(variance)

            upper[i] = mid + stdDevMultiplier * stdDev
            lower[i] = mid - stdDevMultiplier * stdDev
            bandwidth[i] = mid > 0 ? (4 * stdDevMultiplier * stdDev / mid) * 100 : 0
        }

        return BollingerBands(upper: upper, middle: middle, lower: lower, bandwidth: bandwidth)
    }

    // MARK: - ATR (Average True Range)

    static func atr(highs: [Double], lows: [Double], closes: [Double], period: Int = 14) -> [Double?] {
        guard highs.count == lows.count, lows.count == closes.count, highs.count > period else {
            return Array(repeating: nil, count: highs.count)
        }

        var trueRanges: [Double] = []
        trueRanges.append(highs[0] - lows[0])

        for i in 1..<highs.count {
            let hl = highs[i] - lows[i]
            let hc = abs(highs[i] - closes[i - 1])
            let lc = abs(lows[i] - closes[i - 1])
            trueRanges.append(max(hl, max(hc, lc)))
        }

        var result: [Double?] = Array(repeating: nil, count: highs.count)
        let firstATR = trueRanges[0..<period].reduce(0, +) / Double(period)
        result[period - 1] = firstATR

        for i in period..<highs.count {
            let prev = result[i - 1]!
            result[i] = (prev * Double(period - 1) + trueRanges[i]) / Double(period)
        }

        return result
    }

    // MARK: - ADX (Average Directional Index)

    struct ADXResult {
        let adx: [Double?]
        let plusDI: [Double?]
        let minusDI: [Double?]
    }

    static func adx(highs: [Double], lows: [Double], closes: [Double], period: Int = 14) -> ADXResult {
        let count = highs.count
        guard count > period * 2 else {
            return ADXResult(
                adx: Array(repeating: nil, count: count),
                plusDI: Array(repeating: nil, count: count),
                minusDI: Array(repeating: nil, count: count)
            )
        }

        var plusDM: [Double] = [0]
        var minusDM: [Double] = [0]
        var trueRanges: [Double] = [highs[0] - lows[0]]

        for i in 1..<count {
            let upMove = highs[i] - highs[i - 1]
            let downMove = lows[i - 1] - lows[i]

            plusDM.append(upMove > downMove && upMove > 0 ? upMove : 0)
            minusDM.append(downMove > upMove && downMove > 0 ? downMove : 0)

            let hl = highs[i] - lows[i]
            let hc = abs(highs[i] - closes[i - 1])
            let lc = abs(lows[i] - closes[i - 1])
            trueRanges.append(max(hl, max(hc, lc)))
        }

        let smoothedPlusDM = smoothWilder(plusDM, period: period)
        let smoothedMinusDM = smoothWilder(minusDM, period: period)
        let smoothedTR = smoothWilder(trueRanges, period: period)

        var plusDI: [Double?] = Array(repeating: nil, count: count)
        var minusDI: [Double?] = Array(repeating: nil, count: count)
        var dx: [Double?] = Array(repeating: nil, count: count)

        for i in (period - 1)..<count {
            guard let tr = smoothedTR[i], tr > 0,
                  let pDM = smoothedPlusDM[i],
                  let mDM = smoothedMinusDM[i] else { continue }

            let pDI = (pDM / tr) * 100
            let mDI = (mDM / tr) * 100
            plusDI[i] = pDI
            minusDI[i] = mDI

            let diSum = pDI + mDI
            dx[i] = diSum > 0 ? abs(pDI - mDI) / diSum * 100 : 0
        }

        var adxArr: [Double?] = Array(repeating: nil, count: count)
        let adxStart = period * 2 - 1
        if adxStart < count {
            var dxSum = 0.0
            var dxCount = 0
            for j in (period - 1)..<adxStart {
                if let d = dx[j] {
                    dxSum += d
                    dxCount += 1
                }
            }
            if dxCount > 0 {
                adxArr[adxStart] = dxSum / Double(dxCount)
            }

            for i in (adxStart + 1)..<count {
                if let prev = adxArr[i - 1], let d = dx[i] {
                    adxArr[i] = (prev * Double(period - 1) + d) / Double(period)
                }
            }
        }

        return ADXResult(adx: adxArr, plusDI: plusDI, minusDI: minusDI)
    }

    private static func smoothWilder(_ data: [Double], period: Int) -> [Double?] {
        var result: [Double?] = Array(repeating: nil, count: data.count)
        guard data.count >= period else { return result }

        result[period - 1] = data[0..<period].reduce(0, +)

        for i in period..<data.count {
            if let prev = result[i - 1] {
                result[i] = prev - (prev / Double(period)) + data[i]
            }
        }

        return result
    }

    // MARK: - Stochastic Oscillator

    struct StochasticResult {
        let k: [Double?]
        let d: [Double?]
    }

    static func stochastic(
        highs: [Double],
        lows: [Double],
        closes: [Double],
        kPeriod: Int = 14,
        dPeriod: Int = 3
    ) -> StochasticResult {
        let count = closes.count
        var kValues: [Double?] = Array(repeating: nil, count: count)

        for i in (kPeriod - 1)..<count {
            let highSlice = Array(highs[(i - kPeriod + 1)...i])
            let lowSlice = Array(lows[(i - kPeriod + 1)...i])
            let highestHigh = highSlice.max()!
            let lowestLow = lowSlice.min()!
            let range = highestHigh - lowestLow

            kValues[i] = range > 0 ? ((closes[i] - lowestLow) / range) * 100 : 50
        }

        let kNonNil = kValues.compactMap { $0 }
        let dSMA = sma(kNonNil, period: dPeriod)

        var dValues: [Double?] = Array(repeating: nil, count: count)
        let startIdx = kPeriod - 1
        for (j, val) in dSMA.enumerated() {
            let i = startIdx + j
            if i < count {
                dValues[i] = val
            }
        }

        return StochasticResult(k: kValues, d: dValues)
    }

    // MARK: - Williams %R

    static func williamsR(highs: [Double], lows: [Double], closes: [Double], period: Int = 14) -> [Double?] {
        let count = closes.count
        var result: [Double?] = Array(repeating: nil, count: count)

        for i in (period - 1)..<count {
            let highSlice = Array(highs[(i - period + 1)...i])
            let lowSlice = Array(lows[(i - period + 1)...i])
            let highestHigh = highSlice.max()!
            let lowestLow = lowSlice.min()!
            let range = highestHigh - lowestLow

            result[i] = range > 0 ? ((highestHigh - closes[i]) / range) * -100 : -50
        }

        return result
    }

    // MARK: - OBV (On-Balance Volume)

    static func obv(closes: [Double], volumes: [Double]) -> [Double] {
        guard !closes.isEmpty else { return [] }

        var result: [Double] = [volumes[0]]

        for i in 1..<closes.count {
            if closes[i] > closes[i - 1] {
                result.append(result[i - 1] + volumes[i])
            } else if closes[i] < closes[i - 1] {
                result.append(result[i - 1] - volumes[i])
            } else {
                result.append(result[i - 1])
            }
        }

        return result
    }

    // MARK: - VWAP (Volume-Weighted Average Price)

    static func vwap(highs: [Double], lows: [Double], closes: [Double], volumes: [Double]) -> [Double?] {
        let count = closes.count
        var result: [Double?] = Array(repeating: nil, count: count)
        var cumulativeTPV = 0.0
        var cumulativeVolume = 0.0

        for i in 0..<count {
            let typicalPrice = (highs[i] + lows[i] + closes[i]) / 3.0
            cumulativeTPV += typicalPrice * volumes[i]
            cumulativeVolume += volumes[i]
            result[i] = cumulativeVolume > 0 ? cumulativeTPV / cumulativeVolume : nil
        }

        return result
    }

    // MARK: - MFI (Money Flow Index)

    static func mfi(highs: [Double], lows: [Double], closes: [Double], volumes: [Double], period: Int = 14) -> [Double?] {
        let count = closes.count
        guard count > period else { return Array(repeating: nil, count: count) }

        var result: [Double?] = Array(repeating: nil, count: count)
        var typicalPrices: [Double] = []

        for i in 0..<count {
            typicalPrices.append((highs[i] + lows[i] + closes[i]) / 3.0)
        }

        for i in period..<count {
            var positiveFlow = 0.0
            var negativeFlow = 0.0

            for j in (i - period + 1)...i {
                let moneyFlow = typicalPrices[j] * volumes[j]
                if typicalPrices[j] > typicalPrices[j - 1] {
                    positiveFlow += moneyFlow
                } else if typicalPrices[j] < typicalPrices[j - 1] {
                    negativeFlow += moneyFlow
                }
            }

            if negativeFlow == 0 {
                result[i] = 100
            } else {
                let moneyRatio = positiveFlow / negativeFlow
                result[i] = 100 - (100 / (1 + moneyRatio))
            }
        }

        return result
    }

    // MARK: - CCI (Commodity Channel Index)

    static func cci(highs: [Double], lows: [Double], closes: [Double], period: Int = 20) -> [Double?] {
        let count = closes.count
        var result: [Double?] = Array(repeating: nil, count: count)

        var typicalPrices: [Double] = []
        for i in 0..<count {
            typicalPrices.append((highs[i] + lows[i] + closes[i]) / 3.0)
        }

        for i in (period - 1)..<count {
            let slice = Array(typicalPrices[(i - period + 1)...i])
            let mean = slice.reduce(0, +) / Double(period)
            let meanDev = slice.map { abs($0 - mean) }.reduce(0, +) / Double(period)

            result[i] = meanDev > 0 ? (typicalPrices[i] - mean) / (0.015 * meanDev) : 0
        }

        return result
    }

    // MARK: - Parabolic SAR

    static func parabolicSAR(highs: [Double], lows: [Double], afStart: Double = 0.02, afIncrement: Double = 0.02, afMax: Double = 0.2) -> [Double?] {
        let count = highs.count
        guard count > 1 else { return Array(repeating: nil, count: count) }

        var result: [Double?] = Array(repeating: nil, count: count)
        var isUpTrend = highs[1] > highs[0]
        var sar = isUpTrend ? lows[0] : highs[0]
        var ep = isUpTrend ? highs[1] : lows[1]
        var af = afStart

        result[1] = sar

        for i in 2..<count {
            let prevSAR = sar

            sar = prevSAR + af * (ep - prevSAR)

            if isUpTrend {
                sar = min(sar, lows[i - 1], lows[i - 2])
                if lows[i] < sar {
                    isUpTrend = false
                    sar = ep
                    ep = lows[i]
                    af = afStart
                } else {
                    if highs[i] > ep {
                        ep = highs[i]
                        af = min(af + afIncrement, afMax)
                    }
                }
            } else {
                sar = max(sar, highs[i - 1], highs[i - 2])
                if highs[i] > sar {
                    isUpTrend = true
                    sar = ep
                    ep = highs[i]
                    af = afStart
                } else {
                    if lows[i] < ep {
                        ep = lows[i]
                        af = min(af + afIncrement, afMax)
                    }
                }
            }

            result[i] = sar
        }

        return result
    }

    // MARK: - Ichimoku Cloud

    struct IchimokuResult {
        let tenkanSen: [Double?]    // Conversion Line (9)
        let kijunSen: [Double?]     // Base Line (26)
        let senkouSpanA: [Double?]  // Leading Span A
        let senkouSpanB: [Double?]  // Leading Span B
        let chikouSpan: [Double?]   // Lagging Span
    }

    static func ichimoku(highs: [Double], lows: [Double], closes: [Double]) -> IchimokuResult {
        let count = closes.count

        func midpoint(_ h: [Double], _ l: [Double], period: Int, at index: Int) -> Double? {
            guard index >= period - 1 else { return nil }
            let hSlice = Array(h[(index - period + 1)...index])
            let lSlice = Array(l[(index - period + 1)...index])
            return (hSlice.max()! + lSlice.min()!) / 2.0
        }

        var tenkan: [Double?] = Array(repeating: nil, count: count)
        var kijun: [Double?] = Array(repeating: nil, count: count)
        var spanA: [Double?] = Array(repeating: nil, count: count + 26)
        var spanB: [Double?] = Array(repeating: nil, count: count + 26)
        var chikou: [Double?] = Array(repeating: nil, count: count)

        for i in 0..<count {
            tenkan[i] = midpoint(highs, lows, period: 9, at: i)
            kijun[i] = midpoint(highs, lows, period: 26, at: i)

            if let t = tenkan[i], let k = kijun[i], (i + 26) < count + 26 {
                spanA[i + 26] = (t + k) / 2.0
            }

            if let mp = midpoint(highs, lows, period: 52, at: i), (i + 26) < count + 26 {
                spanB[i + 26] = mp
            }

            if i >= 26 {
                chikou[i - 26] = closes[i]
            }
        }

        return IchimokuResult(
            tenkanSen: tenkan,
            kijunSen: kijun,
            senkouSpanA: Array(spanA.prefix(count)),
            senkouSpanB: Array(spanB.prefix(count)),
            chikouSpan: chikou
        )
    }

    // MARK: - Fibonacci Retracement Levels

    struct FibonacciLevels {
        let high: Double
        let low: Double
        let level236: Double
        let level382: Double
        let level500: Double
        let level618: Double
        let level786: Double
    }

    static func fibonacciRetracement(highs: [Double], lows: [Double]) -> FibonacciLevels {
        let high = highs.max() ?? 0
        let low = lows.min() ?? 0
        let range = high - low

        return FibonacciLevels(
            high: high,
            low: low,
            level236: high - range * 0.236,
            level382: high - range * 0.382,
            level500: high - range * 0.500,
            level618: high - range * 0.618,
            level786: high - range * 0.786
        )
    }

    // MARK: - ROC (Rate of Change)

    static func roc(_ data: [Double], period: Int = 12) -> [Double?] {
        var result: [Double?] = Array(repeating: nil, count: data.count)

        for i in period..<data.count {
            let prev = data[i - period]
            result[i] = prev != 0 ? ((data[i] - prev) / prev) * 100 : 0
        }

        return result
    }

    // MARK: - Support and Resistance

    struct SupportResistance {
        let supportLevels: [Double]
        let resistanceLevels: [Double]
    }

    static func supportResistance(highs: [Double], lows: [Double], closes: [Double], lookback: Int = 20) -> SupportResistance {
        var supports: [Double] = []
        var resistances: [Double] = []

        let count = closes.count
        guard count > lookback * 2 else {
            return SupportResistance(supportLevels: [], resistanceLevels: [])
        }

        for i in lookback..<(count - lookback) {
            let leftHighs = Array(highs[(i - lookback)..<i])
            let rightHighs = Array(highs[(i + 1)...(min(i + lookback, count - 1))])

            if highs[i] >= (leftHighs.max() ?? 0) && highs[i] >= (rightHighs.max() ?? 0) {
                resistances.append(highs[i])
            }

            let leftLows = Array(lows[(i - lookback)..<i])
            let rightLows = Array(lows[(i + 1)...(min(i + lookback, count - 1))])

            if lows[i] <= (leftLows.min() ?? .infinity) && lows[i] <= (rightLows.min() ?? .infinity) {
                supports.append(lows[i])
            }
        }

        return SupportResistance(
            supportLevels: Array(Set(supports)).sorted().suffix(5),
            resistanceLevels: Array(Set(resistances)).sorted().suffix(5)
        )
    }
}
