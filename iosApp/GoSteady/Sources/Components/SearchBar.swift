import SwiftUI

struct SearchBar: View {
    @Binding var text: String
    var placeholder: String = "Search..."
    var onSubmit: (() -> Void)? = nil
    @FocusState private var isFocused: Bool
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        HStack(spacing: AppSpacing.sm) {
            Image(systemName: "magnifyingglass")
                .font(.subheadline)
                .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))

            TextField(placeholder, text: $text)
                .font(AppTypography.bodyMedium)
                .foregroundStyle(AppTheme.adaptiveOnSurface(colorScheme))
                .focused($isFocused)
                .submitLabel(.search)
                .onSubmit {
                    onSubmit?()
                }

            if !text.isEmpty {
                Button {
                    text = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.subheadline)
                        .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
                }
            }
        }
        .padding(.horizontal, AppSpacing.md)
        .padding(.vertical, AppSpacing.sm + 2)
        .background(AppTheme.adaptiveSurfaceVariant(colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: AppCornerRadius.medium))
    }
}

#Preview {
    VStack {
        SearchBar(text: .constant(""), placeholder: "Search exercises...")
        SearchBar(text: .constant("Bench press"))
    }
    .padding()
    .background(AppTheme.background)
}
