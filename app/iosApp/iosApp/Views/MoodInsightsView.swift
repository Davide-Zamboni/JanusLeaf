import SwiftUI
import Charts
import Shared

// MARK: - Time Period

enum TimePeriod: String, CaseIterable, Identifiable {
    case week = "W"
    case month = "M"
    case sixMonths = "6M"
    case year = "Y"
    
    var id: String { rawValue }
    
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
    
    var color: Color {
        switch score {
        case 1...2: return .red
        case 3...4: return .orange
        case 5...6: return .yellow
        case 7...8: return .green
        case 9...10: return .mint
        default: return .gray
        }
    }
    
    var emoji: String {
        switch score {
        case 1...2: return "😢"
        case 3...4: return "😔"
        case 5...6: return "😐"
        case 7...8: return "😊"
        case 9...10: return "😄"
        default: return "😶"
        }
    }
}

// MARK: - Mood Insights View (Optimized & Minimal)

@available(iOS 17.0, *)
struct MoodInsightsView: View {
    @EnvironmentObject var authManager: AuthManager
    @EnvironmentObject var journalManager: JournalManager
    
    @State private var selectedPeriod: TimePeriod = .month
    @State private var moodData: [MoodDataPoint] = []
    @State private var selectedPoint: MoodDataPoint? = nil
    @State private var showLogout = false
    
    // Computed stats
    private var averageMood: Double {
        guard !moodData.isEmpty else { return 0 }
        return Double(moodData.reduce(0) { $0 + $1.score }) / Double(moodData.count)
    }
    
    private var trend: String {
        guard moodData.count > 2 else { return "stable" }
        let mid = moodData.count / 2
        let firstHalf = moodData.prefix(mid)
        let secondHalf = moodData.suffix(from: mid)
        
        guard !firstHalf.isEmpty, !secondHalf.isEmpty else { return "stable" }
        
        let firstAvg = Double(firstHalf.reduce(0) { $0 + $1.score }) / Double(firstHalf.count)
        let secondAvg = Double(secondHalf.reduce(0) { $0 + $1.score }) / Double(secondHalf.count)
        
        let diff = secondAvg - firstAvg
        if diff > 0.5 { return "improving" }
        if diff < -0.5 { return "declining" }
        return "stable"
    }
    
