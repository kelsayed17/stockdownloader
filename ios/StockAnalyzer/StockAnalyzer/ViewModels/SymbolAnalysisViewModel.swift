import Foundation
import SwiftUI

@MainActor
class SymbolAnalysisViewModel: ObservableObject {
    @Published var symbol: String = ""
    @Published var priceData: [PriceData] = []
    @Published var quote: StockQuote?
    @Published var tradingAlert: TradingAlert?
    @Published var optionsRecommendations: [OptionsRecommendation] = []
    @Published var selectedRange: TimeRange = .oneYear
    @Published var isLoading = false
    @Published var errorMessage: String?

    // Technical indicator display data
    @Published var currentRSI: Double?
    @Published var currentMACD: Double?
    @Published var currentSignalLine: Double?
    @Published var currentSMA50: Double?
    @Published var currentSMA200: Double?
    @Published var currentADX: Double?
    @Published var currentBBUpper: Double?
    @Published var currentBBLower: Double?
    @Published var currentBBMiddle: Double?
    @Published var historicalVol: Double?

    func analyze(symbol: String) async {
        self.symbol = symbol.uppercased()
        isLoading = true
        errorMessage = nil

        do {
            // Fetch quote and historical data concurrently
            async let quoteTask = YahooFinanceService.shared.fetchQuote(symbol: symbol)
            async let dataTask = YahooFinanceService.shared.fetchHistoricalData(
                symbol: symbol,
                range: selectedRange
            )

            let (fetchedQuote, fetchedData) = try await (quoteTask, dataTask)

            self.quote = fetchedQuote
            self.priceData = fetchedData

            // Calculate current indicator values
            calculateCurrentIndicators()

            // Generate trading alert
            self.tradingAlert = SignalGenerator.generateAlert(symbol: symbol, data: fetchedData)

            // Generate options recommendations
            self.optionsRecommendations = SignalGenerator.generateOptionsRecommendations(
                symbol: symbol,
                data: fetchedData
            )

        } catch {
            self.errorMessage = error.localizedDescription
        }

        isLoading = false
    }

    func changeRange(_ range: TimeRange) async {
        selectedRange = range
        guard !symbol.isEmpty else { return }
        await analyze(symbol: symbol)
    }

    private func calculateCurrentIndicators() {
        guard !priceData.isEmpty else { return }

        let closes = priceData.map { $0.closeDouble }
        let highs = priceData.map { $0.highDouble }
        let lows = priceData.map { $0.lowDouble }
        let index = closes.count - 1

        let rsi = TechnicalIndicators.rsi(closes)
        currentRSI = rsi[index]

        let macd = TechnicalIndicators.macd(closes)
        currentMACD = macd.macdLine[index]
        currentSignalLine = macd.signalLine[index]

        let sma50 = TechnicalIndicators.sma(closes, period: 50)
        currentSMA50 = sma50[index]

        let sma200 = TechnicalIndicators.sma(closes, period: 200)
        currentSMA200 = sma200[index]

        let adx = TechnicalIndicators.adx(highs: highs, lows: lows, closes: closes)
        currentADX = adx.adx[index]

        let bb = TechnicalIndicators.bollingerBands(closes)
        currentBBUpper = bb.upper[index]
        currentBBLower = bb.lower[index]
        currentBBMiddle = bb.middle[index]

        historicalVol = BlackScholesModel.historicalVolatility(closes: closes) * 100
    }
}
