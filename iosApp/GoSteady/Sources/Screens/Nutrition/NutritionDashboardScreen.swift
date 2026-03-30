import SwiftUI
import Charts

struct NutritionDashboardScreen: View {
    @Environment(\.modelContext) private var modelContext
    @State private var viewModel = NutritionViewModel()
    @State private var showQuickLog = false
    @State private var showWaterLog = false
    @State private var showFabMenu = false

    var body: some View {
        NavigationStack {
            ZStack(alignment: .bottomTrailing) {
                ScrollView {
                    if viewModel.isLoading {
                        loadingView
                    } else if let error = viewModel.errorMessage {
                        errorView(error)
                    } else {
                        contentView
                    }
                }
                .background(Color(hex: "121212"))

                fabButton
            }
            .navigationTitle("Nutrition")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    NavigationLink(destination: NutritionGoalsScreen(viewModel: viewModel)) {
                        Image(systemName: "gearshape")
                            .foregroundStyle(Color(hex: "4CAF50"))
                    }
                }
            }
        }
        .onAppear {
            viewModel.configure(modelContext: modelContext)
        }
        .sheet(isPresented: $showQuickLog) {
            QuickLogSheet(viewModel: viewModel)
                .presentationDetents([.medium])
        }
        .sheet(isPresented: $showWaterLog) {
            WaterLogSheet(viewModel: viewModel)
                .presentationDetents([.height(220)])
        }
    }

    // MARK: - Content

    private var contentView: some View {
        LazyVStack(spacing: 16) {
            if let nutrition = viewModel.todayNutrition {
                // Day Type Banner
                DayTypeBanner(dayType: viewModel.dayType)

                // Calorie Overview
                CalorieOverviewCard(nutrition: nutrition, onAddWater: { showWaterLog = true })

                // Macro Progress
                MacroProgressCard(nutrition: nutrition)

                // Tips
                if !viewModel.tips.isEmpty {
                    TipsCard(tips: viewModel.tips)
                }

                // Meal Suggestions
                if !viewModel.mealSuggestions.isEmpty {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Suggested for \(viewModel.currentMealType.displayName)")
                            .font(.headline)
                            .foregroundStyle(.white)
                            .padding(.horizontal)

                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 12) {
                                ForEach(viewModel.mealSuggestions) { suggestion in
                                    MealSuggestionCard(suggestion: suggestion) {
                                        viewModel.logMealFromSuggestion(suggestion)
                                    }
                                }
                            }
                            .padding(.horizontal)
                        }
                    }
                }

                // Today's Meals
                let sortedMeals = nutrition.meals.sorted { $0.timestamp > $1.timestamp }
                if !sortedMeals.isEmpty {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Today's Meals")
                            .font(.headline)
                            .foregroundStyle(.white)
                            .padding(.horizontal)

                        ForEach(sortedMeals) { meal in
                            MealEntryRow(meal: meal) {
                                viewModel.removeMeal(meal.id)
                            }
                            .padding(.horizontal)
                        }
                    }
                }

                // Navigation links
                VStack(spacing: 12) {
                    NavigationLink(destination: MealLogScreen(viewModel: viewModel)) {
                        FeatureRow(icon: "plus.circle.fill", title: "Log a Meal", subtitle: "Search or enter manually")
                    }
                    NavigationLink(destination: MealSuggestionsScreen(viewModel: viewModel)) {
                        FeatureRow(icon: "sparkles", title: "AI Meal Suggestions", subtitle: "Get personalized recommendations")
                    }
                }
                .padding(.horizontal)

                Spacer(minLength: 100)
            }
        }
        .padding(.top)
    }

    private var loadingView: some View {
        VStack(spacing: 16) {
            Spacer(minLength: 200)
            ProgressView()
                .scaleEffect(1.5)
                .tint(Color(hex: "4CAF50"))
            Text("Loading nutrition data...")
                .foregroundStyle(.secondary)
            Spacer()
        }
    }

    private func errorView(_ message: String) -> some View {
        VStack(spacing: 16) {
            Spacer(minLength: 200)
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 48))
                .foregroundStyle(Color(hex: "F44336"))
            Text("Something went wrong")
                .font(.headline)
                .foregroundStyle(.white)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Button("Retry") {
                viewModel.loadData()
            }
            .buttonStyle(.borderedProminent)
            .tint(Color(hex: "4CAF50"))
            Spacer()
        }
        .padding()
    }

    private var fabButton: some View {
        VStack(alignment: .trailing, spacing: 12) {
            if showFabMenu {
                NavigationLink(destination: MealLogScreen(viewModel: viewModel)) {
                    HStack {
                        Image(systemName: "camera.fill")
                        Text("Scan Food")
                    }
                    .font(.subheadline.weight(.semibold))
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(Color(hex: "FF9800"))
                    .foregroundStyle(.white)
                    .clipShape(Capsule())
                }
                .transition(.move(edge: .bottom).combined(with: .opacity))

                Button {
                    showFabMenu = false
                    showQuickLog = true
                } label: {
                    HStack {
                        Image(systemName: "pencil")
                        Text("Quick Log")
                    }
                    .font(.subheadline.weight(.semibold))
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(Color(hex: "2196F3"))
                    .foregroundStyle(.white)
                    .clipShape(Capsule())
                }
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }

            Button {
                withAnimation(.spring(response: 0.3)) {
                    showFabMenu.toggle()
                }
            } label: {
                Image(systemName: showFabMenu ? "xmark" : "plus")
                    .font(.title2.weight(.bold))
                    .frame(width: 56, height: 56)
                    .background(Color(hex: "4CAF50"))
                    .foregroundStyle(.white)
                    .clipShape(Circle())
                    .shadow(color: Color(hex: "4CAF50").opacity(0.4), radius: 8, y: 4)
            }
        }
        .padding(.trailing, 20)
        .padding(.bottom, 20)
    }
}