    var body: some View {
        NavigationStack {
            List {
                // Period Picker
                Section {
                    Picker("Period", selection: $selectedPeriod) {
                        ForEach(TimePeriod.allCases) { period in
                            Text(period.displayName).tag(period)
                        }
                    }
                    .pickerStyle(.segmented)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets())
                }
                
                // Stats
                Section("Overview") {
                    statsRow
                }
                
                // Chart
                Section("Mood Over Time") {
                    if moodData.isEmpty {
                        emptyChart
                    } else {
                        chartView
                            .frame(height: 200)
                    }
                }
                
                // Breakdown
                Section("Breakdown") {
                    breakdownView
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle("Insights")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        showLogout = true
                    } label: {
                        Image(systemName: "person.circle")
                    }
                }
            }
            .confirmationDialog("Sign Out", isPresented: $showLogout) {
                Button("Sign Out", role: .destructive) {
                    authManager.logout()
                }
            }
            .onAppear {
                loadMoodData()
            }
            .onChange(of: selectedPeriod) { _, _ in
                loadMoodData()
            }
            .onChange(of: journalManager.entries) { _, _ in
                loadMoodData()
            }
        }
    }
    
    // MARK: - Stats Row
    
    private var statsRow: some View {
        HStack {
            StatItem(
                title: "Entries",
                value: "\(moodData.count)",
                icon: "doc.text.fill",
                color: .blue
            )
            
            Divider()
            
            StatItem(
                title: "Average",
                value: moodData.isEmpty ? "--" : String(format: "%.1f", averageMood),
                icon: "heart.fill",
                color: colorForScore(Int(averageMood.rounded()))
            )
            
            Divider()
            
            StatItem(
                title: "Trend",
                value: trendIcon,
                icon: trendSystemIcon,
                color: trendColor
            )
        }
        .frame(height: 70)
    }
    
    // MARK: - Chart View
    
    private var chartView: some View {
        Chart(moodData) { point in
            LineMark(
                x: .value("Date", point.date),
                y: .value("Mood", point.score)
            )
            .foregroundStyle(Color.green.gradient)
            .interpolationMethod(.catmullRom)
            
            AreaMark(
                x: .value("Date", point.date),
                y: .value("Mood", point.score)
            )
            .foregroundStyle(
                LinearGradient(
                    colors: [.green.opacity(0.3), .green.opacity(0.05)],
                    startPoint: .top,
                    endPoint: .bottom
                )
            )
            .interpolationMethod(.catmullRom)
            
            PointMark(
                x: .value("Date", point.date),
                y: .value("Mood", point.score)
            )
            .foregroundStyle(point.color)
            .symbolSize(selectedPoint?.id == point.id ? 100 : 40)
        }
        .chartYScale(domain: 1...10)
        .chartYAxis {
            AxisMarks(values: [1, 5, 10]) { value in
                AxisGridLine()
                AxisValueLabel {
                    if let intValue = value.as(Int.self) {
                        Text(labelForScore(intValue))
                            .font(.caption2)
                    }
                }
            }
        }
        .chartXAxis {
            AxisMarks(values: .automatic(desiredCount: 5)) { _ in
                AxisGridLine()
                AxisValueLabel(format: .dateTime.month(.abbreviated).day())
            }
        }
        .chartOverlay { proxy in
            GeometryReader { geo in
                Rectangle()
                    .fill(Color.clear)
                    .contentShape(Rectangle())
                    .gesture(
                        DragGesture(minimumDistance: 0)
                            .onChanged { value in
                                selectPoint(at: value.location, proxy: proxy, geo: geo)
                            }
                            .onEnded { _ in
                                selectedPoint = nil
                            }
                    )
            }
        }
    }
    
    // MARK: - Empty Chart
    
    private var emptyChart: some View {
        ContentUnavailableView {
            Label("No Data", systemImage: "chart.line.uptrend.xyaxis")
        } description: {
            Text("Start journaling to see mood patterns")
        }
        .frame(height: 200)
    }
    
    // MARK: - Breakdown View
    
    private var breakdownView: some View {
        VStack(spacing: 0) {
            BreakdownRow(
                title: "High Energy",
                count: moodData.filter { $0.score >= 7 }.count,
                icon: "bolt.fill",
                color: .green
            )
            
            BreakdownRow(
                title: "Neutral",
                count: moodData.filter { $0.score >= 4 && $0.score <= 6 }.count,
                icon: "minus.circle.fill",
                color: .yellow
            )
            
            BreakdownRow(
                title: "Low Energy",
                count: moodData.filter { $0.score < 4 }.count,
                icon: "moon.fill",
                color: .purple
            )
        }
    }
    
    // MARK: - Data Loading
    
    private func loadMoodData() {
        let calendar = Calendar.current
        let endDate = Date()
        let startDate = calendar.date(byAdding: .day, value: -selectedPeriod.days, to: endDate) ?? endDate
        
        moodData = journalManager.entries.compactMap { entry in
            guard let moodScore = entry.moodScore else { return nil }
            
            let components = DateComponents(
                year: Int(entry.entryDate.year),
                month: Int(entry.entryDate.monthNumber),
                day: Int(entry.entryDate.dayOfMonth)
            )
            guard let entryDate = calendar.date(from: components) else { return nil }
            guard entryDate >= startDate && entryDate <= endDate else { return nil }
            
            return MoodDataPoint(
                date: entryDate,
                score: Int(moodScore.intValue),
                entryTitle: entry.title
            )
        }
        .sorted { $0.date < $1.date }
    }
    
    private func selectPoint(at location: CGPoint, proxy: ChartProxy, geo: GeometryProxy) {
        let x = location.x - geo[proxy.plotFrame!].origin.x
        guard let date: Date = proxy.value(atX: x) else { return }
        
        // Find closest point
        selectedPoint = moodData.min { point1, point2 in
            abs(point1.date.timeIntervalSince(date)) < abs(point2.date.timeIntervalSince(date))
        }
    }
    
    // MARK: - Helpers
    
    private func labelForScore(_ score: Int) -> String {
        switch score {
        case 1: return "😢"
        case 5: return "😐"
        case 10: return "😄"
        default: return "\(score)"
        }
    }
    
    private func colorForScore(_ score: Int) -> Color {
        switch score {
        case 1...2: return .red
        case 3...4: return .orange
        case 5...6: return .yellow
        case 7...8: return .green
        case 9...10: return .mint
        default: return .gray
        }
    }
    
    private var trendIcon: String {
        switch trend {
        case "improving": return "📈"
        case "declining": return "📉"
        default: return "➡️"
        }
    }
    
    private var trendSystemIcon: String {
        switch trend {
        case "improving": return "arrow.up.right"
        case "declining": return "arrow.down.right"
        default: return "arrow.right"
        }
    }
    
    private var trendColor: Color {
        switch trend {
        case "improving": return .green
        case "declining": return .orange
        default: return .blue
        }
    }
}

// MARK: - Stat Item

struct StatItem: View {
    let title: String
    let value: String
    let icon: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 4) {
            Image(systemName: icon)
                .foregroundStyle(color)
            
            Text(value)
                .font(.title2.bold())
            
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Breakdown Row

struct BreakdownRow: View {
    let title: String
    let count: Int
    let icon: String
    let color: Color
    
    var body: some View {
        HStack {
            Image(systemName: icon)
                .foregroundStyle(color)
                .frame(width: 24)
            
            Text(title)
            
            Spacer()
            
            Text("\(count)")
                .foregroundStyle(.secondary)
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Preview

@available(iOS 17.0, *)
#Preview {
    MoodInsightsView()
        .environmentObject(AuthManager())
        .environmentObject(JournalManager())
}
