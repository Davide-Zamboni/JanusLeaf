import SwiftUI

// MARK: - Scroll Offset Preference Key

struct ScrollOffsetPreferenceKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

// MARK: - Scroll View with Offset Tracking

struct ScrollViewWithOffset<Content: View>: View {
    let axes: Axis.Set
    let showsIndicators: Bool
    let onOffsetChange: (CGFloat) -> Void
    let content: () -> Content
    
    init(
        axes: Axis.Set = .vertical,
        showsIndicators: Bool = true,
        onOffsetChange: @escaping (CGFloat) -> Void = { _ in },
        @ViewBuilder content: @escaping () -> Content
    ) {
        self.axes = axes
        self.showsIndicators = showsIndicators
        self.onOffsetChange = onOffsetChange
        self.content = content
    }
    
    var body: some View {
        ScrollView(axes, showsIndicators: showsIndicators) {
            ZStack(alignment: .top) {
                // Invisible tracking view
                GeometryReader { geometry in
                    Color.clear
                        .preference(
                            key: ScrollOffsetPreferenceKey.self,
                            value: geometry.frame(in: .named("scrollView")).minY
                        )
                }
                .frame(height: 0)
                
                content()
            }
        }
        .coordinateSpace(name: "scrollView")
        .onPreferenceChange(ScrollOffsetPreferenceKey.self, perform: onOffsetChange)
    }
}
