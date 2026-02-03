import SwiftUI

struct BacktestResultsView: View {
    @ObservedObject var viewModel: BacktestViewModel

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Tab Selector
                Picker("Type", selection: $viewModel.selectedTab) {
                    ForEach(BacktestViewModel.BacktestTab.allCases, id: \.self) { tab in
                        Text(tab.rawValue).tag(tab)
                    }
                }
                .pickerStyle(.segmented)
                .padding(.horizontal)

                switch viewModel.selectedTab {
                case .equity:
                    equityResultsSection
                case .options:
                    optionsResultsSection
                }
            }
            .padding()
        }
    }

    // MARK: - Equity Results

    private var equityResultsSection: some View {
        VStack(spacing: 16) {
            // Summary Cards
            if let best = viewModel.bestEquityStrategy {
                summarySection(best: best)
            }

            // Equity Curve Chart
            if !viewModel.equityResults.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Equity Curves")
                        .font(.headline)
                    EquityCurveView(
                        results: viewModel.equityResults,
                        buyAndHold: viewModel.buyAndHoldResult
                    )
                }
                .padding()
                .background(.ultraThinMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }

            // Buy & Hold Benchmark
            if let bh = viewModel.buyAndHoldResult {
                benchmarkSection(bh)
            }

            // Strategy Comparison Table
            strategyComparisonSection

            // Individual Strategy Details
            ForEach(viewModel.equityResults) { result in
                StrategyDetailCard(result: result)
            }
        }
    }

    private func summarySection(best: BacktestResult) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: "trophy.fill")
                    .foregroundStyle(.yellow)
                Text("Best Strategy")
                    .font(.headline)
            }

            Text(best.strategyName)
                .font(.title3)
                .fontWeight(.bold)

            HStack {
                StatCard(
                    title: "Return",
                    value: best.totalReturn.formattedPercent,
                    color: Color.forValue(best.totalReturn),
                    icon: "chart.line.uptrend.xyaxis"
                )
                StatCard(
                    title: "Win Rate",
                    value: best.winRate.formattedPercent,
                    color: best.winRate > 50 ? .green : .red,
                    icon: "target"
                )
                StatCard(
                    title: "Sharpe",
                    value: best.sharpeRatio.formattedDecimal,
                    color: best.sharpeRatio > 1 ? .green : .orange,
                    icon: "chart.bar"
                )
            }
        }
        .padding()
        .background(Color.yellow.opacity(0.1))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func benchmarkSection(_ bh: BacktestResult) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: "flag.checkered")
                Text("Buy & Hold Benchmark")
                    .font(.headline)
            }

            HStack {
                StatCard(
                    title: "Return",
                    value: bh.totalReturn.formattedPercent,
                    color: Color.forValue(bh.totalReturn)
                )
                StatCard(
                    title: "Final Capital",
                    value: bh.finalCapital.formattedCurrency
                )
                StatCard(
                    title: "Max Drawdown",
                    value: bh.maxDrawdown.formattedPercent,
                    color: .red
                )
            }
        }
        .padding()
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private var strategyComparisonSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Strategy Comparison")
                .font(.headline)

            VStack(spacing: 0) {
                // Header
                HStack {
                    Text("Strategy")
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Text("Return")
                        .frame(width: 70, alignment: .trailing)
                    Text("Win %")
                        .frame(width: 55, alignment: .trailing)
                    Text("Trades")
                        .frame(width: 50, alignment: .trailing)
                }
                .font(.caption)
                .fontWeight(.semibold)
                .foregroundStyle(.secondary)
                .padding(.vertical, 8)
                .padding(.horizontal, 12)

                Divider()

                ForEach(viewModel.equityResults) { result in
                    HStack {
                        Text(result.strategyName)
                            .font(.caption)
                            .lineLimit(1)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        Text(result.totalReturn.formattedPercent)
                            .font(.caption)
                            .monospacedDigit()
                            .foregroundStyle(Color.forValue(result.totalReturn))
                            .frame(width: 70, alignment: .trailing)
                        Text(result.winRate.formattedPercent)
                            .font(.caption)
                            .monospacedDigit()
                            .frame(width: 55, alignment: .trailing)
                        Text("\(result.totalTrades)")
                            .font(.caption)
                            .monospacedDigit()
                            .frame(width: 50, alignment: .trailing)
                    }
                    .padding(.vertical, 6)
                    .padding(.horizontal, 12)

                    if result.id != viewModel.equityResults.last?.id {
                        Divider()
                    }
                }
            }
            .background(.ultraThinMaterial)
            .clipShape(RoundedRectangle(cornerRadius: 10))
        }
    }

    // MARK: - Options Results

    private var optionsResultsSection: some View {
        VStack(spacing: 16) {
            if let best = viewModel.bestOptionsStrategy {
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Image(systemName: "trophy.fill")
                            .foregroundStyle(.yellow)
                        Text("Best Options Strategy")
                            .font(.headline)
                    }

                    Text(best.strategyName)
                        .font(.title3)
                        .fontWeight(.bold)

                    HStack {
                        StatCard(
                            title: "Return",
                            value: best.totalReturn.formattedPercent,
                            color: Color.forValue(best.totalReturn)
                        )
                        StatCard(
                            title: "Win Rate",
                            value: best.winRate.formattedPercent,
                            color: best.winRate > 50 ? .green : .red
                        )
                        StatCard(
                            title: "Net Premium",
                            value: best.netPremium.formattedCurrency,
                            color: Color.forValue(best.netPremium)
                        )
                    }
                }
                .padding()
                .background(Color.yellow.opacity(0.1))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }

            // Options comparison
            VStack(alignment: .leading, spacing: 8) {
                Text("Options Strategy Comparison")
                    .font(.headline)

                VStack(spacing: 0) {
                    HStack {
                        Text("Strategy")
                            .frame(maxWidth: .infinity, alignment: .leading)
                        Text("Return")
                            .frame(width: 70, alignment: .trailing)
                        Text("Win %")
                            .frame(width: 55, alignment: .trailing)
                        Text("Avg Days")
                            .frame(width: 55, alignment: .trailing)
                    }
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundStyle(.secondary)
                    .padding(.vertical, 8)
                    .padding(.horizontal, 12)

                    Divider()

                    ForEach(viewModel.optionsResults) { result in
                        HStack {
                            Text(result.strategyName)
                                .font(.caption2)
                                .lineLimit(1)
                                .frame(maxWidth: .infinity, alignment: .leading)
                            Text(result.totalReturn.formattedPercent)
                                .font(.caption)
                                .monospacedDigit()
                                .foregroundStyle(Color.forValue(result.totalReturn))
                                .frame(width: 70, alignment: .trailing)
                            Text(result.winRate.formattedPercent)
                                .font(.caption)
                                .monospacedDigit()
                                .frame(width: 55, alignment: .trailing)
                            Text(String(format: "%.0f", result.averageDaysHeld))
                                .font(.caption)
                                .monospacedDigit()
                                .frame(width: 55, alignment: .trailing)
                        }
                        .padding(.vertical, 6)
                        .padding(.horizontal, 12)

                        if result.id != viewModel.optionsResults.last?.id {
                            Divider()
                        }
                    }
                }
                .background(.ultraThinMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 10))
            }

            // Individual options results
            ForEach(viewModel.optionsResults) { result in
                OptionsStrategyDetailCard(result: result)
            }
        }
    }
}

