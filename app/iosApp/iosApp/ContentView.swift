import SwiftUI

@available(iOS 17.0, *)
struct ContentView: View {
    @EnvironmentObject var authManager: AuthManager
    @StateObject private var journalManager = JournalManager()
    
    var body: some View {
        Group {
            if authManager.isAuthenticated {
                JournalListView()
                    .environmentObject(journalManager)
            } else {
                AuthView()
            }
        }
        .animation(.easeInOut(duration: 0.3), value: authManager.isAuthenticated)
    }
}

@available(iOS 17.0, *)
#Preview {
    ContentView()
        .environmentObject(AuthManager())
}
