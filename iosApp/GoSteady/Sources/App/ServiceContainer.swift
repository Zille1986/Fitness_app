import SwiftUI
import SwiftData

// MARK: - Service Container (Singleton)

/// Central dependency injection container for the GoSteady app.
/// Holds singleton service instances and provides factory methods for
/// repositories that require a ModelContext.
///
/// Usage in SwiftUI views:
///   @Environment(\.serviceContainer) private var services
///   @Environment(\.locationService) private var location
///
/// Usage in view models:
///   let services = ServiceContainer.shared
///   let repo = services.makeRunRepository(context: modelContext)

@MainActor
final class ServiceContainer: ObservableObject {

    static let shared = ServiceContainer()

    // MARK: - Singleton Services

    let locationService: LocationService
    let healthKitService: HealthKitService
    let watchSyncService: WatchSyncService
    let stravaService: StravaService
    let geminiService: GeminiService
    let notificationService: NotificationService
    let audioService: AudioService

    // MARK: - Init

    private init() {
        self.locationService = LocationService()
        self.healthKitService = HealthKitService()
        self.watchSyncService = WatchSyncService()
        self.stravaService = StravaService()
        self.geminiService = GeminiService()
        self.notificationService = NotificationService()
        self.audioService = AudioService()
    }

    // MARK: - Repository Factories
    // Each repository wraps a ModelContext for SwiftData CRUD operations.
    // Create these from the @Environment(\.modelContext) in your views,
    // or pass a ModelContext from the ModelContainer in background tasks.

    func makeRunRepository(context: ModelContext) -> RunRepository {
        RunRepository(context: context)
    }

    func makeSwimmingRepository(context: ModelContext) -> SwimmingRepository {
        SwimmingRepository(context: context)
    }

    func makeCyclingRepository(context: ModelContext) -> CyclingRepository {
        CyclingRepository(context: context)
    }

    func makeGymRepository(context: ModelContext) -> GymRepository {
        GymRepository(context: context)
    }

    func makeNutritionRepository(context: ModelContext) -> NutritionRepository {
        NutritionRepository(context: context)
    }

    func makeExerciseRepository(context: ModelContext) -> ExerciseRepository {
        ExerciseRepository(context: context)
    }

    func makeTrainingPlanRepository(context: ModelContext) -> TrainingPlanRepository {
        TrainingPlanRepository(context: context)
    }

    func makeGamificationRepository(context: ModelContext) -> GamificationRepository {
        GamificationRepository(context: context)
    }
}

// MARK: - SwiftUI Environment Keys

private struct ServiceContainerKey: EnvironmentKey {
    @MainActor static let defaultValue: ServiceContainer = .shared
}

private struct LocationServiceKey: EnvironmentKey {
    @MainActor static let defaultValue: LocationService = ServiceContainer.shared.locationService
}

private struct HealthKitServiceKey: EnvironmentKey {
    @MainActor static let defaultValue: HealthKitService = ServiceContainer.shared.healthKitService
}

private struct WatchSyncServiceKey: EnvironmentKey {
    @MainActor static let defaultValue: WatchSyncService = ServiceContainer.shared.watchSyncService
}

private struct StravaServiceKey: EnvironmentKey {
    @MainActor static let defaultValue: StravaService = ServiceContainer.shared.stravaService
}

private struct GeminiServiceKey: EnvironmentKey {
    @MainActor static let defaultValue: GeminiService = ServiceContainer.shared.geminiService
}

private struct NotificationServiceKey: EnvironmentKey {
    @MainActor static let defaultValue: NotificationService = ServiceContainer.shared.notificationService
}

private struct AudioServiceKey: EnvironmentKey {
    @MainActor static let defaultValue: AudioService = ServiceContainer.shared.audioService
}

// MARK: - EnvironmentValues Extensions

extension EnvironmentValues {

    var serviceContainer: ServiceContainer {
        get { self[ServiceContainerKey.self] }
        set { self[ServiceContainerKey.self] = newValue }
    }

    var locationService: LocationService {
        get { self[LocationServiceKey.self] }
        set { self[LocationServiceKey.self] = newValue }
    }

    var healthKitService: HealthKitService {
        get { self[HealthKitServiceKey.self] }
        set { self[HealthKitServiceKey.self] = newValue }
    }

    var watchSyncService: WatchSyncService {
        get { self[WatchSyncServiceKey.self] }
        set { self[WatchSyncServiceKey.self] = newValue }
    }

    var stravaService: StravaService {
        get { self[StravaServiceKey.self] }
        set { self[StravaServiceKey.self] = newValue }
    }

    var geminiService: GeminiService {
        get { self[GeminiServiceKey.self] }
        set { self[GeminiServiceKey.self] = newValue }
    }

    var notificationService: NotificationService {
        get { self[NotificationServiceKey.self] }
        set { self[NotificationServiceKey.self] = newValue }
    }

    var audioService: AudioService {
        get { self[AudioServiceKey.self] }
        set { self[AudioServiceKey.self] = newValue }
    }
}
