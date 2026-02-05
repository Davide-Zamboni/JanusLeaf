import SwiftUI
import Shared

private func toBool(_ value: Bool) -> Bool { value }
private func toBool(_ value: KotlinBoolean) -> Bool { value.boolValue }

private func toJournalPreviewList(_ value: Any?) -> [JournalPreview] {
    if let list = value as? [JournalPreview] {
        return list
    }
    if let list = value as? KotlinArray<AnyObject> {
        let count = Int(list.size)
        return (0..<count).compactMap { index in
            list.get(index: Int32(index)) as? JournalPreview
        }
    }
    if let list = value as? [Any] {
        return list.compactMap { $0 as? JournalPreview }
    }
    return []
}

@MainActor
final class JournalListViewModelAdapter: ObservableObject {
    private let viewModel: JournalListViewModel
    private let flowObserver = FlowObserver()
    private var uiStateCancellable: Cancellable?
    private var inspirationCancellable: Cancellable?

    @Published var isLoading: Bool = false
    @Published var errorMessage: String? = nil
    @Published var entries: [JournalPreview] = []
    @Published var hasMore: Bool = true
    @Published var isCreatingEntry: Bool = false

    @Published var inspirationIsLoading: Bool = false
    @Published var inspirationIsNotFound: Bool = false
    @Published var inspirationQuote: InspirationalQuote? = nil
    @Published var inspirationErrorMessage: String? = nil

    init(viewModel: JournalListViewModel = SharedModule.shared.createJournalListViewModel()) {
        self.viewModel = viewModel
        bindState()
    }

    private func bindState() {
        uiStateCancellable = flowObserver.observe(flow: viewModel.uiState) { [weak self] state in
            guard let state = state as? JournalListUiState else { return }
            self?.isLoading = toBool(state.isLoading)
            self?.errorMessage = state.errorMessage
            self?.entries = toJournalPreviewList(state.entries)
            self?.hasMore = toBool(state.hasMore)
            self?.isCreatingEntry = toBool(state.isCreatingEntry)
        }

        inspirationCancellable = flowObserver.observe(flow: viewModel.inspirationState) { [weak self] state in
            guard let state = state as? InspirationUiState else { return }
            self?.inspirationIsLoading = toBool(state.isLoading)
            self?.inspirationIsNotFound = toBool(state.isNotFound)
            self?.inspirationQuote = state.quote
            self?.inspirationErrorMessage = state.errorMessage
        }
    }

    // MARK: - Load Entries

    func loadEntries() {
        viewModel.loadEntries()
    }

    func loadMoreEntries() {
        viewModel.loadMoreEntries()
    }

    func refresh() {
        viewModel.loadEntries()
    }

    func fetchQuote() {
        viewModel.fetchQuote()
    }

    func refreshQuote() {
        viewModel.fetchQuote()
    }

    // MARK: - Create Entry

    func createEntry(
        title: String? = nil,
        body: String? = nil,
        entryDate: Date? = nil,
        completion: @escaping (Journal?) -> Void
    ) {
        let dateString = entryDate.map { formatDate($0) }
        let localDate = SharedModule.shared.parseLocalDate(iso: dateString)
        viewModel.createEntry(title: title, body: body, entryDate: localDate) { journal in
            completion(journal)
        }
    }

    func clearError() {
        viewModel.clearError()
        viewModel.clearInspirationError()
    }

    func formattedGeneratedDate() -> String {
        guard let quote = inspirationQuote else { return "" }
        let epochSeconds = quote.generatedAt.epochSeconds
        let date = Date(timeIntervalSince1970: TimeInterval(epochSeconds))

        let calendar = Calendar.current
        if calendar.isDateInToday(date) {
            return "Generated today"
        } else if calendar.isDateInYesterday(date) {
            return "Generated yesterday"
        } else {
            let formatter = RelativeDateTimeFormatter()
            formatter.unitsStyle = .full
            return "Generated \(formatter.localizedString(for: date, relativeTo: Date()))"
        }
    }

    private func formatDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: date)
    }

    deinit {
        uiStateCancellable?.cancel()
        inspirationCancellable?.cancel()
        flowObserver.cancel()
        viewModel.clear()
    }
}

@MainActor
final class JournalEditorViewModelAdapter: ObservableObject {
    private let viewModel: JournalEditorViewModel
    private let flowObserver = FlowObserver()
    private var uiStateCancellable: Cancellable?

    @Published var isLoading: Bool = false
    @Published var errorMessage: String? = nil
    @Published var currentEntry: Journal? = nil
    @Published var isSaving: Bool = false
    @Published var lastSavedAt: Date? = nil

