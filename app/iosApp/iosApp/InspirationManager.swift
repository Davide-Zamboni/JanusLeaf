import SwiftUI
import Shared

/// ObservableObject wrapper for the Kotlin inspiration service
@MainActor
class InspirationManager: ObservableObject {
    private let inspirationService: IosInspirationService
    
    @Published var isLoading: Bool = false
    @Published var errorMessage: String? = nil
    @Published var quote: InspirationalQuote? = nil
    @Published var isNotFound: Bool = false
    
    private var loadingCancellable: Cancellable?
    private var errorCancellable: Cancellable?
    private var quoteCancellable: Cancellable?
    private var notFoundCancellable: Cancellable?
    
    init() {
        self.inspirationService = SharedModule.shared.createInspirationService()
        setupObservers()
    }
    
    private func setupObservers() {
        loadingCancellable = inspirationService.observeLoading { [weak self] value in
            DispatchQueue.main.async {
                self?.isLoading = value.boolValue
            }
        }
        
        errorCancellable = inspirationService.observeError { [weak self] value in
            DispatchQueue.main.async {
                self?.errorMessage = value
            }
        }
        
        quoteCancellable = inspirationService.observeQuote { [weak self] value in
            DispatchQueue.main.async {
                self?.quote = value
            }
        }
        
        notFoundCancellable = inspirationService.observeNotFound { [weak self] value in
            DispatchQueue.main.async {
                self?.isNotFound = value.boolValue
            }
        }
    }
    
    // MARK: - Fetch Quote
    
    func fetchQuote() {
        inspirationService.fetchQuote {}
    }
    
    func refresh() {
        inspirationService.refresh {}
    }
    
    func clearError() {
        inspirationService.clearError()
    }
    
    deinit {
        loadingCancellable?.cancel()
        errorCancellable?.cancel()
        quoteCancellable?.cancel()
        notFoundCancellable?.cancel()
    }
}

// MARK: - Helper Extensions

extension InspirationManager {
    /// Format the generated date as a relative string
    func formattedGeneratedDate() -> String {
        guard let quote = quote else { return "" }
        
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
}
