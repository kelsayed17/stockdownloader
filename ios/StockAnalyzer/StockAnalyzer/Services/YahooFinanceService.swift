import Foundation

actor YahooFinanceService {
    static let shared = YahooFinanceService()

    private let baseURL = "https://query1.finance.yahoo.com"
    private var crumb: String?
    private var cookies: [HTTPCookie] = []
    private let session: URLSession

    private init() {
        let config = URLSessionConfiguration.default
        config.httpShouldSetCookies = true
        config.httpCookieAcceptPolicy = .always
        config.httpCookieStorage = HTTPCookieStorage.shared
        config.timeoutIntervalForRequest = 30
        self.session = URLSession(configuration: config)
    }

    // MARK: - Authentication

    private func authenticate() async throws {
        let consentURL = URL(string: "https://fc.yahoo.com/")!
        var request = URLRequest(url: consentURL)
        request.httpMethod = "GET"
        request.setValue("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)", forHTTPHeaderField: "User-Agent")

        let (_, response) = try await session.data(for: request)
        if let httpResponse = response as? HTTPURLResponse,
           let headerFields = httpResponse.allHeaderFields as? [String: String],
           let url = httpResponse.url {
            let responseCookies = HTTPCookie.cookies(withResponseHeaderFields: headerFields, for: url)
            self.cookies = responseCookies
            for cookie in responseCookies {
                HTTPCookieStorage.shared.setCookie(cookie)
            }
        }

        let crumbURL = URL(string: "\(baseURL)/v1/test/getcrumb")!
        var crumbRequest = URLRequest(url: crumbURL)
        crumbRequest.setValue("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)", forHTTPHeaderField: "User-Agent")

        let (crumbData, _) = try await session.data(for: crumbRequest)
        self.crumb = String(data: crumbData, encoding: .utf8)?.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private func ensureAuthenticated() async throws {
        if crumb == nil {
            try await authenticate()
        }
    }

    // MARK: - Fetch Historical Data

    func fetchHistoricalData(
        symbol: String,
        range: TimeRange = .fiveYears,
        interval: DataInterval = .daily
    ) async throws -> [PriceData] {
        try await ensureAuthenticated()

        var components = URLComponents(string: "\(baseURL)/v8/finance/chart/\(symbol)")!
        var queryItems = [
            URLQueryItem(name: "range", value: range.rawValue),
            URLQueryItem(name: "interval", value: interval.rawValue),
            URLQueryItem(name: "includePrePost", value: "false"),
            URLQueryItem(name: "events", value: "div,splits")
        ]

        if let crumb = crumb {
            queryItems.append(URLQueryItem(name: "crumb", value: crumb))
        }

        components.queryItems = queryItems

        guard let url = components.url else {
            throw YahooFinanceError.invalidURL
        }

        var request = URLRequest(url: url)
        request.setValue("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)", forHTTPHeaderField: "User-Agent")
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        let (data, response) = try await session.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw YahooFinanceError.invalidResponse
        }

        if httpResponse.statusCode == 401 {
            crumb = nil
            try await authenticate()
            return try await fetchHistoricalData(symbol: symbol, range: range, interval: interval)
        }

        guard httpResponse.statusCode == 200 else {
            throw YahooFinanceError.httpError(httpResponse.statusCode)
        }

        return try parseChartResponse(data)
    }

    // MARK: - Fetch Quote

    func fetchQuote(symbol: String) async throws -> StockQuote {
        try await ensureAuthenticated()

        var components = URLComponents(string: "\(baseURL)/v8/finance/chart/\(symbol)")!
        var queryItems = [
            URLQueryItem(name: "range", value: "5d"),
            URLQueryItem(name: "interval", value: "1d"),
            URLQueryItem(name: "includePrePost", value: "false")
        ]

        if let crumb = crumb {
            queryItems.append(URLQueryItem(name: "crumb", value: crumb))
        }

        components.queryItems = queryItems

        guard let url = components.url else {
            throw YahooFinanceError.invalidURL
        }

        var request = URLRequest(url: url)
        request.setValue("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)", forHTTPHeaderField: "User-Agent")

        let (data, _) = try await session.data(for: request)

        let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        let chart = json?["chart"] as? [String: Any]
        let results = chart?["result"] as? [[String: Any]]
        guard let result = results?.first else {
            throw YahooFinanceError.noData
        }

        let meta = result["meta"] as? [String: Any]
        let regularMarketPrice = meta?["regularMarketPrice"] as? Double ?? 0
        let previousClose = meta?["chartPreviousClose"] as? Double ?? meta?["previousClose"] as? Double ?? 0
        let currency = meta?["currency"] as? String ?? "USD"
        let exchangeName = meta?["exchangeName"] as? String ?? ""
        let shortName = meta?["shortName"] as? String ?? symbol

        let change = regularMarketPrice - previousClose
        let changePercent = previousClose > 0 ? (change / previousClose) * 100 : 0

        return StockQuote(
            symbol: symbol.uppercased(),
            name: shortName,
            price: Decimal(regularMarketPrice),
            change: Decimal(change),
            changePercent: Decimal(changePercent),
            previousClose: Decimal(previousClose),
            currency: currency,
            exchange: exchangeName
        )
    }

    // MARK: - Parsing

    private func parseChartResponse(_ data: Data) throws -> [PriceData] {
        let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        let chart = json?["chart"] as? [String: Any]
        let results = chart?["result"] as? [[String: Any]]

        guard let result = results?.first else {
            throw YahooFinanceError.noData
        }

        guard let timestamps = result["timestamp"] as? [Int] else {
            throw YahooFinanceError.parseError("Missing timestamps")
        }

        let indicators = result["indicators"] as? [String: Any]
        let quotes = (indicators?["quote"] as? [[String: Any]])?.first
        let adjCloseArr = (indicators?["adjclose"] as? [[String: Any]])?.first

        guard let opens = quotes?["open"] as? [Double?],
              let highs = quotes?["high"] as? [Double?],
              let lows = quotes?["low"] as? [Double?],
              let closes = quotes?["close"] as? [Double?],
              let volumes = quotes?["volume"] as? [Int64?] else {
            throw YahooFinanceError.parseError("Missing OHLCV data")
        }

        let adjCloses = adjCloseArr?["adjclose"] as? [Double?]

        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd"
        dateFormatter.timeZone = TimeZone(identifier: "America/New_York")

        var priceData: [PriceData] = []

        for i in 0..<timestamps.count {
            guard let open = opens[safe: i] ?? nil,
                  let high = highs[safe: i] ?? nil,
                  let low = lows[safe: i] ?? nil,
                  let close = closes[safe: i] ?? nil else {
                continue
            }

            let volume = volumes[safe: i] ?? nil ?? 0
            let adjClose = adjCloses?[safe: i] ?? nil ?? close

            let date = Date(timeIntervalSince1970: TimeInterval(timestamps[i]))
            let dateString = dateFormatter.string(from: date)

            let price = PriceData(
                date: dateString,
                open: Decimal(open),
                high: Decimal(high),
                low: Decimal(low),
                close: Decimal(close),
                adjClose: Decimal(adjClose),
                volume: volume
            )

            priceData.append(price)
        }

        return priceData
    }
}

// MARK: - Supporting Types

struct StockQuote: Identifiable {
    var id: String { symbol }
    let symbol: String
    let name: String
    let price: Decimal
    let change: Decimal
    let changePercent: Decimal
    let previousClose: Decimal
    let currency: String
    let exchange: String
}

enum YahooFinanceError: LocalizedError {
    case invalidURL
    case invalidResponse
    case httpError(Int)
    case noData
    case parseError(String)
    case authenticationFailed

    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "Invalid URL"
        case .invalidResponse:
            return "Invalid response from server"
        case .httpError(let code):
            return "HTTP error: \(code)"
        case .noData:
            return "No data available for this symbol"
        case .parseError(let msg):
            return "Parse error: \(msg)"
        case .authenticationFailed:
            return "Authentication failed"
        }
    }
}

// MARK: - Safe Array Access

private extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
