import SwiftUI

// MARK: - Liquid Glass Card

struct LiquidGlassCard<Content: View>: View {
    let content: Content
    var cornerRadius: CGFloat = 24
    var padding: CGFloat = 20
    
    init(
        cornerRadius: CGFloat = 24,
        padding: CGFloat = 20,
        @ViewBuilder content: () -> Content
    ) {
        self.cornerRadius = cornerRadius
        self.padding = padding
        self.content = content()
    }
    
    var body: some View {
        content
            .padding(padding)
            .background(
                RoundedRectangle(cornerRadius: cornerRadius)
                    .fill(.ultraThinMaterial.opacity(0.6))
                    .overlay(
                        RoundedRectangle(cornerRadius: cornerRadius)
                            .stroke(
                                LinearGradient(
                                    colors: [.white.opacity(0.25), .white.opacity(0.08)],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                ),
                                lineWidth: 1
                            )
                    )
                    .shadow(color: .black.opacity(0.15), radius: 20, x: 0, y: 10)
            )
    }
}

// MARK: - Liquid Glass Floating Action Button

struct LiquidGlassFAB: View {
    let icon: String
    let action: () -> Void
    
    @State private var isPressed = false
    
    var body: some View {
        Button(action: {
            let generator = UIImpactFeedbackGenerator(style: .medium)
            generator.impactOccurred()
            action()
        }) {
            ZStack {
                // Outer glow
                Circle()
                    .fill(
                        RadialGradient(
                            colors: [Color.green.opacity(0.3), Color.clear],
                            center: .center,
                            startRadius: 0,
                            endRadius: 40
                        )
                    )
                    .frame(width: 80, height: 80)
                    .blur(radius: 10)
                
                // Main button
                Circle()
                    .fill(
                        LinearGradient(
                            colors: [Color(red: 0.2, green: 0.55, blue: 0.35), Color(red: 0.15, green: 0.45, blue: 0.28)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 60, height: 60)
                    .overlay(
                        Circle()
                            .stroke(
                                LinearGradient(
                                    colors: [.white.opacity(0.4), .white.opacity(0.1)],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                ),
                                lineWidth: 1.5
                            )
                    )
                    .shadow(color: .green.opacity(0.4), radius: 15, x: 0, y: 8)
                
                // Icon
                Image(systemName: icon)
                    .font(.system(size: 24, weight: .semibold))
                    .foregroundColor(.white)
            }
        }
        .scaleEffect(isPressed ? 0.92 : 1.0)
        .animation(.spring(response: 0.3, dampingFraction: 0.6), value: isPressed)
        .onLongPressGesture(minimumDuration: .infinity, pressing: { pressing in
            isPressed = pressing
        }, perform: {})
    }
}

// MARK: - Liquid Glass Chip

struct LiquidGlassChip: View {
    let label: String
    var icon: String? = nil
    var isSelected: Bool = false
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                if let icon = icon {
                    Image(systemName: icon)
                        .font(.system(size: 14))
                }
                
                Text(label)
                    .font(.system(size: 14, weight: .medium))
            }
            .foregroundColor(.white.opacity(isSelected ? 1 : 0.7))
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(
                Capsule()
                    .fill(isSelected ? 
                          AnyShapeStyle(LinearGradient(
                            colors: [Color.green.opacity(0.4), Color.green.opacity(0.2)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                          )) :
                          AnyShapeStyle(.ultraThinMaterial.opacity(0.5))
                    )
                    .overlay(
                        Capsule()
                            .stroke(
                                isSelected ?
                                Color.green.opacity(0.5) :
                                Color.white.opacity(0.15),
                                lineWidth: 1
                            )
                    )
            )
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Liquid Glass Progress Indicator

struct LiquidGlassProgress: View {
    var progress: Double = 0
    var showPercentage: Bool = true
    
    var body: some View {
        VStack(spacing: 8) {
            GeometryReader { geometry in
                ZStack(alignment: .leading) {
                    // Background track
                    Capsule()
                        .fill(.ultraThinMaterial.opacity(0.3))
                    
                    // Progress fill
                    Capsule()
                        .fill(
                            LinearGradient(
                                colors: [Color.green, Color.mint],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .frame(width: geometry.size.width * progress)
                        .shadow(color: .green.opacity(0.5), radius: 8, x: 0, y: 0)
                }
            }
            .frame(height: 8)
            
            if showPercentage {
                Text("\(Int(progress * 100))%")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundColor(.white.opacity(0.6))
            }
        }
    }
}

// MARK: - Liquid Glass Divider

struct LiquidGlassDivider: View {
    var body: some View {
        Rectangle()
            .fill(
                LinearGradient(
                    colors: [.clear, .white.opacity(0.15), .clear],
                    startPoint: .leading,
                    endPoint: .trailing
                )
            )
            .frame(height: 1)
    }
}

// MARK: - Liquid Glass Icon Button

struct LiquidGlassIconButton: View {
    let icon: String
    var size: CGFloat = 44
    let action: () -> Void
    
    @State private var isPressed = false
    
    var body: some View {
        Button(action: {
            let generator = UIImpactFeedbackGenerator(style: .light)
            generator.impactOccurred()
            action()
        }) {
            ZStack {
                Circle()
                    .fill(.ultraThinMaterial.opacity(0.6))
                    .overlay(
                        Circle()
                            .stroke(.white.opacity(0.2), lineWidth: 1)
                    )
                
                Image(systemName: icon)
                    .font(.system(size: size * 0.4))
                    .foregroundColor(.white.opacity(0.8))
            }
            .frame(width: size, height: size)
        }
        .scaleEffect(isPressed ? 0.92 : 1.0)
        .animation(.spring(response: 0.3, dampingFraction: 0.6), value: isPressed)
        .onLongPressGesture(minimumDuration: .infinity, pressing: { pressing in
            isPressed = pressing
        }, perform: {})
    }
}

// MARK: - Animated Mesh Gradient Background (iOS 18+)

@available(iOS 18.0, *)
struct AnimatedMeshBackground: View {
    @State private var t: Float = 0.0
    @State private var timer: Timer? = nil
    
    var body: some View {
        TimelineView(.animation) { timeline in
            let time = timeline.date.timeIntervalSinceReferenceDate
            
            MeshGradient(
                width: 3,
                height: 3,
                points: [
                    [0.0, 0.0], [0.5, 0.0], [1.0, 0.0],
                    [sinInRange(-0.8...(-0.2), offset: 0.439, timeScale: 0.342, time: time), sinInRange(0.3...0.7, offset: 3.42, timeScale: 0.984, time: time)],
                    [sinInRange(0.1...0.9, offset: 0.239, timeScale: 0.084, time: time), sinInRange(0.2...0.8, offset: 5.21, timeScale: 0.242, time: time)],
                    [sinInRange(1.0...1.5, offset: 0.939, timeScale: 0.084, time: time), sinInRange(0.4...0.8, offset: 0.25, timeScale: 0.642, time: time)],
                    [sinInRange(-0.8...0.0, offset: 1.439, timeScale: 0.442, time: time), sinInRange(1.0...1.5, offset: 3.42, timeScale: 0.984, time: time)],
                    [sinInRange(0.3...0.6, offset: 0.339, timeScale: 0.784, time: time), sinInRange(1.0...1.2, offset: 1.22, timeScale: 0.772, time: time)],
                    [sinInRange(1.0...1.5, offset: 0.939, timeScale: 0.056, time: time), sinInRange(1.3...1.7, offset: 0.47, timeScale: 0.342, time: time)]
                ],
                colors: [
                    Color(red: 0.05, green: 0.1, blue: 0.08),
                    Color(red: 0.06, green: 0.12, blue: 0.1),
                    Color(red: 0.04, green: 0.08, blue: 0.06),
                    
                    Color(red: 0.08, green: 0.15, blue: 0.1),
                    Color(red: 0.1, green: 0.2, blue: 0.15),
                    Color(red: 0.06, green: 0.12, blue: 0.08),
                    
                    Color(red: 0.05, green: 0.1, blue: 0.08),
                    Color(red: 0.07, green: 0.14, blue: 0.1),
                    Color(red: 0.04, green: 0.09, blue: 0.07)
                ]
            )
            .ignoresSafeArea()
        }
    }
    
    private func sinInRange(_ range: ClosedRange<Float>, offset: Float, timeScale: Float, time: TimeInterval) -> Float {
        let amplitude = (range.upperBound - range.lowerBound) / 2
        let midPoint = (range.upperBound + range.lowerBound) / 2
        return midPoint + amplitude * sin(timeScale * Float(time) + offset)
    }
}

// MARK: - Shimmer Loading Effect

struct ShimmerEffect: ViewModifier {
    @State private var phase: CGFloat = 0
    
    func body(content: Content) -> some View {
        content
            .overlay(
                LinearGradient(
                    colors: [
                        .clear,
                        .white.opacity(0.1),
                        .clear
                    ],
                    startPoint: .leading,
                    endPoint: .trailing
                )
                .rotationEffect(.degrees(30))
                .offset(x: phase)
            )
            .onAppear {
                withAnimation(.linear(duration: 1.5).repeatForever(autoreverses: false)) {
                    phase = 400
                }
            }
            .mask(content)
    }
}

extension View {
    func shimmer() -> some View {
        modifier(ShimmerEffect())
    }
}

// MARK: - Preview

#Preview {
    ZStack {
        Color(red: 0.06, green: 0.08, blue: 0.1)
            .ignoresSafeArea()
        
        VStack(spacing: 24) {
            LiquidGlassCard {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Liquid Glass Card")
                        .font(.headline)
                        .foregroundColor(.white)
                    
                    Text("Beautiful frosted glass effect")
                        .font(.subheadline)
                        .foregroundColor(.white.opacity(0.7))
                }
            }
            .frame(maxWidth: 300)
            
            HStack(spacing: 12) {
                LiquidGlassChip(label: "All", icon: "square.grid.2x2", isSelected: true) {}
                LiquidGlassChip(label: "Recent", icon: "clock") {}
                LiquidGlassChip(label: "Favorites", icon: "star") {}
            }
            
            LiquidGlassProgress(progress: 0.7)
                .frame(width: 200)
            
            LiquidGlassFAB(icon: "plus") {}
        }
        .padding()
    }
}
