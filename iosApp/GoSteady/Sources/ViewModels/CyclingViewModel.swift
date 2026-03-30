import Foundation
import Observation

// MARK: - ViewModel-specific Data Models

struct CyclingWorkoutSummary: Identifiable {
    let id: String
    let startTime: Date
    let endTime: Date?
    let cyclingType: CyclingType
    let distanceMeters: Double
    let durationMillis: Int64
    let avgSpeedKmh: Double
    let maxSpeedKmh: Double
    let avgPowerWatts: Int?
    let maxPowerWatts: Int?
    let avgCadenceRpm: Int?
    let avgHeartRate: Int?
    let maxHeartRate: Int?
    let caloriesBurned: Int
    let elevationGainMeters: Double
    let elevationLossMeters: Double
    let notes: String?
    let routeCoordinates: [(lat: Double, lon: Double)]
    let splits: [CyclingSplitSummary]

    var distanceKm: Double { distanceMeters / 1000.0 }

    var distanceFormatted: String {
        String(format: "%.1f km", distanceKm)
    }

    var durationFormatted: String {
        CyclingViewModel.formatDuration(durationMillis)
    }

    var avgSpeedFormatted: String {
        String(format: "%.1f km/h", avgSpeedKmh)
    }

    var dateFormatted: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMM d"
        return formatter.string(from: startTime)
    }

    var fullDateFormatted: String {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        return formatter.string(from: startTime)
    }

    var elevationFormatted: String {
        String(format: "%.0f m", elevationGainMeters)
    }
}

struct CyclingSplitSummary: Identifiable {
    let id = UUID()
    let kilometer: Int
    let durationMillis: Int64
    let avgSpeedKmh: Double
    let avgPowerWatts: Int?
    let elevationChange: Double

    var durationFormatted: String {
        let totalSeconds = durationMillis / 1000
        let minutes = totalSeconds / 60
        let seconds = totalSeconds % 60
        return String(format: "%d:%02d", minutes, seconds)
    }
}

struct CyclingTrainingPlanSummary: Identifiable {
    let id: String
    let name: String
    let description: String
    let goalType: String
    let weekProgress: Int
    let totalWeeks: Int
    let isActive: Bool
}

// MARK: - ViewModel

@Observable
final class CyclingViewModel {
    var recentWorkouts: [CyclingWorkoutSummary] = []
    var weeklyStats: CyclingWeeklyStats?
    var activePlan: CyclingTrainingPlanSummary?
    var availablePlans: [CyclingTrainingPlanSummary] = []
    var isLoading = true
    var errorMessage: String?

    private var cyclingRepository: CyclingRepository?

    init(cyclingRepository: CyclingRepository? = nil) {
        self.cyclingRepository = cyclingRepository
        if cyclingRepository != nil {
            loadData()
        }
    }

    func setRepository(_ repository: CyclingRepository) {
        self.cyclingRepository = repository
        loadData()
    }

    func loadData() {
        guard let cyclingRepository else { return }
        isLoading = true
        errorMessage = nil

        let workouts = cyclingRepository.fetchRecentCompleted(limit: 10)
        self.recentWorkouts = workouts.map { w in
            CyclingWorkoutSummary(
                id: w.id.uuidString,
                startTime: w.startTime,
                endTime: w.endTime,
                cyclingType: w.cyclingType,
                distanceMeters: w.distanceMeters,
                durationMillis: w.durationMillis,
                avgSpeedKmh: w.avgSpeedKmh,
                maxSpeedKmh: w.maxSpeedKmh,
                avgPowerWatts: w.avgPowerWatts,
                maxPowerWatts: w.maxPowerWatts,
                avgCadenceRpm: w.avgCadenceRpm,
                avgHeartRate: w.avgHeartRate,
                maxHeartRate: w.maxHeartRate,
                caloriesBurned: w.caloriesBurned,
                elevationGainMeters: w.elevationGainMeters,
                elevationLossMeters: w.elevationLossMeters,
                notes: w.notes,
                routeCoordinates: w.routePoints.map { (lat: $0.latitude, lon: $0.longitude) },
                splits: w.splits.map { s in
                    CyclingSplitSummary(
                        kilometer: s.kilometer,
                        durationMillis: s.durationMillis,
                        avgSpeedKmh: s.avgSpeedKmh,
                        avgPowerWatts: s.avgPowerWatts,
                        elevationChange: s.elevationChange
                    )
                }
            )
        }
        self.weeklyStats = cyclingRepository.weeklyStats()

        if let plan = cyclingRepository.fetchActivePlan() {
            let totalWeeks = Set(plan.weeklySchedule.map(\.weekNumber)).count
            let completedWeeks = plan.weeklySchedule.filter(\.isCompleted).count
            let weekProgress = max(1, completedWeeks / max(1, plan.weeklySchedule.count / max(1, totalWeeks)))
            self.activePlan = CyclingTrainingPlanSummary(
                id: plan.id.uuidString,
                name: plan.name,
                description: plan.planDescription,
                goalType: plan.goalType.displayName,
                weekProgress: weekProgress,
                totalWeeks: totalWeeks,
                isActive: plan.isActive
            )
        } else {
            self.activePlan = nil
        }

        let allPlans = cyclingRepository.fetchAllPlans()
        self.availablePlans = allPlans.map { plan in
            let totalWeeks = Set(plan.weeklySchedule.map(\.weekNumber)).count
            return CyclingTrainingPlanSummary(
                id: plan.id.uuidString,
                name: plan.name,
                description: plan.planDescription,
                goalType: plan.goalType.displayName,
                weekProgress: 0,
                totalWeeks: totalWeeks,
                isActive: plan.isActive
            )
        }
        self.isLoading = false
    }

    func saveManualRide(distanceMeters: Double, durationMillis: Int64, notes: String?) {
        guard let cyclingRepository else { return }
        let speedKmh = durationMillis > 0 ? (distanceMeters / 1000.0) / (Double(durationMillis) / 3600000.0) : 0.0
        let workout = CyclingWorkout(
            startTime: Date().addingTimeInterval(-Double(durationMillis) / 1000.0),
            endTime: Date(),
            cyclingType: .outdoor,
            distanceMeters: distanceMeters,
            durationMillis: durationMillis,
            avgSpeedKmh: speedKmh,
            maxSpeedKmh: speedKmh,
            caloriesBurned: Int(Double(durationMillis) / 60000.0 * 8.0),
            notes: notes,
            isCompleted: true
        )
        cyclingRepository.insert(workout)
        loadData()
    }

    func deleteWorkout(_ workout: CyclingWorkoutSummary) {
        guard let cyclingRepository else { return }
        if let id = UUID(uuidString: workout.id),
           let entity = cyclingRepository.fetchWorkoutById(id) {
            cyclingRepository.delete(entity)
            recentWorkouts.removeAll { $0.id == workout.id }
        }
    }

    static func formatDuration(_ millis: Int64) -> String {
        let totalSeconds = millis / 1000
        let hours = totalSeconds / 3600
        let minutes = (totalSeconds % 3600) / 60
        let seconds = totalSeconds % 60
        if hours > 0 {
            return String(format: "%d:%02d:%02d", hours, minutes, seconds)
        }
        return String(format: "%d:%02d", minutes, seconds)
    }
}

