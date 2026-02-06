import SwiftUI
import Shared
import KMPObservableViewModelCore
import KMPObservableViewModelSwiftUI

extension Kmp_observableviewmodel_coreViewModel: @retroactive ViewModel { }

@main
@available(iOS 17.0, *)
struct JanusLeafApp: App {
    @StateViewModel private var authViewModel = SharedModule.shared.createObservableAuthViewModel()
    
    init() {
        _ = SharedModule.shared
        print("KMP: SharedModule initialized")
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentViewModel(authViewModel)
        }
    }
}
