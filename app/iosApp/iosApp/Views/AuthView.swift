import SwiftUI

struct AuthView: View {
    @EnvironmentObject var authManager: AuthManager
    
    @State private var email = ""
    @State private var password = ""
    @State private var username = ""
    @State private var confirmPassword = ""
    @State private var isRegistering = false
    @State private var showPassword = false
    @State private var showConfirmPassword = false
    
    // Animation states
    @State private var animateGradient = false
    @State private var formAppeared = false
    
    var body: some View {
        GeometryReader { geometry in
            ZStack {
                // Animated gradient background
                AnimatedGradientBackground()
                
                // Floating orbs for depth
                FloatingOrbs()
                
                ScrollView {
                    VStack(spacing: 0) {
                        Spacer()
                            .frame(height: geometry.safeAreaInsets.top + 60)
                        
                        // Header
                        headerSection
                            .opacity(formAppeared ? 1 : 0)
                            .offset(y: formAppeared ? 0 : -30)
                        
                        Spacer()
                            .frame(height: 40)
                        
                        // Glass card with form
                        glassFormCard
                            .opacity(formAppeared ? 1 : 0)
                            .offset(y: formAppeared ? 0 : 50)
                        
                        Spacer()
                            .frame(height: 24)
                        
                        // Toggle auth mode
                        authModeToggle
                            .opacity(formAppeared ? 1 : 0)
                        
                        Spacer()
                            .frame(height: 40)
                    }
                    .padding(.horizontal, 24)
                }
            }
        }
        .ignoresSafeArea()
        .onAppear {
            withAnimation(.easeOut(duration: 0.8).delay(0.2)) {
                formAppeared = true
            }
        }
    }
    
