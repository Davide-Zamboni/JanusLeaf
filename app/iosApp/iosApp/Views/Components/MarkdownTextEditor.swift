import SwiftUI
import UIKit

// MARK: - Append-Only Rich Text Editor

/// A text editor that renders markdown from backend but doesn't allow editing to markdown.
/// Backspace adds strikethrough instead of deleting text.
@available(iOS 17.0, *)
struct MarkdownTextEditor: View {
    @Binding var markdownText: String
    @State private var internalIsFocused: Bool = false
    @FocusState.Binding var isFocused: Bool
    
    let placeholder: String
    let onTextChange: ((String) -> Void)?
    
    init(
        text: Binding<String>,
        placeholder: String = "Start writing...",
        isFocused: FocusState<Bool>.Binding,
        onTextChange: ((String) -> Void)? = nil
    ) {
        self._markdownText = text
        self.placeholder = placeholder
        self._isFocused = isFocused
        self.onTextChange = onTextChange
    }
    
    var body: some View {
        ZStack(alignment: .topLeading) {
            if markdownText.isEmpty && !internalIsFocused {
                Text(placeholder)
                    .font(.body)
                    .foregroundColor(.white.opacity(0.3))
                    .padding(.top, 8)
                    .padding(.leading, 5)
                    .allowsHitTesting(false)
            }
            
            AppendOnlyTextView(
                markdownText: $markdownText,
                isFocused: $internalIsFocused,
                onTextChange: onTextChange
            )
            .frame(minHeight: 300)
        }
        .onChange(of: internalIsFocused) { _, newValue in
            isFocused = newValue
        }
        .onChange(of: isFocused) { _, newValue in
            internalIsFocused = newValue
        }
    }
}

// MARK: - Append-Only Text View (UIKit Bridge)

struct AppendOnlyTextView: UIViewRepresentable {
    @Binding var markdownText: String
    @Binding var isFocused: Bool
    let onTextChange: ((String) -> Void)?
    
    func makeUIView(context: Context) -> UITextView {
        let textView = UITextView()
        textView.delegate = context.coordinator
        textView.backgroundColor = .clear
        textView.textColor = .white
        textView.font = .systemFont(ofSize: 17)
        textView.autocorrectionType = .yes
        textView.autocapitalizationType = .sentences
        textView.keyboardAppearance = .dark
        textView.tintColor = UIColor(Color(red: 0.3, green: 0.7, blue: 0.5))
        textView.textContainerInset = UIEdgeInsets(top: 8, left: 0, bottom: 8, right: 0)
        
        // Set initial content from markdown
        textView.attributedText = MarkdownParser.toAttributedString(markdownText)
        
        // Store coordinator reference and track loaded markdown
        context.coordinator.textView = textView
        context.coordinator.lastLoadedMarkdown = markdownText
        
        return textView
    }
    
