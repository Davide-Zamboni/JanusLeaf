import SwiftUI

// MARK: - AI Mood Loading Animation

/// A chilled and relaxing loading animation for AI-generated mood scores
struct AIMoodLoadingView: View {
    @State private var breathePhase: CGFloat = 0
    @State private var orbitAngle: Double = 0
    @State private var pulseScale: CGFloat = 1.0
    @State private var particlePhase: Double = 0
    
    var body: some View {
        ZStack {
            // Outer breathing ring
            Circle()
                .stroke(
                    AngularGradient(
                        colors: [
                            Color.mint.opacity(0.3),
                            Color.teal.opacity(0.15),
                            Color.cyan.opacity(0.3),
                            Color.mint.opacity(0.15),
                            Color.mint.opacity(0.3)
                        ],
                        center: .center
                    ),
                    lineWidth: 2
                )
                .frame(width: 36, height: 36)
                .scaleEffect(0.9 + breathePhase * 0.15)
                .opacity(0.6 + breathePhase * 0.4)
            
            // Orbiting particles
            ForEach(0..<3, id: \.self) { index in
                Circle()
                    .fill(
                        RadialGradient(
                            colors: [
                                Color.mint.opacity(0.8),
                                Color.cyan.opacity(0.3),
                                Color.clear
                            ],
                            center: .center,
                            startRadius: 0,
                            endRadius: 4
                        )
                    )
                    .frame(width: 6, height: 6)
                    .offset(x: 14 * cos(orbitAngle + Double(index) * 2.1),
                            y: 14 * sin(orbitAngle + Double(index) * 2.1))
                    .opacity(0.6 + 0.4 * sin(particlePhase + Double(index)))
            }
            
            // Center AI brain icon
            ZStack {
                // Glow effect
                Circle()
                    .fill(
                        RadialGradient(
                            colors: [
                                Color.mint.opacity(0.4),
                                Color.teal.opacity(0.1),
                                Color.clear
                            ],
                            center: .center,
                            startRadius: 0,
                            endRadius: 16
                        )
                    )
                    .frame(width: 32, height: 32)
                    .scaleEffect(pulseScale)
                
                // AI sparkle icon
                Image(systemName: "sparkles")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(
                        LinearGradient(
                            colors: [Color.mint, Color.cyan],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .opacity(0.7 + breathePhase * 0.3)
            }
        }
        .frame(width: 44, height: 44)
        .onAppear {
            startAnimations()
        }
    }
    
    private func startAnimations() {
        // Breathing animation - slow and calming
        withAnimation(.easeInOut(duration: 3.0).repeatForever(autoreverses: true)) {
            breathePhase = 1.0
        }
        
        // Orbit animation - continuous rotation
        withAnimation(.linear(duration: 8.0).repeatForever(autoreverses: false)) {
            orbitAngle = .pi * 2
        }
        
        // Pulse animation
        withAnimation(.easeInOut(duration: 2.0).repeatForever(autoreverses: true)) {
            pulseScale = 1.15
        }
        
        // Particle phase animation
        withAnimation(.linear(duration: 4.0).repeatForever(autoreverses: false)) {
            particlePhase = .pi * 2
        }
    }
}

// MARK: - Compact AI Loading Badge

/// A smaller badge version for the journal list
struct AIMoodLoadingBadge: View {
    @State private var breathe: CGFloat = 0
    @State private var shimmerPhase: CGFloat = -100
    
    var body: some View {
        HStack(spacing: 6) {
            // Animated sparkle
            ZStack {
                Image(systemName: "sparkles")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(
                        LinearGradient(
                            colors: [Color.mint, Color.cyan.opacity(0.7)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .opacity(0.7 + breathe * 0.3)
                    .scaleEffect(0.95 + breathe * 0.08)
            }
            
            Text("AI")
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(
                    LinearGradient(
                        colors: [Color.white.opacity(0.8), Color.white.opacity(0.6)],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background(
            ZStack {
                Capsule()
                    .fill(
                        LinearGradient(
                            colors: [
                                Color.mint.opacity(0.15),
                                Color.teal.opacity(0.1),
                                Color.cyan.opacity(0.15)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                
                // Shimmer overlay
                Capsule()
                    .fill(
                        LinearGradient(
                            colors: [
                                Color.clear,
                                Color.white.opacity(0.15),
                                Color.clear
                            ],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .offset(x: shimmerPhase)
                    .mask(Capsule())
                
                Capsule()
                    .stroke(
                        LinearGradient(
                            colors: [
                                Color.mint.opacity(0.4),
                                Color.cyan.opacity(0.2)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 1
                    )
            }
        )
        .onAppear {
            withAnimation(.easeInOut(duration: 2.0).repeatForever(autoreverses: true)) {
                breathe = 1.0
            }
            
            withAnimation(.easeInOut(duration: 2.5).repeatForever(autoreverses: false)) {
                shimmerPhase = 100
            }
        }
    }
}

// MARK: - Animated Mood Score Transition

/// Wrapper that animates the transition from loading to actual score
struct AnimatedMoodBadge: View {
    let score: Int?
    
    @State private var displayScore: Int? = nil
    @State private var showScore: Bool = false
    @State private var celebrationScale: CGFloat = 0.8
    @State private var celebrationOpacity: CGFloat = 0
    
    var body: some View {
        ZStack {
            // Loading state
            if !showScore {
                AIMoodLoadingBadge()
                    .transition(.asymmetric(
                        insertion: .opacity,
                        removal: .scale(scale: 0.8).combined(with: .opacity)
                    ))
            }
            
            // Score state with celebration effect
            if showScore, let score = displayScore {
                ZStack {
                    // Celebration burst
                    Circle()
                        .fill(
                            RadialGradient(
                                colors: [
                                    JournalManager.moodColor(for: score).opacity(0.4),
                                    Color.clear
                                ],
                                center: .center,
                                startRadius: 0,
                                endRadius: 30
                            )
                        )
                        .frame(width: 60, height: 60)
                        .scaleEffect(celebrationScale)
                        .opacity(celebrationOpacity)
                    
                    // Actual badge
                    MoodBadgeDisplay(score: score)
                        .transition(.asymmetric(
                            insertion: .scale(scale: 0.5).combined(with: .opacity),
                            removal: .opacity
                        ))
                }
            }
        }
        .onChange(of: score) { oldValue, newValue in
            if let newScore = newValue, oldValue == nil {
                // Animate transition from loading to score
                withAnimation(.spring(response: 0.5, dampingFraction: 0.7)) {
                    showScore = true
                    displayScore = newScore
                }
                
                // Celebration effect
                withAnimation(.easeOut(duration: 0.6)) {
                    celebrationScale = 1.5
                    celebrationOpacity = 0.8
                }
                
                withAnimation(.easeOut(duration: 0.8).delay(0.3)) {
                    celebrationOpacity = 0
                }
            } else if newValue == nil {
                withAnimation(.easeInOut(duration: 0.3)) {
                    showScore = false
                    displayScore = nil
                }
            } else {
                displayScore = newValue
            }
        }
        .onAppear {
            if let score = score {
                displayScore = score
                showScore = true
            }
        }
    }
}

// MARK: - Static Mood Badge Display

struct MoodBadgeDisplay: View {
    let score: Int
    
    var body: some View {
        HStack(spacing: 4) {
            Text(JournalManager.moodEmoji(for: score))
                .font(.system(size: 14))
            
            Text("\(score)")
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(.white)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background(
            Capsule()
                .fill(JournalManager.moodColor(for: score).opacity(0.3))
        )
    }
}

// MARK: - Preview

#Preview("AI Loading States") {
    ZStack {
        Color(red: 0.06, green: 0.08, blue: 0.1)
            .ignoresSafeArea()
        
        VStack(spacing: 40) {
            VStack(spacing: 12) {
                Text("Full Loading Animation")
                    .font(.caption)
                    .foregroundColor(.white.opacity(0.6))
                AIMoodLoadingView()
            }
            
            VStack(spacing: 12) {
                Text("Badge Loading")
                    .font(.caption)
                    .foregroundColor(.white.opacity(0.6))
                AIMoodLoadingBadge()
            }
            
            VStack(spacing: 12) {
                Text("Animated Transition")
                    .font(.caption)
                    .foregroundColor(.white.opacity(0.6))
                AnimatedMoodBadgePreview()
            }
        }
    }
}

// Preview helper to demonstrate animation
struct AnimatedMoodBadgePreview: View {
    @State private var score: Int? = nil
    
    var body: some View {
        VStack(spacing: 20) {
            AnimatedMoodBadge(score: score)
            
            Button("Toggle Score") {
                if score == nil {
                    score = 8
                } else {
                    score = nil
                }
            }
            .font(.caption)
            .foregroundColor(.mint)
        }
    }
}
