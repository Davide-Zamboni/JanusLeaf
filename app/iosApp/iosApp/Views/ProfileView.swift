import SwiftUI
import Shared
import KMPObservableViewModelSwiftUI

// MARK: - Profile View

@available(iOS 17.0, *)
struct ProfileView: View {
    @StateViewModel private var profileViewModel = SharedModule.shared.createObservableProfileViewModel()
    
    @State private var showLogoutConfirmation = false
    @State private var animateItems = false
    
    var body: some View {
        ZStack {
            // Background
            JournalBackground()
            
            ScrollView(showsIndicators: false) {
                VStack(spacing: 24) {
                    // Header
                    headerSection
                        .padding(.top, 60)
                    
                    // Profile card
                    profileCard
                    
                    // Stats section
                    statsSection
                    
                    // Settings section
                    settingsSection
                    
                    // Sign out button
                    signOutButton
                    
                    // App info
                    appInfoSection
                    
                    Spacer(minLength: 120)
                }
                .padding(.horizontal, 20)
            }
        }
        .ignoresSafeArea()
        .onAppear {
            withAnimation(.easeOut(duration: 0.6).delay(0.2)) {
                animateItems = true
            }
        }
        .confirmationDialog("Sign Out", isPresented: $showLogoutConfirmation, titleVisibility: .visible) {
            Button("Sign Out", role: .destructive) {
                profileViewModel.logout()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Are you sure you want to sign out?")
        }
    }
    
    // MARK: - Header Section
    
    private var headerSection: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("Profile")
                    .font(.system(size: 34, weight: .bold, design: .rounded))
                    .foregroundColor(.white)
                
                Text("Manage your account")
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(.white.opacity(0.6))
            }
            
