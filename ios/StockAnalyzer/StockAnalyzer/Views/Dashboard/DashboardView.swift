import SwiftUI

struct DashboardView: View {
    @StateObject private var viewModel = DashboardViewModel()

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    // Market Overview Header
                    marketOverviewSection

                    // Quick Actions
                    quickActionsSection

                    // Market Quotes
                    quotesSection
                }
                .padding()
            }
            .navigationTitle("Dashboard")
            .refreshable {
                await viewModel.loadDashboard()
            }
            .task {
                if viewModel.watchlistQuotes.isEmpty {
                    await viewModel.loadDashboard()
                }
            }
            .overlay {
                if viewModel.isLoading && viewModel.watchlistQuotes.isEmpty {
                    ProgressView("Loading market data...")
                }
            }
        }
    }

    private var marketOverviewSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Market Overview")
                .font(.title2)
                .fontWeight(.bold)

            if let spy = viewModel.watchlistQuotes.first(where: { $0.symbol == "SPY" }) {
                HStack(spacing: 20) {
                    VStack(alignment: .leading) {
                        Text("S&P 500")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        Text(spy.price.formattedCurrency)
                            .font(.title)
                            .fontWeight(.bold)
                    }

                    VStack(alignment: .leading) {
                        Text("Change")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        HStack(spacing: 4) {
                            Image(systemName: spy.change >= 0 ? "arrow.up.right" : "arrow.down.right")
                            Text(spy.changePercent.formattedPercent)
                        }
                        .font(.title2)
                        .foregroundStyle(Color.forValue(spy.change))
                    }

                    Spacer()
                }
                .padding()
                .background(.ultraThinMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
    }

    private var quickActionsSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Quick Actions")
                .font(.headline)

            HStack(spacing: 12) {
                NavigationLink {
                    SymbolSearchView()
                } label: {
                    quickActionButton(icon: "magnifyingglass", title: "Analyze", color: .blue)
                }

                NavigationLink {
                    BacktestView()
                } label: {
                    quickActionButton(icon: "clock.arrow.circlepath", title: "Backtest", color: .purple)
                }

                NavigationLink {
                    WatchlistView()
                } label: {
                    quickActionButton(icon: "star", title: "Watchlist", color: .orange)
                }
            }
        }
    }

    private func quickActionButton(icon: String, title: String, color: Color) -> some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundStyle(color)
            Text(title)
                .font(.caption)
                .foregroundStyle(.primary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 16)
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private var quotesSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Market Movers")
                .font(.headline)

            LazyVStack(spacing: 8) {
                ForEach(viewModel.watchlistQuotes) { quote in
                    NavigationLink {
                        SymbolDetailView(symbol: quote.symbol)
                    } label: {
                        QuoteCard(quote: quote)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }
}

#Preview {
    DashboardView()
}
