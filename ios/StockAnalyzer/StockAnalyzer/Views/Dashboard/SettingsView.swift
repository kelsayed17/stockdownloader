import SwiftUI

struct SettingsView: View {
    @AppStorage("defaultRange") private var defaultRange = TimeRange.oneYear.rawValue
    @AppStorage("defaultCapital") private var defaultCapital = 100_000.0
    @AppStorage("showVolume") private var showVolume = true
    @AppStorage("commissionPerTrade") private var commissionPerTrade = 0.0
    @AppStorage("optionsCommission") private var optionsCommission = 0.65

    var body: some View {
        NavigationStack {
            Form {
                Section("Analysis Defaults") {
                    Picker("Default Time Range", selection: $defaultRange) {
                        ForEach(TimeRange.allCases) { range in
                            Text(range.displayName).tag(range.rawValue)
                        }
                    }

                    Toggle("Show Volume Chart", isOn: $showVolume)
                }

                Section("Backtesting") {
                    HStack {
                        Text("Initial Capital")
                        Spacer()
                        TextField("Capital", value: $defaultCapital, format: .currency(code: "USD"))
                            .multilineTextAlignment(.trailing)
                            .keyboardType(.decimalPad)
                    }

                    HStack {
                        Text("Commission/Trade")
                        Spacer()
                        TextField("Commission", value: $commissionPerTrade, format: .currency(code: "USD"))
                            .multilineTextAlignment(.trailing)
                            .keyboardType(.decimalPad)
                    }

                    HStack {
                        Text("Options Commission")
                        Spacer()
                        TextField("Commission", value: $optionsCommission, format: .currency(code: "USD"))
                            .multilineTextAlignment(.trailing)
                            .keyboardType(.decimalPad)
                    }
                }

                Section("Strategies") {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Equity Strategies (\(StrategyRegistry.allEquityStrategies.count))")
                            .font(.subheadline)
                            .fontWeight(.medium)

                        ForEach(StrategyRegistry.allEquityStrategies, id: \.name) { strategy in
                            VStack(alignment: .leading, spacing: 2) {
                                Text(strategy.name)
                                    .font(.subheadline)
                                Text(strategy.description)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }

                    VStack(alignment: .leading, spacing: 12) {
                        Text("Options Strategies (\(StrategyRegistry.allOptionsConfigs.count))")
                            .font(.subheadline)
                            .fontWeight(.medium)

                        ForEach(StrategyRegistry.allOptionsConfigs) { config in
                            VStack(alignment: .leading, spacing: 2) {
                                Text(config.name)
                                    .font(.subheadline)
                                Text("\(config.optionType.rawValue) | \(config.dte) DTE | \(Int(config.otmPercent * 100))% OTM")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }

                Section("Technical Indicators") {
                    let indicators = [
                        "SMA (Simple Moving Average)",
                        "EMA (Exponential Moving Average)",
                        "RSI (Relative Strength Index)",
                        "MACD (Moving Average Convergence Divergence)",
                        "Bollinger Bands",
                        "ADX (Average Directional Index)",
                        "ATR (Average True Range)",
                        "Stochastic Oscillator",
                        "Williams %R",
                        "OBV (On-Balance Volume)",
                        "VWAP (Volume-Weighted Avg Price)",
                        "MFI (Money Flow Index)",
                        "CCI (Commodity Channel Index)",
                        "Parabolic SAR",
                        "Ichimoku Cloud",
                        "Fibonacci Retracement",
                        "ROC (Rate of Change)",
                        "Support/Resistance Detection",
                    ]

                    ForEach(indicators, id: \.self) { indicator in
                        HStack {
                            Image(systemName: "checkmark.circle.fill")
                                .font(.caption)
                                .foregroundStyle(.green)
                            Text(indicator)
                                .font(.subheadline)
                        }
                    }
                }

                Section("About") {
                    HStack {
                        Text("Version")
                        Spacer()
                        Text("1.0.0")
                            .foregroundStyle(.secondary)
                    }

                    HStack {
                        Text("Data Source")
                        Spacer()
                        Text("Yahoo Finance")
                            .foregroundStyle(.secondary)
                    }

                    HStack {
                        Text("Pricing Model")
                        Spacer()
                        Text("Black-Scholes")
                            .foregroundStyle(.secondary)
                    }
                }

                Section {
                    Text("This app is for educational and research purposes only. Not financial advice. Past performance does not guarantee future results.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .navigationTitle("Settings")
        }
    }
}

#Preview {
    SettingsView()
}
