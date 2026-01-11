import SwiftUI

// MARK: - Profile View (Optimized & Minimal)

@available(iOS 17.0, *)
struct ProfileView: View {
    @EnvironmentObject var authManager: AuthManager
    @EnvironmentObject var journalManager: JournalManager
    
    @State private var showLogout = false
    
    var body: some View {
        NavigationStack {
            List {
                // Profile Header
                Section {
                    profileHeader
                }
                
                // Stats
                Section("Statistics") {
                    StatRow(title: "Total Entries", value: "\(journalManager.entries.count)", icon: "doc.text.fill")
                    StatRow(title: "Day Streak", value: "\(calculateStreak())", icon: "flame.fill")
                    StatRow(title: "Average Mood", value: averageMoodText, icon: "heart.fill")
                }
                
                // Settings (placeholders)
                Section("Settings") {
                    NavigationLink {
                        Text("Notifications")
                    } label: {
                        Label("Notifications", systemImage: "bell.fill")
                    }
                    
                    NavigationLink {
                        Text("Privacy")
                    } label: {
                        Label("Privacy", systemImage: "lock.fill")
                    }
                    
                    NavigationLink {
                        Text("Appearance")
                    } label: {
                        Label("Appearance", systemImage: "paintbrush.fill")
                    }
                }
                
                // Sign Out
                Section {
                    Button(role: .destructive) {
                        showLogout = true
                    } label: {
                        Label("Sign Out", systemImage: "rectangle.portrait.and.arrow.right")
                    }
                }
                
                // App Info
                Section {
                    HStack {
                        Spacer()
                        VStack(spacing: 4) {
                            Text("🍃 JanusLeaf")
                                .font(.footnote)
                            Text("Version 1.0.0")
                                .font(.caption2)
                                .foregroundStyle(.tertiary)
                        }
                        Spacer()
                    }
                    .listRowBackground(Color.clear)
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle("Profile")
            .confirmationDialog("Sign Out", isPresented: $showLogout) {
                Button("Sign Out", role: .destructive) {
                    authManager.logout()
                }
            } message: {
                Text("Are you sure you want to sign out?")
            }
        }
    }
    
    // MARK: - Profile Header
    
    private var profileHeader: some View {
        HStack(spacing: 16) {
            // Avatar
            ZStack {
                Circle()
                    .fill(Color.green.gradient)
                    .frame(width: 60, height: 60)
                
                Text(userInitial)
                    .font(.title.bold())
                    .foregroundStyle(.white)
            }
            
            VStack(alignment: .leading, spacing: 4) {
                if let username = authManager.currentUsername, !username.isEmpty {
                    Text(username)
                        .font(.headline)
                }
                
                if let email = authManager.currentUserEmail {
                    Text(email)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(.vertical, 8)
    }
    
    // MARK: - Helpers
    
    private var userInitial: String {
        if let username = authManager.currentUsername, let first = username.first {
            return String(first).uppercased()
        }
        if let email = authManager.currentUserEmail, let first = email.first {
            return String(first).uppercased()
        }
        return "U"
    }
    
    private var averageMoodText: String {
        let moods = journalManager.entries.compactMap { $0.moodScore?.intValue }
        guard !moods.isEmpty else { return "--" }
        let average = Double(moods.reduce(0, +)) / Double(moods.count)
        return String(format: "%.1f", average)
    }
    
    private func calculateStreak() -> Int {
        let calendar = Calendar.current
        var streak = 0
        var currentDate = Date()
        
        let sortedEntries = journalManager.entries.sorted { entry1, entry2 in
            let date1 = calendar.date(from: DateComponents(
                year: Int(entry1.entryDate.year),
                month: Int(entry1.entryDate.monthNumber),
                day: Int(entry1.entryDate.dayOfMonth)
            )) ?? Date.distantPast
            
            let date2 = calendar.date(from: DateComponents(
                year: Int(entry2.entryDate.year),
                month: Int(entry2.entryDate.monthNumber),
                day: Int(entry2.entryDate.dayOfMonth)
            )) ?? Date.distantPast
            
            return date1 > date2
        }
        
        for entry in sortedEntries {
            let entryDate = calendar.date(from: DateComponents(
                year: Int(entry.entryDate.year),
                month: Int(entry.entryDate.monthNumber),
                day: Int(entry.entryDate.dayOfMonth)
            )) ?? Date.distantPast
            
            if calendar.isDate(entryDate, inSameDayAs: currentDate) ||
               calendar.isDate(entryDate, inSameDayAs: calendar.date(byAdding: .day, value: -1, to: currentDate) ?? Date()) {
                streak += 1
                currentDate = entryDate
            } else {
                break
            }
        }
        
        return streak
    }
}

// MARK: - Stat Row

struct StatRow: View {
    let title: String
    let value: String
    let icon: String
    
    var body: some View {
        HStack {
            Label(title, systemImage: icon)
            Spacer()
            Text(value)
                .foregroundStyle(.secondary)
        }
    }
}

// MARK: - Preview

@available(iOS 17.0, *)
#Preview {
    ProfileView()
        .environmentObject(AuthManager())
        .environmentObject(JournalManager())
}
