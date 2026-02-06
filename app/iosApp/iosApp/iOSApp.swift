import SwiftUI
import Shared
import KMPObservableViewModelCore
import KMPObservableViewModelSwiftUI

extension Kmp_observableviewmodel_coreViewModel: @retroactive ViewModel { }

@main
@available(iOS 17.0, *)
struct JanusLeafApp: App {
    init() {
        _ = SharedModule.shared
        print("KMP: SharedModule initialized")
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
