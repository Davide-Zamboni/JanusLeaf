import SwiftUI
import Shared

// MARK: - Time Period

enum TimePeriod: String, CaseIterable {
    case week = "W"
    case month = "M"
    case sixMonths = "6M"
    case year = "Y"
    
    var displayName: String {
        switch self {
        case .week: return "Week"
        case .month: return "Month"
        case .sixMonths: return "6 Months"
        case .year: return "Year"
        }
    }
    
    var days: Int {
        switch self {
        case .week: return 7
        case .month: return 30
        case .sixMonths: return 180
        case .year: return 365
        }
    }
}

// MARK: - Mood Data Point

struct MoodDataPoint: Identifiable {
    let id = UUID()
    let date: Date
    let score: Int
    let entryTitle: String
    
    var normalizedScore: CGFloat {
        CGFloat(score - 1) / 9.0 // Convert 1-10 to 0-1
    }
    
    var color: Color {
        MoodHelpers.color(for: score)
    }
    
    var emoji: String {
        MoodHelpers.emoji(for: score)
    }
}

// MARK: - Mood Helpers (Non-MainActor)

enum MoodHelpers {
    static func emoji(for score: Int?) -> String {
        guard let score = score else { return "😶" }
        switch score {
        case 1...2: return "😢"
        case 3...4: return "😔"
        case 5...6: return "😐"
        case 7...8: return "😊"
        case 9...10: return "😄"
        default: return "😶"
        }
    }
    
    static func color(for score: Int?) -> Color {
        guard let score = score else { return .gray }
        switch score {
        case 1...2: return .red
        case 3...4: return .orange
        case 5...6: return .yellow
        case 7...8: return .green
        case 9...10: return .mint
        default: return .gray
        }
    }
}

// MARK: - Mood Insights View

@available(iOS 17.0, *)
struct MoodInsightsView: View {
    @EnvironmentObject var authViewModel: AuthViewModelAdapter
    @StateObject private var moodInsightsViewModel = MoodInsightsViewModelAdapter()
    
    @State private var selectedPeriod: TimePeriod = .sixMonths
    @State private var moodData: [MoodDataPoint] = []
    @State private var animateChart: Bool = false
    @State private var selectedDataPoint: MoodDataPoint? = nil
    @State private var showDetail: Bool = false
    @State private var showLogoutConfirmation = false
    
    // Stats
    @State private var averageMood: Double = 0
    @State private var totalEntries: Int = 0
    @State private var moodTrend: String = "stable"
    