// MARK: - Day Type Banner

private struct DayTypeBanner: View {
    let dayType: DayType

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: dayType.icon)
                .font(.title2)
                .foregroundStyle(Color(hex: dayType.color))
                .frame(width: 40)

            VStack(alignment: .leading, spacing: 2) {
                Text(dayType.displayName)
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(Color(hex: dayType.color))
                Text("Nutrition adjusted for your activity level")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer()
        }
        .padding()
        .background(Color(hex: dayType.color).opacity(0.15))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal)
    }
}

// MARK: - Calorie Overview Card

private struct CalorieOverviewCard: View {
    let nutrition: DailyNutrition
    let onAddWater: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Remaining")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text("\(nutrition.remainingCalories)")
                        .font(.system(size: 40, weight: .bold, design: .rounded))
                        .foregroundStyle(.white)
                    Text("calories")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                Spacer()

                CalorieRing(progress: CGFloat(nutrition.calorieProgress), size: 100)
            }

            HStack(spacing: 0) {
                CalorieBreakdownItem(icon: "fork.knife", value: "\(nutrition.consumedCalories)", label: "Eaten", unit: "cal")
                Spacer()
                CalorieBreakdownItem(icon: "flame", value: "\(nutrition.burnedCalories)", label: "Burned", unit: "cal")
                Spacer()
                CalorieBreakdownItem(icon: "figure.walk", value: "\(nutrition.stepCount)", label: "Steps", unit: "")
            }

            // Water tracker
            Button(action: onAddWater) {
                HStack {
                    Image(systemName: "drop.fill")
                        .foregroundStyle(Color(hex: "2196F3"))
                    VStack(alignment: .leading, spacing: 4) {
                        Text("\(nutrition.waterMl) / \(nutrition.targetWaterMl) ml")
                            .font(.subheadline.weight(.medium))
                            .foregroundStyle(.white)
                        GeometryReader { geo in
                            ZStack(alignment: .leading) {
                                Capsule()
                                    .fill(Color.white.opacity(0.2))
                                    .frame(height: 6)
                                Capsule()
                                    .fill(Color(hex: "2196F3"))
                                    .frame(width: geo.size.width * CGFloat(min(nutrition.waterProgress, 1.0)), height: 6)
                            }
                        }
                        .frame(height: 6)
                    }
                    Image(systemName: "plus")
                        .foregroundStyle(.white.opacity(0.7))
                }
                .padding(12)
                .background(Color.white.opacity(0.1))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .buttonStyle(.plain)
        }
        .padding(20)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
    }
}

private struct CalorieRing: View {
    let progress: CGFloat
    let size: CGFloat

