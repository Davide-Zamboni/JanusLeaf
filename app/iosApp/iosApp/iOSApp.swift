import SwiftUI
import Shared

final class SharedViewModelOwner<ViewModel>: ObservableObject {
    let viewModel: ViewModel
    private let onDeinit: (ViewModel) -> Void

    init(viewModel: ViewModel, onDeinit: @escaping (ViewModel) -> Void) {
        self.viewModel = viewModel
        self.onDeinit = onDeinit
    }

    deinit {
        onDeinit(viewModel)
    }
}

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