            Spacer()
        }
        .opacity(animateItems ? 1 : 0)
        .offset(y: animateItems ? 0 : -20)
    }
    
    // MARK: - Profile Card
    
    private var profileCard: some View {
        LiquidGlassCard(cornerRadius: 24, padding: 24) {
            VStack(spacing: 20) {
                // Avatar
                ZStack {
                    // Outer glow
                    Circle()
                        .fill(
                            RadialGradient(
                                colors: [Color.green.opacity(0.3), Color.clear],
                                center: .center,
                                startRadius: 0,
                                endRadius: 50
                            )
                        )
                        .frame(width: 100, height: 100)
                    
                    // Avatar circle
                    Circle()
                        .fill(
                            LinearGradient(
                                colors: [
                                    Color(red: 0.15, green: 0.5, blue: 0.35),
                                    Color(red: 0.1, green: 0.4, blue: 0.28)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .frame(width: 80, height: 80)
                        .overlay(
                            Circle()
                                .stroke(
                                    LinearGradient(
                                        colors: [.white.opacity(0.4), .white.opacity(0.1)],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    ),
                                    lineWidth: 2
                                )
                        )
                        .shadow(color: .green.opacity(0.3), radius: 15, x: 0, y: 8)
                    
                    // Initial or emoji
                    Text(userInitial)
                        .font(.system(size: 32, weight: .bold, design: .rounded))
                        .foregroundColor(.white)
                }
                
                // User info
                VStack(spacing: 6) {
                    if let username = profileViewModel.currentUsername, !username.isEmpty {
                        Text(username)
                            .font(.system(size: 22, weight: .bold))
                            .foregroundColor(.white)
                    }
                    
                    if let email = profileViewModel.currentUserEmail {
                        Text(email)
                            .font(.system(size: 14))
                            .foregroundColor(.white.opacity(0.6))
                    }
                }
                
                // Member since badge
                HStack(spacing: 6) {
                    Image(systemName: "leaf.fill")
                        .font(.system(size: 12))
                        .foregroundColor(.green)
                    
                    Text("JanusLeaf Member")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundColor(.white.opacity(0.7))
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(
                    Capsule()
                        .fill(Color.green.opacity(0.15))
                        .overlay(
                            Capsule()
                                .stroke(Color.green.opacity(0.3), lineWidth: 1)
                        )
                )
            }
        }
        .opacity(animateItems ? 1 : 0)
        .offset(y: animateItems ? 0 : 20)
        .animation(.spring(response: 0.5, dampingFraction: 0.8).delay(0.1), value: animateItems)
    }
    
    // MARK: - Stats Section
    
    private var statsSection: some View {
        LiquidGlassCard(cornerRadius: 20, padding: 16) {
            HStack(spacing: 0) {
                // Total entries
                ProfileStatItem(
                    value: "\(profileViewModel.entries.count)",
                    label: "Entries",
                    icon: "doc.text.fill"
                )
                
                // Divider
                Rectangle()
                    .fill(Color.white.opacity(0.1))
                    .frame(width: 1, height: 50)
                
                // Current streak (placeholder)
                ProfileStatItem(
                    value: "\(calculateStreak())",
                    label: "Day Streak",
                    icon: "flame.fill"
                )
                
                // Divider
                Rectangle()
                    .fill(Color.white.opacity(0.1))
                    .frame(width: 1, height: 50)
                
                // Average mood
                ProfileStatItem(
                    value: averageMoodText,
                    label: "Avg Mood",
                    icon: "heart.fill"
                )
            }
        }
        .opacity(animateItems ? 1 : 0)
        .offset(y: animateItems ? 0 : 20)
        .animation(.spring(response: 0.5, dampingFraction: 0.8).delay(0.2), value: animateItems)
    }
    
    // MARK: - Settings Section
    
    private var settingsSection: some View {
        LiquidGlassCard(cornerRadius: 20, padding: 0) {
            VStack(spacing: 0) {
                SettingsRow(icon: "bell.fill", title: "Notifications", color: .red)
                
                LiquidGlassDivider()
                    .padding(.horizontal, 16)
                
                SettingsRow(icon: "lock.fill", title: "Privacy", color: .blue)
                
                LiquidGlassDivider()
                    .padding(.horizontal, 16)
                
                SettingsRow(icon: "paintbrush.fill", title: "Appearance", color: .purple)
                
                LiquidGlassDivider()
                    .padding(.horizontal, 16)
                
                SettingsRow(icon: "icloud.fill", title: "Data & Backup", color: .cyan)
            }
            .padding(.vertical, 8)
        }
        .opacity(animateItems ? 1 : 0)
        .offset(y: animateItems ? 0 : 20)
        .animation(.spring(response: 0.5, dampingFraction: 0.8).delay(0.3), value: animateItems)
    }
    
    // MARK: - Sign Out Button
    
    private var signOutButton: some View {
        Button(action: { showLogoutConfirmation = true }) {
            HStack {
                Image(systemName: "rectangle.portrait.and.arrow.right")
                    .font(.system(size: 16))
                
                Text("Sign Out")
                    .font(.system(size: 16, weight: .medium))
            }
            .foregroundColor(.red.opacity(0.9))
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(.ultraThinMaterial.opacity(0.5))
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(Color.red.opacity(0.2), lineWidth: 1)
                    )
            )
        }
        .buttonStyle(ScaleButtonStyle())
        .opacity(animateItems ? 1 : 0)
        .offset(y: animateItems ? 0 : 20)
        .animation(.spring(response: 0.5, dampingFraction: 0.8).delay(0.4), value: animateItems)
    }
    
    // MARK: - App Info Section
    
    private var appInfoSection: some View {
        VStack(spacing: 8) {
            Text("🍃 JanusLeaf")
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(.white.opacity(0.4))
            
            Text("Version 1.0.0")
                .font(.system(size: 12))
                .foregroundColor(.white.opacity(0.3))
        }
        .padding(.top, 16)
        .opacity(animateItems ? 1 : 0)
        .animation(.easeOut(duration: 0.5).delay(0.5), value: animateItems)
    }
    
    // MARK: - Helpers
    
    private var userInitial: String {
        if let username = profileViewModel.currentUsername, let first = username.first {
            return String(first).uppercased()
        }
        if let email = profileViewModel.currentUserEmail, let first = email.first {
            return String(first).uppercased()
        }
        return "U"
    }
    
    private var averageMoodText: String {
        let moods = profileViewModel.entries.compactMap { $0.moodScore?.intValue }
        guard !moods.isEmpty else { return "--" }
        let average = Double(moods.reduce(0, +)) / Double(moods.count)
        return String(format: "%.1f", average)
    }
    
    private func calculateStreak() -> Int {
        // Simple streak calculation - consecutive days with entries
        let calendar = Calendar.current
        var streak = 0
        var currentDate = Date()
        
        let sortedEntries = profileViewModel.entries.sorted { entry1, entry2 in
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

// MARK: - Profile Stat Item

struct ProfileStatItem: View {
    let value: String
    let label: String
    let icon: String
    
    var body: some View {
        VStack(spacing: 6) {
            Image(systemName: icon)
                .font(.system(size: 14))
                .foregroundColor(.green.opacity(0.8))
            
            Text(value)
                .font(.system(size: 20, weight: .bold, design: .rounded))
                .foregroundColor(.white)
            
            Text(label)
                .font(.system(size: 11))
                .foregroundColor(.white.opacity(0.5))
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Settings Row

struct SettingsRow: View {
    let icon: String
    let title: String
    let color: Color
    
    var body: some View {
        Button(action: {}) {
            HStack(spacing: 14) {
                // Icon
                ZStack {
                    RoundedRectangle(cornerRadius: 8)
                        .fill(color.opacity(0.15))
                        .frame(width: 32, height: 32)
                    
                    Image(systemName: icon)
                        .font(.system(size: 14))
                        .foregroundColor(color)
                }
                
                // Title
                Text(title)
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(.white.opacity(0.9))
                
                Spacer()
                
                // Arrow
                Image(systemName: "chevron.right")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(.white.opacity(0.3))
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Preview

@available(iOS 17.0, *)
#Preview {
    ProfileView()
}
