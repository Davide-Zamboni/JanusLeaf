import SwiftUI

struct ContentView: View {
    @EnvironmentObject var authManager: AuthManager
    
    var body: some View {
        Group {
            if authManager.isAuthenticated {
                HomeView()
            } else {
                AuthView()
            }
        }
        .animation(.easeInOut(duration: 0.3), value: authManager.isAuthenticated)
    }
}

#Preview {
    ContentView()
        .environmentObject(AuthManager())
}
