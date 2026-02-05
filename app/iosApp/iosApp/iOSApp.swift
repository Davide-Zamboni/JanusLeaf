import SwiftUI
import Shared

@main
@available(iOS 17.0, *)
struct JanusLeafApp: App {
    @StateObject private var authManager = AuthManager()
    
    init() {
        let bridge = SharedBridge()
        print("KMP: \(bridge.hello())")
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(authManager)
        }
    }
}
