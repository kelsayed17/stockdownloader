import Foundation

enum TradeDirection: String, Codable {
    case long = "LONG"
    case short = "SHORT"
}

enum TradeStatus: String, Codable {
    case open = "OPEN"
    case closed = "CLOSED"
}

struct Trade: Identifiable, Codable {
    let id: UUID
    let direction: TradeDirection
    let entryDate: String
    let entryPrice: Decimal
    let shares: Int
    var exitDate: String?
    var exitPrice: Decimal?
    var status: TradeStatus
    var profitLoss: Decimal?
    var returnPercent: Decimal?

    init(
        direction: TradeDirection,
        entryDate: String,
        entryPrice: Decimal,
        shares: Int
    ) {
        self.id = UUID()
        self.direction = direction
        self.entryDate = entryDate
        self.entryPrice = entryPrice
        self.shares = shares
        self.exitDate = nil
        self.exitPrice = nil
        self.status = .open
        self.profitLoss = nil
        self.returnPercent = nil
    }

    mutating func close(exitDate: String, exitPrice: Decimal) {
        self.exitDate = exitDate
        self.exitPrice = exitPrice
        self.status = .closed

        let cost = entryPrice * Decimal(shares)
        let revenue = exitPrice * Decimal(shares)

        switch direction {
        case .long:
            self.profitLoss = revenue - cost
        case .short:
            self.profitLoss = cost - revenue
        }

        if cost != 0 {
            self.returnPercent = (self.profitLoss! / cost) * 100
        }
    }
}

struct OptionsTrade: Identifiable, Codable {
    let id: UUID
    let optionType: OptionType
    let strikePrice: Decimal
    let expirationDate: String
    let entryDate: String
    let entryPremium: Decimal
    let contracts: Int
    var exitDate: String?
    var exitPremium: Decimal?
    var status: TradeStatus
    var profitLoss: Decimal?
    var daysHeld: Int?

    init(
        optionType: OptionType,
        strikePrice: Decimal,
        expirationDate: String,
        entryDate: String,
        entryPremium: Decimal,
        contracts: Int
    ) {
        self.id = UUID()
        self.optionType = optionType
        self.strikePrice = strikePrice
        self.expirationDate = expirationDate
        self.entryDate = entryDate
        self.entryPremium = entryPremium
        self.contracts = contracts
        self.exitDate = nil
        self.exitPremium = nil
        self.status = .open
        self.profitLoss = nil
        self.daysHeld = nil
    }

    mutating func close(exitDate: String, exitPremium: Decimal, daysHeld: Int) {
        self.exitDate = exitDate
        self.exitPremium = exitPremium
        self.status = .closed
        self.daysHeld = daysHeld

        let multiplier = Decimal(contracts * 100)
        switch optionType {
        case .coveredCall:
            self.profitLoss = (entryPremium - exitPremium) * multiplier
        case .protectivePut:
            self.profitLoss = (exitPremium - entryPremium) * multiplier
        }
    }
}

enum OptionType: String, Codable, CaseIterable {
    case coveredCall = "Covered Call"
    case protectivePut = "Protective Put"
}