    var body: some View {
        ZStack {
            Circle()
                .stroke(Color.white.opacity(0.2), lineWidth: 8)

            Circle()
                .trim(from: 0, to: min(progress, 1.0))
                .stroke(
                    progress > 1.0 ? Color(hex: "E57373") : Color(hex: "4CAF50"),
                    style: StrokeStyle(lineWidth: 8, lineCap: .round)
                )
                .rotationEffect(.degrees(-90))
                .animation(.easeInOut(duration: 1.0), value: progress)

            Text("\(Int(progress * 100))%")
                .font(.title2.weight(.bold))
                .foregroundStyle(.white)
        }
        .frame(width: size, height: size)
    }
}

private struct CalorieBreakdownItem: View {
    let icon: String
    let value: String
    let label: String
    let unit: String

    var body: some View {
        VStack(spacing: 4) {
            Image(systemName: icon)
                .font(.caption)
                .foregroundStyle(.secondary)
            Text(unit.isEmpty ? value : "\(value) \(unit)")
                .font(.subheadline.weight(.bold))
                .foregroundStyle(.white)
            Text(label)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
    }
}

// MARK: - Macro Progress Card

private struct MacroProgressCard: View {
    let nutrition: DailyNutrition

    var body: some View {
        VStack(spacing: 16) {
            Text("Macros")
                .font(.headline)
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity, alignment: .leading)

            HStack(spacing: 20) {
                MacroCircle(
                    label: "Protein",
                    consumed: nutrition.consumedProteinGrams,
                    target: nutrition.targetProteinGrams,
                    progress: CGFloat(nutrition.proteinProgress),
                    colorHex: "E91E63"
                )
                MacroCircle(
                    label: "Carbs",
                    consumed: nutrition.consumedCarbsGrams,
                    target: nutrition.targetCarbsGrams,
                    progress: CGFloat(nutrition.carbsProgress),
                    colorHex: "2196F3"
                )
                MacroCircle(
                    label: "Fat",
                    consumed: nutrition.consumedFatGrams,
                    target: nutrition.targetFatGrams,
                    progress: CGFloat(nutrition.fatProgress),
                    colorHex: "FF9800"
                )
            }
        }
        .padding(16)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
    }
}

private struct MacroCircle: View {
    let label: String
    let consumed: Int
    let target: Int
    let progress: CGFloat
    let colorHex: String

    var body: some View {
        VStack(spacing: 4) {
            ZStack {
                Circle()
                    .stroke(Color(hex: colorHex).opacity(0.2), lineWidth: 6)
                Circle()
                    .trim(from: 0, to: min(progress, 1.0))
                    .stroke(Color(hex: colorHex), style: StrokeStyle(lineWidth: 6, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .animation(.easeInOut(duration: 0.8), value: progress)
                Text("\(consumed)")
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(.white)
            }
            .frame(width: 70, height: 70)

            Text(label)
                .font(.caption.weight(.medium))
                .foregroundStyle(Color(hex: colorHex))
            Text("/ \(target)g")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Tips Card

private struct TipsCard: View {
    let tips: [String]

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: "lightbulb.fill")
                    .foregroundStyle(Color(hex: "FF9800"))
                Text("Tips for Today")
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(Color(hex: "FF9800"))
            }
            ForEach(tips, id: \.self) { tip in
                Text(tip)
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.8))
                    .padding(.vertical, 2)
            }
        }
        .padding(16)
        .background(Color(hex: "FF9800").opacity(0.1))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal)
    }
}

// MARK: - Meal Suggestion Card

private struct MealSuggestionCard: View {
    let suggestion: MealSuggestion
    let onLog: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(suggestion.name)
                .font(.subheadline.weight(.bold))
                .foregroundStyle(.white)
                .lineLimit(1)
            Text(suggestion.suggestionDescription)
                .font(.caption)
                .foregroundStyle(.secondary)
                .lineLimit(2)

            HStack {
                Text("\(suggestion.calories) cal")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Color(hex: "4CAF50"))
                Spacer()
                Text("\(Int(suggestion.proteinGrams))g protein")
                    .font(.caption2)
                    .foregroundStyle(Color(hex: "E91E63"))
            }

            if !suggestion.tags.isEmpty {
                HStack(spacing: 4) {
                    ForEach(suggestion.tags.prefix(2), id: \.self) { tag in
                        Text(tag)
                            .font(.caption2)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 2)
                            .background(Color.white.opacity(0.1))
                            .clipShape(Capsule())
                            .foregroundStyle(.secondary)
                    }
                }
            }

            Button(action: onLog) {
                HStack {
                    Image(systemName: "plus")
                    Text("Log")
                }
                .font(.caption.weight(.semibold))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
                .background(Color(hex: "4CAF50"))
                .foregroundStyle(.white)
                .clipShape(RoundedRectangle(cornerRadius: 8))
            }
        }
        .padding(12)
        .frame(width: 200)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

