import SwiftUI

struct SymbolDetailView: View {
    let symbol: String
    @StateObject private var viewModel = SymbolAnalysisViewModel()
    @State private var showSMA50 = false
    @State private var showSMA200 = false
    @State private var showBollingerBands = false
    @State private var selectedChartSection: ChartSection = .price

    enum ChartSection: String, CaseIterable {
        case price = "Price"
        case rsi = "RSI"
        case macd = "MACD"
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Quote Header
                if let quote = viewModel.quote {
                    quoteHeader(quote)
                }

                // Time Range Selector
                timeRangeSelector

                // Chart Section
                chartSection

                // Trading Signal
                if let alert = viewModel.tradingAlert {
                    tradingSignalSection(alert)
                }

                // Technical Indicators Summary
                indicatorsSummary

                // Options Recommendations
                if !viewModel.optionsRecommendations.isEmpty {
                    optionsSection
                }

                // Action Buttons
                actionButtons
            }
            .padding()
        }
        .navigationTitle(symbol)
        .navigationBarTitleDisplayMode(.large)
        .task {
            await viewModel.analyze(symbol: symbol)
        }
        .overlay {
            if viewModel.isLoading {
                ProgressView("Analyzing \(symbol)...")
                    .padding()
                    .background(.ultraThinMaterial)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
        .alert("Error", isPresented: .init(
            get: { viewModel.errorMessage != nil },
            set: { if !$0 { viewModel.errorMessage = nil } }
        )) {
            Button("OK") { viewModel.errorMessage = nil }
        } message: {
            Text(viewModel.errorMessage ?? "")
        }
    }

    private func quoteHeader(_ quote: StockQuote) -> some View {
        VStack(spacing: 8) {
            HStack(alignment: .bottom) {
                VStack(alignment: .leading) {
                    Text(quote.name)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    Text(quote.price.formattedCurrency)
                        .font(.system(size: 36, weight: .bold))
                }
                Spacer()
                VStack(alignment: .trailing) {
                    HStack(spacing: 4) {
                        Image(systemName: quote.change >= 0 ? "arrow.up.right" : "arrow.down.right")
                        Text(quote.change.formattedCurrency)
                    }
                    .font(.headline)
                    .foregroundStyle(Color.forValue(quote.change))

                    Text(quote.changePercent.formattedPercent)
                        .font(.subheadline)
                        .foregroundStyle(Color.forValue(quote.change))
                }
            }

            HStack {
                Text(quote.exchange)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Spacer()
                Text(quote.currency)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding()
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private var timeRangeSelector: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(TimeRange.allCases) { range in
                    Button {
                        Task {
                            await viewModel.changeRange(range)
                        }
                    } label: {
                        Text(range.displayName)
                            .font(.caption)
                            .fontWeight(viewModel.selectedRange == range ? .bold : .regular)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(viewModel.selectedRange == range ? Color.accentColor : Color.clear)
                            .foregroundStyle(viewModel.selectedRange == range ? .white : .primary)
                            .clipShape(Capsule())
                            .overlay(
                                Capsule()
                                    .stroke(Color.accentColor.opacity(0.3), lineWidth: 1)
                            )
                    }
                }
            }
            .padding(.horizontal, 4)
        }
    }

    private var chartSection: some View {
        VStack(spacing: 8) {
            // Chart type selector
            Picker("Chart", selection: $selectedChartSection) {
                ForEach(ChartSection.allCases, id: \.self) { section in
                    Text(section.rawValue).tag(section)
                }
            }
            .pickerStyle(.segmented)

            // Overlay toggles (only for price chart)
            if selectedChartSection == .price {
                HStack(spacing: 12) {
                    Toggle("SMA 50", isOn: $showSMA50)
                    Toggle("SMA 200", isOn: $showSMA200)
                    Toggle("BB", isOn: $showBollingerBands)
                }
                .font(.caption)
                .toggleStyle(.button)
                .buttonStyle(.bordered)
            }

            // Chart
            if !viewModel.priceData.isEmpty {
                switch selectedChartSection {
                case .price:
                    PriceChartView(
                        data: viewModel.priceData,
                        showSMA50: showSMA50,
                        showSMA200: showSMA200,
                        showBollingerBands: showBollingerBands
                    )
                case .rsi:
                    VStack(alignment: .leading) {
                        Text("RSI (14)")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        RSIChartView(data: viewModel.priceData)
                    }
                case .macd:
                    VStack(alignment: .leading) {
                        Text("MACD (12, 26, 9)")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        MACDChartView(data: viewModel.priceData)
                    }
                }
            }
        }
        .padding()
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func tradingSignalSection(_ alert: TradingAlert) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Trading Signal")
                    .font(.headline)
                Spacer()
                SignalBadge(signal: alert.signal)
            }

            HStack {
                StatCard(
                    title: "Confluence",
                    value: String(format: "%.1f%%", alert.confluenceScore * 100),
                    color: alert.confluenceScore > 0 ? .green : alert.confluenceScore < 0 ? .red : .secondary
                )

                StatCard(
                    title: "Buy Signals",
                    value: "\(alert.buySignals)",
                    color: .green,
                    icon: "arrow.up"
                )

                StatCard(
                    title: "Sell Signals",
                    value: "\(alert.sellSignals)",
                    color: .red,
                    icon: "arrow.down"
                )
            }

            // Individual indicators
            VStack(spacing: 0) {
                ForEach(alert.indicators) { indicator in
                    IndicatorRow(indicator: indicator)
                    if indicator.id != alert.indicators.last?.id {
                        Divider()
                    }
                }
            }
            .padding()
            .background(.ultraThinMaterial)
            .clipShape(RoundedRectangle(cornerRadius: 10))
        }
    }

    private var indicatorsSummary: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Technical Summary")
                .font(.headline)

            LazyVGrid(columns: [
                GridItem(.flexible()),
                GridItem(.flexible()),
            ], spacing: 8) {
                if let rsi = viewModel.currentRSI {
                    StatCard(title: "RSI (14)", value: rsi.formattedDecimal,
                             color: rsi < 30 ? .green : rsi > 70 ? .red : .primary)
                }

                if let adx = viewModel.currentADX {
                    StatCard(title: "ADX", value: adx.formattedDecimal,
                             color: adx > 25 ? .blue : .secondary)
                }

                if let sma50 = viewModel.currentSMA50 {
                    StatCard(title: "SMA 50", value: "$\(sma50.formattedDecimal)")
                }

                if let sma200 = viewModel.currentSMA200 {
                    StatCard(title: "SMA 200", value: "$\(sma200.formattedDecimal)")
                }

                if let vol = viewModel.historicalVol {
                    StatCard(title: "Hist. Volatility", value: vol.formattedPercent)
                }

                if let macd = viewModel.currentMACD {
                    StatCard(title: "MACD", value: macd.formattedDecimal,
                             color: macd > 0 ? .green : .red)
                }
            }
        }
    }

    private var optionsSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Options Recommendations")
                .font(.headline)

            ForEach(viewModel.optionsRecommendations) { rec in
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        HStack {
                            Text(rec.optionType.rawValue)
                                .font(.subheadline)
                                .fontWeight(.medium)
                            Text("$\(rec.strikePrice.formattedNumber)")
                                .font(.subheadline)
                                .foregroundStyle(.blue)
                        }
                        Text(rec.rationale)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    VStack(alignment: .trailing) {
                        Text(rec.estimatedPremium.formattedCurrency)
                            .font(.subheadline)
                            .fontWeight(.medium)
                        Text("\(rec.suggestedDTE) DTE")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
                .padding()
                .background(.ultraThinMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 10))
            }
        }
    }

    private var actionButtons: some View {
        NavigationLink {
            BacktestRunView(symbol: symbol)
        } label: {
            HStack {
                Image(systemName: "clock.arrow.circlepath")
                Text("Run Backtest")
            }
            .font(.headline)
            .frame(maxWidth: .infinity)
            .padding()
            .background(Color.accentColor)
            .foregroundStyle(.white)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }
}

#Preview {
    NavigationStack {
        SymbolDetailView(symbol: "AAPL")
    }
}
