import Foundation
import SwiftUI

@MainActor
class WatchlistViewModel: ObservableObject {
    @Published var items: [WatchlistItem] = []
    @Published var quotes: [String: StockQuote] = [:]
    @Published var isLoading = false
    @Published var newSymbol = ""

    private let storageKey = "watchlist_items"

    init() {
        loadFromStorage()
    }

    func addSymbol(_ symbol: String) {
        let trimmed = symbol.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        guard !trimmed.isEmpty,
              !items.contains(where: { $0.symbol == trimmed }) else { return }

        let item = WatchlistItem(symbol: trimmed)
        items.append(item)
        saveToStorage()

        Task {
            await refreshQuote(symbol: trimmed)
        }
    }

    func removeItem(at offsets: IndexSet) {
        let symbolsToRemove = offsets.map { items[$0].symbol }
        items.remove(atOffsets: offsets)
        for symbol in symbolsToRemove {
            quotes.removeValue(forKey: symbol)
        }
        saveToStorage()
    }

    func refreshAll() async {
        isLoading = true
        for item in items {
            await refreshQuote(symbol: item.symbol)
        }
        isLoading = false
    }

    func refreshQuote(symbol: String) async {
        do {
            let quote = try await YahooFinanceService.shared.fetchQuote(symbol: symbol)
            quotes[symbol] = quote

            if let index = items.firstIndex(where: { $0.symbol == symbol }) {
                items[index].lastPrice = quote.price
                items[index].changePercent = quote.changePercent
            }
        } catch {
            // Silently handle quote refresh failure
        }
    }

    private func saveToStorage() {
        if let data = try? JSONEncoder().encode(items) {
            UserDefaults.standard.set(data, forKey: storageKey)
        }
    }

    private func loadFromStorage() {
        if let data = UserDefaults.standard.data(forKey: storageKey),
           let saved = try? JSONDecoder().decode([WatchlistItem].self, from: data) {
            items = saved
        }
    }
}