    // MARK: - Header Section
    private var headerSection: some View {
        VStack(spacing: 16) {
            // Leaf emoji with glow
            Text("🍃")
                .font(.system(size: 72))
                .shadow(color: .green.opacity(0.5), radius: 20, x: 0, y: 0)
            
            Text("JanusLeaf")
                .font(.system(size: 36, weight: .bold, design: .rounded))
                .foregroundStyle(
                    LinearGradient(
                        colors: [.white, .white.opacity(0.8)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
            
            Text(isRegistering ? "Begin your mindful journey" : "Welcome back to your journey")
                .font(.system(size: 17, weight: .medium, design: .rounded))
                .foregroundColor(.white.opacity(0.7))
                .animation(.easeInOut(duration: 0.3), value: isRegistering)
        }
    }
    
    // MARK: - Glass Form Card
    private var glassFormCard: some View {
        VStack(spacing: 20) {
            // Error message
            if let error = authManager.errorMessage {
                errorBanner(message: error)
            }
            
            // Email field
            GlassTextField(
                text: $email,
                placeholder: "Email",
                icon: "envelope.fill",
                keyboardType: .emailAddress
            )
            
            // Username field (register only)
            if isRegistering {
                GlassTextField(
                    text: $username,
                    placeholder: "Username",
                    icon: "person.fill"
                )
                .transition(.asymmetric(
                    insertion: .opacity.combined(with: .move(edge: .top)),
                    removal: .opacity.combined(with: .move(edge: .top))
                ))
            }
            
            // Password field
            GlassTextField(
                text: $password,
                placeholder: "Password",
                icon: "lock.fill",
                isSecure: !showPassword,
                trailingIcon: showPassword ? "eye.slash.fill" : "eye.fill",
                trailingAction: { showPassword.toggle() }
            )
            
            // Confirm password (register only)
            if isRegistering {
                GlassTextField(
                    text: $confirmPassword,
                    placeholder: "Confirm Password",
                    icon: "lock.fill",
                    isSecure: !showConfirmPassword,
                    trailingIcon: showConfirmPassword ? "eye.slash.fill" : "eye.fill",
                    trailingAction: { showConfirmPassword.toggle() }
                )
                .transition(.asymmetric(
                    insertion: .opacity.combined(with: .move(edge: .top)),
                    removal: .opacity.combined(with: .move(edge: .top))
                ))
            }
            
            Spacer().frame(height: 8)
            
            // Submit button
            GlassButton(
                title: isRegistering ? "Create Account" : "Sign In",
                isLoading: authManager.isLoading,
                action: submit
            )
            .disabled(!isFormValid)
        }
        .padding(24)
        .background(
            RoundedRectangle(cornerRadius: 32)
                .fill(.ultraThinMaterial)
                .overlay(
                    RoundedRectangle(cornerRadius: 32)
                        .stroke(
                            LinearGradient(
                                colors: [.white.opacity(0.3), .white.opacity(0.1)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                            lineWidth: 1
                        )
                )
        )
        .shadow(color: .black.opacity(0.2), radius: 40, x: 0, y: 20)
        .animation(.spring(response: 0.5, dampingFraction: 0.8), value: isRegistering)
    }
    
    // MARK: - Error Banner
    private func errorBanner(message: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundColor(.red)
            
            Text(message)
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(.white)
            
            Spacer()
            
            Button(action: { authManager.clearError() }) {
                Image(systemName: "xmark")
                    .foregroundColor(.white.opacity(0.7))
            }
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color.red.opacity(0.3))
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(Color.red.opacity(0.5), lineWidth: 1)
                )
        )
        .transition(.opacity.combined(with: .move(edge: .top)))
    }
    
    // MARK: - Auth Mode Toggle
    private var authModeToggle: some View {
        HStack(spacing: 4) {
            Text(isRegistering ? "Already have an account?" : "Don't have an account?")
                .font(.system(size: 15))
                .foregroundColor(.white.opacity(0.7))
            
            Button(action: {
                withAnimation(.spring(response: 0.5, dampingFraction: 0.8)) {
                    isRegistering.toggle()
                    authManager.clearError()
                    password = ""
                    confirmPassword = ""
                }
            }) {
                Text(isRegistering ? "Sign In" : "Sign Up")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(.white)
            }
        }
    }
    
    // MARK: - Validation
    private var isFormValid: Bool {
        let emailValid = authManager.isValidEmail(email)
        let passwordValid = authManager.isValidPassword(password)
        
        if isRegistering {
            let usernameValid = authManager.isValidUsername(username)
            let passwordsMatch = password == confirmPassword
            return emailValid && passwordValid && usernameValid && passwordsMatch && !confirmPassword.isEmpty
        }
        
        return emailValid && passwordValid
    }
    
    // MARK: - Submit
    private func submit() {
        if isRegistering {
            authManager.register(email: email, username: username, password: password)
        } else {
            authManager.login(email: email, password: password)
        }
    }
}

// MARK: - Animated Gradient Background
struct AnimatedGradientBackground: View {
    @State private var animateGradient = false
    
    var body: some View {
        LinearGradient(
            colors: [
                Color(red: 0.1, green: 0.2, blue: 0.15),
                Color(red: 0.05, green: 0.15, blue: 0.1),
                Color(red: 0.15, green: 0.1, blue: 0.2),
                Color(red: 0.1, green: 0.1, blue: 0.15)
            ],
            startPoint: animateGradient ? .topLeading : .bottomTrailing,
            endPoint: animateGradient ? .bottomTrailing : .topLeading
        )
        .ignoresSafeArea()
        .onAppear {
            withAnimation(.easeInOut(duration: 8).repeatForever(autoreverses: true)) {
                animateGradient.toggle()
            }
        }
    }
}

// MARK: - Floating Orbs
struct FloatingOrbs: View {
    @State private var animate = false
    
    var body: some View {
        ZStack {
            // Top-left orb
            Circle()
                .fill(
                    RadialGradient(
                        colors: [Color.green.opacity(0.3), Color.clear],
                        center: .center,
                        startRadius: 0,
                        endRadius: 150
                    )
                )
                .frame(width: 300, height: 300)
                .offset(x: -100, y: animate ? -50 : -80)
                .blur(radius: 60)
            
            // Top-right orb
            Circle()
                .fill(
                    RadialGradient(
                        colors: [Color.purple.opacity(0.25), Color.clear],
                        center: .center,
                        startRadius: 0,
                        endRadius: 120
                    )
                )
                .frame(width: 250, height: 250)
                .offset(x: 120, y: animate ? 100 : 130)
                .blur(radius: 50)
            
            // Bottom orb
            Circle()
                .fill(
                    RadialGradient(
                        colors: [Color.orange.opacity(0.2), Color.clear],
                        center: .center,
                        startRadius: 0,
                        endRadius: 100
                    )
                )
                .frame(width: 200, height: 200)
                .offset(x: -50, y: animate ? 350 : 320)
                .blur(radius: 40)
        }
        .onAppear {
            withAnimation(.easeInOut(duration: 6).repeatForever(autoreverses: true)) {
                animate = true
            }
        }
    }
}

#Preview {
    AuthView()
        .environmentObject(AuthManager())
}
