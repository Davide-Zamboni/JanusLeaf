import SwiftUI

@main
@available(iOS 17.0, *)
struct JanusLeafApp: App {
    @StateObject private var authManager = AuthManager()
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(authManager)
        }
    }
}
