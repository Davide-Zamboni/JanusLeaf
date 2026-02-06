import SwiftUI
import Shared
import KMPObservableViewModelSwiftUI

@available(iOS 17.0, *)
struct ContentView: View {
    @StateViewModel private var sessionViewModel = SharedModule.shared.createObservableSessionViewModel()
    
    var body: some View {
        Group {
            if sessionViewModel.isAuthenticated {
                MainTabView()
            } else {
                AuthView()
            }
        }
        .animation(.easeInOut(duration: 0.3), value: sessionViewModel.isAuthenticated)
    }
}

@available(iOS 17.0, *)
#Preview {
    ContentView()
}
