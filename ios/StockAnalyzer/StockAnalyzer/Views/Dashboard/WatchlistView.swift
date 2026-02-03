import SwiftUI

struct WatchlistView: View {
    @StateObject private var viewModel = WatchlistViewModel()
    @State private var showingAddSheet = false

    var body: some View {
        NavigationStack {
            Group {
                if viewModel.items.isEmpty {
                    emptyState
                } else {
                    watchlistContent
                }
            }
            .navigationTitle("Watchlist")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button {
                        showingAddSheet = true
                    } label: {
                        Image(systemName: "plus")
                    }
                }
            }
            .refreshable {
                await viewModel.refreshAll()
            }
            .sheet(isPresented: $showingAddSheet) {
                addSymbolSheet
            }
            .task {
                await viewModel.refreshAll()
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "star")
                .font(.system(size: 48))
                .foregroundStyle(.secondary)
            Text("No Watchlist Items")
                .font(.headline)
            Text("Add symbols to track their prices and get quick access to analysis")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Button {
                showingAddSheet = true
            } label: {
                HStack {
                    Image(systemName: "plus")
                    Text("Add Symbol")
                }
            }
            .buttonStyle(.borderedProminent)
        }
        .padding()
    }

    private var watchlistContent: some View {
        List {
            ForEach(viewModel.items) { item in
                NavigationLink {
                    SymbolDetailView(symbol: item.symbol)
                } label: {
                    watchlistRow(item)
                }
            }
            .onDelete { offsets in
                viewModel.removeItem(at: offsets)
            }
        }
    }

    private func watchlistRow(_ item: WatchlistItem) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(item.symbol)
                    .font(.headline)
                Text("Added \(item.addedDate, format: .dateTime.month().day())")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            if let price = item.lastPrice {
                VStack(alignment: .trailing, spacing: 2) {
                    Text(price.formattedCurrency)
                        .font(.subheadline)
                        .fontWeight(.medium)
                    if let change = item.changePercent {
                        HStack(spacing: 2) {
                            Image(systemName: change >= 0 ? "arrow.up.right" : "arrow.down.right")
                                .font(.caption2)
                            Text(change.formattedPercent)
                                .font(.caption)
                        }
                        .foregroundStyle(Color.forValue(change))
                    }
                }
            } else {
                ProgressView()
                    .scaleEffect(0.7)
            }
        }
    }

    private var addSymbolSheet: some View {
        NavigationStack {
            VStack(spacing: 20) {
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundStyle(.secondary)
                    TextField("Symbol (e.g. AAPL)", text: $viewModel.newSymbol)
                        .textInputAutocapitalization(.characters)
                        .autocorrectionDisabled()
                        .onSubmit {
                            addSymbol()
                        }
                }
                .padding()
                .background(.ultraThinMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 10))

                Button {
                    addSymbol()
                } label: {
                    Text("Add to Watchlist")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(viewModel.newSymbol.isEmpty ? Color.gray : Color.accentColor)
                        .foregroundStyle(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .disabled(viewModel.newSymbol.isEmpty)

                // Quick Add suggestions
                VStack(alignment: .leading, spacing: 8) {
                    Text("Quick Add")
                        .font(.headline)

                    let suggestions = ["SPY", "QQQ", "AAPL", "MSFT", "TSLA", "AMZN", "GOOGL", "NVDA"]
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 70))], spacing: 8) {
                        ForEach(suggestions, id: \.self) { symbol in
                            Button {
                                viewModel.addSymbol(symbol)
                                showingAddSheet = false
                            } label: {
                                Text(symbol)
                                    .font(.caption)
                                    .fontWeight(.medium)
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 8)
                                    .background(.ultraThinMaterial)
                                    .clipShape(Capsule())
                            }
                            .disabled(viewModel.items.contains { $0.symbol == symbol })
                        }
                    }
                }

                Spacer()
            }
            .padding()
            .navigationTitle("Add Symbol")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        showingAddSheet = false
                    }
                }
            }
        }
        .presentationDetents([.medium])
    }

    private func addSymbol() {
        viewModel.addSymbol(viewModel.newSymbol)
        viewModel.newSymbol = ""
        showingAddSheet = false
    }
}

#Preview {
    WatchlistView()
}
