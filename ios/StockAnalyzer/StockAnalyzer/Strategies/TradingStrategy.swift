import Foundation

enum TradeAction {
    case buy
    case sell
    case hold
}

protocol TradingStrategy {
    var name: String { get }
    var description: String { get }
    var warmupPeriod: Int { get }

    func evaluate(data: [PriceData], index: Int, indicators: StrategyIndicators) -> TradeAction
}

struct StrategyIndicators {
    let closes: [Double]
    let highs: [Double]
    let lows: [Double]
    let volumes: [Double]
    let sma20: [Double?]
    let sma50: [Double?]
    let sma200: [Double?]
    let ema12: [Double?]
    let ema26: [Double?]
    let rsi14: [Double?]
    let macd: TechnicalIndicators.MACDResult
    let bollingerBands: TechnicalIndicators.BollingerBands
    let adx: TechnicalIndicators.ADXResult
    let atr: [Double?]
    let obv: [Double]
    let stochastic: TechnicalIndicators.StochasticResult

    static func calculate(from data: [PriceData]) -> StrategyIndicators {
        let closes = data.map { $0.closeDouble }
        let highs = data.map { $0.highDouble }
        let lows = data.map { $0.lowDouble }
        let volumes = data.map { $0.volumeDouble }

        return StrategyIndicators(
            closes: closes,
            highs: highs,
            lows: lows,
            volumes: volumes,
            sma20: TechnicalIndicators.sma(closes, period: 20),
            sma50: TechnicalIndicators.sma(closes, period: 50),
            sma200: TechnicalIndicators.sma(closes, period: 200),
            ema12: TechnicalIndicators.ema(closes, period: 12),
            ema26: TechnicalIndicators.ema(closes, period: 26),
            rsi14: TechnicalIndicators.rsi(closes, period: 14),
            macd: TechnicalIndicators.macd(closes),
            bollingerBands: TechnicalIndicators.bollingerBands(closes),
            adx: TechnicalIndicators.adx(highs: highs, lows: lows, closes: closes),
            atr: TechnicalIndicators.atr(highs: highs, lows: lows, closes: closes),
            obv: TechnicalIndicators.obv(closes: closes, volumes: volumes),
            stochastic: TechnicalIndicators.stochastic(highs: highs, lows: lows, closes: closes)
        )
    }
}
