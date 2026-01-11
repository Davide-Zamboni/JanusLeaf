import SwiftUI

// MARK: - Auth View (Optimized & Minimal)

struct AuthView: View {
    @EnvironmentObject var authManager: AuthManager
    
    @State private var email = ""
    @State private var password = ""
    @State private var username = ""
    @State private var confirmPassword = ""
    @State private var isRegistering = false
    @State private var showPassword = false
    
    @FocusState private var focusedField: Field?
    
    enum Field: Hashable {
        case email, username, password, confirmPassword
    }
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 32) {
                    // Logo
                    headerSection
                        .padding(.top, 40)
                    
                    // Form
                    formSection
                    
                    // Toggle
                    toggleSection
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 40)
            }
            .scrollDismissesKeyboard(.interactively)
            .background(Color(.systemBackground))
            .navigationBarHidden(true)
        }
    }
    
    // MARK: - Header
    
    private var headerSection: some View {
        VStack(spacing: 12) {
            Text("🍃")
                .font(.system(size: 56))
            
            Text("JanusLeaf")
                .font(.system(size: 32, weight: .bold, design: .rounded))
            
            Text(isRegistering ? "Start your journey" : "Welcome back")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
    }
    
    // MARK: - Form
    
    private var formSection: some View {
        VStack(spacing: 16) {
            // Error
            if let error = authManager.errorMessage {
                ErrorBanner(message: error) {
                    authManager.clearError()
                }
            }
            
            // Email
            VStack(alignment: .leading, spacing: 4) {
                MinimalTextField(
                    text: $email,
                    placeholder: "Email",
                    icon: "envelope",
                    keyboardType: .emailAddress
                )
                .focused($focusedField, equals: .email)
                .textContentType(.emailAddress)
                .autocapitalization(.none)
                
                if !email.isEmpty && !authManager.isValidEmail(email) {
                    HintText("Enter a valid email")
                }
            }
            
            // Username (register)
            if isRegistering {
                VStack(alignment: .leading, spacing: 4) {
                    MinimalTextField(
                        text: $username,
                        placeholder: "Username",
                        icon: "person"
                    )
                    .focused($focusedField, equals: .username)
                    .textContentType(.username)
                    
                    if !username.isEmpty && !authManager.isValidUsername(username) {
                        HintText("2-50 characters required")
                    }
                }
            }
            
            // Password
            VStack(alignment: .leading, spacing: 4) {
                MinimalSecureField(
                    text: $password,
                    placeholder: "Password",
                    showPassword: $showPassword
                )
                .focused($focusedField, equals: .password)
                .textContentType(isRegistering ? .newPassword : .password)
                
                if !password.isEmpty && !authManager.isValidPassword(password) {
                    HintText("At least 8 characters")
                }
            }
            
            // Confirm password (register)
            if isRegistering {
                VStack(alignment: .leading, spacing: 4) {
                    MinimalSecureField(
                        text: $confirmPassword,
                        placeholder: "Confirm Password",
                        showPassword: $showPassword
                    )
                    .focused($focusedField, equals: .confirmPassword)
                    .textContentType(.newPassword)
                    
                    if !confirmPassword.isEmpty && password != confirmPassword {
                        HintText("Passwords don't match")
                    }
                }
            }
            
            // Submit
            Button(action: submit) {
                Group {
                    if authManager.isLoading {
                        ProgressView()
                            .tint(.white)
                    } else {
                        Text(isRegistering ? "Create Account" : "Sign In")
                            .fontWeight(.semibold)
                    }
                }
                .frame(maxWidth: .infinity)
                .frame(height: 50)
            }
            .buttonStyle(.borderedProminent)
            .tint(.green)
            .disabled(!isFormValid || authManager.isLoading)
            .padding(.top, 8)
        }
    }
    
    // MARK: - Toggle
    
    private var toggleSection: some View {
        HStack(spacing: 4) {
            Text(isRegistering ? "Have an account?" : "Need an account?")
                .foregroundStyle(.secondary)
            
            Button(isRegistering ? "Sign In" : "Sign Up") {
                withAnimation(.easeInOut(duration: 0.2)) {
                    isRegistering.toggle()
                    authManager.clearError()
                    password = ""
                    confirmPassword = ""
                }
            }
            .fontWeight(.medium)
        }
        .font(.subheadline)
    }
    
    // MARK: - Validation
    
    private var isFormValid: Bool {
        let emailValid = authManager.isValidEmail(email)
        let passwordValid = authManager.isValidPassword(password)
        
        if isRegistering {
            return emailValid && passwordValid &&
                   authManager.isValidUsername(username) &&
                   password == confirmPassword &&
                   !confirmPassword.isEmpty
        }
        return emailValid && passwordValid
    }
    
    private func submit() {
        focusedField = nil
        if isRegistering {
            authManager.register(email: email, username: username, password: password)
        } else {
            authManager.login(email: email, password: password)
        }
    }
}

// MARK: - Minimal Text Field

struct MinimalTextField: View {
    @Binding var text: String
    let placeholder: String
    var icon: String? = nil
    var keyboardType: UIKeyboardType = .default
    
    var body: some View {
        HStack(spacing: 12) {
            if let icon {
                Image(systemName: icon)
                    .foregroundStyle(.secondary)
                    .frame(width: 20)
            }
            
            TextField(placeholder, text: $text)
                .keyboardType(keyboardType)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

// MARK: - Minimal Secure Field

struct MinimalSecureField: View {
    @Binding var text: String
    let placeholder: String
    @Binding var showPassword: Bool
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "lock")
                .foregroundStyle(.secondary)
                .frame(width: 20)
            
            Group {
                if showPassword {
                    TextField(placeholder, text: $text)
                } else {
                    SecureField(placeholder, text: $text)
                }
            }
            
            Button {
                showPassword.toggle()
            } label: {
                Image(systemName: showPassword ? "eye.slash" : "eye")
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

// MARK: - Error Banner

struct ErrorBanner: View {
    let message: String
    let dismiss: () -> Void
    
    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundStyle(.red)
            
            Text(message)
                .font(.subheadline)
            
            Spacer()
            
            Button(action: dismiss) {
                Image(systemName: "xmark")
                    .foregroundStyle(.secondary)
            }
        }
        .padding(14)
        .background(Color.red.opacity(0.1))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

// MARK: - Hint Text

struct HintText: View {
    let text: String
    
    init(_ text: String) {
        self.text = text
    }
    
    var body: some View {
        Label(text, systemImage: "info.circle")
            .font(.caption)
            .foregroundStyle(.orange)
            .padding(.leading, 4)
    }
}

#Preview {
    AuthView()
        .environmentObject(AuthManager())
}
