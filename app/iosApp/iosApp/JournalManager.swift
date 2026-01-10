import SwiftUI
import Shared

/// ObservableObject wrapper for the Kotlin journal service
@MainActor
class JournalManager: ObservableObject {
    private let journalService: IosJournalService
    
    @Published var isLoading: Bool = false
    @Published var errorMessage: String? = nil
    @Published var entries: [JournalPreview] = []
    @Published var currentEntry: Journal? = nil
    @Published var hasMore: Bool = true
    
    // Auto-save state
    @Published var isSaving: Bool = false
    @Published var lastSavedAt: Date? = nil
    
    private var loadingCancellable: Cancellable?
    private var errorCancellable: Cancellable?
    private var entriesCancellable: Cancellable?
    private var currentEntryCancellable: Cancellable?
    private var hasMoreCancellable: Cancellable?
    
    // Auto-save timer
    private var autoSaveTimer: Timer?
    private var pendingBodyUpdate: String?
    private var currentVersion: Int64?
    
    init() {
        self.journalService = SharedModule.shared.createJournalService()
        setupObservers()
    }
    
    private func setupObservers() {
        loadingCancellable = journalService.observeLoading { [weak self] value in
            DispatchQueue.main.async {
                self?.isLoading = value.boolValue
            }
        }
        
        errorCancellable = journalService.observeError { [weak self] value in
            DispatchQueue.main.async {
                self?.errorMessage = value
            }
        }
        
        entriesCancellable = journalService.observeEntries { [weak self] value in
            DispatchQueue.main.async {
                self?.entries = value.map { $0 }
            }
        }
        
        currentEntryCancellable = journalService.observeCurrentEntry { [weak self] value in
            DispatchQueue.main.async {
                self?.currentEntry = value
                self?.currentVersion = value?.version
            }
        }
        
        hasMoreCancellable = journalService.observeHasMore { [weak self] value in
            DispatchQueue.main.async {
                self?.hasMore = value.boolValue
            }
        }
    }
    
    // MARK: - Load Entries
    
    func loadEntries() {
        journalService.loadEntries {}
    }
    
    func loadMoreEntries() {
        journalService.loadMoreEntries {}
    }
    
    func refresh() {
        journalService.loadEntries {}
    }
    
    // MARK: - Create Entry
    
    func createEntry(
        title: String? = nil,
        body: String? = nil,
        entryDate: Date? = nil,
        completion: @escaping (Journal?) -> Void
    ) {
        let dateString = entryDate.map { formatDate($0) }
        
        journalService.createEntry(
            title: title,
            body: body,
            entryDate: dateString,
            onSuccess: { journal in
                DispatchQueue.main.async {
                    completion(journal)
                }
            },
            onError: { _ in
                DispatchQueue.main.async {
                    completion(nil)
                }
            }
        )
    }
    
    // MARK: - Get Entry
    
    func getEntry(id: String, completion: @escaping (Journal?) -> Void) {
        journalService.getEntry(
            id: id,
            onSuccess: { journal in
                DispatchQueue.main.async {
                    completion(journal)
                }
            },
            onError: { _ in
                DispatchQueue.main.async {
                    completion(nil)
                }
            }
        )
    }
    
    // MARK: - Update Body (with auto-save support)
    
    func updateBody(_ body: String, for entryId: String) {
        pendingBodyUpdate = body
        
        // Debounce auto-save
        autoSaveTimer?.invalidate()
        autoSaveTimer = Timer.scheduledTimer(withTimeInterval: 1.5, repeats: false) { [weak self] _ in
            Task { @MainActor in
                self?.performAutoSave(entryId: entryId)
            }
        }
    }
    
