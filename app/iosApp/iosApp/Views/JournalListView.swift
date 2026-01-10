import SwiftUI
import Shared

@available(iOS 17.0, *)
struct JournalListView: View {
    @EnvironmentObject var authManager: AuthManager
    @EnvironmentObject var journalManager: JournalManager
    
    @State private var showNewEntry = false
    @State private var selectedEntryId: String? = nil
    @State private var showLogoutConfirmation = false
    @State private var animateItems = false
    @State private var moodPollingTimer: Timer? = nil
    
    var body: some View {
        NavigationStack {
            ZStack {
                // Animated background
                JournalBackground()
                
                VStack(spacing: 0) {
                    // Custom header
                    headerView
                    
                    if journalManager.isLoading && journalManager.entries.isEmpty {
                        loadingView
                    } else if journalManager.entries.isEmpty {
                        emptyStateView
                    } else {
                        journalListView
                    }
                }
            }
            .toolbar(.hidden, for: .navigationBar)
            .navigationDestination(isPresented: Binding(
                get: { selectedEntryId != nil },
                set: { if !$0 { selectedEntryId = nil } }
            )) {
                if let entryId = selectedEntryId {
                    JournalEditorView(entryId: entryId)
                        .environmentObject(journalManager)
                }
            }
            .sheet(isPresented: $showNewEntry) {
                NewEntrySheet { journal in
                    if let journal = journal {
                        // Navigate to editor with the new entry
                        selectedEntryId = journal.id
                    }
                }
            }
            .confirmationDialog("Sign Out", isPresented: $showLogoutConfirmation, titleVisibility: .visible) {
                Button("Sign Out", role: .destructive) {
                    authManager.logout()
                }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("Are you sure you want to sign out?")
            }
        }
        .onAppear {
            journalManager.loadEntries()
            withAnimation(.easeOut(duration: 0.6).delay(0.3)) {
                animateItems = true
            }
            startMoodPolling()
        }
        .onDisappear {
            stopMoodPolling()
        }
    }
    
    // MARK: - Mood Polling
    
    /// Start polling for mood score updates (for entries with AI-generated scores pending)
    private func startMoodPolling() {
        // Poll every 5 seconds to check for mood score updates
        moodPollingTimer = Timer.scheduledTimer(withTimeInterval: 5.0, repeats: true) { _ in
            // Only refresh if there are entries with nil mood scores
            let hasPendingMoods = journalManager.entries.contains { $0.moodScore == nil }
            if hasPendingMoods {
                journalManager.refresh()
            }
        }
    }
    
    private func stopMoodPolling() {
        moodPollingTimer?.invalidate()
        moodPollingTimer = nil
    }
    
    // MARK: - Header View
    
