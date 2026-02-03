import Foundation
import SwiftUI

// MARK: - Decimal Formatting

extension Decimal {
    var formattedCurrency: String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.currencyCode = "USD"
        return formatter.string(from: NSDecimalNumber(decimal: self)) ?? "$0.00"
    }

    var formattedPercent: String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2
        return (formatter.string(from: NSDecimalNumber(decimal: self)) ?? "0.00") + "%"
    }

    var formattedNumber: String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2
        return formatter.string(from: NSDecimalNumber(decimal: self)) ?? "0.00"
    }

    var doubleValue: Double {
        NSDecimalNumber(decimal: self).doubleValue
    }
}

// MARK: - Color Helpers

extension Color {
    static var profit: Color { .green }
    static var loss: Color { .red }
    static var neutral: Color { .secondary }

    static func forValue(_ value: Decimal) -> Color {
        if value > 0 { return .profit }
        if value < 0 { return .loss }
        return .neutral
    }

    static func forSignal(_ signal: SignalType) -> Color {
        switch signal {
        case .strongBuy: return .green
        case .buy: return .mint
        case .hold: return .yellow
        case .sell: return .orange
        case .strongSell: return .red
        }
    }
}

// MARK: - Double Formatting

extension Double {
    var formattedPercent: String {
        String(format: "%.2f%%", self)
    }

    var formattedDecimal: String {
        String(format: "%.2f", self)
    }

    var formattedVolume: String {
        if self >= 1_000_000_000 {
            return String(format: "%.1fB", self / 1_000_000_000)
        } else if self >= 1_000_000 {
            return String(format: "%.1fM", self / 1_000_000)
        } else if self >= 1_000 {
            return String(format: "%.1fK", self / 1_000)
        }
        return String(format: "%.0f", self)
    }
}

// MARK: - Int64 Formatting

extension Int64 {
    var formattedVolume: String {
        Double(self).formattedVolume
    }
}
