import SwiftUI

struct BacktestView: View {
    @State private var symbol = ""
    @State private var selectedRange: TimeRange = .fiveYears
    @State private var navigateToResults = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                // Header
                VStack(spacing: 8) {
                    Image(systemName: "clock.arrow.circlepath")
                        .font(.system(size: 48))
                        .foregroundStyle(.accentColor)

                    Text("Strategy Backtester")
                        .font(.title2)
                        .fontWeight(.bold)

                    Text("Test 9 equity and 6 options strategies against historical data")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                }
                .padding(.top, 20)

                // Symbol Input
                VStack(alignment: .leading, spacing: 8) {
                    Text("Symbol")
                        .font(.headline)

                    HStack {
                        Image(systemName: "magnifyingglass")
                            .foregroundStyle(.secondary)
                        TextField("Enter symbol (e.g. SPY)", text: $symbol)
                            .textInputAutocapitalization(.characters)
                            .autocorrectionDisabled()
                    }
                    .padding()
                    .background(.ultraThinMaterial)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                }

                // Time Range
                VStack(alignment: .leading, spacing: 8) {
                    Text("Data Range")
                        .font(.headline)

                    Picker("Range", selection: $selectedRange) {
                        ForEach(TimeRange.allCases) { range in
                            Text(range.displayName).tag(range)
                        }
                    }
                    .pickerStyle(.segmented)
                }

                // Strategy Overview
                VStack(alignment: .leading, spacing: 8) {
                    Text("Strategies to Test")
                        .font(.headline)

                    VStack(alignment: .leading, spacing: 4) {
                        strategyItem("SMA Crossover (50/200)", icon: "chart.line.uptrend.xyaxis")
                        strategyItem("SMA Crossover (20/50)", icon: "chart.line.uptrend.xyaxis")
                        strategyItem("RSI (30/70)", icon: "waveform.path.ecg")
                        strategyItem("RSI (25/75)", icon: "waveform.path.ecg")
                        strategyItem("MACD (12/26/9)", icon: "chart.bar")
                        strategyItem("Bollinger Band + RSI", icon: "chart.xyaxis.line")
                        strategyItem("Momentum Confluence", icon: "arrow.triangle.merge")
                        strategyItem("Breakout Strategy", icon: "arrow.up.right.circle")
                        strategyItem("Multi-Indicator", icon: "squares.leadinghalf.filled")
                    }
                    .padding()
                    .background(.ultraThinMaterial)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                }

                Spacer()

                // Run Button
                NavigationLink {
                    BacktestRunView(symbol: symbol.uppercased(), selectedRange: selectedRange)
                } label: {
                    HStack {
                        Image(systemName: "play.fill")
                        Text("Run Backtest")
                    }
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(symbol.isEmpty ? Color.gray : Color.accentColor)
                    .foregroundStyle(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .disabled(symbol.isEmpty)
            }
            .padding()
            .navigationTitle("Backtest")
        }
    }

    private func strategyItem(_ name: String, icon: String) -> some View {
        HStack(spacing: 8) {
            Image(systemName: icon)
                .font(.caption)
                .foregroundStyle(.accentColor)
                .frame(width: 20)
            Text(name)
                .font(.subheadline)
            Spacer()
            Image(systemName: "checkmark.circle.fill")
                .font(.caption)
                .foregroundStyle(.green)
        }
    }
}

struct BacktestRunView: View {
    let symbol: String
    var selectedRange: TimeRange = .fiveYears

    @StateObject private var viewModel = BacktestViewModel()

    var body: some View {
        Group {
            if viewModel.isRunning {
                runningView
            } else if !viewModel.equityResults.isEmpty {
                BacktestResultsView(viewModel: viewModel)
            } else if let error = viewModel.errorMessage {
                errorView(error)
            } else {
                Color.clear
            }
        }
        .navigationTitle("\(symbol) Backtest")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            viewModel.selectedRange = selectedRange
            await viewModel.runBacktest(symbol: symbol)
        }
    }

    private var runningView: some View {
        VStack(spacing: 20) {
            ProgressView(value: viewModel.progress) {
                Text("Running backtests...")
                    .font(.headline)
            } currentValueLabel: {
                Text("\(Int(viewModel.progress * 100))%")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            .padding()

            Text("Testing \(StrategyRegistry.allEquityStrategies.count) equity + \(StrategyRegistry.allOptionsConfigs.count) options strategies")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding()
    }

    private func errorView(_ error: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 48))
                .foregroundStyle(.orange)
            Text("Backtest Failed")
                .font(.headline)
            Text(error)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            Button("Retry") {
                Task {
                    await viewModel.runBacktest(symbol: symbol)
                }
            }
            .buttonStyle(.borderedProminent)
        }
        .padding()
    }
}

#Preview {
    BacktestView()
}
