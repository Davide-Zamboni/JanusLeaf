import SwiftUI
import Shared

@available(iOS 17.0, *)
struct ContentView: View {
    @StateObject private var sessionViewModelOwner = SharedViewModelOwner(
        viewModel: SharedModule.shared.createWelcomeViewModel(),
        onDeinit: { (viewModel: WelcomeViewModel) in
            viewModel.clear()
        }
    )
    @State private var isAuthenticated = false

    private var sessionViewModel: WelcomeViewModel {
        sessionViewModelOwner.viewModel
    }
    
    var body: some View {
        Group {
            if isAuthenticated {
                MainTabView()
            } else {
                AuthView()
            }
        }
        .animation(.easeInOut(duration: 0.3), value: isAuthenticated)
        .task {
            await observeSessionAuthState()
        }
    }
    
    @MainActor
    private func observeSessionAuthState() async {
        for await authState in sessionViewModel.authState {
            isAuthenticated = authState.isAuthenticated
        }
    }
}

@available(iOS 17.0, *)
#Preview {
    ContentView()
}
