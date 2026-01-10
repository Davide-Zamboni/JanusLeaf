import SwiftUI

struct GlassButton: View {
    let title: String
    var isLoading: Bool = false
    var isEnabled: Bool = true
    let action: () -> Void
    
    @State private var isPressed = false
    
    private var isInteractive: Bool {
        !isLoading && isEnabled
    }
    
    var body: some View {
        Button(action: {
            if isInteractive {
                // Haptic feedback
                let generator = UIImpactFeedbackGenerator(style: .medium)
                generator.impactOccurred()
                action()
            }
        }) {
            ZStack {
                // Button background with gradient
                RoundedRectangle(cornerRadius: 16)
                    .fill(
                        LinearGradient(
                            colors: isInteractive 
                                ? [Color(red: 0.1, green: 0.4, blue: 0.2), Color(red: 0.15, green: 0.5, blue: 0.25)]
                                : [Color.gray.opacity(0.3), Color.gray.opacity(0.2)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(
                                LinearGradient(
                                    colors: [.white.opacity(isInteractive ? 0.3 : 0.15), .white.opacity(isInteractive ? 0.1 : 0.05)],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                ),
                                lineWidth: 1
                            )
                    )
                    .shadow(color: Color.green.opacity(isInteractive ? 0.3 : 0), radius: 20, x: 0, y: 10)
                
                // Content
                if isLoading {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        .scaleEffect(1.1)
                } else {
                    Text(title)
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundColor(.white.opacity(isInteractive ? 1.0 : 0.5))
                }
            }
            .frame(height: 56)
        }
        .scaleEffect(isPressed ? 0.97 : 1.0)
        .animation(.spring(response: 0.3, dampingFraction: 0.6), value: isPressed)
        .onLongPressGesture(minimumDuration: .infinity, pressing: { pressing in
            if isInteractive {
                isPressed = pressing
            }
        }, perform: {})
        .disabled(!isInteractive)
    }
}

// Secondary button variant
struct GlassSecondaryButton: View {
    let title: String
    let action: () -> Void
    
    @State private var isPressed = false
    
    var body: some View {
        Button(action: {
            let generator = UIImpactFeedbackGenerator(style: .light)
            generator.impactOccurred()
            action()
        }) {
            ZStack {
                RoundedRectangle(cornerRadius: 16)
                    .fill(.ultraThinMaterial.opacity(0.5))
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(
                                LinearGradient(
                                    colors: [.white.opacity(0.2), .white.opacity(0.05)],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                ),
                                lineWidth: 1
                            )
                    )
                
                Text(title)
                    .font(.system(size: 17, weight: .medium))
                    .foregroundColor(.white.opacity(0.9))
            }
            .frame(height: 56)
        }
        .scaleEffect(isPressed ? 0.97 : 1.0)
        .animation(.spring(response: 0.3, dampingFraction: 0.6), value: isPressed)
        .onLongPressGesture(minimumDuration: .infinity, pressing: { pressing in
            isPressed = pressing
        }, perform: {})
    }
}

#Preview {
    ZStack {
        Color.black
        
        VStack(spacing: 20) {
            GlassButton(title: "Sign In", action: {})
            GlassButton(title: "Loading...", isLoading: true, action: {})
            GlassButton(title: "Disabled", isEnabled: false, action: {})
            GlassSecondaryButton(title: "Sign Up", action: {})
        }
        .padding()
    }
}
