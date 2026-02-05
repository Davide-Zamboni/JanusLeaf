import SwiftUI
import Shared

private func toBool(_ value: Bool) -> Bool { value }
private func toBool(_ value: KotlinBoolean) -> Bool { value.boolValue }

/// ObservableObject wrapper for the shared auth ViewModel.
@MainActor
final class AuthViewModelAdapter: ObservableObject {
    private let viewModel: AuthViewModel
    private let flowObserver = FlowObserver()
    private var stateCancellable: Cancellable?

    @Published var isLoading: Bool = false
    @Published var isAuthenticated: Bool = false
    @Published var errorMessage: String? = nil
    @Published var currentUserEmail: String? = nil
    @Published var currentUsername: String? = nil

    /// Static preview instance for SwiftUI previews
    static var preview: AuthViewModelAdapter {
        let adapter = AuthViewModelAdapter()
        adapter.isAuthenticated = true
        adapter.currentUserEmail = "preview@example.com"
        adapter.currentUsername = "PreviewUser"
        adapter.isLoading = false
        adapter.errorMessage = nil
        return adapter
    }

    init(viewModel: AuthViewModel = SharedModule.shared.createAuthViewModel()) {
        self.viewModel = viewModel
        bindState()
    }

    private func bindState() {
        stateCancellable = flowObserver.observe(flow: viewModel.uiState) { [weak self] state in
            guard let state = state as? AuthUiState else { return }
            self?.isLoading = toBool(state.isLoading)
            self?.isAuthenticated = toBool(state.isAuthenticated)
            self?.errorMessage = state.errorMessage
            self?.currentUsername = state.user?.username
            self?.currentUserEmail = state.user?.email
        }
    }

    func login(email: String, password: String) {
        viewModel.login(email: email, password: password)
    }

    func register(email: String, username: String, password: String) {
        viewModel.register(email: email, username: username, password: password)
    }

    func logout() {
        viewModel.logout()
    }

    func clearError() {
        viewModel.clearError()
    }

    func isValidEmail(_ email: String) -> Bool {
        viewModel.isValidEmail(email: email)
    }

    func isValidPassword(_ password: String) -> Bool {
        viewModel.isValidPassword(password: password)
    }

    func isValidUsername(_ username: String) -> Bool {
        viewModel.isValidUsername(username: username)
    }

    deinit {
        stateCancellable?.cancel()
        flowObserver.cancel()
        viewModel.clear()
    }
}
