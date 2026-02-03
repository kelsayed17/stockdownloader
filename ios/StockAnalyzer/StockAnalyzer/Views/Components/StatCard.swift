import SwiftUI

struct StatCard: View {
    let title: String
    let value: String
    var subtitle: String? = nil
    var color: Color = .primary
    var icon: String? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                if let icon = icon {
                    Image(systemName: icon)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Text(title)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Text(value)
                .font(.headline)
                .fontWeight(.semibold)
                .foregroundStyle(color)
                .lineLimit(1)
                .minimumScaleFactor(0.7)

            if let subtitle = subtitle {
                Text(subtitle)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }
}

struct QuoteCard: View {
    let quote: StockQuote

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(quote.symbol)
                    .font(.headline)
                    .fontWeight(.bold)
                Spacer()
                Text(quote.price.formattedCurrency)
                    .font(.headline)
            }

            HStack {
                Text(quote.name)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                Spacer()
                HStack(spacing: 4) {
                    Image(systemName: quote.change >= 0 ? "arrow.up.right" : "arrow.down.right")
                        .font(.caption2)
                    Text(quote.changePercent.formattedPercent)
                        .font(.subheadline)
                        .fontWeight(.medium)
                }
                .foregroundStyle(Color.forValue(quote.change))
            }
        }
        .padding(12)
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }
}

struct SignalBadge: View {
    let signal: SignalType

    var body: some View {
        Text(signal.rawValue)
            .font(.caption)
            .fontWeight(.semibold)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(Color.forSignal(signal).opacity(0.2))
            .foregroundStyle(Color.forSignal(signal))
            .clipShape(Capsule())
    }
}

struct IndicatorRow: View {
    let indicator: IndicatorSignal

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(indicator.name)
                    .font(.subheadline)
                    .fontWeight(.medium)
                Text(indicator.description)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            Text(String(format: "%.2f", indicator.value))
                .font(.subheadline)
                .monospacedDigit()

            SignalBadge(signal: indicator.signal)
        }
        .padding(.vertical, 4)
    }
}

struct TradeRowView: View {
    let trade: Trade

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(trade.entryDate)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Image(systemName: "arrow.right")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                Text(trade.exitDate ?? "Open")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Spacer()
                if let pl = trade.profitLoss {
                    Text(pl.formattedCurrency)
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundStyle(Color.forValue(pl))
                }
            }

            HStack {
                Text("\(trade.shares) shares @ \(trade.entryPrice.formattedCurrency)")
                    .font(.caption)
                Spacer()
                if let ret = trade.returnPercent {
                    Text(ret.formattedPercent)
                        .font(.caption)
                        .foregroundStyle(Color.forValue(ret))
                }
            }
        }
        .padding(.vertical, 2)
    }
}

#Preview {
    VStack(spacing: 12) {
        StatCard(title: "Total Return", value: "+23.45%", color: .green, icon: "chart.line.uptrend.xyaxis")
        SignalBadge(signal: .strongBuy)
        SignalBadge(signal: .sell)
    }
    .padding()
}
