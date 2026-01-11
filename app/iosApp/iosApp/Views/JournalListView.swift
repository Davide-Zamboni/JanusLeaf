import SwiftUI
import Shared

// MARK: - Journal List View (Standalone - kept for compatibility)

@available(iOS 17.0, *)
struct JournalListView: View {
    @EnvironmentObject var authManager: AuthManager
    @EnvironmentObject var journalManager: JournalManager
    @StateObject private var inspirationManager = InspirationManager()
    
    @State private var selectedEntryId: String? = nil
    @State private var showLogout = false
    @State private var isCreatingEntry = false
    
    var body: some View {
        NavigationStack {
            Group {
                if journalManager.isLoading && journalManager.entries.isEmpty {
                    loadingView
                } else if journalManager.entries.isEmpty {
                    emptyView
                } else {
                    listView
                }
            }
            .navigationTitle(headerTitle)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    HStack(spacing: 12) {
                        Button {
                            createNewEntry()
                        } label: {
                            if isCreatingEntry {
                                ProgressView()
                            } else {
                                Image(systemName: "plus")
                            }
                        }
                        .disabled(isCreatingEntry)
                        
                        Button {
                            showLogout = true
                        } label: {
                            Image(systemName: "person.circle")
                        }
                    }
                }
            }
            .navigationDestination(isPresented: Binding(
                get: { selectedEntryId != nil },
                set: { if !$0 { 
                    selectedEntryId = nil
                    journalManager.refresh()
                }}
            )) {
                if let entryId = selectedEntryId {
                    JournalEditorView(entryId: entryId)
                        .environmentObject(journalManager)
                }
            }
            .confirmationDialog("Sign Out", isPresented: $showLogout) {
                Button("Sign Out", role: .destructive) {
                    authManager.logout()
                }
            } message: {
                Text("Are you sure you want to sign out?")
            }
        }
        .onAppear {
            journalManager.loadEntries()
            inspirationManager.fetchQuote()
        }
    }
    
    // MARK: - List View
    
    private var listView: some View {
        List {
            Section {
                ForEach(journalManager.entries, id: \.id) { entry in
                    JournalRow(entry: entry)
                        .contentShape(Rectangle())
                        .onTapGesture {
                            selectedEntryId = entry.id
                        }
                }
                
                if journalManager.hasMore {
                    Button {
                        journalManager.loadMoreEntries()
                    } label: {
                        HStack {
                            Spacer()
                            if journalManager.isLoading {
                                ProgressView()
                            } else {
                                Text("Load More")
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                        }
                    }
                    .disabled(journalManager.isLoading)
                }
            } header: {
                Text(formattedDate)
            }
        }
        .listStyle(.insetGrouped)
        .refreshable {
            journalManager.refresh()
            inspirationManager.refresh()
        }
    }
    
    // MARK: - Loading View
    
    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
            Text("Loading journal...")
                .foregroundStyle(.secondary)
        }
    }
    
    // MARK: - Empty View
    
    private var emptyView: some View {
        ContentUnavailableView {
            Label("No Entries", systemImage: "book.closed")
        } description: {
            Text("Start your journaling journey")
        } actions: {
            Button {
                createNewEntry()
            } label: {
                if isCreatingEntry {
                    ProgressView()
                } else {
                    Text("Create Entry")
                }
            }
            .buttonStyle(.borderedProminent)
            .tint(.green)
            .disabled(isCreatingEntry)
        }
    }
    
    // MARK: - Helpers
    
    private var headerTitle: String {
        if let username = authManager.currentUsername, !username.isEmpty {
            return "\(username)'s Journal"
        }
        return "Journal"
    }
    
    private var formattedDate: String {
        Date().formatted(.dateTime.weekday(.wide).month(.wide).day())
    }
    
    private func createNewEntry() {
        guard !isCreatingEntry else { return }
        isCreatingEntry = true
        
        journalManager.createEntry(
            title: nil,
            body: nil,
            entryDate: Date()
        ) { journal in
            isCreatingEntry = false
            if let journal {
                selectedEntryId = journal.id
            }
        }
    }
}

// MARK: - Preview

@available(iOS 17.0, *)
#Preview {
    JournalListView()
        .environmentObject(AuthManager())
        .environmentObject(JournalManager())
}
