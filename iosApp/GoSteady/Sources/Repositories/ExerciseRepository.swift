import Foundation
import SwiftData

final class ExerciseRepository {
    private let context: ModelContext

    init(context: ModelContext) {
        self.context = context
    }

    // MARK: - Fetch

    func fetchAll() -> [Exercise] {
        let descriptor = FetchDescriptor<Exercise>(sortBy: [SortDescriptor(\.name)])
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchById(_ id: UUID) -> Exercise? {
        let descriptor = FetchDescriptor<Exercise>(predicate: #Predicate { $0.id == id })
        return try? context.fetch(descriptor).first
    }

    func fetchByMuscleGroup(_ muscleGroup: MuscleGroup) -> [Exercise] {
        let descriptor = FetchDescriptor<Exercise>(
            predicate: #Predicate { $0.muscleGroup == muscleGroup },
            sortBy: [SortDescriptor(\.name)]
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchByEquipment(_ equipment: Equipment) -> [Exercise] {
        let descriptor = FetchDescriptor<Exercise>(
            predicate: #Predicate { $0.equipment == equipment },
            sortBy: [SortDescriptor(\.name)]
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func search(query: String) -> [Exercise] {
        let lowered = query.lowercased()
        let all = fetchAll()
        return all.filter { $0.name.lowercased().contains(lowered) }
    }

    func fetchCustomExercises() -> [Exercise] {
        let descriptor = FetchDescriptor<Exercise>(
            predicate: #Predicate { $0.isCustom == true },
            sortBy: [SortDescriptor(\.name)]
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchByType(_ type: ExerciseType) -> [Exercise] {
        let descriptor = FetchDescriptor<Exercise>(
            predicate: #Predicate { $0.exerciseType == type },
            sortBy: [SortDescriptor(\.name)]
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchByDifficulty(_ difficulty: Difficulty) -> [Exercise] {
        let descriptor = FetchDescriptor<Exercise>(
            predicate: #Predicate { $0.difficulty == difficulty },
            sortBy: [SortDescriptor(\.name)]
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func exerciseCount() -> Int {
        let descriptor = FetchDescriptor<Exercise>()
        return (try? context.fetchCount(descriptor)) ?? 0
    }

    // MARK: - Create / Update / Delete

    func insert(_ exercise: Exercise) {
        context.insert(exercise)
        try? context.save()
    }

    func insertMany(_ exercises: [Exercise]) {
        for exercise in exercises {
            context.insert(exercise)
        }
        try? context.save()
    }

    func update(_ exercise: Exercise) {
        try? context.save()
    }

    func delete(_ exercise: Exercise) {
        context.delete(exercise)
        try? context.save()
    }

    // MARK: - Default Library Seeding

    func initializeDefaultExercises() {
        guard exerciseCount() == 0 else { return }
        let defaults = ExerciseLibrary.defaultExercises()
        insertMany(defaults)
    }
}

// MARK: - Exercise Library

enum ExerciseLibrary {
    static func defaultExercises() -> [Exercise] {
        [
            // CHEST
            Exercise(name: "Barbell Bench Press", exerciseDescription: "Classic compound chest exercise",
                     muscleGroup: .chest, secondaryMuscleGroups: [.triceps, .shoulders],
                     equipment: .barbell, exerciseType: .compound, videoFileName: "barbell-bench-press"),
            Exercise(name: "Incline Dumbbell Press", exerciseDescription: "Upper chest focused press",
                     muscleGroup: .chest, secondaryMuscleGroups: [.triceps, .shoulders],
                     equipment: .dumbbell, exerciseType: .compound, videoFileName: "incline-dumbbell-press"),
            Exercise(name: "Dumbbell Flyes", exerciseDescription: "Chest isolation exercise",
                     muscleGroup: .chest, equipment: .dumbbell, exerciseType: .isolation, videoFileName: "dumbbell-flyes"),
            Exercise(name: "Cable Crossover", exerciseDescription: "Cable chest isolation",
                     muscleGroup: .chest, equipment: .cable, exerciseType: .isolation, videoFileName: "cable-crossover"),
            Exercise(name: "Push-ups", exerciseDescription: "Bodyweight chest exercise",
                     muscleGroup: .chest, secondaryMuscleGroups: [.triceps, .shoulders],
                     equipment: .bodyweight, exerciseType: .compound, difficulty: .beginner, videoFileName: "push-ups"),
            // BACK
            Exercise(name: "Barbell Row", exerciseDescription: "Compound back exercise",
                     muscleGroup: .back, secondaryMuscleGroups: [.biceps, .lats],
                     equipment: .barbell, exerciseType: .compound, videoFileName: "barbell-row"),
            Exercise(name: "Pull-ups", exerciseDescription: "Bodyweight back exercise",
                     muscleGroup: .back, secondaryMuscleGroups: [.biceps, .lats],
                     equipment: .pullUpBar, exerciseType: .compound, videoFileName: "pull-ups"),
            Exercise(name: "Lat Pulldown", exerciseDescription: "Cable lat exercise",
                     muscleGroup: .lats, secondaryMuscleGroups: [.biceps],
                     equipment: .cable, exerciseType: .compound, videoFileName: "lat-pulldown"),
            Exercise(name: "Seated Cable Row", exerciseDescription: "Cable row for mid-back",
                     muscleGroup: .back, secondaryMuscleGroups: [.biceps],
                     equipment: .cable, exerciseType: .compound, videoFileName: "seated-cable-row"),
            // SHOULDERS
            Exercise(name: "Overhead Press", exerciseDescription: "Compound shoulder press",
                     muscleGroup: .shoulders, secondaryMuscleGroups: [.triceps],
                     equipment: .barbell, exerciseType: .compound, videoFileName: "overhead-press"),
            Exercise(name: "Lateral Raises", exerciseDescription: "Side deltoid isolation",
                     muscleGroup: .shoulders, equipment: .dumbbell, exerciseType: .isolation, videoFileName: "lateral-raises"),
            Exercise(name: "Face Pulls", exerciseDescription: "Rear delt and rotator cuff",
                     muscleGroup: .shoulders, secondaryMuscleGroups: [.traps],
                     equipment: .cable, exerciseType: .isolation, videoFileName: "face-pulls"),
            // BICEPS
            Exercise(name: "Barbell Curl", exerciseDescription: "Classic bicep exercise",
                     muscleGroup: .biceps, equipment: .barbell, exerciseType: .isolation, videoFileName: "barbell-curl"),
            Exercise(name: "Dumbbell Curl", exerciseDescription: "Dumbbell bicep curl",
                     muscleGroup: .biceps, equipment: .dumbbell, exerciseType: .isolation, videoFileName: "dumbbell-curl"),
            Exercise(name: "Hammer Curl", exerciseDescription: "Neutral grip curl for brachialis",
                     muscleGroup: .biceps, secondaryMuscleGroups: [.forearms],
                     equipment: .dumbbell, exerciseType: .isolation, videoFileName: "hammer-curl"),
            // TRICEPS
            Exercise(name: "Tricep Pushdown", exerciseDescription: "Cable tricep isolation",
                     muscleGroup: .triceps, equipment: .cable, exerciseType: .isolation, videoFileName: "tricep-pushdown"),
            Exercise(name: "Skull Crushers", exerciseDescription: "Lying tricep extension",
                     muscleGroup: .triceps, equipment: .ezBar, exerciseType: .isolation, videoFileName: "skull-crushers"),
            Exercise(name: "Dips", exerciseDescription: "Bodyweight tricep and chest exercise",
                     muscleGroup: .triceps, secondaryMuscleGroups: [.chest],
                     equipment: .dipBars, exerciseType: .compound, videoFileName: "dips"),
            // LEGS
            Exercise(name: "Barbell Squat", exerciseDescription: "King of leg exercises",
                     muscleGroup: .quads, secondaryMuscleGroups: [.glutes, .hamstrings],
                     equipment: .barbell, exerciseType: .compound, videoFileName: "barbell-squat"),
            Exercise(name: "Romanian Deadlift", exerciseDescription: "Hamstring and glute focused deadlift",
                     muscleGroup: .hamstrings, secondaryMuscleGroups: [.glutes, .lowerBack],
                     equipment: .barbell, exerciseType: .compound, videoFileName: "romanian-deadlift"),
            Exercise(name: "Leg Press", exerciseDescription: "Machine quad exercise",
                     muscleGroup: .quads, secondaryMuscleGroups: [.glutes],
                     equipment: .machine, exerciseType: .compound, videoFileName: "leg-press"),
            Exercise(name: "Leg Curl", exerciseDescription: "Machine hamstring isolation",
                     muscleGroup: .hamstrings, equipment: .machine, exerciseType: .isolation, videoFileName: "leg-curl"),
            Exercise(name: "Leg Extension", exerciseDescription: "Machine quad isolation",
                     muscleGroup: .quads, equipment: .machine, exerciseType: .isolation, videoFileName: "leg-extension"),
            Exercise(name: "Calf Raises", exerciseDescription: "Standing calf raise",
                     muscleGroup: .calves, equipment: .machine, exerciseType: .isolation, videoFileName: "calf-raises"),
            Exercise(name: "Hip Thrust", exerciseDescription: "Barbell glute exercise",
                     muscleGroup: .glutes, secondaryMuscleGroups: [.hamstrings],
                     equipment: .barbell, exerciseType: .compound, videoFileName: "hip-thrust"),
            // ABS
            Exercise(name: "Plank", exerciseDescription: "Core stabilization hold",
                     muscleGroup: .abs, equipment: .bodyweight, exerciseType: .isolation, difficulty: .beginner, videoFileName: "plank"),
            Exercise(name: "Cable Crunch", exerciseDescription: "Weighted ab exercise",
                     muscleGroup: .abs, equipment: .cable, exerciseType: .isolation, videoFileName: "cable-crunch"),
            Exercise(name: "Hanging Leg Raise", exerciseDescription: "Advanced ab exercise",
                     muscleGroup: .abs, secondaryMuscleGroups: [.hipFlexors],
                     equipment: .pullUpBar, exerciseType: .isolation, difficulty: .advanced, videoFileName: "hanging-leg-raise"),
            // COMPOUND
            Exercise(name: "Deadlift", exerciseDescription: "Full body compound lift",
                     muscleGroup: .back, secondaryMuscleGroups: [.hamstrings, .glutes, .lowerBack, .traps],
                     equipment: .barbell, exerciseType: .compound, videoFileName: "deadlift"),
        ]
    }
}
