import SwiftUI

@available(iOS 17.0, *)
struct ContentView: View {
    @EnvironmentObject var authManager: AuthManager
    @StateObject private var journalManager = JournalManager()
    
    var body: some View {
        Group {
            if authManager.isAuthenticated {
                MainTabView()
                    .environmentObject(journalManager)
            } else {
                AuthView()
            }
        }
    }
}

@available(iOS 17.0, *)
#Preview {
    ContentView()
        .environmentObject(AuthManager())
}
