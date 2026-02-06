import SwiftUI
import Shared
import KMPObservableViewModelSwiftUI

@available(iOS 17.0, *)
struct ContentView: View {
    @EnvironmentViewModel var authViewModel: ObservableAuthViewModel
    
    var body: some View {
        Group {
            if authViewModel.isAuthenticated {
                MainTabView()
            } else {
                AuthView()
            }
        }
        .animation(.easeInOut(duration: 0.3), value: authViewModel.isAuthenticated)
    }
}

@available(iOS 17.0, *)
#Preview {
    ContentView()
        .environmentViewModel(SharedModule.shared.createObservableAuthViewModel())
}
