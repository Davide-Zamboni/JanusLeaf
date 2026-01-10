import SwiftUI

struct HomeView: View {
    @EnvironmentObject var authManager: AuthManager
    @State private var showLogoutConfirmation = false
    
    var body: some View {
        ZStack {
            // Background
            AnimatedGradientBackground()
            
            VStack(spacing: 32) {
                Spacer()
                
                // Welcome section
                VStack(spacing: 16) {
                    Text("🍃")
                        .font(.system(size: 80))
                        .shadow(color: .green.opacity(0.5), radius: 30, x: 0, y: 0)
                    
                    Text("Welcome to JanusLeaf!")
                        .font(.system(size: 28, weight: .bold, design: .rounded))
                        .foregroundColor(.white)
                    
                    if let email = authManager.currentUserEmail {
                        Text(email)
                            .font(.system(size: 16, weight: .medium))
                            .foregroundColor(.white.opacity(0.7))
                    }
                    
                    Text("You're successfully logged in.\nJournal features coming soon...")
                        .font(.system(size: 17))
                        .foregroundColor(.white.opacity(0.6))
                        .multilineTextAlignment(.center)
                        .padding(.top, 8)
                }
                
                Spacer()
                
                // Logout button
                GlassSecondaryButton(title: "Sign Out") {
                    showLogoutConfirmation = true
                }
                .padding(.horizontal, 60)
                .padding(.bottom, 40)
            }
            .padding()
        }
        .ignoresSafeArea()
        .confirmationDialog("Sign Out", isPresented: $showLogoutConfirmation, titleVisibility: .visible) {
            Button("Sign Out", role: .destructive) {
                authManager.logout()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Are you sure you want to sign out?")
        }
    }
}

#Preview {
    HomeView()
        .environmentObject(AuthManager())
}
