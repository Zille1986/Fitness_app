import SwiftUI
import SwiftData

struct MealSuggestionsScreen: View {
    @Bindable var viewModel: NutritionViewModel
    @State private var selectedMealType: MealType = .breakfast

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Remaining macros summary
                if let nutrition = viewModel.todayNutrition {
                    remainingMacrosCard(nutrition)
                }

                // Meal type selector
                mealTypeSelector

                // Dietary filter toggles
                dietaryFilters

                // Suggestions
                let suggestions = viewModel.filteredSuggestions(for: selectedMealType)
                if suggestions.isEmpty {
                    emptyStateView
                } else {
                    LazyVStack(spacing: 12) {
                        ForEach(suggestions) { suggestion in
                            SuggestionDetailCard(suggestion: suggestion) {
                                viewModel.logMealFromSuggestion(suggestion)
                            }
                        }
                    }
                }
            }
            .padding()
        }
        .background(Color(hex: "121212"))
        .navigationTitle("Meal Suggestions")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            selectedMealType = viewModel.currentMealTypeAutoDetected
        }
    }

    // MARK: - Remaining Macros

    private func remainingMacrosCard(_ nutrition: DailyNutrition) -> some View {
        VStack(spacing: 12) {
            Text("Remaining Today")
                .font(.subheadline.weight(.bold))
                .foregroundStyle(.white)

            HStack(spacing: 16) {
                remainingItem(
                    label: "Calories",
                    value: nutrition.remainingCalories,
                    unit: "cal",
                    colorHex: "4CAF50"
                )
                remainingItem(
                    label: "Protein",
                    value: nutrition.targetProteinGrams - nutrition.consumedProteinGrams,
                    unit: "g",
                    colorHex: "E91E63"
                )
                remainingItem(
                    label: "Carbs",
                    value: nutrition.targetCarbsGrams - nutrition.consumedCarbsGrams,
                    unit: "g",
                    colorHex: "2196F3"
                )
                remainingItem(
                    label: "Fat",
                    value: nutrition.targetFatGrams - nutrition.consumedFatGrams,
                    unit: "g",
                    colorHex: "FF9800"
                )
            }
        }
        .padding(16)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func remainingItem(label: String, value: Int, unit: String, colorHex: String) -> some View {
        VStack(spacing: 4) {
            Text("\(max(0, value))")
                .font(.headline.weight(.bold))
                .foregroundStyle(Color(hex: colorHex))
            Text("\(unit)")
                .font(.caption2)
                .foregroundStyle(Color(hex: colorHex).opacity(0.7))
            Text(label)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Meal Type Selector

    private var mealTypeSelector: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(MealType.allCases, id: \.self) { type in
                    Button {
                        withAnimation(.easeInOut(duration: 0.2)) {
                            selectedMealType = type
                        }
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: type.icon)
                                .font(.caption)
                            Text(type.displayName)
                                .font(.caption.weight(.medium))
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 10)
                        .background(
                            selectedMealType == type
                                ? Color(hex: "4CAF50")
                                : Color(hex: "1E1E1E")
                        )
                        .foregroundStyle(
                            selectedMealType == type ? .white : .secondary
                        )
                        .clipShape(Capsule())
                    }
                }
            }
        }
    }

    // MARK: - Dietary Filters

    private var dietaryFilters: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                FilterChip(label: "High Protein", isActive: $viewModel.preferHighProtein)
                FilterChip(label: "Low Carb", isActive: $viewModel.preferLowCarb)
                FilterChip(label: "Vegetarian", isActive: $viewModel.preferVegetarian)
                FilterChip(label: "Vegan", isActive: $viewModel.preferVegan)
            }
        }
    }

    // MARK: - Empty State

    private var emptyStateView: some View {
        VStack(spacing: 16) {
            Image(systemName: "sparkles")
                .font(.system(size: 48))
                .foregroundStyle(Color(hex: "FF9800"))
            Text("No Suggestions Available")
                .font(.headline)
                .foregroundStyle(.white)
            Text("Try adjusting your filters or meal type to see suggestions that fit your remaining macros.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(40)
    }
}

// MARK: - Suggestion Detail Card

private struct SuggestionDetailCard: View {
    let suggestion: MealSuggestion
    let onLog: () -> Void
    @State private var expanded = false

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(suggestion.name)
                        .font(.headline)
                        .foregroundStyle(.white)
                    Text(suggestion.suggestionDescription)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(expanded ? nil : 2)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text("\(suggestion.calories)")
                        .font(.title3.weight(.bold))
                        .foregroundStyle(Color(hex: "4CAF50"))
                    Text("cal")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }
            .contentShape(Rectangle())
            .onTapGesture {
                withAnimation(.easeInOut(duration: 0.2)) { expanded.toggle() }
            }

            // Macro bars
            HStack(spacing: 16) {
                macroBar(label: "Protein", value: Int(suggestion.proteinGrams), colorHex: "E91E63")
                macroBar(label: "Carbs", value: Int(suggestion.carbsGrams), colorHex: "2196F3")
                macroBar(label: "Fat", value: Int(suggestion.fatGrams), colorHex: "FF9800")
            }

            // Tags
            if !suggestion.tags.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        ForEach(suggestion.tags, id: \.self) { tag in
                            Text(tag)
                                .font(.caption2)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 3)
                                .background(Color.white.opacity(0.1))
                                .foregroundStyle(.secondary)
                                .clipShape(Capsule())
                        }
                        if suggestion.isHighProtein {
                            dietBadge(text: "High Protein", colorHex: "E91E63")
                        }
                        if suggestion.isVegetarian {
                            dietBadge(text: "Vegetarian", colorHex: "4CAF50")
                        }
                        if suggestion.isVegan {
                            dietBadge(text: "Vegan", colorHex: "8BC34A")
                        }
                    }
                }
            }

            // Expanded details
            if expanded {
                VStack(alignment: .leading, spacing: 8) {
                    if !suggestion.ingredients.isEmpty {
                        Text("Ingredients")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(.white)
                        Text(suggestion.ingredients.joined(separator: ", "))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }

                    HStack(spacing: 16) {
                        Label("\(suggestion.prepTimeMinutes) min", systemImage: "clock")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        if suggestion.fiberGrams > 0 {
                            Label("\(Int(suggestion.fiberGrams))g fiber", systemImage: "leaf")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
                .transition(.opacity.combined(with: .move(edge: .top)))
            }

            // Log button
            Button(action: onLog) {
                HStack {
                    Image(systemName: "plus.circle.fill")
                    Text("Log This Meal")
                }
                .font(.subheadline.weight(.semibold))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(Color(hex: "4CAF50"))
                .foregroundStyle(.white)
                .clipShape(RoundedRectangle(cornerRadius: 10))
            }
        }
        .padding(16)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func macroBar(label: String, value: Int, colorHex: String) -> some View {
        VStack(spacing: 4) {
            Text("\(value)g")
                .font(.caption.weight(.bold))
                .foregroundStyle(Color(hex: colorHex))
            Text(label)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .background(Color(hex: colorHex).opacity(0.1))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    private func dietBadge(text: String, colorHex: String) -> some View {
        Text(text)
            .font(.caption2.weight(.medium))
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(Color(hex: colorHex).opacity(0.15))
            .foregroundStyle(Color(hex: colorHex))
            .clipShape(Capsule())
    }
}

// MARK: - Filter Chip

private struct FilterChip: View {
    let label: String
    @Binding var isActive: Bool

    var body: some View {
        Button {
            withAnimation(.easeInOut(duration: 0.15)) { isActive.toggle() }
        } label: {
            Text(label)
                .font(.caption.weight(.medium))
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(isActive ? Color(hex: "FF9800").opacity(0.2) : Color(hex: "1E1E1E"))
                .foregroundStyle(isActive ? Color(hex: "FF9800") : .secondary)
                .clipShape(Capsule())
                .overlay(
                    Capsule()
                        .stroke(isActive ? Color(hex: "FF9800").opacity(0.5) : Color.clear, lineWidth: 1)
                )
        }
    }
}

#Preview {
    let container = try! ModelContainer(for: DailyNutrition.self, NutritionGoals.self, configurations: ModelConfiguration(isStoredInMemoryOnly: true))
    let vm = NutritionViewModel()
    vm.configure(modelContext: container.mainContext)
    return NavigationStack {
        MealSuggestionsScreen(viewModel: vm)
    }
    .preferredColorScheme(.dark)
}
