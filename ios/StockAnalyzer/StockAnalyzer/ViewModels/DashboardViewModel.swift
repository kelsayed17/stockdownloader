import Foundation
import SwiftUI

@MainActor
class DashboardViewModel: ObservableObject {
    @Published var watchlistQuotes: [StockQuote] = []
    @Published var isLoading = false
    @Published var errorMessage: String?

    private let defaultSymbols = ["SPY", "QQQ", "AAPL", "MSFT", "TSLA", "AMZN", "GOOGL", "NVDA"]

    func loadDashboard() async {
        isLoading = true
        errorMessage = nil

        var quotes: [StockQuote] = []

        for symbol in defaultSymbols {
            do {
                let quote = try await YahooFinanceService.shared.fetchQuote(symbol: symbol)
                quotes.append(quote)
            } catch {
                // Skip failed quotes
            }
        }

        watchlistQuotes = quotes
        isLoading = false
    }

    func refreshQuote(symbol: String) async {
        do {
            let quote = try await YahooFinanceService.shared.fetchQuote(symbol: symbol)
            if let index = watchlistQuotes.firstIndex(where: { $0.symbol == symbol }) {
                watchlistQuotes[index] = quote
            } else {
                watchlistQuotes.append(quote)
            }
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