    var body: some View {
        ZStack {
            // Background
            JournalBackground()
            
            ScrollView(showsIndicators: false) {
                VStack(spacing: 24) {
                    // Header
                    headerSection
                        .padding(.top, 60)
                    
                    // Time period selector
                    periodSelector
                    
                    // Stats overview
                    statsOverview
                    
                    // Mood chart
                    moodChartSection
                    
                    // Mood breakdown
                    moodBreakdownSection
                    
                    Spacer(minLength: 100)
                }
                .padding(.horizontal, 20)
            }
        }
        .ignoresSafeArea()
        .confirmationDialog("Sign Out", isPresented: $showLogoutConfirmation, titleVisibility: .visible) {
            Button("Sign Out", role: .destructive) {
                authViewModel.logout()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Are you sure you want to sign out?")
        }
        .onAppear {
            moodInsightsViewModel.loadEntries()
            loadMoodData()
            withAnimation(.easeOut(duration: 0.8).delay(0.3)) {
                animateChart = true
            }
        }
        .onChange(of: selectedPeriod) { _, _ in
            withAnimation(.easeInOut(duration: 0.3)) {
                animateChart = false
            }
            loadMoodData()
            withAnimation(.easeOut(duration: 0.6).delay(0.1)) {
                animateChart = true
            }
        }
        .onChange(of: moodInsightsViewModel.entries) { _, _ in
            loadMoodData()
        }
    }
    
    // MARK: - Header Section
    
    private var headerSection: some View {
        HStack(alignment: .center) {
            VStack(alignment: .leading, spacing: 4) {
                Text("State of Mind")
                    .font(.system(size: 34, weight: .bold, design: .rounded))
                    .foregroundColor(.white)
                
                Text(dateRangeText)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(.white.opacity(0.6))
            }
            
            Spacer()
            
            // Profile button
            Button(action: { showLogoutConfirmation = true }) {
                ZStack {
                    Circle()
                        .fill(.ultraThinMaterial)
                        .frame(width: 44, height: 44)
                    
                    Image(systemName: "person.fill")
                        .font(.system(size: 18))
                        .foregroundColor(.white.opacity(0.8))
                }
            }
        }
    }
    
    // MARK: - Period Selector
    
    private var periodSelector: some View {
        HStack(spacing: 0) {
            ForEach(TimePeriod.allCases, id: \.self) { period in
                Button(action: {
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.7)) {
                        selectedPeriod = period
                    }
                }) {
                    Text(period.rawValue)
                        .font(.system(size: 15, weight: selectedPeriod == period ? .semibold : .medium))
                        .foregroundColor(selectedPeriod == period ? .white : .white.opacity(0.5))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(
                            selectedPeriod == period ?
                            AnyView(
                                Capsule()
                                    .fill(Color.white.opacity(0.15))
                            ) :
                            AnyView(Color.clear)
                        )
                }
                .buttonStyle(.plain)
            }
        }
        .padding(4)
        .background(
            Capsule()
                .fill(.ultraThinMaterial.opacity(0.5))
                .overlay(
                    Capsule()
                        .stroke(Color.white.opacity(0.1), lineWidth: 1)
                )
        )
    }
    
    // MARK: - Stats Overview
    
    private var statsOverview: some View {
        HStack(spacing: 16) {
            StatCard(
                title: "TOTAL",
                value: "\(totalEntries)",
                subtitle: "entries",
                icon: "doc.text.fill",
                accentColor: .mint
            )
            
            StatCard(
                title: "AVERAGE",
                value: String(format: "%.1f", averageMood),
                subtitle: "mood",
                icon: "heart.fill",
                accentColor: moodColorForAverage
            )
            
            StatCard(
                title: "TREND",
                value: trendEmoji,
                subtitle: moodTrend,
                icon: trendIcon,
                accentColor: trendColor
            )
        }
    }
    
    // MARK: - Mood Chart Section
    
    private var moodChartSection: some View {
        LiquidGlassCard(cornerRadius: 24, padding: 20) {
            VStack(alignment: .leading, spacing: 16) {
                // Chart header
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Mood Over Time")
                            .font(.system(size: 17, weight: .semibold))
                            .foregroundColor(.white)
                        
                        if let selected = selectedDataPoint {
                            Text("\(selected.emoji) \(selected.score) • \(formatDate(selected.date))")
                                .font(.system(size: 13))
                                .foregroundColor(.white.opacity(0.7))
                                .transition(.opacity)
                        }
                    }
                    
                    Spacer()
                    
                    // Y-axis labels
                    VStack(alignment: .trailing, spacing: 0) {
                        Text("Very Pleasant")
                            .font(.system(size: 9, weight: .medium))
                            .foregroundColor(.white.opacity(0.4))
                        Spacer()
                        Text("Neutral")
                            .font(.system(size: 9, weight: .medium))
                            .foregroundColor(.white.opacity(0.4))
                        Spacer()
                        Text("Very Unpleasant")
                            .font(.system(size: 9, weight: .medium))
                            .foregroundColor(.white.opacity(0.4))
                    }
                    .frame(height: 180)
                }
                
                // The chart
                if moodData.isEmpty {
                    emptyChartPlaceholder
                } else {
                    GeometryReader { geometry in
                        MoodChart(
                            data: moodData,
                            size: geometry.size,
                            animate: animateChart,
                            selectedPoint: $selectedDataPoint
                        )
                    }
                    .frame(height: 200)
                }
                
                // X-axis labels
                xAxisLabels
            }
        }
    }
    
    // MARK: - Empty Chart Placeholder
    
    private var emptyChartPlaceholder: some View {
        VStack(spacing: 16) {
            Image(systemName: "chart.line.uptrend.xyaxis")
                .font(.system(size: 48))
                .foregroundColor(.white.opacity(0.2))
            
            Text("No mood data yet")
                .font(.system(size: 15, weight: .medium))
                .foregroundColor(.white.opacity(0.5))
            
            Text("Start journaling to see your mood patterns")
                .font(.system(size: 13))
                .foregroundColor(.white.opacity(0.3))
                .multilineTextAlignment(.center)
        }
        .frame(height: 200)
        .frame(maxWidth: .infinity)
    }
    
    // MARK: - X-Axis Labels
    
    private var xAxisLabels: some View {
        HStack {
            ForEach(xAxisDates, id: \.self) { date in
                Text(formatAxisDate(date))
                    .font(.system(size: 10, weight: .medium))
                    .foregroundColor(.white.opacity(0.4))
                if date != xAxisDates.last {
                    Spacer()
                }
            }
        }
    }
    
    // MARK: - Mood Breakdown Section
    
    private var moodBreakdownSection: some View {
        LiquidGlassCard(cornerRadius: 24, padding: 20) {
            VStack(alignment: .leading, spacing: 16) {
                // Segmented control
                HStack(spacing: 0) {
                    BreakdownTab(title: "States", isSelected: true)
                    BreakdownTab(title: "Associations", isSelected: false)
                    BreakdownTab(title: "Life Factors", isSelected: false)
                }
                .padding(4)
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color.white.opacity(0.05))
                )
                
                // Mood categories
                VStack(spacing: 12) {
                    MoodCategoryRow(
                        title: "Daily Moods",
                        count: totalEntries,
                        icon: "sun.max.fill",
                        color: .yellow
                    )
                    
                    LiquidGlassDivider()
                    
                    MoodCategoryRow(
                        title: "High Energy",
                        count: moodData.filter { $0.score >= 7 }.count,
                        icon: "bolt.fill",
                        color: .green
                    )
                    
                    LiquidGlassDivider()
                    
                    MoodCategoryRow(
                        title: "Low Energy",
                        count: moodData.filter { $0.score <= 4 }.count,
                        icon: "moon.fill",
                        color: .purple
                    )
                }
            }
        }
    }
    
    // MARK: - Helpers
    
    private var dateRangeText: String {
        let endDate = Date()
        let startDate = Calendar.current.date(byAdding: .day, value: -selectedPeriod.days, to: endDate) ?? endDate
        
        let formatter = DateFormatter()
        formatter.dateFormat = "d MMM yyyy"
        
        return "\(formatter.string(from: startDate)) – \(formatter.string(from: endDate))"
    }
    
    private var xAxisDates: [Date] {
        let endDate = Date()
        let startDate = Calendar.current.date(byAdding: .day, value: -selectedPeriod.days, to: endDate) ?? endDate
        
        let count = 5
        var dates: [Date] = []
        
        for i in 0..<count {
            let fraction = Double(i) / Double(count - 1)
            let dayOffset = Int(Double(selectedPeriod.days) * fraction)
            if let date = Calendar.current.date(byAdding: .day, value: dayOffset, to: startDate) {
                dates.append(date)
            }
        }
        
        return dates
    }
    
    private var moodColorForAverage: Color {
        MoodHelpers.color(for: Int(averageMood.rounded()))
    }
    
    private var trendEmoji: String {
        switch moodTrend {
        case "improving": return "📈"
        case "declining": return "📉"
        default: return "➡️"
        }
    }
    
    private var trendIcon: String {
        switch moodTrend {
        case "improving": return "arrow.up.right"
        case "declining": return "arrow.down.right"
        default: return "arrow.right"
        }
    }
    
    private var trendColor: Color {
        switch moodTrend {
        case "improving": return .green
        case "declining": return .orange
        default: return .cyan
        }
    }
    
    private func formatDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMM d"
        return formatter.string(from: date)
    }
    
    private func formatAxisDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = selectedPeriod == .week ? "EEE" : "MMM"
        return formatter.string(from: date)
    }
    
    // MARK: - Data Loading
    
    private func loadMoodData() {
        let calendar = Calendar.current
        let endDate = Date()
        let startDate = calendar.date(byAdding: .day, value: -selectedPeriod.days, to: endDate) ?? endDate
        
        // Convert journal entries to mood data points
        moodData = moodInsightsViewModel.entries.compactMap { entry in
            guard let moodScore = entry.moodScore else { return nil }
            
            // Convert Kotlin LocalDate to Swift Date
            let components = DateComponents(
                year: Int(entry.entryDate.year),
                month: Int(entry.entryDate.monthNumber),
                day: Int(entry.entryDate.dayOfMonth)
            )
            guard let entryDate = calendar.date(from: components) else { return nil }
            
            // Filter by date range
            guard entryDate >= startDate && entryDate <= endDate else { return nil }
            
            return MoodDataPoint(
                date: entryDate,
                score: Int(moodScore.intValue),
                entryTitle: entry.title
            )
        }
        .sorted { $0.date < $1.date }
        
        // Calculate stats
        totalEntries = moodData.count
        
        if !moodData.isEmpty {
            averageMood = Double(moodData.reduce(0) { $0 + $1.score }) / Double(moodData.count)
            
            // Calculate trend (compare first half to second half)
            let midIndex = moodData.count / 2
            if midIndex > 0 {
                let firstHalf = moodData.prefix(midIndex)
                let secondHalf = moodData.suffix(from: midIndex)
                
                let firstAvg = Double(firstHalf.reduce(0) { $0 + $1.score }) / Double(firstHalf.count)
                let secondAvg = Double(secondHalf.reduce(0) { $0 + $1.score }) / Double(secondHalf.count)
                
                let diff = secondAvg - firstAvg
                if diff > 0.5 {
                    moodTrend = "improving"
                } else if diff < -0.5 {
                    moodTrend = "declining"
                } else {
                    moodTrend = "stable"
                }
            }
        } else {
            averageMood = 0
            moodTrend = "stable"
        }
    }
}

