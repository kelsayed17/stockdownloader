import Foundation
import SwiftUI

@MainActor
class BacktestViewModel: ObservableObject {
    @Published var symbol: String = ""
    @Published var selectedRange: TimeRange = .fiveYears
    @Published var equityResults: [BacktestResult] = []
    @Published var optionsResults: [OptionsBacktestResult] = []
    @Published var buyAndHoldResult: BacktestResult?
    @Published var isRunning = false
    @Published var progress: Double = 0
    @Published var errorMessage: String?
    @Published var selectedTab: BacktestTab = .equity

    enum BacktestTab: String, CaseIterable {
        case equity = "Equity"
        case options = "Options"
    }

    func runBacktest(symbol: String) async {
        self.symbol = symbol.uppercased()
        isRunning = true
        errorMessage = nil
        progress = 0

        do {
            let data = try await YahooFinanceService.shared.fetchHistoricalData(
                symbol: symbol,
                range: selectedRange
            )

            guard data.count > 200 else {
                errorMessage = "Not enough data for backtesting. Need at least 200 data points, got \(data.count)."
                isRunning = false
                return
            }

            // Run equity backtests
            let engine = BacktestEngine()
            let strategies = StrategyRegistry.allEquityStrategies
            var results: [BacktestResult] = []

            for (i, strategy) in strategies.enumerated() {
                let result = engine.run(strategy: strategy, data: data)
                results.append(result)
                progress = Double(i + 1) / Double(strategies.count + StrategyRegistry.allOptionsConfigs.count + 1)
            }

            self.equityResults = results.sorted { abs(NSDecimalNumber(decimal: $0.totalReturn).doubleValue) > abs(NSDecimalNumber(decimal: $1.totalReturn).doubleValue) }

            // Buy & Hold benchmark
            self.buyAndHoldResult = engine.buyAndHoldResult(data: data)
            progress = Double(strategies.count + 1) / Double(strategies.count + StrategyRegistry.allOptionsConfigs.count + 1)

            // Run options backtests
            let optionsEngine = OptionsBacktestEngine()
            var optResults: [OptionsBacktestResult] = []

            for (i, config) in StrategyRegistry.allOptionsConfigs.enumerated() {
                let result = optionsEngine.run(config: config, data: data)
                optResults.append(result)
                progress = Double(strategies.count + 1 + i + 1) / Double(strategies.count + StrategyRegistry.allOptionsConfigs.count + 1)
            }

            self.optionsResults = optResults

            progress = 1.0

        } catch {
            self.errorMessage = error.localizedDescription
        }

        isRunning = false
    }

    var bestEquityStrategy: BacktestResult? {
        equityResults.max { a, b in
            NSDecimalNumber(decimal: a.totalReturn).doubleValue < NSDecimalNumber(decimal: b.totalReturn).doubleValue
        }
    }

    var bestOptionsStrategy: OptionsBacktestResult? {
        optionsResults.max { a, b in
            NSDecimalNumber(decimal: a.totalReturn).doubleValue < NSDecimalNumber(decimal: b.totalReturn).doubleValue
        }
    }
}
