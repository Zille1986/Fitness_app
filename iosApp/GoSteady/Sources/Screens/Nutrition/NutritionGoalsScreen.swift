import SwiftUI
import SwiftData

struct NutritionGoalsScreen: View {
    @Bindable var viewModel: NutritionViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var customCalories = ""
    @State private var customProtein = ""
    @State private var customCarbs = ""
    @State private var customFat = ""
    @State private var customWater = ""
    @State private var showSavedConfirmation = false

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // TDEE Calculator
                tdeeCalculatorSection

                // Preview
                if viewModel.calculatedTDEE > 0 {
                    caloriePreviewCard
                }

                // Goal Selection
                goalSelectionSection

                // Activity Level
                activityLevelSection

                // Custom Targets
                customTargetsSection

                // Save Button
                Button {
                    let cal = Int(customCalories) ?? viewModel.calculatedTDEE
                    let pro = Int(customProtein) ?? 150
                    let carb = Int(customCarbs) ?? 250
                    let fat = Int(customFat) ?? 65
                    let water = Int(customWater) ?? 2500
                    viewModel.saveGoals(calories: cal, protein: pro, carbs: carb, fat: fat, water: water)
                    showSavedConfirmation = true
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                        dismiss()
                    }
                } label: {
                    Text("Save Goals")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color(hex: "4CAF50"))
                        .foregroundStyle(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .padding(.top, 8)
            }
            .padding()
        }
        .background(Color(hex: "121212"))
        .navigationTitle("Nutrition Goals")
        .navigationBarTitleDisplayMode(.inline)
        .overlay {
            if showSavedConfirmation {
                VStack {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 48))
                        .foregroundStyle(Color(hex: "4CAF50"))
                    Text("Goals Saved!")
                        .font(.headline)
                        .foregroundStyle(.white)
                }
                .padding(32)
                .background(.ultraThinMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 20))
                .transition(.scale.combined(with: .opacity))
            }
        }
        .animation(.spring(response: 0.3), value: showSavedConfirmation)
        .onAppear {
            loadExistingGoals()
        }
    }

    // MARK: - TDEE Calculator

    private var tdeeCalculatorSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            sectionHeader(icon: "function", title: "TDEE Calculator")

            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Age")
                        .font(.caption.weight(.medium))
                        .foregroundStyle(.secondary)
                    TextField("30", text: $viewModel.tdeeAge)
                        .keyboardType(.numberPad)
                        .textFieldStyle(GoalTextFieldStyle())
                }

                VStack(alignment: .leading, spacing: 6) {
                    Text("Weight (kg)")
                        .font(.caption.weight(.medium))
                        .foregroundStyle(.secondary)
                    TextField("70", text: $viewModel.tdeeWeight)
                        .keyboardType(.decimalPad)
                        .textFieldStyle(GoalTextFieldStyle())
                }
            }

            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Height (cm)")
                        .font(.caption.weight(.medium))
                        .foregroundStyle(.secondary)
                    TextField("170", text: $viewModel.tdeeHeight)
                        .keyboardType(.numberPad)
                        .textFieldStyle(GoalTextFieldStyle())
                }

                VStack(alignment: .leading, spacing: 6) {
                    Text("Sex")
                        .font(.caption.weight(.medium))
                        .foregroundStyle(.secondary)
                    Picker("Sex", selection: $viewModel.tdeeMale) {
                        Text("Male").tag(true)
                        Text("Female").tag(false)
                    }
                    .pickerStyle(.segmented)
                }
            }

            Button {
                viewModel.calculateTDEE()
                customCalories = "\(viewModel.calculatedTDEE)"
                let macros = TDEECalculator.calculateMacros(
                    calories: viewModel.calculatedTDEE,
                    weightKg: Double(viewModel.tdeeWeight) ?? 70,
                    proteinPerKg: 1.6,
                    carbPercentage: 0.45,
                    fatPercentage: 0.25
                )
                customProtein = "\(macros.protein)"
                customCarbs = "\(macros.carbs)"
                customFat = "\(macros.fat)"
            } label: {
                HStack {
                    Image(systemName: "arrow.triangle.2.circlepath")
                    Text("Calculate")
                }
                .font(.subheadline.weight(.semibold))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(Color(hex: "2196F3"))
                .foregroundStyle(.white)
                .clipShape(RoundedRectangle(cornerRadius: 10))
            }
        }
        .padding(16)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var caloriePreviewCard: some View {
        VStack(spacing: 8) {
            Text("Your Daily Target")
                .font(.caption)
                .foregroundStyle(.secondary)
            Text("\(viewModel.calculatedTDEE)")
                .font(.system(size: 48, weight: .bold, design: .rounded))
                .foregroundStyle(Color(hex: "4CAF50"))
            Text("calories per day")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(24)
        .background(Color(hex: "4CAF50").opacity(0.1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Goal Selection

    private var goalSelectionSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            sectionHeader(icon: "target", title: "Your Goal")

            ForEach(NutritionGoalType.allCases, id: \.self) { goal in
                GoalOptionRow(
                    goal: goal,
                    isSelected: viewModel.tdeeGoal == goal,
                    onSelect: {
                        viewModel.tdeeGoal = goal
                        if viewModel.calculatedTDEE > 0 {
                            viewModel.calculateTDEE()
                            customCalories = "\(viewModel.calculatedTDEE)"
                        }
                    }
                )
            }
        }
        .padding(16)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Activity Level

    private var activityLevelSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            sectionHeader(icon: "figure.run", title: "Activity Level")

            ForEach(ActivityLevel.allCases, id: \.self) { level in
                ActivityLevelRow(
                    level: level,
                    isSelected: viewModel.tdeeActivityLevel == level,
                    onSelect: {
                        viewModel.tdeeActivityLevel = level
                        if viewModel.calculatedTDEE > 0 {
                            viewModel.calculateTDEE()
                            customCalories = "\(viewModel.calculatedTDEE)"
                        }
                    }
                )
            }
        }
        .padding(16)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Custom Targets

    private var customTargetsSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            sectionHeader(icon: "slider.horizontal.3", title: "Daily Targets")

            GoalTextField(title: "Calories", text: $customCalories, unit: "cal", recommendation: "Based on TDEE calculation")
            GoalTextField(title: "Protein", text: $customProtein, unit: "g", recommendation: "1.6-2.2g per kg for muscle building")
            GoalTextField(title: "Carbs", text: $customCarbs, unit: "g", recommendation: "45-65% of total calories")
            GoalTextField(title: "Fat", text: $customFat, unit: "g", recommendation: "20-35% of total calories")
            GoalTextField(title: "Water", text: $customWater, unit: "ml", recommendation: "~35ml per kg bodyweight")
        }
        .padding(16)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Helpers

    private func sectionHeader(icon: String, title: String) -> some View {
        HStack(spacing: 8) {
            Image(systemName: icon)
                .foregroundStyle(Color(hex: "4CAF50"))
            Text(title)
                .font(.headline)
                .foregroundStyle(.white)
        }
    }

    private func loadExistingGoals() {
        if let goals = viewModel.goals {
            viewModel.tdeeGoal = goals.goal
            viewModel.tdeeActivityLevel = goals.activityLevel
        }
        if let nutrition = viewModel.todayNutrition {
            customCalories = "\(nutrition.targetCalories)"
            customProtein = "\(nutrition.targetProteinGrams)"
            customCarbs = "\(nutrition.targetCarbsGrams)"
            customFat = "\(nutrition.targetFatGrams)"
            customWater = "\(nutrition.targetWaterMl)"
        }
    }
}