    func updateUIView(_ textView: UITextView, context: Context) {
        // Update content if markdown changed from outside (e.g., loading from backend)
        // Only update if not currently editing to avoid cursor jumps
        if !textView.isFirstResponder {
            // Check if the source markdown changed (not just the rendered content)
            if context.coordinator.lastLoadedMarkdown != markdownText {
                textView.attributedText = MarkdownParser.toAttributedString(markdownText)
                context.coordinator.lastLoadedMarkdown = markdownText
            }
        }
        
        // Handle focus
        if isFocused && !textView.isFirstResponder {
            textView.becomeFirstResponder()
        } else if !isFocused && textView.isFirstResponder {
            textView.resignFirstResponder()
        }
    }
    
    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }
    
    // MARK: - Coordinator
    
    class Coordinator: NSObject, UITextViewDelegate {
        var parent: AppendOnlyTextView
        weak var textView: UITextView?
        
        // Track the last loaded markdown to detect external changes
        var lastLoadedMarkdown: String = ""
        
        // Track the last known length to detect backspace
        private var lastTextLength: Int = 0
        private var isProcessingStrikethrough = false
        
        // Default typing attributes (no strikethrough)
        private var defaultTypingAttributes: [NSAttributedString.Key: Any] {
            [
                .font: UIFont.systemFont(ofSize: 17),
                .foregroundColor: UIColor.white,
                .strikethroughStyle: 0
            ]
        }
        
        init(_ parent: AppendOnlyTextView) {
            self.parent = parent
        }
        
        func textViewDidBeginEditing(_ textView: UITextView) {
            parent.isFocused = true
            lastTextLength = textView.text.count
            // Ensure typing attributes are normal when starting to edit
            textView.typingAttributes = defaultTypingAttributes
        }
        
        func textViewDidEndEditing(_ textView: UITextView) {
            parent.isFocused = false
        }
        
        // Reset typing attributes whenever selection changes
        func textViewDidChangeSelection(_ textView: UITextView) {
            if !isProcessingStrikethrough {
                // Always reset typing attributes to prevent inheriting strikethrough
                textView.typingAttributes = defaultTypingAttributes
            }
        }
        
        // Intercept text changes to handle backspace specially
        func textView(_ textView: UITextView, shouldChangeTextIn range: NSRange, replacementText text: String) -> Bool {
            // If processing strikethrough, allow the change
            if isProcessingStrikethrough {
                return true
            }
            
            // Detect backspace: range has length > 0 and replacement is empty
            if text.isEmpty && range.length > 0 {
                // Get the text being deleted
                let nsText = textView.text as NSString
                let deletedText = nsText.substring(with: range)
                
                // Allow actual deletion for spaces and whitespace
                if deletedText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    return true
                }
                
                // Check if already has strikethrough - if so, skip it and find previous non-struck text
                let attrs = textView.attributedText.attributes(at: range.location, effectiveRange: nil)
                if let strikeStyle = attrs[.strikethroughStyle] as? Int, strikeStyle != 0 {
                    // Already struck through - find the previous non-struck character
                    var newLocation = range.location - 1
                    while newLocation >= 0 {
                        let prevAttrs = textView.attributedText.attributes(at: newLocation, effectiveRange: nil)
                        let prevStrike = (prevAttrs[.strikethroughStyle] as? Int ?? 0) != 0
                        if !prevStrike {
                            // Found non-struck character - check if it's whitespace
                            let charRange = NSRange(location: newLocation, length: 1)
                            let char = nsText.substring(with: charRange)
                            if char.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                                // It's whitespace - actually delete it
                                return true
                            }
                            // Apply strikethrough to this character
                            applyStrikethrough(in: textView, range: charRange)
                            return false
                        }
                        newLocation -= 1
                    }
                    // All previous text is struck through, do nothing
                    return false
                }
                
                // This is a delete operation on non-struck text - convert to strikethrough
                applyStrikethrough(in: textView, range: range)
                return false
            }
            
            // Allow normal text input
            return true
        }
        
        func textViewDidChange(_ textView: UITextView) {
            if isProcessingStrikethrough {
                return
            }
            
            // Convert attributed text back to markdown
            let markdown = MarkdownParser.toMarkdown(textView.attributedText)
            parent.markdownText = markdown
            lastLoadedMarkdown = markdown  // Track our own changes
            parent.onTextChange?(markdown)
            
            lastTextLength = textView.text.count
        }
        
        private func applyStrikethrough(in textView: UITextView, range: NSRange) {
            isProcessingStrikethrough = true
            
            let mutableAttr = NSMutableAttributedString(attributedString: textView.attributedText)
            
            // Add strikethrough
            mutableAttr.addAttribute(.strikethroughStyle, value: NSUnderlineStyle.single.rawValue, range: range)
            mutableAttr.addAttribute(.strikethroughColor, value: UIColor.white.withAlphaComponent(0.6), range: range)
            
            // Also dim the text slightly
            mutableAttr.addAttribute(.foregroundColor, value: UIColor.white.withAlphaComponent(0.5), range: range)
            
            textView.attributedText = mutableAttr
            
            // Set cursor BEFORE the strikethrough text so backspace continues working backwards
            let newCursorPosition = range.location
            if let newPosition = textView.position(from: textView.beginningOfDocument, offset: newCursorPosition) {
                textView.selectedTextRange = textView.textRange(from: newPosition, to: newPosition)
            }
            
            // Update markdown
            let markdown = MarkdownParser.toMarkdown(textView.attributedText)
            parent.markdownText = markdown
            lastLoadedMarkdown = markdown  // Track our own changes
            parent.onTextChange?(markdown)
            
            isProcessingStrikethrough = false
            
            // Reset typing attributes AFTER everything else to ensure new text is normal
            // This must be done after isProcessingStrikethrough is false
            DispatchQueue.main.async { [weak textView, weak self] in
                guard let textView = textView, let self = self else { return }
                textView.typingAttributes = self.defaultTypingAttributes
            }
        }
    }
}

