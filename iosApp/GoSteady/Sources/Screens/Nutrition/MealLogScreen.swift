import SwiftUI
import SwiftData
import PhotosUI

struct MealLogScreen: View {
    @Bindable var viewModel: NutritionViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var selectedTab = 0
    @State private var searchText = ""
    @State private var selectedPhotoItem: PhotosPickerItem?
    @State private var showCamera = false

    var body: some View {
        VStack(spacing: 0) {
            // Tab selector
            Picker("Entry Mode", selection: $selectedTab) {
                Text("Manual").tag(0)
                Text("Photo AI").tag(1)
                Text("Search").tag(2)
            }
            .pickerStyle(.segmented)
            .padding()

            switch selectedTab {
            case 0:
                manualEntryView
            case 1:
                photoEntryView
            default:
                searchEntryView
            }
        }
        .background(Color(hex: "121212"))
        .navigationTitle("Log a Meal")
        .navigationBarTitleDisplayMode(.inline)
    }

    // MARK: - Manual Entry

    private var manualEntryView: some View {
        ScrollView {
            VStack(spacing: 16) {
                FormField(title: "Meal Name", text: $viewModel.mealName, placeholder: "e.g., Grilled Chicken Salad")

                FormField(title: "Calories", text: $viewModel.mealCalories, placeholder: "0", keyboardType: .numberPad)

                HStack(spacing: 12) {
                    FormField(title: "Protein (g)", text: $viewModel.mealProtein, placeholder: "0", keyboardType: .numberPad)
                    FormField(title: "Carbs (g)", text: $viewModel.mealCarbs, placeholder: "0", keyboardType: .numberPad)
                }

                HStack(spacing: 12) {
                    FormField(title: "Fat (g)", text: $viewModel.mealFat, placeholder: "0", keyboardType: .numberPad)
                    FormField(title: "Serving Size", text: $viewModel.mealServingSize, placeholder: "1 serving")
                }

                // Macro preview
                if let cal = Int(viewModel.mealCalories), cal > 0 {
                    macroPreview(
                        calories: cal,
                        protein: Int(viewModel.mealProtein) ?? 0,
                        carbs: Int(viewModel.mealCarbs) ?? 0,
                        fat: Int(viewModel.mealFat) ?? 0
                    )
                }

                Button {
                    viewModel.logMealFromForm()
                    dismiss()
                } label: {
                    Text("Log Meal")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(
                            (Int(viewModel.mealCalories) ?? 0) > 0
                                ? Color(hex: "4CAF50")
                                : Color.gray.opacity(0.3)
                        )
                        .foregroundStyle(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .disabled((Int(viewModel.mealCalories) ?? 0) <= 0)
            }
            .padding()
        }
    }

    // MARK: - Photo Entry

    private var photoEntryView: some View {
        ScrollView {
            VStack(spacing: 20) {
                if viewModel.isAnalyzingPhoto {
                    analyzingView
                } else if let result = viewModel.photoAnalysisResult {
                    analysisResultView(result)
                } else {
                    captureOptionsView
                }
            }
            .padding()
        }
    }

    private var captureOptionsView: some View {
        VStack(spacing: 24) {
            Image(systemName: "camera.viewfinder")
                .font(.system(size: 64))
                .foregroundStyle(Color(hex: "4CAF50"))

            Text("Scan Your Food")
                .font(.title2.weight(.bold))
                .foregroundStyle(.white)

            Text("Take a photo or select from your library to estimate calories using AI")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            HStack(spacing: 16) {
                Button {
                    showCamera = true
                } label: {
                    HStack {
                        Image(systemName: "camera.fill")
                        Text("Camera")
                    }
                    .font(.subheadline.weight(.semibold))
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color(hex: "4CAF50"))
                    .foregroundStyle(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }

                PhotosPicker(selection: $selectedPhotoItem, matching: .images) {
                    HStack {
                        Image(systemName: "photo.on.rectangle")
                        Text("Gallery")
                    }
                    .font(.subheadline.weight(.semibold))
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color(hex: "1E1E1E"))
                    .foregroundStyle(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(Color.white.opacity(0.2), lineWidth: 1)
                    )
                }
                .onChange(of: selectedPhotoItem) { _, newValue in
                    guard let item = newValue else { return }
                    Task {
                        if let data = try? await item.loadTransferable(type: Data.self) {
                            viewModel.analyzePhoto(imageData: data)
                        }
                    }
                }
            }

            // How it works
            howItWorksView
        }
        .padding(.vertical, 20)
    }

    private var howItWorksView: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("How it works")
                .font(.headline)
                .foregroundStyle(.white)

            ForEach(Array([
                ("1", "Take a photo", "Capture your meal from above for best results"),
                ("2", "AI Analysis", "AI identifies foods and estimates nutrition"),
                ("3", "Review & Log", "Adjust estimates if needed and log your meal")
            ].enumerated()), id: \.offset) { _, item in
                HStack(alignment: .top, spacing: 12) {
                    Text(item.0)
                        .font(.caption.weight(.bold))
                        .foregroundStyle(.white)
                        .frame(width: 24, height: 24)
                        .background(Color(hex: "4CAF50"))
                        .clipShape(Circle())

                    VStack(alignment: .leading, spacing: 2) {
                        Text(item.1)
                            .font(.subheadline.weight(.medium))
                            .foregroundStyle(.white)
                        Text(item.2)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
            }

            Text("AI estimates may vary. For precise tracking, manually verify nutritional information.")
                .font(.caption)
                .foregroundStyle(.secondary)
                .padding(.top, 4)
        }
        .padding(16)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private var analyzingView: some View {
        VStack(spacing: 24) {
            ProgressView()
                .scaleEffect(2)
                .tint(Color(hex: "4CAF50"))

            Text("Analyzing your food...")
                .font(.headline)
                .foregroundStyle(.white)

            Text("AI is identifying foods and estimating nutritional content")
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(48)
        .frame(maxWidth: .infinity)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func analysisResultView(_ result: FoodAnalysisDisplay) -> some View {
        VStack(spacing: 16) {
            // Confidence badge
            HStack {
                Text(result.foodName)
                    .font(.title3.weight(.bold))
                    .foregroundStyle(.white)
                Spacer()
                Text(result.confidence.rawValue.capitalized)
                    .font(.caption.weight(.medium))
                    .padding(.horizontal, 12)
                    .padding(.vertical, 4)
                    .background(confidenceColor(result.confidence).opacity(0.15))
                    .foregroundStyle(confidenceColor(result.confidence))
                    .clipShape(Capsule())
            }

            if !result.portionSize.isEmpty {
                Text(result.portionSize)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            // Editable fields (pre-filled from analysis)
            FormField(title: "Calories", text: $viewModel.mealCalories, placeholder: "0", keyboardType: .numberPad)

            HStack(spacing: 12) {
                FormField(title: "Protein (g)", text: $viewModel.mealProtein, placeholder: "0", keyboardType: .numberPad)
                FormField(title: "Carbs (g)", text: $viewModel.mealCarbs, placeholder: "0", keyboardType: .numberPad)
                FormField(title: "Fat (g)", text: $viewModel.mealFat, placeholder: "0", keyboardType: .numberPad)
            }

            HStack(spacing: 12) {
                Button {
                    viewModel.photoAnalysisResult = nil
                    viewModel.mealCalories = ""
                    viewModel.mealProtein = ""
                    viewModel.mealCarbs = ""
                    viewModel.mealFat = ""
                    viewModel.mealName = ""
                } label: {
                    HStack {
                        Image(systemName: "arrow.counterclockwise")
                        Text("Retake")
                    }
                    .font(.subheadline.weight(.medium))
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color(hex: "1E1E1E"))
                    .foregroundStyle(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(Color.white.opacity(0.2), lineWidth: 1)
                    )
                }

                Button {
                    viewModel.logMealFromForm()
                    dismiss()
                } label: {
                    HStack {
                        Image(systemName: "checkmark")
                        Text("Log Meal")
                    }
                    .font(.subheadline.weight(.semibold))
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color(hex: "4CAF50"))
                    .foregroundStyle(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
        }
        .padding(16)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func confidenceColor(_ confidence: AnalysisConfidence) -> Color {
        switch confidence {
        case .high: return Color(hex: "4CAF50")
        case .medium: return Color(hex: "FF9800")
        case .low: return Color(hex: "F44336")
        }
    }

    // MARK: - Search Entry

    private var searchEntryView: some View {
        VStack(spacing: 0) {
            HStack {
                Image(systemName: "magnifyingglass")
                    .foregroundStyle(.secondary)
                TextField("Search foods...", text: $searchText)
                    .foregroundStyle(.white)
                if !searchText.isEmpty {
                    Button { searchText = "" } label: {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundStyle(.secondary)
                    }
                }
            }
            .padding(12)
            .background(Color(hex: "1E1E1E"))
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .padding()

            ScrollView {
                LazyVStack(spacing: 8) {
                    let filtered = MealSuggestionEngine.mealDatabase.filter {
                        searchText.isEmpty || $0.name.localizedCaseInsensitiveContains(searchText)
                    }
                    ForEach(filtered) { item in
                        Button {
                            viewModel.mealName = item.name
                            viewModel.mealCalories = "\(item.calories)"
                            viewModel.mealProtein = "\(Int(item.proteinGrams))"
                            viewModel.mealCarbs = "\(Int(item.carbsGrams))"
                            viewModel.mealFat = "\(Int(item.fatGrams))"
                            selectedTab = 0 // switch to manual to review
                        } label: {
                            HStack {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(item.name)
                                        .font(.subheadline.weight(.medium))
                                        .foregroundStyle(.white)
                                    Text(item.suggestionDescription)
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                        .lineLimit(1)
                                }
                                Spacer()
                                VStack(alignment: .trailing, spacing: 2) {
                                    Text("\(item.calories) cal")
                                        .font(.caption.weight(.bold))
                                        .foregroundStyle(Color(hex: "4CAF50"))
                                    Text("P:\(Int(item.proteinGrams))g")
                                        .font(.caption2)
                                        .foregroundStyle(Color(hex: "E91E63"))
                                }
                            }
                            .padding(12)
                            .background(Color(hex: "1E1E1E"))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                        }
                    }
                }
                .padding(.horizontal)
            }
        }
    }

    // MARK: - Helpers

    private func macroPreview(calories: Int, protein: Int, carbs: Int, fat: Int) -> some View {
        HStack(spacing: 16) {
            macroItem(label: "Protein", value: protein, unit: "g", colorHex: "E91E63")
            macroItem(label: "Carbs", value: carbs, unit: "g", colorHex: "2196F3")
            macroItem(label: "Fat", value: fat, unit: "g", colorHex: "FF9800")
        }
        .padding(16)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func macroItem(label: String, value: Int, unit: String, colorHex: String) -> some View {
        VStack(spacing: 4) {
            Text("\(value)\(unit)")
                .font(.headline.weight(.bold))
                .foregroundStyle(Color(hex: colorHex))
            Text(label)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Form Field

private struct FormField: View {
    let title: String
    @Binding var text: String
    var placeholder: String = ""
    var keyboardType: UIKeyboardType = .default

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.caption.weight(.medium))
                .foregroundStyle(.secondary)
            TextField(placeholder, text: $text)
                .keyboardType(keyboardType)
                .padding(12)
                .background(Color(hex: "1E1E1E"))
                .clipShape(RoundedRectangle(cornerRadius: 10))
                .foregroundStyle(.white)
        }
    }
}

#Preview {
    let container = try! ModelContainer(for: DailyNutrition.self, NutritionGoals.self, configurations: ModelConfiguration(isStoredInMemoryOnly: true))
    let vm = NutritionViewModel()
    vm.configure(modelContext: container.mainContext)
    return NavigationStack {
        MealLogScreen(viewModel: vm)
    }
    .preferredColorScheme(.dark)
}
