import SwiftUI
import Shared

// MARK: - Main Tab View (Optimized & Minimal)

@available(iOS 17.0, *)
struct MainTabView: View {
    @EnvironmentObject var authManager: AuthManager
    @EnvironmentObject var journalManager: JournalManager
    
    @State private var selectedTab: Tab = .journal
    @State private var isCreatingEntry = false
    @State private var newEntryId: String? = nil
    
    enum Tab: String {
        case journal = "Journal"
        case insights = "Insights"
    }
    
    var body: some View {
        TabView(selection: $selectedTab) {
            JournalTabView(
                isCreatingEntry: $isCreatingEntry,
                newEntryId: $newEntryId,
                createEntry: createNewEntry
            )
            .tabItem {
                Label("Journal", systemImage: "book.fill")
            }
            .tag(Tab.journal)
            
            MoodInsightsView()
                .tabItem {
                    Label("Insights", systemImage: "chart.line.uptrend.xyaxis")
                }
                .tag(Tab.insights)
        }
        .tint(.green)
    }
    
    private func createNewEntry() {
        guard !isCreatingEntry else { return }
        
        UIImpactFeedbackGenerator(style: .medium).impactOccurred()
        isCreatingEntry = true
        selectedTab = .journal
        
        journalManager.createEntry(
            title: nil,
            body: nil,
            entryDate: Date()
        ) { journal in
            isCreatingEntry = false
            if let journal {
                newEntryId = journal.id
            }
        }
    }
}

// MARK: - Journal Tab View

@available(iOS 17.0, *)
struct JournalTabView: View {
    @EnvironmentObject var authManager: AuthManager
    @EnvironmentObject var journalManager: JournalManager
    @StateObject private var inspirationManager = InspirationManager()
    
    @Binding var isCreatingEntry: Bool
    @Binding var newEntryId: String?
    let createEntry: () -> Void
    
    @State private var selectedEntryId: String? = nil
    @State private var showLogout = false
    @State private var pollTimer: Timer? = nil
    
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
                            createEntry()
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
                get: { selectedEntryId != nil || newEntryId != nil },
                set: { if !$0 { 
                    selectedEntryId = nil
                    newEntryId = nil
                    journalManager.refresh()
                }}
            )) {
                if let id = newEntryId ?? selectedEntryId {
                    JournalEditorView(entryId: id)
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
            startPolling()
        }
        .onDisappear {
            stopPolling()
        }
        .onChange(of: newEntryId) { _, newValue in
            if newValue != nil {
                selectedEntryId = newValue
            }
        }
    }
    
    // MARK: - List View
    
    private var listView: some View {
        List {
            // Inspiration section
            if inspirationManager.quote != nil || inspirationManager.isNotFound {
                Section {
                    QuoteCard(inspirationManager: inspirationManager)
                        .listRowInsets(EdgeInsets())
                        .listRowBackground(Color.clear)
                }
            }
            
            // Entries
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
                createEntry()
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
    
    // MARK: - Polling (reduced frequency)
    
    private func startPolling() {
        // Poll every 15 seconds instead of 5
        pollTimer = Timer.scheduledTimer(withTimeInterval: 15.0, repeats: true) { _ in
            Task { @MainActor in
                if journalManager.entries.contains(where: { $0.moodScore == nil }) {
                    journalManager.refresh()
                }
                if inspirationManager.isNotFound {
                    inspirationManager.refresh()
                }
            }
        }
    }
    
    private func stopPolling() {
        pollTimer?.invalidate()
        pollTimer = nil
    }
}

// MARK: - Journal Row

struct JournalRow: View {
    let entry: JournalPreview
    
    var body: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(entry.title)
                    .font(.headline)
                    .lineLimit(1)
                
                if !entry.bodyPreview.isEmpty {
                    Text(cleanPreview(entry.bodyPreview))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }
                
                Text(formatDate(entry.entryDate))
                    .font(.caption)
                    .foregroundStyle(.tertiary)
            }
            
            Spacer()
            
            MoodIndicator(score: entry.moodScore.map { Int($0.intValue) })
        }
        .padding(.vertical, 4)
    }
    
    private func cleanPreview(_ text: String) -> String {
        // Remove markdown formatting for cleaner preview
        text.replacingOccurrences(of: "~~", with: "")
            .replacingOccurrences(of: "**", with: "")
            .replacingOccurrences(of: "*", with: "")
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
        return "\(date.month) \(date.dayOfMonth)"
    }
}

// MARK: - Mood Indicator

struct MoodIndicator: View {
    let score: Int?
    
    var body: some View {
        if let score {
            Text(emoji(for: score))
                .font(.title2)
        } else {
            // Simple loading indicator
            Image(systemName: "sparkles")
                .foregroundStyle(.mint)
                .symbolEffect(.pulse)
        }
    }
    
    private func emoji(for score: Int) -> String {
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

// MARK: - Quote Card

struct QuoteCard: View {
    @ObservedObject var inspirationManager: InspirationManager
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            if inspirationManager.isNotFound {
                pendingView
            } else if let quote = inspirationManager.quote {
                quoteView(quote)
            }
        }
        .padding(16)
        .background(Color(.secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal)
        .padding(.vertical, 8)
    }
    
    private func quoteView(_ quote: InspirationalQuote) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 6) {
                Image(systemName: "sparkles")
                    .foregroundStyle(.mint)
                Text("Daily Inspiration")
                    .font(.caption)
                    .fontWeight(.medium)
                    .foregroundStyle(.secondary)
            }
            
            Text("\"\(quote.quote)\"")
                .font(.callout)
                .italic()
            
            // Tags
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(Array(quote.tags.prefix(4)), id: \.self) { tag in
                        Text(tag)
                            .font(.caption2)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color.mint.opacity(0.15))
                            .clipShape(Capsule())
                    }
                }
            }
            
            Text(inspirationManager.formattedGeneratedDate())
                .font(.caption2)
                .foregroundStyle(.tertiary)
        }
    }
    
    private var pendingView: some View {
        HStack(spacing: 12) {
            Image(systemName: "flame")
                .font(.title2)
                .foregroundStyle(.orange)
            
            VStack(alignment: .leading, spacing: 2) {
                Text("Inspiration brewing...")
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                Text("Keep journaling to unlock personalized quotes")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
    }
}

// MARK: - Preview

@available(iOS 17.0, *)
#Preview {
    MainTabView()
        .environmentObject(AuthManager())
        .environmentObject(JournalManager())
}
