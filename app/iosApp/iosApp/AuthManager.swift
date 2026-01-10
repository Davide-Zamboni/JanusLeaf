import SwiftUI
import Shared

/// ObservableObject wrapper for the Kotlin auth service
@MainActor
class AuthManager: ObservableObject {
    private let authService: IosAuthService
    
    @Published var isLoading: Bool = false
    @Published var isAuthenticated: Bool = false
    @Published var errorMessage: String? = nil
    @Published var currentUserEmail: String? = nil
    @Published var currentUsername: String? = nil
    
    private var loadingCancellable: Cancellable?
    private var authCancellable: Cancellable?
    private var errorCancellable: Cancellable?
    private var userCancellable: Cancellable?
    
    init() {
        self.authService = SharedModule.shared.createAuthService()
        setupObservers()
    }
    
    private func setupObservers() {
        // Observe loading state
        loadingCancellable = authService.observeLoading { [weak self] value in
            DispatchQueue.main.async {
                self?.isLoading = value.boolValue
            }
        }
        
        // Observe auth state
        authCancellable = authService.observeAuthenticated { [weak self] value in
            DispatchQueue.main.async {
                self?.isAuthenticated = value.boolValue
            }
        }
        
        // Observe errors
        errorCancellable = authService.observeError { [weak self] value in
            DispatchQueue.main.async {
                self?.errorMessage = value
            }
        }
        
        // Observe current user
        userCancellable = authService.observeUser { [weak self] user in
            DispatchQueue.main.async {
                self?.currentUsername = user?.username
                self?.currentUserEmail = user?.email
            }
        }
    }
    
    func login(email: String, password: String) {
        authService.login(
            email: email,
            password: password,
            onSuccess: { [weak self] in
                DispatchQueue.main.async {
                    self?.currentUserEmail = email
                }
            },
            onError: { _ in }
        )
    }
    
    func register(email: String, username: String, password: String) {
        authService.register(
            email: email,
            username: username,
            password: password,
            onSuccess: { [weak self] in
                DispatchQueue.main.async {
                    self?.currentUserEmail = email
                }
            },
            onError: { _ in }
        )
    }
    
    func logout() {
        authService.logout {
            DispatchQueue.main.async { [weak self] in
                self?.currentUserEmail = nil
                self?.currentUsername = nil
            }
        }
    }
    
    func clearError() {
        authService.clearError()
    }
    
    func isValidEmail(_ email: String) -> Bool {
        return authService.isValidEmail(email: email)
    }
    
    func isValidPassword(_ password: String) -> Bool {
        return authService.isValidPassword(password: password)
    }
    
    func isValidUsername(_ username: String) -> Bool {
        return authService.isValidUsername(username: username)
    }
    
    deinit {
        loadingCancellable?.cancel()
        authCancellable?.cancel()
        errorCancellable?.cancel()
        userCancellable?.cancel()
    }
}
