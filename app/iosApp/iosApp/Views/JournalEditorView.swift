import SwiftUI
import Shared

@available(iOS 17.0, *)
struct JournalEditorView: View {
    @EnvironmentObject var journalManager: JournalManager
    @Environment(\.dismiss) var dismiss
    
    let entryId: String
    
    @State private var title: String = ""
    @State private var bodyText: String = ""
    @State private var moodScore: Int? = nil
    @State private var entryDate: Kotlinx_datetimeLocalDate? = nil
    @State private var version: Int64 = 0
    
    @State private var showDeleteConfirmation = false
    @State private var hasUnsavedChanges = false
    @State private var isLoading = true
    
    @FocusState private var isEditorFocused: Bool
    @FocusState private var isTitleFocused: Bool
    
    var body: some View {
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
        .background(Color(red: 0.05, green: 0.06, blue: 0.08))
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
        .onAppear { loadEntry() }
        .onDisappear { journalManager.clearCurrentEntry() }
        .confirmationDialog("Delete Entry", isPresented: $showDeleteConfirmation) {
            Button("Delete", role: .destructive) { deleteEntry() }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This entry will be permanently deleted.")
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
            
            // Status row
            HStack {
                if journalManager.isSaving {
                    ProgressView()
                        .scaleEffect(0.6)
                        .tint(.white.opacity(0.5))
                    Text("Saving...")
                        .font(.caption)
                        .foregroundColor(.white.opacity(0.5))
                } else if journalManager.lastSavedAt != nil {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.caption)
                        .foregroundColor(.green)
                    Text("Saved")
                        .font(.caption)
                        .foregroundColor(.white.opacity(0.5))
                }
                
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
                
                // Body Text Editor
                ZStack(alignment: .topLeading) {
                    if bodyText.isEmpty {
                        Text("Start writing...")
                            .font(.body)
                            .foregroundColor(.white.opacity(0.3))
                            .padding(.top, 8)
                            .padding(.leading, 4)
                    }
                    
                    TextEditor(text: $bodyText)
                        .font(.body)
                        .foregroundStyle(.white)
                        .scrollContentBackground(.hidden)
                        .frame(minHeight: 300)
                        .focused($isEditorFocused)
                        .onChange(of: bodyText) { _, newValue in
                            hasUnsavedChanges = true
                            journalManager.updateBody(newValue, for: entryId)
                        }
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
        if hasUnsavedChanges {
            journalManager.forceSave(entryId: entryId) { _ in dismiss() }
        } else {
            dismiss()
        }
    }
    
    private func loadEntry() {
        journalManager.getEntry(id: entryId) { journal in
            guard let journal = journal else { return }
            self.title = journal.title
            self.bodyText = journal.body
            self.moodScore = journal.moodScore.map { Int($0.intValue) }
            self.entryDate = journal.entryDate
            self.version = journal.version
            self.isLoading = false
        }
    }
    
    private func saveTitle() {
        guard !title.isEmpty else { return }
        journalManager.updateTitle(title, for: entryId) { success in
            if success {
                print("Title saved successfully")
            } else {
                print("Failed to save title")
            }
        }
    }
    
    private func deleteEntry() {
        journalManager.deleteEntry(id: entryId) { success in
            if success { dismiss() }
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
            .environmentObject(JournalManager())
    }
}
