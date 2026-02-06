import SwiftUI
import Shared
import KMPObservableViewModelSwiftUI

private func toSwiftBool(_ value: Bool) -> Bool { value }
private func toSwiftBool(_ value: KotlinBoolean) -> Bool { value.boolValue }

@available(iOS 17.0, *)
struct JournalEditorView: View {
    @Environment(\.dismiss) var dismiss
    
    let entryId: String

    @StateViewModel private var editorViewModel = SharedModule.shared.createObservableJournalEditorViewModel()
    
    @State private var title: String = ""
    @State private var bodyText: String = ""
    @State private var moodScore: Int? = nil
    @State private var entryDate: Kotlinx_datetimeLocalDate? = nil
    @State private var version: Int64 = 0
    
    @State private var showDeleteConfirmation = false
    @State private var hasUnsavedBodyChanges = false
    @State private var originalTitle: String = ""
    @State private var isLoading = true
    
    // Error tracking
    @State private var consecutiveFailures = 0
    @State private var showErrorBanner = false
    private let failureThreshold = 3
    
    // Strikethrough visibility toggle
    @State private var showStrikethrough = true
    
    @FocusState private var isEditorFocused: Bool
    @FocusState private var isTitleFocused: Bool

    var body: some View {
        ZStack(alignment: .top) {
            VStack(spacing: 0) {
                // Custom navigation bar
                customNavBar
                
                // Content
                if isLoading {
                    loadingContent
                } else {
                    editorContent
                }
            }
            
            // Error banner overlay
            if showErrorBanner {
                errorBanner
                    .transition(.move(edge: .top).combined(with: .opacity))
                    .zIndex(100)
            }
        }
        .background(Color(red: 0.05, green: 0.06, blue: 0.08))
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
        .onAppear { loadEntry() }
        .onDisappear { editorViewModel.clearCurrentEntry() }
        .confirmationDialog("Delete Entry", isPresented: $showDeleteConfirmation) {
            Button("Delete", role: .destructive) { deleteEntry() }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This entry will be permanently deleted.")
        }
        .onChange(of: editorViewModel.errorMessage) { _, error in
            if error != nil {
                handleSaveError()
                editorViewModel.clearError()
            } else {
                // Success - reset failure count
                consecutiveFailures = 0
            }
        }
        .animation(.spring(response: 0.3, dampingFraction: 0.8), value: showErrorBanner)
    }
    
    // MARK: - Error Banner
    