    private var headerView: some View {
        HStack(alignment: .center) {
            VStack(alignment: .leading, spacing: 4) {
                Text("Journal")
                    .font(.system(size: 34, weight: .bold, design: .rounded))
                    .foregroundColor(.white)
                
                Text(formattedDate)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(.white.opacity(0.6))
            }
            
            Spacer()
            
            // Profile button
            Button(action: { showLogoutConfirmation = true }) {
                ZStack {
                    Circle()
                        .fill(.ultraThinMaterial)
                        .frame(width: 44, height: 44)
                    
                    Image(systemName: "person.fill")
                        .font(.system(size: 18))
                        .foregroundColor(.white.opacity(0.8))
                }
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, 60)
        .padding(.bottom, 20)
        .opacity(animateItems ? 1 : 0)
        .offset(y: animateItems ? 0 : -20)
    }
    
    // MARK: - Journal List
    
    private var journalListView: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                // New entry button
                newEntryButton
                    .opacity(animateItems ? 1 : 0)
                    .offset(y: animateItems ? 0 : 30)
                
                ForEach(Array(journalManager.entries.enumerated()), id: \.element.id) { index, entry in
                    JournalEntryCard(entry: entry) {
                        selectedEntryId = entry.id
                    }
                    .opacity(animateItems ? 1 : 0)
                    .offset(y: animateItems ? 0 : 30)
                    .animation(
                        .spring(response: 0.5, dampingFraction: 0.8)
                        .delay(Double(index) * 0.05),
                        value: animateItems
                    )
                }
                
                // Load more indicator
                if journalManager.hasMore {
                    loadMoreIndicator
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 100)
        }
        .refreshable {
            journalManager.refresh()
        }
    }
    
    // MARK: - New Entry Button
    
    private var newEntryButton: some View {
        Button(action: { showNewEntry = true }) {
            HStack(spacing: 16) {
                ZStack {
                    Circle()
                        .fill(
                            LinearGradient(
                                colors: [Color(red: 0.2, green: 0.6, blue: 0.4), Color(red: 0.15, green: 0.5, blue: 0.35)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .frame(width: 48, height: 48)
                        .shadow(color: .green.opacity(0.4), radius: 12, x: 0, y: 6)
                    
                    Image(systemName: "plus")
                        .font(.system(size: 22, weight: .semibold))
                        .foregroundColor(.white)
                }
                
                VStack(alignment: .leading, spacing: 4) {
                    Text("New Entry")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(.white)
                    
                    Text("Start writing your thoughts")
                        .font(.system(size: 14))
                        .foregroundColor(.white.opacity(0.6))
                }
                
                Spacer()
                
                Image(systemName: "chevron.right")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.white.opacity(0.4))
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 20)
                    .fill(.ultraThinMaterial.opacity(0.8))
                    .overlay(
                        RoundedRectangle(cornerRadius: 20)
                            .stroke(
                                LinearGradient(
                                    colors: [.white.opacity(0.3), .white.opacity(0.1)],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                ),
                                lineWidth: 1
                            )
                    )
            )
        }
        .buttonStyle(ScaleButtonStyle())
    }
    
    // MARK: - Empty State
    
    private var emptyStateView: some View {
        VStack(spacing: 24) {
            Spacer()
            
            ZStack {
                Circle()
                    .fill(
                        RadialGradient(
                            colors: [Color.green.opacity(0.2), Color.clear],
                            center: .center,
                            startRadius: 0,
                            endRadius: 80
                        )
                    )
                    .frame(width: 160, height: 160)
                
                Text("📔")
                    .font(.system(size: 72))
            }
            
            VStack(spacing: 12) {
                Text("Start Your Journey")
                    .font(.system(size: 24, weight: .bold, design: .rounded))
                    .foregroundColor(.white)
                
                Text("Your journal is empty.\nTap below to create your first entry.")
                    .font(.system(size: 16))
                    .foregroundColor(.white.opacity(0.6))
                    .multilineTextAlignment(.center)
            }
            
            Button(action: { showNewEntry = true }) {
                HStack(spacing: 8) {
                    Image(systemName: "plus.circle.fill")
                    Text("Create Entry")
                }
                .font(.system(size: 17, weight: .semibold))
                .foregroundColor(.white)
                .padding(.horizontal, 32)
                .padding(.vertical, 16)
                .background(
                    Capsule()
                        .fill(
                            LinearGradient(
                                colors: [Color(red: 0.2, green: 0.5, blue: 0.3), Color(red: 0.15, green: 0.45, blue: 0.25)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                )
                .shadow(color: .green.opacity(0.4), radius: 20, x: 0, y: 10)
            }
            .buttonStyle(ScaleButtonStyle())
            
            Spacer()
        }
        .padding(.horizontal, 40)
    }
    
    // MARK: - Loading View
    
    private var loadingView: some View {
        VStack(spacing: 20) {
            Spacer()
            
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                .scaleEffect(1.5)
            
            Text("Loading your journal...")
                .font(.system(size: 16))
                .foregroundColor(.white.opacity(0.6))
            
            Spacer()
        }
    }
    
    // MARK: - Load More
    
    private var loadMoreIndicator: some View {
        Button(action: { journalManager.loadMoreEntries() }) {
            HStack(spacing: 8) {
                if journalManager.isLoading {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                } else {
                    Text("Load More")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(.white.opacity(0.7))
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
        }
        .disabled(journalManager.isLoading)
    }
    
    // MARK: - Helpers
    
    private var formattedDate: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "EEEE, MMMM d"
        return formatter.string(from: Date())
    }
}

// MARK: - Journal Entry Card

struct JournalEntryCard: View {
    let entry: JournalPreview
    let onTap: () -> Void
    
    @State private var isPressed = false
    
    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 12) {
                // Header row
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(entry.title)
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundColor(.white)
                            .lineLimit(1)
                        
                        Text(formatEntryDate(entry.entryDate))
                            .font(.system(size: 13))
                            .foregroundColor(.white.opacity(0.5))
                    }
                    
                    Spacer()
                    
                    // Mood indicator - shows AI loading animation when nil
                    MoodBadge(score: entry.moodScore.map { Int($0.intValue) })
                }
                
                // Body preview
                if !entry.bodyPreview.isEmpty {
                    Text(entry.bodyPreview)
                        .font(.system(size: 15))
                        .foregroundColor(.white.opacity(0.7))
                        .lineLimit(3)
                        .multilineTextAlignment(.leading)
                }
                
                // Footer
                HStack {
                    Text(formatTimeAgo(entry.updatedAt))
                        .font(.system(size: 12))
                        .foregroundColor(.white.opacity(0.4))
                    
                    Spacer()
                    
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(.white.opacity(0.3))
                }
            }
            .padding(18)
            .background(
                RoundedRectangle(cornerRadius: 20)
                    .fill(.ultraThinMaterial.opacity(0.6))
                    .overlay(
                        RoundedRectangle(cornerRadius: 20)
                            .stroke(
                                LinearGradient(
                                    colors: [.white.opacity(0.2), .white.opacity(0.05)],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                ),
                                lineWidth: 1
                            )
                    )
            )
            .shadow(color: .black.opacity(0.1), radius: 10, x: 0, y: 5)
        }
        .buttonStyle(ScaleButtonStyle())
    }
    
    private func formatEntryDate(_ date: Kotlinx_datetimeLocalDate) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMMM d, yyyy"
        
        // Convert Kotlin LocalDate to Swift Date
        let calendar = Calendar.current
        let components = DateComponents(
            year: Int(date.year),
            month: Int(date.monthNumber),
            day: Int(date.dayOfMonth)
        )
        if let swiftDate = calendar.date(from: components) {
            return formatter.string(from: swiftDate)
        }
        return "\(date.month) \(date.dayOfMonth), \(date.year)"
    }
    
    private func formatTimeAgo(_ instant: Kotlinx_datetimeInstant) -> String {
        let epochSeconds = instant.epochSeconds
        let date = Date(timeIntervalSince1970: TimeInterval(epochSeconds))
        
        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .short
        return formatter.localizedString(for: date, relativeTo: Date())
    }
}

