import SwiftUI
import Shared

// MARK: - Inspirational Quote Card

struct InspirationalQuoteView: View {
    let journalListViewModel: ObservableJournalListViewModel
    
    var body: some View {
        VStack(spacing: 0) {
            if journalListViewModel.inspirationIsLoading && journalListViewModel.inspirationQuote == nil && !journalListViewModel.inspirationIsNotFound {
                InspirationLoadingView()
                    .transition(.opacity.combined(with: .scale(scale: 0.95)))
            } else if journalListViewModel.inspirationIsNotFound {
                InspirationPendingView()
                    .transition(.asymmetric(
                        insertion: .opacity.combined(with: .scale(scale: 0.9)),
                        removal: .opacity
                    ))
            } else if let quote = journalListViewModel.inspirationQuote {
                QuoteContentView(
                    quote: quote.quote,
                    tags: quote.tags.map { $0 },
                    generatedInfo: formattedGeneratedDate(for: quote)
                )
                .transition(.asymmetric(
                    insertion: .opacity.combined(with: .move(edge: .bottom)),
                    removal: .opacity.combined(with: .scale(scale: 0.95))
                ))
            }
        }
        .animation(.spring(response: 0.6, dampingFraction: 0.8), value: journalListViewModel.inspirationQuote?.id)
        .animation(.spring(response: 0.5, dampingFraction: 0.8), value: journalListViewModel.inspirationIsNotFound)
        .animation(.easeInOut(duration: 0.3), value: journalListViewModel.inspirationIsLoading)
    }

    private func formattedGeneratedDate(for quote: InspirationalQuote) -> String {
        let epochSeconds = quote.generatedAt.epochSeconds
        let date = Date(timeIntervalSince1970: TimeInterval(epochSeconds))

        let calendar = Calendar.current
        if calendar.isDateInToday(date) {
            return "Generated today"
        }
        if calendar.isDateInYesterday(date) {
            return "Generated yesterday"
        }

        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .full
        return "Generated \(formatter.localizedString(for: date, relativeTo: Date()))"
    }
}

// MARK: - Quote Content View

struct QuoteContentView: View {
    let quote: String
    let tags: [String]
    let generatedInfo: String
    
    @State private var animateIn = false
    @State private var quoteOpacity: Double = 0
    
