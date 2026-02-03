import SwiftUI

struct SymbolSearchView: View {
    @State private var searchText = ""
    @State private var recentSearches: [String] = UserDefaults.standard.stringArray(forKey: "recent_searches") ?? []

    private let popularSymbols = [
        ("SPY", "S&P 500 ETF"),
        ("QQQ", "Nasdaq 100 ETF"),
        ("AAPL", "Apple"),
        ("MSFT", "Microsoft"),
        ("GOOGL", "Alphabet"),
        ("AMZN", "Amazon"),
        ("TSLA", "Tesla"),
        ("NVDA", "NVIDIA"),
        ("META", "Meta Platforms"),
        ("JPM", "JPMorgan Chase"),
        ("V", "Visa"),
        ("JNJ", "Johnson & Johnson"),
    ]

    var body: some View {
        NavigationStack {
            List {
                if !searchText.isEmpty {
                    Section {
                        NavigationLink {
                            SymbolDetailView(symbol: searchText.uppercased())
                        } label: {
                            HStack {
                                Image(systemName: "magnifyingglass")
                                    .foregroundStyle(.blue)
                                Text("Analyze \(searchText.uppercased())")
                                    .fontWeight(.medium)
                            }
                        }
                    }
                }

                if !recentSearches.isEmpty {
                    Section("Recent") {
                        ForEach(recentSearches, id: \.self) { symbol in
                            NavigationLink {
                                SymbolDetailView(symbol: symbol)
                            } label: {
                                HStack {
                                    Image(systemName: "clock")
                                        .foregroundStyle(.secondary)
                                    Text(symbol)
                                        .fontWeight(.medium)
                                }
                            }
                        }
                        .onDelete { offsets in
                            recentSearches.remove(atOffsets: offsets)
                            UserDefaults.standard.set(recentSearches, forKey: "recent_searches")
                        }
                    }
                }

                Section("Popular Symbols") {
                    ForEach(popularSymbols, id: \.0) { symbol, name in
                        NavigationLink {
                            SymbolDetailView(symbol: symbol)
                        } label: {
                            HStack {
                                Text(symbol)
                                    .fontWeight(.bold)
                                    .frame(width: 60, alignment: .leading)
                                Text(name)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }
            }
            .navigationTitle("Analyze")
            .searchable(text: $searchText, prompt: "Enter stock symbol (e.g. AAPL)")
            .textInputAutocapitalization(.characters)
            .onSubmit(of: .search) {
                let symbol = searchText.uppercased().trimmingCharacters(in: .whitespacesAndNewlines)
                guard !symbol.isEmpty else { return }
                addToRecent(symbol)
            }
        }
    }

    private func addToRecent(_ symbol: String) {
        recentSearches.removeAll { $0 == symbol }
        recentSearches.insert(symbol, at: 0)
        if recentSearches.count > 10 {
            recentSearches = Array(recentSearches.prefix(10))
        }
        UserDefaults.standard.set(recentSearches, forKey: "recent_searches")
    }
}

#Preview {
    SymbolSearchView()
}