// MARK: - Mood Chart

struct MoodChart: View {
    let data: [MoodDataPoint]
    let size: CGSize
    let animate: Bool
    @Binding var selectedPoint: MoodDataPoint?
    
    @State private var dragLocation: CGPoint? = nil
    
    var body: some View {
        ZStack {
            // Grid lines
            gridLines
            
            // Gradient fill under the line
            if data.count > 1 {
                gradientFill
            }
            
            // Line path
            if data.count > 1 {
                linePath
            }
            
            // Data points
            dataPoints
        }
        .gesture(
            DragGesture(minimumDistance: 0)
                .onChanged { value in
                    dragLocation = value.location
                    selectNearestPoint(at: value.location)
                }
                .onEnded { _ in
                    dragLocation = nil
                    withAnimation(.easeOut(duration: 0.3)) {
                        selectedPoint = nil
                    }
                }
        )
    }
    
    private var gridLines: some View {
        Canvas { context, size in
            // Horizontal lines (5 levels)
            for i in 0..<5 {
                let y = size.height * CGFloat(i) / 4
                var path = Path()
                path.move(to: CGPoint(x: 0, y: y))
                path.addLine(to: CGPoint(x: size.width, y: y))
                context.stroke(path, with: .color(.white.opacity(0.08)), lineWidth: 1)
            }
            
            // Vertical lines
            let verticalLines = 6
            for i in 0..<verticalLines {
                let x = size.width * CGFloat(i) / CGFloat(verticalLines - 1)
                var path = Path()
                path.move(to: CGPoint(x: x, y: 0))
                path.addLine(to: CGPoint(x: x, y: size.height))
                context.stroke(path, with: .color(.white.opacity(0.05)), lineWidth: 1)
            }
        }
    }
    