    private func performAutoSave(entryId: String) {
        guard let body = pendingBodyUpdate else { return }
        
        isSaving = true
        
        journalService.updateBody(
            id: entryId,
            body: body,
            expectedVersion: currentVersion.map { KotlinLong(value: $0) },
            onSuccess: { [weak self] newVersion in
                Task { @MainActor in
                    self?.currentVersion = newVersion.int64Value
                    self?.isSaving = false
                    self?.lastSavedAt = Date()
                    self?.pendingBodyUpdate = nil
                }
            },
            onError: { [weak self] error in
                Task { @MainActor in
                    self?.isSaving = false
                    // Show error but don't clear pending - will retry
                }
            }
        )
    }
    
    func forceSave(entryId: String, completion: @escaping (Bool) -> Void) {
        autoSaveTimer?.invalidate()
        
        guard let body = pendingBodyUpdate ?? currentEntry?.body else {
            completion(true)
            return
        }
        
        isSaving = true
        
        journalService.updateBody(
            id: entryId,
            body: body,
            expectedVersion: currentVersion.map { KotlinLong(value: $0) },
            onSuccess: { [weak self] newVersion in
                DispatchQueue.main.async {
                    self?.currentVersion = newVersion.int64Value
                    self?.isSaving = false
                    self?.lastSavedAt = Date()
                    self?.pendingBodyUpdate = nil
                    completion(true)
                }
            },
            onError: { [weak self] _ in
                DispatchQueue.main.async {
                    self?.isSaving = false
                    completion(false)
                }
            }
        )
    }
    
    // MARK: - Update Metadata
    
    func updateTitle(_ title: String, for entryId: String, completion: @escaping (Bool) -> Void) {
        journalService.updateMetadata(
            id: entryId,
            title: title,
            moodScore: nil,
            onSuccess: { _ in
                DispatchQueue.main.async {
                    completion(true)
                }
            },
            onError: { _ in
                DispatchQueue.main.async {
                    completion(false)
                }
            }
        )
    }
    
    func updateMoodScore(_ score: Int, for entryId: String, completion: @escaping (Bool) -> Void) {
        journalService.updateMetadata(
            id: entryId,
            title: nil,
            moodScore: KotlinInt(int: Int32(score)),
            onSuccess: { _ in
                DispatchQueue.main.async {
                    completion(true)
                }
            },
            onError: { _ in
                DispatchQueue.main.async {
                    completion(false)
                }
            }
        )
    }
    
    // MARK: - Delete Entry
    
    func deleteEntry(id: String, completion: @escaping (Bool) -> Void) {
        journalService.deleteEntry(
            id: id,
            onSuccess: {
                DispatchQueue.main.async {
                    completion(true)
                }
            },
            onError: { _ in
                DispatchQueue.main.async {
                    completion(false)
                }
            }
        )
    }
    
    // MARK: - Helpers
    
    func clearCurrentEntry() {
        autoSaveTimer?.invalidate()
        pendingBodyUpdate = nil
        currentVersion = nil
        journalService.clearCurrentEntry()
    }
    
    func clearError() {
        journalService.clearError()
    }
    
    private func formatDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: date)
    }
    
    deinit {
        autoSaveTimer?.invalidate()
        loadingCancellable?.cancel()
        errorCancellable?.cancel()
        entriesCancellable?.cancel()
        currentEntryCancellable?.cancel()
        hasMoreCancellable?.cancel()
    }
}

// MARK: - Mood Score Helper

extension JournalManager {
    static func moodEmoji(for score: Int?) -> String {
        guard let score = score else { return "😶" }
        switch score {
        case 1...2: return "😢"
        case 3...4: return "😔"
        case 5...6: return "😐"
        case 7...8: return "😊"
        case 9...10: return "😄"
        default: return "😶"
        }
    }
    
    static func moodColor(for score: Int?) -> Color {
        guard let score = score else { return .gray }
        switch score {
        case 1...2: return .red
        case 3...4: return .orange
        case 5...6: return .yellow
        case 7...8: return .green
        case 9...10: return .mint
        default: return .gray
        }
    }
}