// MARK: - Meal Entry Row

private struct MealEntryRow: View {
    let meal: MealEntry
    let onRemove: () -> Void
    @State private var expanded = false

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(meal.name)
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(.white)
                    Text(meal.mealType.displayName)
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Text("\(meal.calories) cal")
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(Color(hex: "4CAF50"))
            }
            .contentShape(Rectangle())
            .onTapGesture { withAnimation(.easeInOut(duration: 0.2)) { expanded.toggle() } }

            if expanded {
                VStack(spacing: 8) {
                    HStack(spacing: 12) {
                        MacroChip(text: "P: \(Int(meal.proteinGrams))g", colorHex: "E91E63")
                        MacroChip(text: "C: \(Int(meal.carbsGrams))g", colorHex: "2196F3")
                        MacroChip(text: "F: \(Int(meal.fatGrams))g", colorHex: "FF9800")
                    }
                    Button(action: onRemove) {
                        HStack {
                            Image(systemName: "trash")
                            Text("Remove")
                        }
                        .font(.caption)
                        .foregroundStyle(Color(hex: "F44336"))
                    }
                }
                .padding(.top, 12)
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .padding(16)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

private struct MacroChip: View {
    let text: String
    let colorHex: String

    var body: some View {
        Text(text)
            .font(.caption2.weight(.medium))
            .foregroundStyle(Color(hex: colorHex))
            .padding(.horizontal, 12)
            .padding(.vertical, 4)
            .background(Color(hex: colorHex).opacity(0.15))
            .clipShape(Capsule())
    }
}

// MARK: - Feature Row

private struct FeatureRow: View {
    let icon: String
    let title: String
    let subtitle: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundStyle(Color(hex: "4CAF50"))
                .frame(width: 40)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(.white)
                Text(subtitle)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding()
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

// MARK: - Quick Log Sheet

private struct QuickLogSheet: View {
    @Bindable var viewModel: NutritionViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var calories = ""
    @State private var protein = ""
    @State private var carbs = ""
    @State private var fat = ""

    var body: some View {
        NavigationStack {
            Form {
                TextField("Meal Name", text: $name)
                TextField("Calories", text: $calories)
                    .keyboardType(.numberPad)
                HStack {
                    TextField("Protein (g)", text: $protein)
                        .keyboardType(.numberPad)
                    TextField("Carbs (g)", text: $carbs)
                        .keyboardType(.numberPad)
                }
                TextField("Fat (g)", text: $fat)
                    .keyboardType(.numberPad)
            }
            .navigationTitle("Quick Log")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Log") {
                        viewModel.logQuickMeal(
                            name: name,
                            calories: Int(calories) ?? 0,
                            protein: Int(protein) ?? 0,
                            carbs: Int(carbs) ?? 0,
                            fat: Int(fat) ?? 0
                        )
                        dismiss()
                    }
                    .disabled(calories.isEmpty)
                }
            }
        }
    }
}

// MARK: - Water Log Sheet

private struct WaterLogSheet: View {
    @Bindable var viewModel: NutritionViewModel
    @Environment(\.dismiss) private var dismiss

    private let presets = [250, 500, 750, 1000]

    var body: some View {
        VStack(spacing: 20) {
            Text("Add Water")
                .font(.headline)
            HStack(spacing: 12) {
                ForEach(presets, id: \.self) { ml in
                    Button {
                        viewModel.addWater(ml)
                        dismiss()
                    } label: {
                        Text("\(ml)ml")
                            .font(.subheadline.weight(.medium))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(Color(hex: "2196F3").opacity(0.2))
                            .foregroundStyle(Color(hex: "2196F3"))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                }
            }
            Button("Cancel") { dismiss() }
                .foregroundStyle(.secondary)
        }
        .padding(24)
    }
}

#Preview {
    NutritionDashboardScreen()
        .preferredColorScheme(.dark)
}
