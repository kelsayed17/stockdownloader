import SwiftUI
import Charts

struct PriceChartView: View {
    let data: [PriceData]
    var showVolume: Bool = true
    var showSMA50: Bool = false
    var showSMA200: Bool = false
    var showBollingerBands: Bool = false
    var height: CGFloat = 300

    private var sma50Values: [Double?] {
        TechnicalIndicators.sma(data.map { $0.closeDouble }, period: 50)
    }

    private var sma200Values: [Double?] {
        TechnicalIndicators.sma(data.map { $0.closeDouble }, period: 200)
    }

    private var bbValues: TechnicalIndicators.BollingerBands {
        TechnicalIndicators.bollingerBands(data.map { $0.closeDouble })
    }

    var body: some View {
        VStack(spacing: 0) {
            // Price Chart
            Chart {
                ForEach(Array(data.enumerated()), id: \.offset) { index, price in
                    LineMark(
                        x: .value("Date", index),
                        y: .value("Price", price.closeDouble)
                    )
                    .foregroundStyle(.blue)
                    .lineStyle(StrokeStyle(lineWidth: 1.5))

                    if showSMA50, let sma = sma50Values[index] {
                        LineMark(
                            x: .value("Date", index),
                            y: .value("SMA50", sma),
                            series: .value("Indicator", "SMA50")
                        )
                        .foregroundStyle(.orange)
                        .lineStyle(StrokeStyle(lineWidth: 1, dash: [4, 2]))
                    }

                    if showSMA200, let sma = sma200Values[index] {
                        LineMark(
                            x: .value("Date", index),
                            y: .value("SMA200", sma),
                            series: .value("Indicator", "SMA200")
                        )
                        .foregroundStyle(.purple)
                        .lineStyle(StrokeStyle(lineWidth: 1, dash: [4, 2]))
                    }

                    if showBollingerBands {
                        if let upper = bbValues.upper[index] {
                            LineMark(
                                x: .value("Date", index),
                                y: .value("BB Upper", upper),
                                series: .value("Indicator", "BB Upper")
                            )
                            .foregroundStyle(.gray.opacity(0.5))
                            .lineStyle(StrokeStyle(lineWidth: 0.5))
                        }
                        if let lower = bbValues.lower[index] {
                            LineMark(
                                x: .value("Date", index),
                                y: .value("BB Lower", lower),
                                series: .value("Indicator", "BB Lower")
                            )
                            .foregroundStyle(.gray.opacity(0.5))
                            .lineStyle(StrokeStyle(lineWidth: 0.5))
                        }
                    }
                }
            }
            .frame(height: height)
            .chartXAxis(.hidden)
            .chartYAxis {
                AxisMarks(position: .trailing) { value in
                    AxisValueLabel {
                        if let doubleVal = value.as(Double.self) {
                            Text("$\(doubleVal, specifier: "%.0f")")
                                .font(.caption2)
                        }
                    }
                    AxisGridLine(stroke: StrokeStyle(lineWidth: 0.3))
                }
            }

            // Volume Chart
            if showVolume {
                Chart {
                    ForEach(Array(data.enumerated()), id: \.offset) { index, price in
                        BarMark(
                            x: .value("Date", index),
                            y: .value("Volume", price.volume)
                        )
                        .foregroundStyle(
                            index > 0 && price.closeDouble >= data[index - 1].closeDouble
                                ? Color.green.opacity(0.5)
                                : Color.red.opacity(0.5)
                        )
                    }
                }
                .frame(height: 60)
                .chartXAxis(.hidden)
                .chartYAxis {
                    AxisMarks(position: .trailing, values: .automatic(desiredCount: 2)) { value in
                        AxisValueLabel {
                            if let intVal = value.as(Int.self) {
                                Text(Double(intVal).formattedVolume)
                                    .font(.caption2)
                            }
                        }
                    }
                }
            }

            // Date axis
            if data.count > 1 {
                HStack {
                    Text(data.first?.date ?? "")
                    Spacer()
                    Text(data.last?.date ?? "")
                }
                .font(.caption2)
                .foregroundStyle(.secondary)
                .padding(.horizontal, 4)
                .padding(.top, 4)
            }
        }
    }
}

struct EquityCurveView: View {
    let results: [BacktestResult]
    var buyAndHold: BacktestResult?
    var height: CGFloat = 250

