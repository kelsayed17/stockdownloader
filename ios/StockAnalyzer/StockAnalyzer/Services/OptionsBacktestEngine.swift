import Foundation

class OptionsBacktestEngine {
    let initialCapital: Decimal
    let commissionPerContract: Decimal

    init(initialCapital: Decimal = 100_000, commissionPerContract: Decimal = 0.65) {
        self.initialCapital = initialCapital
        self.commissionPerContract = commissionPerContract
    }

    func run(config: OptionsStrategyConfig, data: [PriceData]) -> OptionsBacktestResult {
        let closes = data.map { $0.closeDouble }
        let sma = TechnicalIndicators.sma(closes, period: config.maPeriod)

        guard data.count > config.maPeriod + config.dte else {
            return OptionsBacktestResult(
                strategyName: config.name,
                optionType: config.optionType,
                initialCapital: initialCapital,
                finalCapital: initialCapital,
                trades: []
            )
        }

        var capital = initialCapital
        var trades: [OptionsTrade] = []
        var currentTrade: OptionsTrade?
        var barsInTrade = 0

        for i in config.maPeriod..<data.count {
            guard let maValue = sma[i] else { continue }

            let price = data[i].close
            let priceDouble = data[i].closeDouble
            let vol = BlackScholesModel.historicalVolatility(closes: Array(closes.prefix(i + 1)))

            // Check if we should exit current trade
            if var trade = currentTrade {
                barsInTrade += 1

                if barsInTrade >= config.dte || i == data.count - 1 {
                    // Time to exit
                    let remainingDTE = max(config.dte - barsInTrade, 0)
                    let timeToExpiry = Double(remainingDTE) / 365.0
                    let isCall = config.optionType == .coveredCall

                    let exitPremium = BlackScholesModel.price(
                        spotPrice: priceDouble,
                        strikePrice: NSDecimalNumber(decimal: trade.strikePrice).doubleValue,
                        timeToExpiry: timeToExpiry,
                        volatility: vol,
                        isCall: isCall
                    )

                    let commission = commissionPerContract * Decimal(trade.contracts)
                    trade.close(exitDate: data[i].date, exitPremium: Decimal(exitPremium), daysHeld: barsInTrade)

                    if let pl = trade.profitLoss {
                        capital += pl - commission
                    }

                    trades.append(trade)
                    currentTrade = nil
                    barsInTrade = 0
                    continue
                }
            }

            // Check if we should enter a new trade
            if currentTrade == nil {
                let shouldEnter: Bool
                switch config.optionType {
                case .coveredCall:
                    shouldEnter = priceDouble > maValue
                case .protectivePut:
                    shouldEnter = priceDouble < maValue
                }

                if shouldEnter {
                    let isCall = config.optionType == .coveredCall
                    let strikeDouble: Double
                    if isCall {
                        strikeDouble = priceDouble * (1.0 + config.otmPercent)
                    } else {
                        strikeDouble = priceDouble * (1.0 - config.otmPercent)
                    }

                    let timeToExpiry = Double(config.dte) / 365.0
                    let premium = BlackScholesModel.price(
                        spotPrice: priceDouble,
                        strikePrice: strikeDouble,
                        timeToExpiry: timeToExpiry,
                        volatility: vol,
                        isCall: isCall
                    )

                    let contracts = max(1, Int(NSDecimalNumber(decimal: capital / (Decimal(premium) * 100 + commissionPerContract)).intValue / 10))
                    let cappedContracts = min(contracts, 10) // Cap at 10 contracts

                    let commission = commissionPerContract * Decimal(cappedContracts)

                    currentTrade = OptionsTrade(
                        optionType: config.optionType,
                        strikePrice: Decimal(strikeDouble),
                        expirationDate: data[min(i + config.dte, data.count - 1)].date,
                        entryDate: data[i].date,
                        entryPremium: Decimal(premium),
                        contracts: cappedContracts
                    )

                    capital -= commission
                    barsInTrade = 0
                }
            }
        }

        // Close remaining trade
        if var trade = currentTrade, let lastData = data.last {
            let vol = BlackScholesModel.historicalVolatility(closes: closes)
            let exitPremium = BlackScholesModel.price(
                spotPrice: lastData.closeDouble,
                strikePrice: NSDecimalNumber(decimal: trade.strikePrice).doubleValue,
                timeToExpiry: 0,
                volatility: vol,
                isCall: config.optionType == .coveredCall
            )

            trade.close(exitDate: lastData.date, exitPremium: Decimal(exitPremium), daysHeld: barsInTrade)
            if let pl = trade.profitLoss {
                capital += pl - commissionPerContract * Decimal(trade.contracts)
            }
            trades.append(trade)
        }

        return OptionsBacktestResult(
            strategyName: config.name,
            optionType: config.optionType,
            initialCapital: initialCapital,
            finalCapital: capital,
            trades: trades
        )
    }

    func runAllStrategies(data: [PriceData]) -> [OptionsBacktestResult] {
        return StrategyRegistry.allOptionsConfigs.map { config in
            run(config: config, data: data)
        }
    }
}
