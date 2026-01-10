import SwiftUI

struct GlassTextField: View {
    @Binding var text: String
    let placeholder: String
    var icon: String? = nil
    var keyboardType: UIKeyboardType = .default
    var isSecure: Bool = false
    var trailingIcon: String? = nil
    var trailingAction: (() -> Void)? = nil
    
    @FocusState private var isFocused: Bool
    
    var body: some View {
        HStack(spacing: 14) {
            // Leading icon
            if let icon = icon {
                Image(systemName: icon)
                    .font(.system(size: 18))
                    .foregroundColor(isFocused ? .white : .white.opacity(0.5))
                    .frame(width: 24)
                    .animation(.easeInOut(duration: 0.2), value: isFocused)
            }
            
            // Text field
            Group {
                if isSecure {
                    SecureField(placeholder, text: $text)
                } else {
                    TextField(placeholder, text: $text)
                        .keyboardType(keyboardType)
                        .autocapitalization(keyboardType == .emailAddress ? .none : .sentences)
                }
            }
            .font(.system(size: 17))
            .foregroundColor(.white)
            .focused($isFocused)
            .tint(.green)
            
            // Trailing icon (like password visibility toggle)
            if let trailingIcon = trailingIcon, let action = trailingAction {
                Button(action: action) {
                    Image(systemName: trailingIcon)
                        .font(.system(size: 18))
                        .foregroundColor(.white.opacity(0.5))
                }
            }
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(.ultraThinMaterial.opacity(0.5))
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(
                            isFocused 
                                ? LinearGradient(
                                    colors: [.green.opacity(0.6), .green.opacity(0.3)],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                                : LinearGradient(
                                    colors: [.white.opacity(0.2), .white.opacity(0.05)],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                ),
                            lineWidth: 1
                        )
                )
        )
        .animation(.easeInOut(duration: 0.2), value: isFocused)
    }
}

#Preview {
    ZStack {
        Color.black
        
        VStack(spacing: 20) {
            GlassTextField(
                text: .constant(""),
                placeholder: "Email",
                icon: "envelope.fill",
                keyboardType: .emailAddress
            )
            
            GlassTextField(
                text: .constant("password"),
                placeholder: "Password",
                icon: "lock.fill",
                isSecure: true,
                trailingIcon: "eye.fill",
                trailingAction: {}
            )
        }
        .padding()
    }
}