// MARK: - Mood Badge

struct MoodBadge: View {
    let score: Int?
    
    var body: some View {
        AnimatedMoodBadge(score: score)
    }
}

// MARK: - New Entry Sheet

@available(iOS 17.0, *)
struct NewEntrySheet: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var journalManager: JournalManager
    
    @State private var title = ""
    @State private var selectedDate = Date()
    @State private var isCreating = false
    
    let onComplete: (Journal?) -> Void
    
    var body: some View {
        NavigationView {
            ZStack {
                Color(red: 0.08, green: 0.1, blue: 0.12)
                    .ignoresSafeArea()
                
                VStack(spacing: 24) {
                    // Title input
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Title (optional)")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(.white.opacity(0.6))
                        
                        TextField("Leave empty for today's date", text: $title)
                            .font(.system(size: 17))
                            .foregroundColor(.white)
                            .padding(16)
                            .background(
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(.white.opacity(0.08))
                            )
                    }
                    
                    // Date picker
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Entry Date")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(.white.opacity(0.6))
                        
                        DatePicker("", selection: $selectedDate, displayedComponents: .date)
                            .datePickerStyle(.graphical)
                            .colorScheme(.dark)
                            .tint(.green)
                    }
                    
                    Spacer()
                    
                    // Create button
                    Button(action: createEntry) {
                        HStack {
                            if isCreating {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            } else {
                                Text("Start Writing")
                                    .font(.system(size: 17, weight: .semibold))
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(
                            RoundedRectangle(cornerRadius: 16)
                                .fill(
                                    LinearGradient(
                                        colors: [Color(red: 0.2, green: 0.5, blue: 0.3), Color(red: 0.15, green: 0.45, blue: 0.25)],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                )
                        )
                        .foregroundColor(.white)
                    }
                    .disabled(isCreating)
                }
                .padding(24)
            }
            .navigationTitle("New Entry")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                    .foregroundColor(.white.opacity(0.7))
                }
            }
        }
    }
    
    private func createEntry() {
        isCreating = true
        
        journalManager.createEntry(
            title: title.isEmpty ? nil : title,
            body: nil,
            entryDate: selectedDate
        ) { journal in
            isCreating = false
            dismiss()
            onComplete(journal)
        }
    }
}

// MARK: - Journal Background

struct JournalBackground: View {
    @State private var animate = false
    
    var body: some View {
        ZStack {
            // Base gradient
            LinearGradient(
                colors: [
                    Color(red: 0.06, green: 0.08, blue: 0.1),
                    Color(red: 0.08, green: 0.1, blue: 0.14),
                    Color(red: 0.05, green: 0.08, blue: 0.1)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()
            
            // Animated orbs
            Circle()
                .fill(
                    RadialGradient(
                        colors: [Color.green.opacity(0.15), Color.clear],
                        center: .center,
                        startRadius: 0,
                        endRadius: 200
                    )
                )
                .frame(width: 400, height: 400)
                .offset(x: -100, y: animate ? -150 : -180)
                .blur(radius: 60)
            
            Circle()
                .fill(
                    RadialGradient(
                        colors: [Color.teal.opacity(0.1), Color.clear],
                        center: .center,
                        startRadius: 0,
                        endRadius: 150
                    )
                )
                .frame(width: 300, height: 300)
                .offset(x: 150, y: animate ? 200 : 230)
                .blur(radius: 50)
            
            Circle()
                .fill(
                    RadialGradient(
                        colors: [Color.mint.opacity(0.08), Color.clear],
                        center: .center,
                        startRadius: 0,
                        endRadius: 100
                    )
                )
                .frame(width: 200, height: 200)
                .offset(x: -80, y: animate ? 400 : 370)
                .blur(radius: 40)
        }
        .onAppear {
            withAnimation(.easeInOut(duration: 8).repeatForever(autoreverses: true)) {
                animate = true
            }
        }
    }
}

// MARK: - Scale Button Style

struct ScaleButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.97 : 1.0)
            .animation(.spring(response: 0.3, dampingFraction: 0.6), value: configuration.isPressed)
    }
}

// MARK: - Preview

@available(iOS 17.0, *)
#Preview {
    JournalListView()
        .environmentObject(AuthManager())
        .environmentObject(JournalManager())
}