// MARK: - Markdown Parser

struct MarkdownParser {
    
    /// Convert markdown string to NSAttributedString for display
    static func toAttributedString(_ markdown: String) -> NSAttributedString {
        let result = NSMutableAttributedString()
        let lines = markdown.components(separatedBy: "\n")
        
        for (index, line) in lines.enumerated() {
            let attributedLine = parseLine(line)
            result.append(attributedLine)
            
            if index < lines.count - 1 {
                result.append(NSAttributedString(string: "\n"))
            }
        }
        
        // Ensure we have at least default attributes
        if result.length == 0 {
            return NSAttributedString(string: "", attributes: [
                .font: UIFont.systemFont(ofSize: 17),
                .foregroundColor: UIColor.white
            ])
        }
        
        return result
    }
    
    private static func parseLine(_ line: String) -> NSAttributedString {
        let baseFont: UIFont = .systemFont(ofSize: 17)
        let baseColor: UIColor = .white
        
        // Parse inline formatting
        return parseInlineFormatting(line, baseFont: baseFont, baseColor: baseColor)
    }
    
    private static func parseInlineFormatting(_ text: String, baseFont: UIFont, baseColor: UIColor) -> NSAttributedString {
        let result = NSMutableAttributedString()
        var currentIndex = text.startIndex
        
        while currentIndex < text.endIndex {
            // Check for strikethrough (~~text~~)
            if text[currentIndex...].hasPrefix("~~") {
                let afterOpen = text.index(currentIndex, offsetBy: 2)
                if afterOpen < text.endIndex, let closeRange = text[afterOpen...].range(of: "~~") {
                    let strikeContent = String(text[afterOpen..<closeRange.lowerBound])
                    let strikeAttr = NSMutableAttributedString(string: strikeContent, attributes: [
                        .font: baseFont,
                        .foregroundColor: baseColor.withAlphaComponent(0.5),
                        .strikethroughStyle: NSUnderlineStyle.single.rawValue,
                        .strikethroughColor: baseColor.withAlphaComponent(0.6)
                    ])
                    result.append(strikeAttr)
                    
                    currentIndex = closeRange.upperBound
                    continue
                }
            }
            
            // Check for bold (**text**)
            if text[currentIndex...].hasPrefix("**") {
                let afterOpen = text.index(currentIndex, offsetBy: 2)
                if afterOpen < text.endIndex, let closeRange = text[afterOpen...].range(of: "**") {
                    let boldContent = String(text[afterOpen..<closeRange.lowerBound])
                    let boldFont = UIFont.systemFont(ofSize: baseFont.pointSize, weight: .bold)
                    let boldAttr = NSAttributedString(string: boldContent, attributes: [
                        .font: boldFont,
                        .foregroundColor: baseColor
                    ])
                    result.append(boldAttr)
                    
                    currentIndex = closeRange.upperBound
                    continue
                }
            }
            
            // Check for italic (*text*) - but not **
            if text[currentIndex...].hasPrefix("*") && !text[currentIndex...].hasPrefix("**") {
                let afterOpen = text.index(currentIndex, offsetBy: 1)
                if afterOpen < text.endIndex {
                    var searchIndex = afterOpen
                    var foundClose: String.Index? = nil
                    
                    while searchIndex < text.endIndex {
                        if text[searchIndex] == "*" {
                            let nextIndex = text.index(after: searchIndex)
                            if nextIndex >= text.endIndex || text[nextIndex] != "*" {
                                foundClose = searchIndex
                                break
                            }
                        }
                        searchIndex = text.index(after: searchIndex)
                    }
                    
                    if let closeIndex = foundClose {
                        let italicContent = String(text[afterOpen..<closeIndex])
                        let italicFont = UIFont.italicSystemFont(ofSize: baseFont.pointSize)
                        let italicAttr = NSAttributedString(string: italicContent, attributes: [
                            .font: italicFont,
                            .foregroundColor: baseColor
                        ])
                        result.append(italicAttr)
                        
                        currentIndex = text.index(after: closeIndex)
                        continue
                    }
                }
            }
            
            // Check for underline (<u>text</u>)
            if text[currentIndex...].hasPrefix("<u>") {
                let afterOpen = text.index(currentIndex, offsetBy: 3)
                if afterOpen < text.endIndex, let closeRange = text[afterOpen...].range(of: "</u>") {
                    let underlineContent = String(text[afterOpen..<closeRange.lowerBound])
                    let underlineAttr = NSMutableAttributedString(string: underlineContent, attributes: [
                        .font: baseFont,
                        .foregroundColor: baseColor,
                        .underlineStyle: NSUnderlineStyle.single.rawValue
                    ])
                    result.append(underlineAttr)
                    
                    currentIndex = closeRange.upperBound
                    continue
                }
            }
            
            // Regular character
            let char = String(text[currentIndex])
            let charAttr = NSAttributedString(string: char, attributes: [
                .font: baseFont,
                .foregroundColor: baseColor
            ])
            result.append(charAttr)
            currentIndex = text.index(after: currentIndex)
        }
        
        return result
    }
    
