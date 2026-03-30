import Foundation
import Observation
import AuthenticationServices
import Security

// MARK: - Strava Config

private enum StravaConfig {
    static var clientId: String {
        Bundle.main.object(forInfoDictionaryKey: "STRAVA_CLIENT_ID") as? String ?? ""
    }
    static var clientSecret: String {
        Bundle.main.object(forInfoDictionaryKey: "STRAVA_CLIENT_SECRET") as? String ?? ""
    }
    static let redirectURI = "gosteady://strava/callback"
    static let authURL = "https://www.strava.com/oauth/authorize"
    static let tokenURL = "https://www.strava.com/oauth/token"
    static let apiBaseURL = "https://www.strava.com/api/v3/"
    static let scope = "activity:write,activity:read_all,profile:read_all"
}

// MARK: - Strava API Models (canonical types in Models/StravaModels.swift)

struct StravaActivityCreate: Codable {
    let name: String
    let sportType: String
    let startDateLocal: String
    let elapsedTime: Int
    let distance: Float
    let description: String?

    enum CodingKeys: String, CodingKey {
        case name
        case sportType = "sport_type"
        case startDateLocal = "start_date_local"
        case elapsedTime = "elapsed_time"
        case distance, description
    }
}

struct StravaUploadResponse: Codable {
    let id: Int
    let status: String?
    let activityId: Int?
    let error: String?

    enum CodingKeys: String, CodingKey {
        case id, status, error
        case activityId = "activity_id"
    }
}

// MARK: - Keychain Helper

private enum KeychainHelper {
    static func save(key: String, value: String) {
        guard let data = value.data(using: .utf8) else { return }

        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: key,
            kSecAttrService as String: "com.gosteady.strava"
        ]
        SecItemDelete(query as CFDictionary)

        var addQuery = query
        addQuery[kSecValueData as String] = data
        SecItemAdd(addQuery as CFDictionary, nil)
    }

    static func load(key: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: key,
            kSecAttrService as String: "com.gosteady.strava",
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]

        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)

        guard status == errSecSuccess, let data = result as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }

    static func delete(key: String) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: key,
            kSecAttrService as String: "com.gosteady.strava"
        ]
        SecItemDelete(query as CFDictionary)
    }
}

// MARK: - Strava Service

@Observable
final class StravaService {

    var isConnected: Bool { accessToken != nil }
    var athleteName: String?
    var athlete: StravaAthlete?
    var lastError: String?

    private var accessToken: String? {
        get { KeychainHelper.load(key: "strava_access_token") }
        set {
            if let value = newValue {
                KeychainHelper.save(key: "strava_access_token", value: value)
            } else {
                KeychainHelper.delete(key: "strava_access_token")
            }
        }
    }

    private var refreshToken: String? {
        get { KeychainHelper.load(key: "strava_refresh_token") }
        set {
            if let value = newValue {
                KeychainHelper.save(key: "strava_refresh_token", value: value)
            } else {
                KeychainHelper.delete(key: "strava_refresh_token")
            }
        }
    }

    private var expiresAt: Int {
        get { UserDefaults.standard.integer(forKey: "strava_expires_at") }
        set { UserDefaults.standard.set(newValue, forKey: "strava_expires_at") }
    }

    private let session = URLSession.shared
    private let jsonDecoder: JSONDecoder = {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return decoder
    }()

    init() {
        athleteName = UserDefaults.standard.string(forKey: "strava_athlete_name")
    }

    // MARK: - OAuth Flow

    func getAuthURL() -> URL? {
        var components = URLComponents(string: StravaConfig.authURL)
        components?.queryItems = [
            URLQueryItem(name: "client_id", value: StravaConfig.clientId),
            URLQueryItem(name: "redirect_uri", value: StravaConfig.redirectURI),
            URLQueryItem(name: "response_type", value: "code"),
            URLQueryItem(name: "scope", value: StravaConfig.scope)
        ]
        return components?.url
    }

