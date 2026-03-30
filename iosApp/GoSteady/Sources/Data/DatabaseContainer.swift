import Foundation
import SwiftData

enum DatabaseContainer {

    /// All SwiftData model types registered in the app container.
    static let allModelTypes: [any PersistentModel.Type] = [
        Run.self,
        Exercise.self,
        GymWorkout.self,
        WorkoutTemplate.self,
        ExerciseHistory.self,
        TrainingPlan.self,
        DailyNutrition.self,
        NutritionGoals.self,
        PersonalBest.self,
        UserProfile.self,
        UserGamification.self,
        Achievement.self,
        UserAchievement.self,
        DailyRings.self,
        XpTransaction.self,
        MoodEntry.self,
        MindfulnessSession.self,
        WellnessCheckin.self,
        SwimmingWorkout.self,
        SwimmingTrainingPlan.self,
        CyclingWorkout.self,
        CyclingTrainingPlan.self,
        HIITSession.self,
        BodyScan.self,
        ScheduledGymWorkout.self,
    ]

    /// Create the shared ModelContainer with all model types and optional migration plan.
    static func create(inMemory: Bool = false) throws -> ModelContainer {
        let schema = Schema(allModelTypes)

        let configuration: ModelConfiguration
        if inMemory {
            configuration = ModelConfiguration(
                "GoSteady",
                schema: schema,
                isStoredInMemoryOnly: true
            )
        } else {
            configuration = ModelConfiguration(
                "GoSteady",
                schema: schema,
                url: storeURL,
                allowsSave: true
            )
        }

        return try ModelContainer(for: schema, configurations: [configuration])
    }

    /// Create a preview / test container (in-memory).
    static func preview() -> ModelContainer {
        do {
            return try create(inMemory: true)
        } catch {
            fatalError("Failed to create preview ModelContainer: \(error)")
        }
    }

    // MARK: - Store URL

    private static var storeURL: URL {
        let appSupport = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
        let dir = appSupport.appendingPathComponent("GoSteady", isDirectory: true)
        if !FileManager.default.fileExists(atPath: dir.path) {
            try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        }
        return dir.appendingPathComponent("gosteady.store")
    }

    // MARK: - Migration Plan

    enum GoSteadyMigrationPlan: SchemaMigrationPlan {
        static var schemas: [VersionedSchema.Type] {
            [GoSteadySchemaV1.self]
        }

        static var stages: [MigrationStage] {
            []
        }
    }

    enum GoSteadySchemaV1: VersionedSchema {
        static var versionIdentifier: Schema.Version { Schema.Version(1, 0, 0) }

        static var models: [any PersistentModel.Type] {
            allModelTypes
        }
    }
}