    var body: some View {
        VStack(spacing: 12) {
            // Floating Tags
            FloatingTagsView(tags: tags)
                .frame(height: 90)
                .opacity(animateIn ? 1 : 0)
            
            // Quote card
            VStack(spacing: 16) {
                // Header with sparkle
                HStack(spacing: 8) {
                    ZStack {
                        Circle()
                            .fill(
                                RadialGradient(
                                    colors: [Color.mint.opacity(0.4), Color.clear],
                                    center: .center,
                                    startRadius: 0,
                                    endRadius: 16
                                )
                            )
                            .frame(width: 32, height: 32)
                        
                        Image(systemName: "sparkles")
                            .font(.system(size: 16, weight: .medium))
                            .foregroundStyle(
                                LinearGradient(
                                    colors: [Color.mint, Color.cyan],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                    }
                    
                    Text("Your Daily Inspiration")
                        .font(.system(size: 14, weight: .semibold, design: .rounded))
                        .foregroundStyle(
                            LinearGradient(
                                colors: [Color.white.opacity(0.9), Color.white.opacity(0.7)],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                    
                    Spacer()
                }
                .opacity(quoteOpacity)
                
                // Quote text
                Text("\"\(quote)\"")
                    .font(.system(size: 17, weight: .medium, design: .serif))
                    .foregroundColor(.white.opacity(0.9))
                    .multilineTextAlignment(.leading)
                    .lineSpacing(6)
                    .fixedSize(horizontal: false, vertical: true)
                    .opacity(quoteOpacity)
                
                // Footer
                HStack {
                    Text(generatedInfo)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundColor(.white.opacity(0.4))
                    
                    Spacer()
                }
                .opacity(quoteOpacity * 0.8)
            }
            .padding(20)
            .background(
                ZStack {
                    // Main glass background
                    RoundedRectangle(cornerRadius: 24)
                        .fill(.ultraThinMaterial.opacity(0.7))
                    
                    // Subtle gradient overlay
                    RoundedRectangle(cornerRadius: 24)
                        .fill(
                            LinearGradient(
                                colors: [
                                    Color.mint.opacity(0.05),
                                    Color.cyan.opacity(0.03),
                                    Color.clear
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                    
                    // Border
                    RoundedRectangle(cornerRadius: 24)
                        .stroke(
                            LinearGradient(
                                colors: [
                                    Color.white.opacity(0.25),
                                    Color.mint.opacity(0.15),
                                    Color.white.opacity(0.08)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                            lineWidth: 1
                        )
                }
            )
            .shadow(color: .black.opacity(0.15), radius: 20, x: 0, y: 10)
            .shadow(color: .mint.opacity(0.1), radius: 30, x: 0, y: 5)
            .opacity(animateIn ? 1 : 0)
            .offset(y: animateIn ? 0 : 20)
        }
        .onAppear {
            withAnimation(.spring(response: 0.7, dampingFraction: 0.8).delay(0.1)) {
                animateIn = true
            }
            withAnimation(.easeOut(duration: 0.8).delay(0.4)) {
                quoteOpacity = 1
            }
        }
    }
}

// MARK: - Floating Tags View

struct FloatingTagsView: View {
    let tags: [String]
    
    private let tagColors: [Color] = [
        .mint, .cyan, .teal, .green
    ]
    
    var body: some View {
        GeometryReader { geometry in
            ZStack {
                ForEach(Array(tags.prefix(4).enumerated()), id: \.offset) { index, tag in
                    FloatingTag(
                        text: tag,
                        color: tagColors[index % tagColors.count],
                        index: index
                    )
                    .position(tagPosition(for: index, in: geometry.size))
                }
            }
        }
    }
    
    private func tagPosition(for index: Int, in size: CGSize) -> CGPoint {
        let centerX = size.width / 2
        let centerY = size.height / 2
        
        let positions: [CGPoint] = [
            CGPoint(x: centerX - 70, y: centerY - 15),
            CGPoint(x: centerX + 50, y: centerY - 25),
            CGPoint(x: centerX + 90, y: centerY + 20),
            CGPoint(x: centerX - 20, y: centerY + 25)
        ]
        return positions[index % positions.count]
    }
}

// MARK: - Floating Tag

struct FloatingTag: View {
    let text: String
    let color: Color
    let index: Int
    
    @State private var appeared = false
    @State private var floatOffset: CGFloat = 0
    @State private var pulseScale: CGFloat = 1.0
    
    var body: some View {
        HStack(spacing: 6) {
            Circle()
                .fill(color)
                .frame(width: 6, height: 6)
                .shadow(color: color.opacity(0.6), radius: 3)
            
            Text(text)
                .font(.system(size: 13, weight: .medium, design: .rounded))
                .foregroundColor(.white.opacity(0.9))
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 8)
        .background(
            Capsule()
                .fill(.ultraThinMaterial.opacity(0.6))
                .overlay(
                    Capsule()
                        .stroke(color.opacity(0.3), lineWidth: 1)
                )
        )
        .scaleEffect(pulseScale)
        .offset(y: floatOffset)
        .opacity(appeared ? 1 : 0)
        .scaleEffect(appeared ? 1 : 0.5)
        .onAppear {
            let delay = Double(index) * 0.1
            
            // Appear animation
            withAnimation(.spring(response: 0.6, dampingFraction: 0.7).delay(delay)) {
                appeared = true
            }
            
            // Smooth floating animation - each tag has different timing
            let duration = 3.0 + Double(index) * 0.5
            let floatAmount: CGFloat = index % 2 == 0 ? 6 : -6
            
            withAnimation(
                .easeInOut(duration: duration)
                .repeatForever(autoreverses: true)
                .delay(delay + 0.3)
            ) {
                floatOffset = floatAmount
            }
            
            // Subtle pulse
            withAnimation(
                .easeInOut(duration: 2.5 + Double(index) * 0.3)
                .repeatForever(autoreverses: true)
                .delay(delay)
            ) {
                pulseScale = 1.03
            }
        }
    }
}

// MARK: - Loading View

struct InspirationLoadingView: View {
    @State private var breathe: CGFloat = 0
    @State private var orbitAngle: Double = 0
    @State private var shimmerPhase: CGFloat = -200
    
    var body: some View {
        VStack(spacing: 16) {
            ZStack {
                ForEach(0..<4, id: \.self) { index in
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
                                endRadius: 8
                            )
                        )
                        .frame(width: 10, height: 10)
                        .offset(x: 24 * cos(orbitAngle + Double(index) * .pi / 2),
                                y: 24 * sin(orbitAngle + Double(index) * .pi / 2))
                        .opacity(0.6 + 0.4 * sin(orbitAngle + Double(index)))
                }
                
                Image(systemName: "sparkles")
                    .font(.system(size: 28, weight: .medium))
                    .foregroundStyle(
                        LinearGradient(
                            colors: [Color.mint, Color.cyan],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .scaleEffect(0.9 + breathe * 0.15)
                    .opacity(0.7 + breathe * 0.3)
            }
            .frame(width: 80, height: 80)
            
            VStack(spacing: 8) {
                Text("Crafting your inspiration...")
                    .font(.system(size: 16, weight: .medium, design: .rounded))
                    .foregroundColor(.white.opacity(0.8))
                
                Text("Analyzing your journal themes")
                    .font(.system(size: 13))
                    .foregroundColor(.white.opacity(0.5))
            }
        }
        .padding(.vertical, 32)
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 24)
                .fill(.ultraThinMaterial.opacity(0.5))
                .overlay(
                    RoundedRectangle(cornerRadius: 24)
                        .fill(
                            LinearGradient(
                                colors: [
                                    Color.clear,
                                    Color.white.opacity(0.08),
                                    Color.clear
                                ],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .offset(x: shimmerPhase)
                        .mask(RoundedRectangle(cornerRadius: 24))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 24)
                        .stroke(Color.white.opacity(0.15), lineWidth: 1)
                )
        )
        .onAppear {
            withAnimation(.easeInOut(duration: 2.5).repeatForever(autoreverses: true)) {
                breathe = 1.0
            }
            withAnimation(.linear(duration: 6).repeatForever(autoreverses: false)) {
                orbitAngle = .pi * 2
            }
            withAnimation(.easeInOut(duration: 2).repeatForever(autoreverses: false)) {
                shimmerPhase = 400
            }
        }
    }
}

// MARK: - Pending View (No Quote Yet)

struct InspirationPendingView: View {
    @State private var animate = false
    @State private var particlePhase: Double = 0
    @State private var glowPulse: CGFloat = 0
    
    var body: some View {
        VStack(spacing: 20) {
            ZStack {
                ForEach(0..<3, id: \.self) { index in
                    Circle()
                        .stroke(
                            LinearGradient(
                                colors: [
                                    Color.orange.opacity(0.3 - Double(index) * 0.1),
                                    Color.yellow.opacity(0.2 - Double(index) * 0.05),
                                    Color.clear
                                ],
                                startPoint: .top,
                                endPoint: .bottom
                            ),
                            lineWidth: 2
                        )
                        .frame(width: 60 + CGFloat(index) * 20, height: 60 + CGFloat(index) * 20)
                        .scaleEffect(1 + glowPulse * 0.1 * CGFloat(index + 1))
                        .opacity(0.8 - Double(index) * 0.2)
                }
                
                ForEach(0..<6, id: \.self) { index in
                    Circle()
                        .fill(
                            RadialGradient(
                                colors: [
                                    [Color.orange, Color.yellow, Color.mint][index % 3].opacity(0.8),
                                    Color.clear
                                ],
                                center: .center,
                                startRadius: 0,
                                endRadius: 6
                            )
                        )
                        .frame(width: 8, height: 8)
                        .offset(
                            x: 40 * cos(particlePhase + Double(index) * .pi / 3),
                            y: 40 * sin(particlePhase + Double(index) * .pi / 3) * 0.6
                        )
                        .offset(y: animate ? -8 : 8)
                        .opacity(0.5 + 0.5 * sin(particlePhase + Double(index)))
                }
                
                ZStack {
                    Circle()
                        .fill(
                            RadialGradient(
                                colors: [
                                    Color.orange.opacity(0.3),
                                    Color.yellow.opacity(0.15),
                                    Color.clear
                                ],
                                center: .center,
                                startRadius: 0,
                                endRadius: 30
                            )
                        )
                        .frame(width: 60, height: 60)
                        .scaleEffect(1 + glowPulse * 0.15)
                    
                    Image(systemName: "flame.fill")
                        .font(.system(size: 24, weight: .medium))
                        .foregroundStyle(
                            LinearGradient(
                                colors: [Color.orange, Color.yellow],
                                startPoint: .bottom,
                                endPoint: .top
                            )
                        )
                        .offset(y: animate ? -2 : 2)
                }
            }
            .frame(width: 120, height: 120)
            
            VStack(spacing: 10) {
                Text("Your inspiration is brewing...")
                    .font(.system(size: 17, weight: .semibold, design: .rounded))
                    .foregroundStyle(
                        LinearGradient(
                            colors: [Color.white, Color.white.opacity(0.8)],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                
                Text("Write more journal entries to unlock\npersonalized AI-generated quotes")
                    .font(.system(size: 14))
                    .foregroundColor(.white.opacity(0.6))
                    .multilineTextAlignment(.center)
                    .lineSpacing(4)
            }
        }
        .padding(.vertical, 28)
        .padding(.horizontal, 20)
        .frame(maxWidth: .infinity)
        .background(
            ZStack {
                RoundedRectangle(cornerRadius: 24)
                    .fill(.ultraThinMaterial.opacity(0.6))
                
                RoundedRectangle(cornerRadius: 24)
                    .fill(
                        LinearGradient(
                            colors: [
                                Color.orange.opacity(0.05),
                                Color.yellow.opacity(0.03),
                                Color.clear
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                
                RoundedRectangle(cornerRadius: 24)
                    .stroke(
                        LinearGradient(
                            colors: [
                                Color.orange.opacity(0.25),
                                Color.yellow.opacity(0.15),
                                Color.white.opacity(0.1)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 1
                    )
            }
        )
        .shadow(color: .black.opacity(0.1), radius: 15, x: 0, y: 8)
        .shadow(color: .orange.opacity(0.08), radius: 20, x: 0, y: 5)
        .onAppear {
            withAnimation(.easeInOut(duration: 3).repeatForever(autoreverses: true)) {
                animate = true
            }
            withAnimation(.linear(duration: 8).repeatForever(autoreverses: false)) {
                particlePhase = .pi * 2
            }
            withAnimation(.easeInOut(duration: 2).repeatForever(autoreverses: true)) {
                glowPulse = 1
            }
        }
    }
}

// MARK: - Preview

#Preview("Quote Content") {
    ZStack {
        JournalBackground()
        
        ScrollView {
            VStack(spacing: 24) {
                QuoteContentView(
                    quote: "Your journey through reflection shows remarkable growth. Each entry reveals strength you may not see, but it's there—woven through your words like threads of resilience.",
                    tags: ["growth", "resilience", "self-discovery", "reflection"],
                    generatedInfo: "Generated today"
                )
                .padding(.horizontal, 20)
            }
            .padding(.top, 60)
        }
    }
    .ignoresSafeArea()
}

#Preview("Pending State") {
    ZStack {
        JournalBackground()
        
        InspirationPendingView()
            .padding(.horizontal, 20)
    }
    .ignoresSafeArea()
}

#Preview("Loading State") {
    ZStack {
        JournalBackground()
        
        InspirationLoadingView()
            .padding(.horizontal, 20)
    }
    .ignoresSafeArea()
}