// MARK: - Strategy Detail Card

struct StrategyDetailCard: View {
    let result: BacktestResult
    @State private var isExpanded = false

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Header
            Button {
                withAnimation {
                    isExpanded.toggle()
                }
            } label: {
                HStack {
                    Text(result.strategyName)
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundStyle(.primary)
                    Spacer()
                    Text(result.totalReturn.formattedPercent)
                        .font(.subheadline)
                        .fontWeight(.bold)
                        .foregroundStyle(Color.forValue(result.totalReturn))
                    Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            if isExpanded {
                Divider()

                // Metrics Grid
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                    metricItem("Final Capital", result.finalCapital.formattedCurrency)
                    metricItem("Total Trades", "\(result.totalTrades)")
                    metricItem("Win Rate", result.winRate.formattedPercent)
                    metricItem("Profit Factor", result.profitFactor.formattedNumber)
                    metricItem("Sharpe Ratio", result.sharpeRatio.formattedDecimal)
                    metricItem("Max Drawdown", result.maxDrawdown.formattedPercent)
                    metricItem("Avg Win", result.averageWin.formattedCurrency)
                    metricItem("Avg Loss", result.averageLoss.formattedCurrency)
                    metricItem("Total P/L", (result.finalCapital - result.initialCapital).formattedCurrency)
                }

                // Trade List
                if !result.trades.isEmpty {
                    Divider()
                    Text("Trades (\(result.trades.count))")
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundStyle(.secondary)

                    ForEach(result.trades.suffix(10)) { trade in
                        TradeRowView(trade: trade)
                        if trade.id != result.trades.suffix(10).last?.id {
                            Divider()
                        }
                    }

                    if result.trades.count > 10 {
                        Text("... and \(result.trades.count - 10) more trades")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
            }
        }
        .padding()
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func metricItem(_ title: String, _ value: String) -> some View {
        VStack(spacing: 2) {
            Text(title)
                .font(.caption2)
                .foregroundStyle(.secondary)
            Text(value)
                .font(.caption)
                .fontWeight(.medium)
                .monospacedDigit()
        }
    }
}

struct OptionsStrategyDetailCard: View {
    let result: OptionsBacktestResult
    @State private var isExpanded = false

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Button {
                withAnimation {
                    isExpanded.toggle()
                }
            } label: {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(result.strategyName)
                            .font(.caption)
                            .fontWeight(.semibold)
                            .foregroundStyle(.primary)
                        Text(result.optionType.rawValue)
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    Text(result.totalReturn.formattedPercent)
                        .font(.subheadline)
                        .fontWeight(.bold)
                        .foregroundStyle(Color.forValue(result.totalReturn))
                    Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            if isExpanded {
                Divider()

                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                    VStack(spacing: 2) {
                        Text("Final Capital")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                        Text(result.finalCapital.formattedCurrency)
                            .font(.caption)
                            .fontWeight(.medium)
                    }
                    VStack(spacing: 2) {
                        Text("Win Rate")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                        Text(result.winRate.formattedPercent)
                            .font(.caption)
                            .fontWeight(.medium)
                    }
                    VStack(spacing: 2) {
                        Text("Total Trades")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                        Text("\(result.totalTrades)")
                            .font(.caption)
                            .fontWeight(.medium)
                    }
                    VStack(spacing: 2) {
                        Text("Net Premium")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                        Text(result.netPremium.formattedCurrency)
                            .font(.caption)
                            .fontWeight(.medium)
                            .foregroundStyle(Color.forValue(result.netPremium))
                    }
                    VStack(spacing: 2) {
                        Text("Avg Days Held")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                        Text(String(format: "%.0f", result.averageDaysHeld))
                            .font(.caption)
                            .fontWeight(.medium)
                    }
                }
            }
        }
        .padding()
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

#Preview {
    BacktestResultsView(viewModel: BacktestViewModel())
}
