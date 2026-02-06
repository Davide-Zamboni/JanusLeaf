import SwiftUI
import Shared

// MARK: - Main Tab View

@available(iOS 17.0, *)
struct MainTabView: View {
    @StateObject private var journalListViewModelOwner = SharedViewModelOwner(
        viewModel: SharedModule.shared.createJournalListViewModel(),
        onDeinit: { (viewModel: JournalListViewModel) in
            viewModel.clear()
        }
    )
    
    @State private var selectedTab: Int = 0
    @State private var isCreatingEntry = false
    @State private var navigateToEntry: String? = nil
    @State private var isEditorPresented = false

    private var journalListViewModel: JournalListViewModel {
        journalListViewModelOwner.viewModel
    }
    
    var body: some View {
        ZStack(alignment: .bottom) {
            // Content
            Group {
                if selectedTab == 0 {
                    JournalTabContent(
                        journalListViewModel: journalListViewModel,
                        navigateToEntry: $navigateToEntry,
                        isEditorPresented: $isEditorPresented,
                        createNewEntry: createNewEntry
                    )
                } else {
                    MoodInsightsView()
                }
            }
            
            // Custom floating tab bar (App Store style)
            if !isEditorPresented {
                floatingTabBar
                    .padding(.bottom, 8)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(.spring(response: 0.35, dampingFraction: 0.8), value: isEditorPresented)
        .preferredColorScheme(.dark)
    }
    
    // MARK: - Floating Tab Bar (Liquid Glass Style)
    
    private var floatingTabBar: some View {
        HStack(spacing: 16) {
            // Main tab bar pill with liquid glass effect
            liquidGlassTabPill
            
            // Separate + button with liquid glass effect
            liquidGlassAddButton
        }
    }
    
    private var liquidGlassTabPill: some View {
        HStack(spacing: 0) {
            ForEach(0..<2) { index in
                LiquidGlassTabItem(
                    icon: index == 0 ? "book.fill" : "chart.line.uptrend.xyaxis",
                    label: index == 0 ? "Journal" : "Insights",
                    isSelected: selectedTab == index
                ) {
                    withAnimation(.spring(response: 0.4, dampingFraction: 0.75)) {
                        selectedTab = index
                    }
                }
            }
        }
        .padding(.horizontal, 6)
        .padding(.vertical, 6)
        .background {
            // Layered liquid glass effect
            ZStack {
                // Deep blur layer
                Capsule()
                    .fill(.ultraThinMaterial)
                    .environment(\.colorScheme, .dark)
                
                // Inner subtle gradient for depth
                Capsule()
                    .fill(
                        LinearGradient(
                            colors: [
                                Color.white.opacity(0.08),
                                Color.white.opacity(0.02),
                                Color.clear
                            ],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    )
                
                // Subtle edge highlight
                Capsule()
                    .strokeBorder(
                        LinearGradient(
                            colors: [
                                Color.white.opacity(0.25),
                                Color.white.opacity(0.08),
                                Color.white.opacity(0.03)
                            ],
                            startPoint: .top,
                            endPoint: .bottom
                        ),
                        lineWidth: 0.5
                    )
            }
        }
        .shadow(color: Color.black.opacity(0.3), radius: 20, x: 0, y: 10)
        .shadow(color: Color.black.opacity(0.15), radius: 5, x: 0, y: 2)
    }
    
    private var liquidGlassAddButton: some View {
        Button(action: createNewEntry) {
            ZStack {
                if isCreatingEntry {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        .scaleEffect(0.9)
                } else {
                    Image(systemName: "plus")
                        .font(.system(size: 22, weight: .semibold))
                        .foregroundStyle(
                            LinearGradient(
                                colors: [.white, .white.opacity(0.85)],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                }
            }
            .frame(width: 52, height: 52)
            .background {
                ZStack {
                    // Deep blur layer
                    Circle()
                        .fill(.ultraThinMaterial)
                        .environment(\.colorScheme, .dark)
                    
                    // Inner subtle gradient
                    Circle()
                        .fill(
                            LinearGradient(
                                colors: [
                                    Color.white.opacity(0.1),
                                    Color.white.opacity(0.02)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                    
                    // Edge highlight
                    Circle()
                        .strokeBorder(
                            LinearGradient(
                                colors: [
                                    Color.white.opacity(0.3),
                                    Color.white.opacity(0.08)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                            lineWidth: 0.5
                        )
                }
            }
        }
        .disabled(isCreatingEntry)
        .scaleEffect(isCreatingEntry ? 0.92 : 1.0)
        .shadow(color: Color.black.opacity(0.3), radius: 15, x: 0, y: 8)
        .shadow(color: Color.black.opacity(0.15), radius: 4, x: 0, y: 2)
        .animation(.spring(response: 0.3, dampingFraction: 0.7), value: isCreatingEntry)
    }
    
    // MARK: - Create New Entry
    
    private func createNewEntry() {
        guard !isCreatingEntry else { return }
        
        let generator = UIImpactFeedbackGenerator(style: .medium)
        generator.impactOccurred()
        
        isCreatingEntry = true
        selectedTab = 0

        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd"
        let localDate = SharedModule.shared.parseLocalDate(iso: dateFormatter.string(from: Date()))
        
        journalListViewModel.createEntry(
            title: nil,
            body: nil,
            entryDate: localDate
        ) { journal in
            DispatchQueue.main.async {
                isCreatingEntry = false
                if let journal = journal {
                    navigateToEntry = journal.id
                }
            }
        }
    }
}

// MARK: - Liquid Glass Tab Item

struct LiquidGlassTabItem: View {
    let icon: String
    let label: String
    let isSelected: Bool
    let action: () -> Void
    
    // Accent color for the app
    private let accentColor = Color(red: 0.25, green: 0.78, blue: 0.55) // Refined green
    
    var body: some View {
        Button(action: {
            let generator = UIImpactFeedbackGenerator(style: .light)
            generator.impactOccurred()
            action()
        }) {
            HStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.system(size: 18, weight: .medium))
                
                Text(label)
                    .font(.system(size: 14, weight: .semibold))
            }
            .foregroundStyle(isSelected ? accentColor : .white.opacity(0.55))
            .padding(.horizontal, isSelected ? 18 : 14)
            .padding(.vertical, 12)
            .background {
                if isSelected {
                    Capsule()
                        .fill(
                            LinearGradient(
                                colors: [
                                    accentColor.opacity(0.25),
                                    accentColor.opacity(0.15)
                                ],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                        .overlay(
                            Capsule()
                                .strokeBorder(
                                    accentColor.opacity(0.3),
                                    lineWidth: 0.5
                                )
                        )
                }
            }
            .contentShape(Capsule())
        }
        .buttonStyle(LiquidGlassButtonStyle())
    }
}

// MARK: - Liquid Glass Button Style

struct LiquidGlassButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.95 : 1.0)
            .animation(.spring(response: 0.25, dampingFraction: 0.7), value: configuration.isPressed)
    }
}

// MARK: - Journal Tab Content

@available(iOS 17.0, *)
struct JournalTabContent: View {
    let journalListViewModel: JournalListViewModel
    
    @Binding var navigateToEntry: String?
    @Binding var isEditorPresented: Bool
    let createNewEntry: () -> Void
    
    @State private var selectedEntryId: String? = nil
    @State private var animateItems = false
    @State private var moodPollingTimer: Timer? = nil
    @State private var inspirationPollingTimer: Timer? = nil
    @State private var showLogoutConfirmation = false
    @State private var entries: [JournalPreview] = []
    @State private var hasMore = false
    @State private var isLoading = false
    @State private var currentUsername: String? = nil
    @State private var inspirationIsLoading = false
    @State private var inspirationIsNotFound = false
    @State private var inspirationQuote: InspirationalQuote? = nil
    
    private var isNavigating: Bool {
        selectedEntryId != nil || navigateToEntry != nil
    }
    
    var body: some View {
        NavigationStack {
            ZStack {
                JournalBackground()
                
                VStack(spacing: 0) {
                    headerView
                    
                    if isLoading && entries.isEmpty {
                        loadingView
                    } else if entries.isEmpty {
                        emptyStateView
                    } else {
                        journalListView
                    }
                }
            }
            .toolbar(.hidden, for: .navigationBar)
            .toolbar(.hidden, for: .tabBar)
            .navigationDestination(isPresented: Binding(
                get: { isNavigating },
                set: { 
                    if !$0 { 
                        selectedEntryId = nil
                        navigateToEntry = nil
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                            journalListViewModel.loadEntries()
                        }
                    } 
                }
            )) {
                if let entryId = navigateToEntry ?? selectedEntryId {
                    JournalEditorView(entryId: entryId)
                }
            }
            .confirmationDialog("Sign Out", isPresented: $showLogoutConfirmation, titleVisibility: .visible) {
                Button("Sign Out", role: .destructive) {
                    journalListViewModel.logout()
                }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("Are you sure you want to sign out?")
            }
        }
        .onAppear {
            journalListViewModel.loadEntries()
            journalListViewModel.fetchQuote()
            withAnimation(.easeOut(duration: 0.6).delay(0.3)) {
                animateItems = true
            }
            startMoodPolling()
            startInspirationPolling()
        }
        .onDisappear {
            stopMoodPolling()
            stopInspirationPolling()
        }
        .task {
            await observeJournalUiState()
        }
        .task {
            await observeAuthState()
        }
        .task {
            await observeInspirationState()
        }
        .onChange(of: navigateToEntry) { _, newValue in
            if newValue != nil {
                selectedEntryId = newValue
            }
        }
        .onChange(of: isNavigating) { _, newValue in
            withAnimation(.easeInOut(duration: 0.2)) {
                isEditorPresented = newValue
            }
        }
    }
    
    // MARK: - Header View
    
    private var headerView: some View {
        HStack(alignment: .center) {
            VStack(alignment: .leading, spacing: 4) {
                Text(headerTitle)
                    .font(.system(size: 34, weight: .bold, design: .rounded))
                    .foregroundColor(.white)
                
                Text(formattedDate)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(.white.opacity(0.6))
            }
            
            Spacer()
            
            Button(action: { showLogoutConfirmation = true }) {
                ZStack {
                    // Liquid glass background
                    Circle()
                        .fill(.ultraThinMaterial)
                        .environment(\.colorScheme, .dark)
                    
                    // Inner gradient for depth
                    Circle()
                        .fill(
                            LinearGradient(
                                colors: [
                                    Color.white.opacity(0.1),
                                    Color.white.opacity(0.02)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                    
                    // Edge highlight
                    Circle()
                        .strokeBorder(
                            LinearGradient(
                                colors: [
                                    Color.white.opacity(0.25),
                                    Color.white.opacity(0.05)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                            lineWidth: 0.5
                        )
                    
                    Image(systemName: "person.fill")
                        .font(.system(size: 18, weight: .medium))
                        .foregroundStyle(
                            LinearGradient(
                                colors: [.white.opacity(0.9), .white.opacity(0.7)],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                }
                .frame(width: 44, height: 44)
                .shadow(color: Color.black.opacity(0.2), radius: 10, x: 0, y: 4)
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, 60)
        .padding(.bottom, 8)
        .opacity(animateItems ? 1 : 0)
        .offset(y: animateItems ? 0 : -20)
    }
    
    // MARK: - Journal List
    
    private var journalListView: some View {
        ScrollView(showsIndicators: false) {
            LazyVStack(spacing: 16) {
                InspirationalQuoteView(
                    isLoading: inspirationIsLoading,
                    quote: inspirationQuote,
                    isNotFound: inspirationIsNotFound
                )
                    .padding(.top, 4)
                    .opacity(animateItems ? 1 : 0)
                    .offset(y: animateItems ? 0 : 20)
                    .animation(
                        .spring(response: 0.6, dampingFraction: 0.8).delay(0.1),
                        value: animateItems
                    )
                
                ForEach(Array(entries.enumerated()), id: \.element.id) { index, entry in
                    JournalEntryCard(entry: entry) {
                        selectedEntryId = entry.id
                    }
                    .opacity(animateItems ? 1 : 0)
                    .offset(y: animateItems ? 0 : 30)
                    .animation(
                        .spring(response: 0.5, dampingFraction: 0.8)
                        .delay(0.2 + Double(index) * 0.05),
                        value: animateItems
                    )
                }
                
                if hasMore {
                    loadMoreIndicator
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 140)
        }
        .refreshable {
            journalListViewModel.loadEntries()
            journalListViewModel.fetchQuote()
        }
    }
    
    // MARK: - Empty State
    
    private var emptyStateView: some View {
        ScrollView {
            VStack(spacing: 24) {
                InspirationPendingView()
                    .padding(.top, 20)
                
                VStack(spacing: 32) {
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
                        
                        Text("Your journal is empty.\nTap the + button to create your first entry.")
                            .font(.system(size: 16))
                            .foregroundColor(.white.opacity(0.6))
                            .multilineTextAlignment(.center)
                    }
                }
                .padding(.top, 40)
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 140)
        }
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
        Button(action: { journalListViewModel.loadMoreEntries() }) {
            HStack(spacing: 8) {
                if isLoading {
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
        .disabled(isLoading)
    }
    
    // MARK: - Helpers
    
    private var headerTitle: String {
        if let username = currentUsername, !username.isEmpty {
            return "\(username)'s Journal"
        }
        return "Journal"
    }
    
    private var formattedDate: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "EEEE, MMMM d"
        return formatter.string(from: Date())
    }
    
    // MARK: - Polling
    
    private func startMoodPolling() {
        moodPollingTimer = Timer.scheduledTimer(withTimeInterval: 5.0, repeats: true) { [weak journalListViewModel] _ in
            Task { @MainActor in
                guard let journalListViewModel = journalListViewModel else { return }
                let hasPendingMoods = entries.contains { $0.moodScore == nil }
                if hasPendingMoods {
                    journalListViewModel.loadEntries()
                }
            }
        }
    }
    
    private func stopMoodPolling() {
        moodPollingTimer?.invalidate()
        moodPollingTimer = nil
    }
    
    private func startInspirationPolling() {
        inspirationPollingTimer = Timer.scheduledTimer(withTimeInterval: 10.0, repeats: true) { [weak journalListViewModel] _ in
            Task { @MainActor in
                guard let journalListViewModel = journalListViewModel else { return }
                if inspirationIsNotFound || (inspirationQuote == nil && !inspirationIsLoading) {
                    journalListViewModel.fetchQuote()
                }
            }
        }
    }
    
    private func stopInspirationPolling() {
        inspirationPollingTimer?.invalidate()
        inspirationPollingTimer = nil
    }

    @MainActor
    private func observeJournalUiState() async {
        for await state in journalListViewModel.uiState {
            entries = state.entries
            hasMore = state.hasMore
            isLoading = state.isLoading
        }
    }

    @MainActor
    private func observeAuthState() async {
        for await state in journalListViewModel.authState {
            currentUsername = state.user?.username
        }
    }

    @MainActor
    private func observeInspirationState() async {
        for await state in journalListViewModel.inspirationState {
            inspirationIsLoading = state.isLoading
            inspirationIsNotFound = state.isNotFound
            inspirationQuote = state.quote
        }
    }
}

// MARK: - Preview

@available(iOS 17.0, *)
#Preview {
    MainTabView()
}
