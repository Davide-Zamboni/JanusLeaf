import SwiftUI
import Shared

@main
@available(iOS 17.0, *)
struct JanusLeafApp: App {
    @StateObject private var authViewModel = AuthViewModelAdapter()
    
    init() {
        _ = SharedModule.shared
        print("KMP: SharedModule initialized")
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(authViewModel)
        }
    }
}
