import Foundation

enum SignalType: String, Codable {
    case strongBuy = "Strong Buy"
    case buy = "Buy"
    case hold = "Hold"
    case sell = "Sell"
    case strongSell = "Strong Sell"

    var color: String {
        switch self {
        case .strongBuy: return "green"
        case .buy: return "mint"
        case .hold: return "yellow"
        case .sell: return "orange"
        case .strongSell: return "red"
        }
    }
}

struct IndicatorSignal: Identifiable {
    let id = UUID()
    let name: String
    let value: Double
    let signal: SignalType
    let description: String
}

struct TradingAlert: Identifiable {
    let id = UUID()
    let symbol: String
    let date: String
    let signal: SignalType
    let indicators: [IndicatorSignal]
    let confluenceScore: Double
    let priceAtSignal: Decimal

    var buySignals: Int {
        indicators.filter { $0.signal == .buy || $0.signal == .strongBuy }.count
    }

    var sellSignals: Int {
        indicators.filter { $0.signal == .sell || $0.signal == .strongSell }.count
    }
}

struct OptionsRecommendation: Identifiable {
    let id = UUID()
    let optionType: OptionType
    let strikePrice: Decimal
    let suggestedDTE: Int
    let estimatedPremium: Decimal
    let rationale: String
}

struct WatchlistItem: Identifiable, Codable {
    let id: UUID
    let symbol: String
    let addedDate: Date
    var lastPrice: Decimal?
    var changePercent: Decimal?

    init(symbol: String) {
        self.id = UUID()
        self.symbol = symbol.uppercased()
        self.addedDate = Date()
        self.lastPrice = nil
        self.changePercent = nil
    }
}
