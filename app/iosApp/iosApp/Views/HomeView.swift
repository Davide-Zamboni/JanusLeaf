import SwiftUI

// MARK: - Home View (Placeholder - not actively used)

struct HomeView: View {
    @EnvironmentObject var authManager: AuthManager
    @State private var showLogout = false
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                Spacer()
                
                Text("🍃")
                    .font(.system(size: 64))
                
                Text("Welcome to JanusLeaf!")
                    .font(.title.bold())
                
                if let email = authManager.currentUserEmail {
                    Text(email)
                        .foregroundStyle(.secondary)
                }
                
                Spacer()
                
                Button("Sign Out") {
                    showLogout = true
                }
                .buttonStyle(.bordered)
            }
            .padding()
            .navigationTitle("Home")
            .confirmationDialog("Sign Out", isPresented: $showLogout) {
                Button("Sign Out", role: .destructive) {
                    authManager.logout()
                }
            }
        }
    }
}

#Preview {
    HomeView()
        .environmentObject(AuthManager())
}
