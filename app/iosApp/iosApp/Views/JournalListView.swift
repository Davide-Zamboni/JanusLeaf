import SwiftUI
import Shared

struct JournalEntryCard: View {
    let entry: JournalPreview
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(entry.title)
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundColor(.white)
                            .lineLimit(1)

                        Text(formatEntryDate(entry.entryDate))
                            .font(.system(size: 13))
                            .foregroundColor(.white.opacity(0.5))
                    }

                    Spacer()

                    MoodBadge(score: entry.moodScore.map { Int($0.intValue) })
                }

                if !entry.bodyPreview.isEmpty {
                    MarkdownRenderer(
                        entry.bodyPreview,
                        lineLimit: 3,
                        baseColor: .white.opacity(0.7)
                    )
                }

                HStack {
                    Text(formatTimeAgo(entry.updatedAt))
                        .font(.system(size: 12))
                        .foregroundColor(.white.opacity(0.4))

                    Spacer()

                    Image(systemName: "chevron.right")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(.white.opacity(0.3))
                }
            }
            .padding(18)
            .background(
                RoundedRectangle(cornerRadius: 20)
                    .fill(.ultraThinMaterial.opacity(0.6))
                    .overlay(
                        RoundedRectangle(cornerRadius: 20)
                            .stroke(
                                LinearGradient(
                                    colors: [.white.opacity(0.2), .white.opacity(0.05)],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                ),
                                lineWidth: 1
                            )
                    )
            )
            .shadow(color: .black.opacity(0.1), radius: 10, x: 0, y: 5)
        }
        .buttonStyle(ScaleButtonStyle())
    }

    private func formatEntryDate(_ date: Kotlinx_datetimeLocalDate) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMMM d, yyyy"

        let calendar = Calendar.current
        let components = DateComponents(
            year: Int(date.year),
            month: Int(date.monthNumber),
            day: Int(date.dayOfMonth)
        )
        if let swiftDate = calendar.date(from: components) {
            return formatter.string(from: swiftDate)
        }
        return "\(date.month) \(date.dayOfMonth), \(date.year)"
    }

    private func formatTimeAgo(_ instant: Kotlinx_datetimeInstant) -> String {
        let epochSeconds = instant.epochSeconds
        let date = Date(timeIntervalSince1970: TimeInterval(epochSeconds))

        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .short
        return formatter.localizedString(for: date, relativeTo: Date())
    }
}

struct MoodBadge: View {
    let score: Int?

    var body: some View {
        AnimatedMoodBadge(score: score)
    }
}

struct JournalBackground: View {
    @State private var animate = false

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(red: 0.06, green: 0.08, blue: 0.1),
                    Color(red: 0.08, green: 0.1, blue: 0.14),
                    Color(red: 0.05, green: 0.08, blue: 0.1)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            Circle()
                .fill(
                    RadialGradient(
                        colors: [Color.green.opacity(0.15), Color.clear],
                        center: .center,
                        startRadius: 0,
                        endRadius: 200
                    )
                )
                .frame(width: 400, height: 400)
                .offset(x: -100, y: animate ? -150 : -180)
                .blur(radius: 60)

            Circle()
                .fill(
                    RadialGradient(
                        colors: [Color.teal.opacity(0.1), Color.clear],
                        center: .center,
                        startRadius: 0,
                        endRadius: 150
                    )
                )
                .frame(width: 300, height: 300)
                .offset(x: 150, y: animate ? 200 : 230)
                .blur(radius: 50)

            Circle()
                .fill(
                    RadialGradient(
                        colors: [Color.mint.opacity(0.08), Color.clear],
                        center: .center,
                        startRadius: 0,
                        endRadius: 100
                    )
                )
                .frame(width: 200, height: 200)
                .offset(x: -80, y: animate ? 400 : 370)
                .blur(radius: 40)
        }
        .onAppear {
            withAnimation(.easeInOut(duration: 8).repeatForever(autoreverses: true)) {
                animate = true
            }
        }
    }
}

struct ScaleButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.97 : 1.0)
            .animation(.spring(response: 0.3, dampingFraction: 0.6), value: configuration.isPressed)
    }
}
