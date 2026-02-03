import Foundation

struct BlackScholesModel {
    /// Calculate the theoretical price of a European option using Black-Scholes
    /// - Parameters:
    ///   - spotPrice: Current price of the underlying
    ///   - strikePrice: Option strike price
    ///   - timeToExpiry: Time to expiration in years
    ///   - riskFreeRate: Annual risk-free interest rate (e.g. 0.05 for 5%)
    ///   - volatility: Annual volatility (e.g. 0.20 for 20%)
    ///   - isCall: true for call, false for put
    /// - Returns: Theoretical option price
    static func price(
        spotPrice: Double,
        strikePrice: Double,
        timeToExpiry: Double,
        riskFreeRate: Double = 0.05,
        volatility: Double,
        isCall: Bool
    ) -> Double {
        guard timeToExpiry > 0, volatility > 0, spotPrice > 0, strikePrice > 0 else {
            // At expiration, return intrinsic value
            if isCall {
                return max(spotPrice - strikePrice, 0)
            } else {
                return max(strikePrice - spotPrice, 0)
            }
        }

        let d1 = (log(spotPrice / strikePrice) + (riskFreeRate + 0.5 * volatility * volatility) * timeToExpiry) /
                  (volatility * sqrt(timeToExpiry))
        let d2 = d1 - volatility * sqrt(timeToExpiry)

        if isCall {
            return spotPrice * cumulativeNormalDistribution(d1) -
                   strikePrice * exp(-riskFreeRate * timeToExpiry) * cumulativeNormalDistribution(d2)
        } else {
            return strikePrice * exp(-riskFreeRate * timeToExpiry) * cumulativeNormalDistribution(-d2) -
                   spotPrice * cumulativeNormalDistribution(-d1)
        }
    }

    /// Calculate delta
    static func delta(
        spotPrice: Double,
        strikePrice: Double,
        timeToExpiry: Double,
        riskFreeRate: Double = 0.05,
        volatility: Double,
        isCall: Bool
    ) -> Double {
        guard timeToExpiry > 0, volatility > 0 else { return isCall ? 1.0 : -1.0 }

        let d1 = (log(spotPrice / strikePrice) + (riskFreeRate + 0.5 * volatility * volatility) * timeToExpiry) /
                  (volatility * sqrt(timeToExpiry))

        return isCall ? cumulativeNormalDistribution(d1) : cumulativeNormalDistribution(d1) - 1.0
    }

    /// Calculate theta (daily)
    static func theta(
        spotPrice: Double,
        strikePrice: Double,
        timeToExpiry: Double,
        riskFreeRate: Double = 0.05,
        volatility: Double,
        isCall: Bool
    ) -> Double {
        guard timeToExpiry > 0, volatility > 0 else { return 0 }

        let d1 = (log(spotPrice / strikePrice) + (riskFreeRate + 0.5 * volatility * volatility) * timeToExpiry) /
                  (volatility * sqrt(timeToExpiry))
        let d2 = d1 - volatility * sqrt(timeToExpiry)

        let normalPDF = exp(-d1 * d1 / 2) / sqrt(2 * .pi)

        let term1 = -(spotPrice * normalPDF * volatility) / (2 * sqrt(timeToExpiry))

        if isCall {
            let term2 = riskFreeRate * strikePrice * exp(-riskFreeRate * timeToExpiry) * cumulativeNormalDistribution(d2)
            return (term1 - term2) / 365.0
        } else {
            let term2 = riskFreeRate * strikePrice * exp(-riskFreeRate * timeToExpiry) * cumulativeNormalDistribution(-d2)
            return (term1 + term2) / 365.0
        }
    }

    /// Historical volatility from close prices
    static func historicalVolatility(closes: [Double], period: Int = 20) -> Double {
        guard closes.count > period else { return 0.2 } // default 20%

        let recentCloses = Array(closes.suffix(period + 1))
        var logReturns: [Double] = []

        for i in 1..<recentCloses.count {
            if recentCloses[i - 1] > 0 {
                logReturns.append(log(recentCloses[i] / recentCloses[i - 1]))
            }
        }

        guard !logReturns.isEmpty else { return 0.2 }

        let mean = logReturns.reduce(0, +) / Double(logReturns.count)
        let variance = logReturns.map { ($0 - mean) * ($0 - mean) }.reduce(0, +) / Double(logReturns.count - 1)

        return sqrt(variance * 252) // Annualize
    }

    /// Cumulative standard normal distribution approximation
    private static func cumulativeNormalDistribution(_ x: Double) -> Double {
        let a1 = 0.254829592
        let a2 = -0.284496736
        let a3 = 1.421413741
        let a4 = -1.453152027
        let a5 = 1.061405429
        let p = 0.3275911

        let sign: Double = x < 0 ? -1 : 1
        let absX = abs(x) / sqrt(2.0)

        let t = 1.0 / (1.0 + p * absX)
        let y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * exp(-absX * absX)

        return 0.5 * (1.0 + sign * y)
    }
}