    var body: some View {
        Chart {
            if let bh = buyAndHold, !bh.equityCurve.isEmpty {
                ForEach(Array(bh.equityCurve.enumerated()), id: \.offset) { index, point in
                    LineMark(
                        x: .value("Date", index),
                        y: .value("Equity", point.1.doubleValue),
                        series: .value("Strategy", "Buy & Hold")
                    )
                    .foregroundStyle(.gray)
                    .lineStyle(StrokeStyle(lineWidth: 1, dash: [5, 3]))
                }
            }

            ForEach(Array(results.prefix(5).enumerated()), id: \.offset) { rIndex, result in
                ForEach(Array(result.equityCurve.enumerated()), id: \.offset) { index, point in
                    LineMark(
                        x: .value("Date", index),
                        y: .value("Equity", point.1.doubleValue),
                        series: .value("Strategy", result.strategyName)
                    )
                    .foregroundStyle(chartColor(for: rIndex))
                    .lineStyle(StrokeStyle(lineWidth: 1.5))
                }
            }
        }
        .frame(height: height)
        .chartXAxis(.hidden)
        .chartYAxis {
            AxisMarks(position: .trailing) { value in
                AxisValueLabel {
                    if let doubleVal = value.as(Double.self) {
                        Text("$\(doubleVal / 1000, specifier: "%.0f")K")
                            .font(.caption2)
                    }
                }
                AxisGridLine(stroke: StrokeStyle(lineWidth: 0.3))
            }
        }
        .chartLegend(position: .bottom, alignment: .leading, spacing: 8)
    }

    private func chartColor(for index: Int) -> Color {
        let colors: [Color] = [.blue, .green, .orange, .purple, .pink]
        return colors[index % colors.count]
    }
}

struct RSIChartView: View {
    let data: [PriceData]
    var height: CGFloat = 120

    private var rsiValues: [Double?] {
        TechnicalIndicators.rsi(data.map { $0.closeDouble })
    }

    var body: some View {
        Chart {
            // Overbought/Oversold zones
            RuleMark(y: .value("Overbought", 70))
                .foregroundStyle(.red.opacity(0.3))
                .lineStyle(StrokeStyle(lineWidth: 0.5, dash: [3, 3]))

            RuleMark(y: .value("Oversold", 30))
                .foregroundStyle(.green.opacity(0.3))
                .lineStyle(StrokeStyle(lineWidth: 0.5, dash: [3, 3]))

            ForEach(Array(data.enumerated()), id: \.offset) { index, _ in
                if let rsi = rsiValues[index] {
                    LineMark(
                        x: .value("Date", index),
                        y: .value("RSI", rsi)
                    )
                    .foregroundStyle(.purple)
                    .lineStyle(StrokeStyle(lineWidth: 1.5))
                }
            }
        }
        .frame(height: height)
        .chartXAxis(.hidden)
        .chartYScale(domain: 0...100)
        .chartYAxis {
            AxisMarks(values: [0, 30, 50, 70, 100]) { value in
                AxisValueLabel {
                    if let v = value.as(Int.self) {
                        Text("\(v)")
                            .font(.caption2)
                    }
                }
                AxisGridLine(stroke: StrokeStyle(lineWidth: 0.2))
            }
        }
    }
}

struct MACDChartView: View {
    let data: [PriceData]
    var height: CGFloat = 120

    private var macdResult: TechnicalIndicators.MACDResult {
        TechnicalIndicators.macd(data.map { $0.closeDouble })
    }

    var body: some View {
        Chart {
            RuleMark(y: .value("Zero", 0))
                .foregroundStyle(.gray.opacity(0.3))

            ForEach(Array(data.enumerated()), id: \.offset) { index, _ in
                if let histogram = macdResult.histogram[index] {
                    BarMark(
                        x: .value("Date", index),
                        y: .value("Histogram", histogram)
                    )
                    .foregroundStyle(histogram >= 0 ? Color.green.opacity(0.4) : Color.red.opacity(0.4))
                }

                if let macd = macdResult.macdLine[index] {
                    LineMark(
                        x: .value("Date", index),
                        y: .value("MACD", macd),
                        series: .value("Line", "MACD")
                    )
                    .foregroundStyle(.blue)
                    .lineStyle(StrokeStyle(lineWidth: 1.5))
                }

                if let signal = macdResult.signalLine[index] {
                    LineMark(
                        x: .value("Date", index),
                        y: .value("Signal", signal),
                        series: .value("Line", "Signal")
                    )
                    .foregroundStyle(.orange)
                    .lineStyle(StrokeStyle(lineWidth: 1))
                }
            }
        }
        .frame(height: height)
        .chartXAxis(.hidden)
        .chartYAxis {
            AxisMarks(position: .trailing, values: .automatic(desiredCount: 3)) { value in
                AxisValueLabel {
                    if let v = value.as(Double.self) {
                        Text(String(format: "%.1f", v))
                            .font(.caption2)
                    }
                }
            }
        }
    }
}

#Preview {
    PriceChartView(data: [])
        .padding()
}