    private var errorBanner: some View {
        HStack(spacing: 10) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 14))
                .foregroundColor(.orange)
            
            Text("Having trouble saving. Check your connection.")
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(.white.opacity(0.9))
            
            Spacer()
            
            Button(action: { 
                withAnimation { showErrorBanner = false }
            }) {
                Image(systemName: "xmark")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(.white.opacity(0.6))
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(red: 0.2, green: 0.15, blue: 0.1))
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color.orange.opacity(0.3), lineWidth: 1)
                )
        )
        .padding(.horizontal, 16)
        .padding(.top, 100) // Below nav bar
    }
    
    private func handleSaveError() {
        consecutiveFailures += 1
        
        // Only show banner after multiple failures
        if consecutiveFailures >= failureThreshold && !showErrorBanner {
            withAnimation {
                showErrorBanner = true
            }
            
            // Auto-hide after 5 seconds
            DispatchQueue.main.asyncAfter(deadline: .now() + 5) {
                withAnimation {
                    showErrorBanner = false
                }
            }
        }
    }
    
    // MARK: - Custom Navigation Bar
    
    private var customNavBar: some View {
        VStack(spacing: 4) {
            HStack {
                Button(action: handleBack) {
                    HStack(spacing: 4) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 16, weight: .semibold))
                        Text("Back")
                            .font(.system(size: 16))
                    }
                    .foregroundColor(.white.opacity(0.8))
                }
                
                Spacer()
                
                // Strikethrough visibility toggle
                Button(action: {
                    withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) {
                        showStrikethrough.toggle()
                    }
                }) {
                    Image(systemName: showStrikethrough ? "eye" : "eye.slash")
                        .font(.system(size: 18))
                        .foregroundColor(showStrikethrough ? .white.opacity(0.7) : .orange.opacity(0.8))
                }
                .padding(.trailing, 8)
                
                Menu {
                    Button(role: .destructive) { showDeleteConfirmation = true } label: {
                        Label("Delete", systemImage: "trash")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                        .font(.system(size: 20))
                        .foregroundColor(.white.opacity(0.7))
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
            
            // Date row
            HStack {
                Spacer()
                
                if let date = entryDate {
                    Text(formatDate(date))
                        .font(.caption)
                        .foregroundColor(.white.opacity(0.4))
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 8)
        }
        .background(Color(red: 0.05, green: 0.06, blue: 0.08))
    }
    
    // MARK: - Editor Content
    
    private var editorContent: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // Title - editable TextField
                TextField("Enter title...", text: $title)
                    .font(.title.bold())
                    .foregroundColor(.white)
                    .focused($isTitleFocused)
                    .onSubmit {
                        saveTitle()
                    }
                    .onChange(of: isTitleFocused) { _, focused in
                        // Save when losing focus
                        if !focused && !title.isEmpty {
                            saveTitle()
                        }
                    }
                
                // Markdown Body Text Editor
                MarkdownTextEditor(
                    text: $bodyText,
                    placeholder: "Start writing...",
                    isFocused: $isEditorFocused,
                    showStrikethrough: showStrikethrough
                ) { newValue in
                    hasUnsavedBodyChanges = true
                    editorViewModel.updateBody(body: newValue, entryId: entryId)
                }
            }
            .padding(16)
        }
        .background(Color(red: 0.05, green: 0.06, blue: 0.08))
    }
    
    // MARK: - Loading Content
    
    private var loadingContent: some View {
        VStack {
            Spacer()
            ProgressView()
                .tint(.white)
                .scaleEffect(1.5)
            Text("Loading...")
                .font(.subheadline)
                .foregroundColor(.white.opacity(0.6))
                .padding(.top, 16)
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(red: 0.05, green: 0.06, blue: 0.08))
    }
    
    // MARK: - Actions
    
    private func handleBack() {
        let hasTitleChanges = title != originalTitle && !title.isEmpty
        
        // Save title first if changed, then handle body changes
        if hasTitleChanges {
            editorViewModel.updateTitle(title: title, entryId: entryId) { _ in
                if hasUnsavedBodyChanges {
                    editorViewModel.forceSave(entryId: entryId) { _ in dismiss() }
                } else {
                    dismiss()
                }
            }
        } else if hasUnsavedBodyChanges {
            editorViewModel.forceSave(entryId: entryId) { _ in dismiss() }
        } else {
            dismiss()
        }
    }
    
    private func loadEntry() {
        editorViewModel.loadEntry(id: entryId) { journal in
            guard let journal = journal else { return }
            self.title = journal.title
            self.originalTitle = journal.title
            self.bodyText = journal.body
            self.moodScore = journal.moodScore.map { Int($0.intValue) }
            self.entryDate = journal.entryDate
            self.version = journal.version
            self.isLoading = false
        }
    }
    
    private func saveTitle() {
        guard !title.isEmpty, title != originalTitle else { return }
        editorViewModel.updateTitle(title: title, entryId: entryId) { success in
            if toSwiftBool(success) {
                self.originalTitle = self.title
            }
        }
    }
    
    private func deleteEntry() {
        editorViewModel.deleteEntry(entryId: entryId) { success in
            if toSwiftBool(success) { dismiss() }
        }
    }
    
    private func formatDate(_ date: Kotlinx_datetimeLocalDate) -> String {
        "\(date.month.name.prefix(3)) \(date.dayOfMonth), \(date.year)"
    }
}

// MARK: - Preview

@available(iOS 17.0, *)
#Preview {
    NavigationStack {
        JournalEditorView(entryId: "test")
    }
}