    @MainActor
    func authenticate(presentingWindow: ASPresentationAnchor) async throws {
        guard let authURL = getAuthURL() else {
            throw StravaError.invalidURL
        }

        let callbackURL = try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<URL, Error>) in
            let session = ASWebAuthenticationSession(
                url: authURL,
                callbackURLScheme: "gosteady"
            ) { callbackURL, error in
                if let error {
                    continuation.resume(throwing: error)
                    return
                }
                guard let url = callbackURL else {
                    continuation.resume(throwing: StravaError.noCallbackURL)
                    return
                }
                continuation.resume(returning: url)
            }
            session.presentationContextProvider = PresentationContextProvider(anchor: presentingWindow)
            session.prefersEphemeralWebBrowserSession = false
            session.start()
        }

        guard let components = URLComponents(url: callbackURL, resolvingAgainstBaseURL: false),
              let code = components.queryItems?.first(where: { $0.name == "code" })?.value else {
            throw StravaError.noAuthCode
        }

        try await exchangeToken(code: code)
    }

    func handleAuthCallback(url: URL) async throws {
        guard let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
              let code = components.queryItems?.first(where: { $0.name == "code" })?.value else {
            throw StravaError.noAuthCode
        }
        try await exchangeToken(code: code)
    }

    private func exchangeToken(code: String) async throws {
        let url = URL(string: StravaConfig.tokenURL)!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")

        let body = [
            "client_id": StravaConfig.clientId,
            "client_secret": StravaConfig.clientSecret,
            "code": code,
            "grant_type": "authorization_code"
        ]
        request.httpBody = body.map { "\($0.key)=\($0.value)" }.joined(separator: "&").data(using: .utf8)

        let (data, response) = try await session.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            throw StravaError.authFailed
        }

        let tokenResponse = try jsonDecoder.decode(StravaTokenResponse.self, from: data)
        saveTokens(tokenResponse)
    }

    private func saveTokens(_ tokenResponse: StravaTokenResponse) {
        accessToken = tokenResponse.accessToken
        refreshToken = tokenResponse.refreshToken
        expiresAt = tokenResponse.expiresAt

        if let athleteData = tokenResponse.athlete {
            athlete = athleteData
            athleteName = athleteData.fullName
            UserDefaults.standard.set(athleteData.fullName, forKey: "strava_athlete_name")
        }
    }

    private func ensureValidToken() async throws -> String {
        let currentTime = Int(Date().timeIntervalSince1970)

        if currentTime >= expiresAt - 60 {
            guard let refresh = refreshToken else {
                throw StravaError.noRefreshToken
            }

            let url = URL(string: StravaConfig.tokenURL)!
            var request = URLRequest(url: url)
            request.httpMethod = "POST"
            request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")

            let body = [
                "client_id": StravaConfig.clientId,
                "client_secret": StravaConfig.clientSecret,
                "refresh_token": refresh,
                "grant_type": "refresh_token"
            ]
            request.httpBody = body.map { "\($0.key)=\($0.value)" }.joined(separator: "&").data(using: .utf8)

            let (data, response) = try await session.data(for: request)
            guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
                throw StravaError.tokenRefreshFailed
            }

            let tokenResponse = try jsonDecoder.decode(StravaTokenResponse.self, from: data)
            saveTokens(tokenResponse)
        }

        guard let token = accessToken else {
            throw StravaError.notAuthenticated
        }
        return token
    }

    // MARK: - Athlete Profile

    func fetchAthlete() async throws -> StravaAthlete {
        let token = try await ensureValidToken()
        let url = URL(string: "\(StravaConfig.apiBaseURL)athlete")!
        var request = URLRequest(url: url)
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        let (data, response) = try await session.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            throw StravaError.apiFailed("Failed to fetch athlete")
        }

        let athleteData = try jsonDecoder.decode(StravaAthlete.self, from: data)
        athlete = athleteData
        athleteName = athleteData.fullName
        UserDefaults.standard.set(athleteData.fullName, forKey: "strava_athlete_name")
        return athleteData
    }

    // MARK: - Import Activities

    func fetchActivities(page: Int = 1, perPage: Int = 30) async throws -> [StravaActivity] {
        let token = try await ensureValidToken()

        var components = URLComponents(string: "\(StravaConfig.apiBaseURL)athlete/activities")!
        components.queryItems = [
            URLQueryItem(name: "page", value: "\(page)"),
            URLQueryItem(name: "per_page", value: "\(perPage)")
        ]

        var request = URLRequest(url: components.url!)
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        let (data, response) = try await session.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            throw StravaError.apiFailed("Failed to fetch activities")
        }

        return try jsonDecoder.decode([StravaActivity].self, from: data)
    }

    func fetchRunActivities(page: Int = 1, perPage: Int = 30) async throws -> [StravaActivity] {
        let activities = try await fetchActivities(page: page, perPage: perPage)
        return activities.filter {
            $0.sportType?.lowercased() == "run" || $0.type.lowercased() == "run"
        }
    }

    // MARK: - Upload Activity (Simple)

    func uploadActivity(
        name: String,
        sportType: String,
        startDate: Date,
        durationSeconds: Int,
        distanceMeters: Double,
        description: String? = nil
    ) async throws -> Int {
        let token = try await ensureValidToken()

        let dateFormatter = ISO8601DateFormatter()
        dateFormatter.timeZone = .current

        let activity = StravaActivityCreate(
            name: name,
            sportType: sportType,
            startDateLocal: dateFormatter.string(from: startDate),
            elapsedTime: durationSeconds,
            distance: Float(distanceMeters),
            description: description
        )

        let url = URL(string: "\(StravaConfig.apiBaseURL)activities")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(activity)

        let (data, response) = try await session.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 201 else {
            let errorBody = String(data: data, encoding: .utf8) ?? "Unknown error"
            throw StravaError.apiFailed("Upload failed: \(errorBody)")
        }

        let createdActivity = try jsonDecoder.decode(StravaActivity.self, from: data)
        return createdActivity.id
    }

    // MARK: - Upload Activity with GPX

    func uploadActivityWithGPX(
        gpxData: Data,
        name: String,
        sportType: String,
        description: String? = nil
    ) async throws -> Int {
        let token = try await ensureValidToken()

        let boundary = UUID().uuidString
        let url = URL(string: "\(StravaConfig.apiBaseURL)uploads")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")

        var body = Data()

        func appendField(_ name: String, _ value: String) {
            body.append("--\(boundary)\r\n".data(using: .utf8)!)
            body.append("Content-Disposition: form-data; name=\"\(name)\"\r\n\r\n".data(using: .utf8)!)
            body.append("\(value)\r\n".data(using: .utf8)!)
        }

        appendField("data_type", "gpx")
        appendField("name", name)
        appendField("sport_type", sportType)
        if let desc = description {
            appendField("description", desc)
        }

        // GPX file part
        body.append("--\(boundary)\r\n".data(using: .utf8)!)
        body.append("Content-Disposition: form-data; name=\"file\"; filename=\"activity.gpx\"\r\n".data(using: .utf8)!)
        body.append("Content-Type: application/gpx+xml\r\n\r\n".data(using: .utf8)!)
        body.append(gpxData)
        body.append("\r\n".data(using: .utf8)!)
        body.append("--\(boundary)--\r\n".data(using: .utf8)!)

        request.httpBody = body

        let (data, response) = try await session.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 201 else {
            let errorBody = String(data: data, encoding: .utf8) ?? "Unknown error"
            throw StravaError.apiFailed("GPX upload failed: \(errorBody)")
        }

        let uploadResponse = try jsonDecoder.decode(StravaUploadResponse.self, from: data)

        if let error = uploadResponse.error, !error.isEmpty {
            throw StravaError.apiFailed("Upload error: \(error)")
        }

        // Poll for activity ID
        return try await pollUploadStatus(uploadId: uploadResponse.id, token: token)
    }

    private func pollUploadStatus(uploadId: Int, token: String) async throws -> Int {
        for attempt in 0..<10 {
            try await Task.sleep(nanoseconds: 2_000_000_000) // 2 seconds

            let url = URL(string: "\(StravaConfig.apiBaseURL)uploads/\(uploadId)")!
            var request = URLRequest(url: url)
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

            let (data, response) = try await session.data(for: request)
            guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else { continue }

            let status = try jsonDecoder.decode(StravaUploadResponse.self, from: data)

            if let error = status.error, !error.isEmpty {
                throw StravaError.apiFailed("Strava processing error: \(error)")
            }

            if let activityId = status.activityId {
                return activityId
            }
            // Still processing, continue polling
        }

        // Timeout, return upload ID as fallback
        return uploadId
    }

    // MARK: - Upload Run with route

    func uploadRun(
        startTime: Date,
        durationSeconds: Int,
        distanceMeters: Double,
        routePoints: [RoutePoint],
        avgPaceSecondsPerKm: Double,
        avgHeartRate: Int?,
        calories: Int?,
        elevationGain: Double
    ) async throws -> Int {
        let name = generateRunName(startTime: startTime, distanceMeters: distanceMeters)
        let description = generateRunDescription(
            avgPaceSecondsPerKm: avgPaceSecondsPerKm,
            avgHeartRate: avgHeartRate,
            calories: calories,
            elevationGain: elevationGain
        )

        if !routePoints.isEmpty {
            let gpxData = generateGPX(routePoints: routePoints, name: name)
            do {
                return try await uploadActivityWithGPX(
                    gpxData: gpxData,
                    name: name,
                    sportType: "Run",
                    description: description
                )
            } catch {
                // Fallback to simple upload
                return try await uploadActivity(
                    name: name,
                    sportType: "Run",
                    startDate: startTime,
                    durationSeconds: durationSeconds,
                    distanceMeters: distanceMeters,
                    description: description
                )
            }
        }

        return try await uploadActivity(
            name: name,
            sportType: "Run",
            startDate: startTime,
            durationSeconds: durationSeconds,
            distanceMeters: distanceMeters,
            description: description
        )
    }

    // MARK: - GPX Generation

    func generateGPX(routePoints: [RoutePoint], name: String) -> Data {
        let dateFormatter = ISO8601DateFormatter()
        dateFormatter.formatOptions = [.withInternetDateTime]

        let pauseGapThreshold: TimeInterval = 30

        var xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <gpx version="1.1" creator="GoSteady" xmlns="http://www.topografix.com/GPX/1/1" xmlns:gpxtpx="http://www.garmin.com/xmlschemas/TrackPointExtension/v1">
          <metadata>
            <name>\(name)</name>
            <time>\(dateFormatter.string(from: routePoints.first?.timestamp ?? Date()))</time>
          </metadata>
          <trk>
            <name>\(name)</name>
            <type>running</type>

        """

        if !routePoints.isEmpty {
            xml += "    <trkseg>\n"
            xml += trackPointXML(routePoints[0], dateFormatter: dateFormatter)

            for i in 1..<routePoints.count {
                let gap = routePoints[i].timestamp.timeIntervalSince(routePoints[i - 1].timestamp)
                if gap > pauseGapThreshold {
                    xml += "    </trkseg>\n    <trkseg>\n"
                }
                xml += trackPointXML(routePoints[i], dateFormatter: dateFormatter)
            }

            xml += "    </trkseg>\n"
        }

        xml += "  </trk>\n</gpx>"

        return xml.data(using: .utf8)!
    }

    private func trackPointXML(_ point: RoutePoint, dateFormatter: ISO8601DateFormatter) -> String {
        var xml = "      <trkpt lat=\"\(point.latitude)\" lon=\"\(point.longitude)\">"

        if let alt = point.altitude, alt != 0 {
            xml += "<ele>\(alt)</ele>"
        }
        xml += "<time>\(dateFormatter.string(from: point.timestamp))</time>"

        if let hr = point.heartRate {
            xml += "<extensions><gpxtpx:TrackPointExtension><gpxtpx:hr>\(hr)</gpxtpx:hr></gpxtpx:TrackPointExtension></extensions>"
        }

        xml += "</trkpt>\n"
        return xml
    }

    // MARK: - Name/Description generators

    private func generateRunName(startTime: Date, distanceMeters: Double) -> String {
        let hour = Calendar.current.component(.hour, from: startTime)
        let timeOfDay: String
        switch hour {
        case 5...11: timeOfDay = "Morning"
        case 12...16: timeOfDay = "Afternoon"
        case 17...20: timeOfDay = "Evening"
        default: timeOfDay = "Night"
        }
        let distanceKm = distanceMeters / 1000.0
        return "\(timeOfDay) Run - \(String(format: "%.1f", distanceKm)) km"
    }

    private func generateRunDescription(
        avgPaceSecondsPerKm: Double,
        avgHeartRate: Int?,
        calories: Int?,
        elevationGain: Double
    ) -> String {
        var desc = "Tracked with GoSteady\n"
        let paceMin = Int(avgPaceSecondsPerKm) / 60
        let paceSec = Int(avgPaceSecondsPerKm) % 60
        desc += "Pace: \(paceMin):\(String(format: "%02d", paceSec))/km\n"

        if let hr = avgHeartRate {
            desc += "Avg HR: \(hr) bpm\n"
        }
        if let cal = calories {
            desc += "Calories: \(cal)\n"
        }
        if elevationGain > 0 {
            desc += "Elevation: +\(Int(elevationGain))m"
        }
        return desc
    }

    // MARK: - Disconnect

    func disconnect() {
        accessToken = nil
        refreshToken = nil
        expiresAt = 0
        athlete = nil
        athleteName = nil
        UserDefaults.standard.removeObject(forKey: "strava_athlete_name")
    }
}

// MARK: - Errors

enum StravaError: LocalizedError {
    case invalidURL
    case noCallbackURL
    case noAuthCode
    case authFailed
    case notAuthenticated
    case noRefreshToken
    case tokenRefreshFailed
    case apiFailed(String)

    var errorDescription: String? {
        switch self {
        case .invalidURL: return "Invalid Strava URL"
        case .noCallbackURL: return "No callback URL received"
        case .noAuthCode: return "No authorization code received"
        case .authFailed: return "Authentication failed"
        case .notAuthenticated: return "Not authenticated with Strava"
        case .noRefreshToken: return "No refresh token available"
        case .tokenRefreshFailed: return "Token refresh failed"
        case .apiFailed(let msg): return msg
        }
    }
}

// MARK: - Presentation Context Provider

private final class PresentationContextProvider: NSObject, ASWebAuthenticationPresentationContextProviding {
    let anchor: ASPresentationAnchor

    init(anchor: ASPresentationAnchor) {
        self.anchor = anchor
    }

    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        anchor
    }
}
