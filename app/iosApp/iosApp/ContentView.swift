import SwiftUI

@available(iOS 17.0, *)
struct ContentView: View {
    @EnvironmentObject var authViewModel: AuthViewModelAdapter
    
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
        .environmentObject(AuthViewModelAdapter())
}
