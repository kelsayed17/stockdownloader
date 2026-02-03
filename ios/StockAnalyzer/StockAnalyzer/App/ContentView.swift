import SwiftUI

struct ContentView: View {
    var body: some View {
        TabView {
            DashboardView()
                .tabItem {
                    Label("Dashboard", systemImage: "chart.line.uptrend.xyaxis")
                }

            SymbolSearchView()
                .tabItem {
                    Label("Analyze", systemImage: "magnifyingglass")
                }

            BacktestView()
                .tabItem {
                    Label("Backtest", systemImage: "clock.arrow.circlepath")
                }

            WatchlistView()
                .tabItem {
                    Label("Watchlist", systemImage: "star")
                }

            SettingsView()
                .tabItem {
                    Label("Settings", systemImage: "gear")
                }
        }
        .tint(.accentColor)
    }
}

#Preview {
    ContentView()
}