    private var gradientFill: some View {
        Path { path in
            guard !data.isEmpty else { return }
            
            let points = data.enumerated().map { index, point -> CGPoint in
                let x = size.width * CGFloat(index) / CGFloat(max(data.count - 1, 1))
                let y = size.height * (1 - point.normalizedScore)
                return CGPoint(x: x, y: y)
            }
            
            path.move(to: CGPoint(x: 0, y: size.height))
            path.addLine(to: points[0])
            
            for i in 1..<points.count {
                let control1 = CGPoint(
                    x: points[i-1].x + (points[i].x - points[i-1].x) * 0.5,
                    y: points[i-1].y
                )
                let control2 = CGPoint(
                    x: points[i-1].x + (points[i].x - points[i-1].x) * 0.5,
                    y: points[i].y
                )
                path.addCurve(to: points[i], control1: control1, control2: control2)
            }
            
            path.addLine(to: CGPoint(x: size.width, y: size.height))
            path.closeSubpath()
        }
        .fill(
            LinearGradient(
                colors: [
                    Color.green.opacity(animate ? 0.25 : 0),
                    Color.mint.opacity(animate ? 0.1 : 0),
                    Color.clear
                ],
                startPoint: .top,
                endPoint: .bottom
            )
        )
    }
    
    private var linePath: some View {
        Path { path in
            guard !data.isEmpty else { return }
            
            let points = data.enumerated().map { index, point -> CGPoint in
                let x = size.width * CGFloat(index) / CGFloat(max(data.count - 1, 1))
                let y = size.height * (1 - point.normalizedScore)
                return CGPoint(x: x, y: y)
            }
            
            path.move(to: points[0])
            
            for i in 1..<points.count {
                let control1 = CGPoint(
                    x: points[i-1].x + (points[i].x - points[i-1].x) * 0.5,
                    y: points[i-1].y
                )
                let control2 = CGPoint(
                    x: points[i-1].x + (points[i].x - points[i-1].x) * 0.5,
                    y: points[i].y
                )
                path.addCurve(to: points[i], control1: control1, control2: control2)
            }
        }
        .trim(from: 0, to: animate ? 1 : 0)
        .stroke(
            LinearGradient(
                colors: [.green, .mint, .cyan],
                startPoint: .leading,
                endPoint: .trailing
            ),
            style: StrokeStyle(lineWidth: 2.5, lineCap: .round, lineJoin: .round)
        )
        .shadow(color: .green.opacity(0.5), radius: 8, x: 0, y: 0)
    }
    