// MARK: - Sub-components

private struct GoalOptionRow: View {
    let goal: NutritionGoalType
    let isSelected: Bool
    let onSelect: () -> Void

    private var adjustmentDescription: String {
        switch goal {
        case .loseWeight: return "500 calorie deficit"
        case .loseWeightSlow: return "250 calorie deficit"
        case .maintain: return "Maintenance calories"
        case .gainMuscle: return "250 calorie surplus"
        case .bulk: return "500 calorie surplus"
        }
    }

    var body: some View {
        Button(action: onSelect) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(goal.displayName)
                        .font(.subheadline.weight(isSelected ? .bold : .regular))
                        .foregroundStyle(.white)
                    Text(adjustmentDescription)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(Color(hex: "4CAF50"))
                }
            }
            .padding(12)
            .background(isSelected ? Color(hex: "4CAF50").opacity(0.1) : Color.clear)
            .clipShape(RoundedRectangle(cornerRadius: 10))
        }
    }
}

private struct ActivityLevelRow: View {
    let level: ActivityLevel
    let isSelected: Bool
    let onSelect: () -> Void

    private var description: String {
        switch level {
        case .sedentary: return "Little to no exercise"
        case .lightlyActive: return "Light exercise 1-3 days/week"
        case .moderatelyActive: return "Moderate exercise 3-5 days/week"
        case .veryActive: return "Hard exercise 6-7 days/week"
        case .extremelyActive: return "Very hard exercise, physical job"
        }
    }

    var body: some View {
        Button(action: onSelect) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(level.displayName)
                        .font(.subheadline.weight(isSelected ? .bold : .regular))
                        .foregroundStyle(.white)
                    Text(description)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(Color(hex: "FF9800"))
                }
            }
            .padding(12)
            .background(isSelected ? Color(hex: "FF9800").opacity(0.1) : Color.clear)
            .clipShape(RoundedRectangle(cornerRadius: 10))
        }
    }
}

private struct GoalTextField: View {
    let title: String
    @Binding var text: String
    var unit: String = ""
    var recommendation: String = ""

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.caption.weight(.medium))
                .foregroundStyle(.secondary)
            HStack {
                TextField("0", text: $text)
                    .keyboardType(.numberPad)
                    .foregroundStyle(.white)
                if !unit.isEmpty {
                    Text(unit)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .padding(12)
            .background(Color.white.opacity(0.05))
            .clipShape(RoundedRectangle(cornerRadius: 10))

            if !recommendation.isEmpty {
                Text(recommendation)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
        }
    }
}

private struct GoalTextFieldStyle: TextFieldStyle {
    func _body(configuration: TextField<Self._Label>) -> some View {
        configuration
            .padding(12)
            .background(Color.white.opacity(0.05))
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .foregroundStyle(.white)
    }
}

#Preview {
    let container = try! ModelContainer(for: DailyNutrition.self, NutritionGoals.self, configurations: ModelConfiguration(isStoredInMemoryOnly: true))
    let vm = NutritionViewModel()
    vm.configure(modelContext: container.mainContext)
    return NavigationStack {
        NutritionGoalsScreen(viewModel: vm)
    }
    .preferredColorScheme(.dark)
}