    /// Convert NSAttributedString back to markdown
    static func toMarkdown(_ attributedString: NSAttributedString) -> String {
        var result = ""
        let text = attributedString.string
        
        guard !text.isEmpty else { return "" }
        
        var i = 0
        while i < text.count {
            let location = i
            
            // Get attributes at this location
            let attrs = attributedString.attributes(at: location, effectiveRange: nil)
            let font = attrs[.font] as? UIFont
            let isStrikethrough = (attrs[.strikethroughStyle] as? Int ?? 0) != 0
            let isBold = font?.fontDescriptor.symbolicTraits.contains(.traitBold) == true
            let isItalic = font?.fontDescriptor.symbolicTraits.contains(.traitItalic) == true
            let isUnderline = (attrs[.underlineStyle] as? Int ?? 0) != 0
            
            // Find run of same formatting
            var runEnd = i + 1
            while runEnd < text.count {
                let nextAttrs = attributedString.attributes(at: runEnd, effectiveRange: nil)
                let nextFont = nextAttrs[.font] as? UIFont
                let nextStrike = (nextAttrs[.strikethroughStyle] as? Int ?? 0) != 0
                let nextBold = nextFont?.fontDescriptor.symbolicTraits.contains(.traitBold) == true
                let nextItalic = nextFont?.fontDescriptor.symbolicTraits.contains(.traitItalic) == true
                let nextUnderline = (nextAttrs[.underlineStyle] as? Int ?? 0) != 0
                
                if nextStrike != isStrikethrough || nextBold != isBold || 
                   nextItalic != isItalic || nextUnderline != isUnderline {
                    break
                }
                runEnd += 1
            }
            
            // Extract the run text
            let startIdx = text.index(text.startIndex, offsetBy: i)
            let endIdx = text.index(text.startIndex, offsetBy: runEnd)
            var runText = String(text[startIdx..<endIdx])
            
            // Apply markdown formatting
            if isBold && isItalic {
                runText = "***\(runText)***"
            } else if isBold {
                runText = "**\(runText)**"
            } else if isItalic {
                runText = "*\(runText)*"
            }
            
            if isUnderline {
                runText = "<u>\(runText)</u>"
            }
            
            if isStrikethrough {
                runText = "~~\(runText)~~"
            }
            
            result += runText
            i = runEnd
        }
        
        return result
    }
}

// MARK: - Preview

@available(iOS 17.0, *)
#Preview {
    struct PreviewWrapper: View {
        @State var text = "This is some text. Try pressing backspace!"
        @FocusState var isFocused: Bool
        
        var body: some View {
            ZStack {
                Color(red: 0.05, green: 0.06, blue: 0.08)
                    .ignoresSafeArea()
                
                VStack {
                    MarkdownTextEditor(
                        text: $text,
                        placeholder: "Start writing...",
                        isFocused: $isFocused
                    )
                    
                    Divider()
                        .background(Color.white.opacity(0.2))
                    
                    Text("Markdown Output:")
                        .foregroundColor(.gray)
                        .font(.caption)
                    
                    ScrollView {
                        Text(text)
                            .font(.system(.caption, design: .monospaced))
                            .foregroundColor(.green)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .frame(height: 100)
                    .padding()
                    .background(Color.black.opacity(0.3))
                }
                .padding()
            }
        }
    }
    
    return PreviewWrapper()
}