    private var dataPoints: some View {
        ForEach(Array(data.enumerated()), id: \.element.id) { index, point in
            let x = size.width * CGFloat(index) / CGFloat(max(data.count - 1, 1))
            let y = size.height * (1 - point.normalizedScore)
            let isSelected = selectedPoint?.id == point.id
            
            ZStack {
                // Outer glow for selected
                if isSelected {
                    Circle()
                        .fill(point.color.opacity(0.3))
                        .frame(width: 24, height: 24)
                        .blur(radius: 4)
                }
                
                // Main dot
                Circle()
                    .fill(point.color)
                    .frame(width: isSelected ? 14 : 10, height: isSelected ? 14 : 10)
                    .overlay(
                        Circle()
                            .stroke(Color.white.opacity(0.8), lineWidth: isSelected ? 2 : 1)
                    )
                    .shadow(color: point.color.opacity(0.5), radius: 4, x: 0, y: 2)
            }
            .position(x: x, y: y)
            .scaleEffect(animate ? 1 : 0)
            .animation(
                .spring(response: 0.5, dampingFraction: 0.6)
                .delay(Double(index) * 0.05),
                value: animate
            )
            .animation(.spring(response: 0.3), value: isSelected)
        }
    }
    
    private func selectNearestPoint(at location: CGPoint) {
        guard !data.isEmpty else { return }
        
        var nearestPoint: MoodDataPoint?
        var nearestDistance: CGFloat = .infinity
        
        for (index, point) in data.enumerated() {
            let x = size.width * CGFloat(index) / CGFloat(max(data.count - 1, 1))
            let y = size.height * (1 - point.normalizedScore)
            let distance = sqrt(pow(location.x - x, 2) + pow(location.y - y, 2))
            
            if distance < nearestDistance && distance < 50 {
                nearestDistance = distance
                nearestPoint = point
            }
        }
        
        withAnimation(.easeOut(duration: 0.15)) {
            selectedPoint = nearestPoint
        }
    }
}

// MARK: - Supporting Views

struct StatCard: View {
    let title: String
    let value: String
    let subtitle: String
    let icon: String
    let accentColor: Color
    
    var body: some View {
        VStack(spacing: 8) {
            // Icon
            Image(systemName: icon)
                .font(.system(size: 16))
                .foregroundColor(accentColor.opacity(0.8))
            
            // Value
            Text(value)
                .font(.system(size: 22, weight: .bold, design: .rounded))
                .foregroundColor(.white)
            
            // Label
            VStack(spacing: 2) {
                Text(title)
                    .font(.system(size: 9, weight: .bold))
                    .foregroundColor(.white.opacity(0.4))
                
                Text(subtitle)
                    .font(.system(size: 11, weight: .medium))
                    .foregroundColor(.white.opacity(0.6))
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 16)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(.ultraThinMaterial.opacity(0.5))
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(
                            LinearGradient(
                                colors: [.white.opacity(0.15), .white.opacity(0.05)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                            lineWidth: 1
                        )
                )
        )
    }
}

struct BreakdownTab: View {
    let title: String
    let isSelected: Bool
    
    var body: some View {
        Text(title)
            .font(.system(size: 13, weight: isSelected ? .semibold : .medium))
            .foregroundColor(isSelected ? .white : .white.opacity(0.5))
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
            .background(
                isSelected ?
                AnyView(
                    RoundedRectangle(cornerRadius: 8)
                        .fill(Color.white.opacity(0.1))
                ) :
                AnyView(Color.clear)
            )
    }
}

struct MoodCategoryRow: View {
    let title: String
    let count: Int
    let icon: String
    let color: Color
    
    var body: some View {
        HStack {
            // Icon
            Image(systemName: icon)
                .font(.system(size: 16))
                .foregroundColor(color)
                .frame(width: 32)
            
            // Title
            Text(title)
                .font(.system(size: 15, weight: .medium))
                .foregroundColor(.white.opacity(0.9))
            
            Spacer()
            
            // Count
            Text(count > 0 ? "\(count) entries" : "--")
                .font(.system(size: 14))
                .foregroundColor(.white.opacity(0.5))
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Preview

@available(iOS 17.0, *)
#Preview {
    MoodInsightsView()
        .environmentObject(AuthViewModelAdapter())
}
