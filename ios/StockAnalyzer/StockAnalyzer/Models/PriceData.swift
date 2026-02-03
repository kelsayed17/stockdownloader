import Foundation

struct PriceData: Identifiable, Codable, Equatable {
    var id: String { date }
    let date: String
    let open: Decimal
    let high: Decimal
    let low: Decimal
    let close: Decimal
    let adjClose: Decimal
    let volume: Int64

    var dateObject: Date? {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.date(from: date)
    }

    var closeDouble: Double {
        NSDecimalNumber(decimal: close).doubleValue
    }

    var openDouble: Double {
        NSDecimalNumber(decimal: open).doubleValue
    }

    var highDouble: Double {
        NSDecimalNumber(decimal: high).doubleValue
    }

    var lowDouble: Double {
        NSDecimalNumber(decimal: low).doubleValue
    }

    var volumeDouble: Double {
        Double(volume)
    }
}

enum TimeRange: String, CaseIterable, Identifiable {
    case oneMonth = "1mo"
    case threeMonths = "3mo"
    case sixMonths = "6mo"
    case oneYear = "1y"
    case twoYears = "2y"
    case fiveYears = "5y"
    case max = "max"

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .oneMonth: return "1M"
        case .threeMonths: return "3M"
        case .sixMonths: return "6M"
        case .oneYear: return "1Y"
        case .twoYears: return "2Y"
        case .fiveYears: return "5Y"
        case .max: return "Max"
        }
    }
}

enum DataInterval: String, CaseIterable, Identifiable {
    case daily = "1d"
    case weekly = "1wk"
    case monthly = "1mo"

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .daily: return "Daily"
        case .weekly: return "Weekly"
        case .monthly: return "Monthly"
        }
    }
}