    init(viewModel: JournalEditorViewModel = SharedModule.shared.createJournalEditorViewModel()) {
        self.viewModel = viewModel
        bindState()
    }

    private func bindState() {
        uiStateCancellable = flowObserver.observe(flow: viewModel.uiState) { [weak self] state in
            guard let state = state as? JournalEditorUiState else { return }
            self?.isLoading = toBool(state.isLoading)
            self?.errorMessage = state.errorMessage
            self?.currentEntry = state.entry
            self?.isSaving = toBool(state.isSaving)

            if let millis = state.lastSavedAtEpochMillis?.int64Value {
                self?.lastSavedAt = Date(timeIntervalSince1970: TimeInterval(millis) / 1000.0)
            } else {
                self?.lastSavedAt = nil
            }
        }
    }

    func loadEntry(id: String, completion: @escaping (Journal?) -> Void) {
        viewModel.bindEntry(entryId: id)
        viewModel.loadEntry(entryId: id, onSuccess: { journal in
            completion(journal)
        }, onError: { _ in
            completion(nil)
        })
    }

    func updateBody(_ body: String, for entryId: String) {
        viewModel.updateBody(entryId: entryId, body: body)
    }

    func updateTitle(_ title: String, for entryId: String, completion: @escaping (Bool) -> Void) {
        viewModel.updateTitle(entryId: entryId, title: title, onComplete: { result in
            completion(toBool(result))
        })
    }

    func updateMoodScore(_ score: Int, for entryId: String, completion: @escaping (Bool) -> Void) {
        viewModel.updateMoodScore(entryId: entryId, score: Int32(score), onComplete: { result in
            completion(toBool(result))
        })
    }

    func forceSave(entryId: String, completion: @escaping (Bool) -> Void) {
        viewModel.forceSave(entryId: entryId, onComplete: { result in
            completion(toBool(result))
        })
    }

    func deleteEntry(id: String, completion: @escaping (Bool) -> Void) {
        viewModel.deleteEntry(entryId: id, onComplete: { result in
            completion(toBool(result))
        })
    }

    func clearCurrentEntry() {
        viewModel.clearBoundEntry()
    }

    func clearError() {
        viewModel.clearError()
    }

    deinit {
        uiStateCancellable?.cancel()
        flowObserver.cancel()
        viewModel.clear()
    }
}

@MainActor
final class MoodInsightsViewModelAdapter: ObservableObject {
    private let viewModel: MoodInsightsViewModel
    private let flowObserver = FlowObserver()
    private var uiStateCancellable: Cancellable?

    @Published var isLoading: Bool = false
    @Published var errorMessage: String? = nil
    @Published var entries: [JournalPreview] = []

    init(viewModel: MoodInsightsViewModel = SharedModule.shared.createMoodInsightsViewModel()) {
        self.viewModel = viewModel
        bindState()
    }

    private func bindState() {
        uiStateCancellable = flowObserver.observe(flow: viewModel.uiState) { [weak self] state in
            guard let state = state as? MoodInsightsUiState else { return }
            self?.isLoading = toBool(state.isLoading)
            self?.errorMessage = state.errorMessage
            self?.entries = toJournalPreviewList(state.entries)
        }
    }

    func loadEntries() {
        viewModel.loadEntries()
    }

    func clearError() {
        viewModel.clearError()
    }

    deinit {
        uiStateCancellable?.cancel()
        flowObserver.cancel()
        viewModel.clear()
    }
}

@MainActor
final class ProfileViewModelAdapter: ObservableObject {
    private let viewModel: ProfileViewModel
    private let flowObserver = FlowObserver()
    private var authStateCancellable: Cancellable?
    private var entriesCancellable: Cancellable?

    @Published var currentUserEmail: String? = nil
    @Published var currentUsername: String? = nil
    @Published var isAuthenticated: Bool = false
    @Published var entries: [JournalPreview] = []

    init(viewModel: ProfileViewModel = SharedModule.shared.createProfileViewModel()) {
        self.viewModel = viewModel
        bindState()
    }

    private func bindState() {
        authStateCancellable = flowObserver.observe(flow: viewModel.authState) { [weak self] state in
            guard let state = state as? AuthUiState else { return }
            self?.isAuthenticated = toBool(state.isAuthenticated)
            self?.currentUserEmail = state.user?.email
            self?.currentUsername = state.user?.username
        }

        entriesCancellable = flowObserver.observe(flow: viewModel.entries) { [weak self] value in
            self?.entries = toJournalPreviewList(value)
        }
    }

    func logout() {
        viewModel.logout()
    }

    deinit {
        authStateCancellable?.cancel()
        entriesCancellable?.cancel()
        flowObserver.cancel()
        viewModel.clear()
    }
}
