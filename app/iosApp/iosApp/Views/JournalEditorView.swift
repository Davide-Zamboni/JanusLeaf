import SwiftUI
import Shared

// MARK: - Journal Editor View (Optimized & Minimal)

@available(iOS 17.0, *)
struct JournalEditorView: View {
    @EnvironmentObject var journalManager: JournalManager
    @Environment(\.dismiss) var dismiss
    
    let entryId: String
    
    @State private var title: String = ""
    @State private var bodyText: String = ""
    @State private var moodScore: Int? = nil
    @State private var entryDate: Kotlinx_datetimeLocalDate? = nil
    
    @State private var originalTitle: String = ""
    @State private var isLoading = true
    @State private var showDelete = false
    @State private var hasChanges = false
    
    @FocusState private var focusedField: Field?
    
    enum Field { case title, body }
    
    var body: some View {
        Group {
            if isLoading {
                loadingView
            } else {
                editorView
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button {
                    handleBack()
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "chevron.left")
                        Text("Back")
                    }
                }
            }
            
            ToolbarItem(placement: .principal) {
                if let date = entryDate {
                    Text(formatDate(date))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
            
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Button(role: .destructive) {
                        showDelete = true
                    } label: {
                        Label("Delete", systemImage: "trash")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                }
            }
        }
        .toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                Button("Done") {
                    focusedField = nil
                }
            }
        }
        .confirmationDialog("Delete Entry", isPresented: $showDelete) {
            Button("Delete", role: .destructive) {
                deleteEntry()
            }
        } message: {
            Text("This entry will be permanently deleted.")
        }
        .onAppear {
            loadEntry()
        }
        .onDisappear {
            journalManager.clearCurrentEntry()
        }
    }
    
    // MARK: - Loading View
    
    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
            Text("Loading...")
                .foregroundStyle(.secondary)
        }
    }
    
    // MARK: - Editor View
    
    private var editorView: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                // Title
                TextField("Title", text: $title)
                    .font(.title.bold())
                    .focused($focusedField, equals: .title)
                    .onSubmit {
                        focusedField = .body
                    }
                    .onChange(of: title) { _, newValue in
                        if newValue != originalTitle {
                            hasChanges = true
                        }
                    }
                
                // Mood indicator
                if let score = moodScore {
                    HStack(spacing: 6) {
                        Text(moodEmoji(for: score))
                        Text("Mood: \(score)/10")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                } else {
                    HStack(spacing: 6) {
                        Image(systemName: "sparkles")
                            .foregroundStyle(.mint)
                        Text("Analyzing mood...")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }
                
                Divider()
                
                // Body
                TextEditor(text: $bodyText)
                    .font(.body)
                    .focused($focusedField, equals: .body)
                    .frame(minHeight: 300)
                    .scrollContentBackground(.hidden)
                    .onChange(of: bodyText) { _, newValue in
                        hasChanges = true
                        journalManager.updateBody(newValue, for: entryId)
                    }
                
                // Save status
                if journalManager.isSaving {
                    HStack(spacing: 6) {
                        ProgressView()
                            .controlSize(.small)
                        Text("Saving...")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                } else if let lastSaved = journalManager.lastSavedAt {
                    Text("Saved \(lastSaved.formatted(.relative(presentation: .named)))")
                        .font(.caption)
                        .foregroundStyle(.tertiary)
                }
            }
            .padding()
        }
        .background(Color(.systemBackground))
    }
    
    // MARK: - Actions
    
    private func handleBack() {
        let titleChanged = title != originalTitle && !title.isEmpty
        
        if titleChanged {
            journalManager.updateTitle(title, for: entryId) { _ in
                if hasChanges {
                    journalManager.forceSave(entryId: entryId) { _ in
                        dismiss()
                    }
                } else {
                    dismiss()
                }
            }
        } else if hasChanges {
            journalManager.forceSave(entryId: entryId) { _ in
                dismiss()
            }
        } else {
            dismiss()
        }
    }
    
    private func loadEntry() {
        journalManager.getEntry(id: entryId) { journal in
            guard let journal else { return }
            title = journal.title
            originalTitle = journal.title
            bodyText = cleanMarkdown(journal.body)
            moodScore = journal.moodScore.map { Int($0.intValue) }
            entryDate = journal.entryDate
            isLoading = false
        }
    }
    
    private func deleteEntry() {
        journalManager.deleteEntry(id: entryId) { success in
            if success {
                dismiss()
            }
        }
    }
    
    private func formatDate(_ date: Kotlinx_datetimeLocalDate) -> String {
        let components = DateComponents(
            year: Int(date.year),
            month: Int(date.monthNumber),
            day: Int(date.dayOfMonth)
        )
        if let swiftDate = Calendar.current.date(from: components) {
            return swiftDate.formatted(.dateTime.month(.abbreviated).day().year())
        }
        return "\(date.month) \(date.dayOfMonth), \(date.year)"
    }
    
    private func cleanMarkdown(_ text: String) -> String {
        // Remove strikethrough markers for display, keep the content
        text.replacingOccurrences(of: "~~", with: "")
    }
    
    private func moodEmoji(for score: Int) -> String {
        switch score {
        case 1...2: return "😢"
        case 3...4: return "😔"
        case 5...6: return "😐"
        case 7...8: return "😊"
        case 9...10: return "😄"
        default: return "😶"
        }
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
