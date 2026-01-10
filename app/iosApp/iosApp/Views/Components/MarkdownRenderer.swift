import SwiftUI

// MARK: - Markdown Renderer

/// A view that renders markdown text with proper styling
/// Supports: **bold**, *italic*, <u>underline</u>, ~~strikethrough~~
struct MarkdownRenderer: View {
    let markdown: String
    let lineLimit: Int?
    let baseColor: Color
    let baseFontSize: CGFloat
    
    init(
        _ markdown: String,
        lineLimit: Int? = nil,
        baseColor: Color = .white,
        baseFontSize: CGFloat = 15
    ) {
        self.markdown = markdown
        self.lineLimit = lineLimit
        self.baseColor = baseColor
        self.baseFontSize = baseFontSize
    }
    
    var body: some View {
        if let lineLimit = lineLimit {
            Text(attributedString)
                .lineLimit(lineLimit)
                .multilineTextAlignment(.leading)
        } else {
            Text(attributedString)
                .multilineTextAlignment(.leading)
        }
    }
    
    private var attributedString: AttributedString {
        var result = AttributedString()
        let lines = markdown.components(separatedBy: "\n")
        
        for (index, line) in lines.enumerated() {
            let styledLine = styleLine(line)
            result.append(styledLine)
            
            if index < lines.count - 1 {
                result.append(AttributedString("\n"))
            }
        }
        
        return result
    }
    
    private func styleLine(_ line: String) -> AttributedString {
        var processedLine = line
        var fontSize: CGFloat = baseFontSize
        var fontWeight: Font.Weight = .regular
        var textColor: Color = baseColor
        
        // Check for block-level formatting
        if line.hasPrefix("# ") {
            processedLine = String(line.dropFirst(2))
            fontSize = baseFontSize * 1.6
            fontWeight = .bold
        } else if line.hasPrefix("## ") {
            processedLine = String(line.dropFirst(3))
            fontSize = baseFontSize * 1.3
            fontWeight = .semibold
            textColor = baseColor.opacity(0.85)
        }
        
        // Apply inline formatting
        return applyInlineFormatting(
            to: processedLine,
            fontSize: fontSize,
            fontWeight: fontWeight,
            textColor: textColor
        )
    }
    
    private func applyInlineFormatting(
        to text: String,
        fontSize: CGFloat,
        fontWeight: Font.Weight,
        textColor: Color
    ) -> AttributedString {
        var result = AttributedString()
        var currentText = text
        
        // Process strikethrough first (~~text~~)
        while let strikeRange = currentText.range(of: "~~") {
            // Text before strike marker
            let before = String(currentText[..<strikeRange.lowerBound])
            if !before.isEmpty {
                let beforeAttr = applyBoldFormatting(
                    to: before,
                    fontSize: fontSize,
                    fontWeight: fontWeight,
                    textColor: textColor,
                    isStrikethrough: false
                )
                result.append(beforeAttr)
            }
            
            // Find closing strike marker
            let afterFirstMarker = currentText[strikeRange.upperBound...]
            if let closingRange = afterFirstMarker.range(of: "~~") {
                let strikeContent = String(afterFirstMarker[..<closingRange.lowerBound])
                
                // Apply strikethrough to content
                let strikeAttr = applyBoldFormatting(
                    to: strikeContent,
                    fontSize: fontSize,
                    fontWeight: fontWeight,
                    textColor: textColor.opacity(0.5),
                    isStrikethrough: true
                )
                result.append(strikeAttr)
                
                currentText = String(afterFirstMarker[closingRange.upperBound...])
            } else {
                // No closing marker, treat as plain text
                var plainAttr = AttributedString("~~")
                plainAttr.font = .system(size: fontSize, weight: fontWeight)
                plainAttr.foregroundColor = textColor
                result.append(plainAttr)
                currentText = String(afterFirstMarker)
            }
        }
        
        // Remaining text
        if !currentText.isEmpty {
            let remaining = applyBoldFormatting(
                to: currentText,
                fontSize: fontSize,
                fontWeight: fontWeight,
                textColor: textColor,
                isStrikethrough: false
            )
            result.append(remaining)
        }
        
        return result
    }
    
    private func applyBoldFormatting(
        to text: String,
        fontSize: CGFloat,
        fontWeight: Font.Weight,
        textColor: Color,
        isStrikethrough: Bool
    ) -> AttributedString {
        var result = AttributedString()
        var currentText = text
        
        // Process bold (** **)
        while let boldRange = currentText.range(of: "**") {
            // Text before bold marker
            let before = String(currentText[..<boldRange.lowerBound])
            if !before.isEmpty {
                let beforeAttr = applyItalicFormatting(
                    to: before,
                    fontSize: fontSize,
                    fontWeight: fontWeight,
                    textColor: textColor,
                    isStrikethrough: isStrikethrough
                )
                result.append(beforeAttr)
            }
            
            // Find closing bold marker
            let afterFirstMarker = currentText[boldRange.upperBound...]
            if let closingRange = afterFirstMarker.range(of: "**") {
                let boldContent = String(afterFirstMarker[..<closingRange.lowerBound])
                
                // Apply bold to content (may contain italic)
                let boldAttr = applyItalicFormatting(
                    to: boldContent,
                    fontSize: fontSize,
                    fontWeight: .bold,
                    textColor: textColor,
                    isStrikethrough: isStrikethrough
                )
                result.append(boldAttr)
                
                currentText = String(afterFirstMarker[closingRange.upperBound...])
            } else {
                // No closing marker, treat as plain text
                var plainAttr = AttributedString("**")
                plainAttr.font = .system(size: fontSize, weight: fontWeight)
                plainAttr.foregroundColor = textColor
                if isStrikethrough {
                    plainAttr.strikethroughStyle = .single
                    plainAttr.strikethroughColor = .gray
                }
                result.append(plainAttr)
                currentText = String(afterFirstMarker)
            }
        }
        
        // Remaining text
        if !currentText.isEmpty {
            let remaining = applyItalicFormatting(
                to: currentText,
                fontSize: fontSize,
                fontWeight: fontWeight,
                textColor: textColor,
                isStrikethrough: isStrikethrough
            )
            result.append(remaining)
        }
        
        return result
    }
    
    private func applyItalicFormatting(
        to text: String,
        fontSize: CGFloat,
        fontWeight: Font.Weight,
        textColor: Color,
        isStrikethrough: Bool
    ) -> AttributedString {
        var result = AttributedString()
        var currentText = text
        
        // Process italic (* *)
        while let italicRange = currentText.range(of: "*") {
            // Text before italic marker
            let before = String(currentText[..<italicRange.lowerBound])
            if !before.isEmpty {
                var beforeAttr = AttributedString(before)
                beforeAttr.font = .system(size: fontSize, weight: fontWeight)
                beforeAttr.foregroundColor = textColor
                if isStrikethrough {
                    beforeAttr.strikethroughStyle = .single
                    beforeAttr.strikethroughColor = .gray
                }
                result.append(beforeAttr)
            }
            
            // Find closing italic marker
            let afterFirstMarker = currentText[italicRange.upperBound...]
            if let closingRange = afterFirstMarker.range(of: "*") {
                let italicContent = String(afterFirstMarker[..<closingRange.lowerBound])
                
                var italicAttr = AttributedString(italicContent)
                // Combine with existing weight
                if fontWeight == .bold {
                    italicAttr.font = .system(size: fontSize, weight: .bold).italic()
                } else if fontWeight == .semibold {
                    italicAttr.font = .system(size: fontSize, weight: .semibold).italic()
                } else {
                    italicAttr.font = .system(size: fontSize).italic()
                }
                italicAttr.foregroundColor = textColor
                if isStrikethrough {
                    italicAttr.strikethroughStyle = .single
                    italicAttr.strikethroughColor = .gray
                }
                result.append(italicAttr)
                
                currentText = String(afterFirstMarker[closingRange.upperBound...])
            } else {
                // No closing marker, treat as plain text
                var plainAttr = AttributedString("*")
                plainAttr.font = .system(size: fontSize, weight: fontWeight)
                plainAttr.foregroundColor = textColor
                if isStrikethrough {
                    plainAttr.strikethroughStyle = .single
                    plainAttr.strikethroughColor = .gray
                }
                result.append(plainAttr)
                currentText = String(afterFirstMarker)
            }
        }
        
        // Remaining text
        if !currentText.isEmpty {
            var remainingAttr = AttributedString(currentText)
            remainingAttr.font = .system(size: fontSize, weight: fontWeight)
            remainingAttr.foregroundColor = textColor
            if isStrikethrough {
                remainingAttr.strikethroughStyle = .single
                remainingAttr.strikethroughColor = .gray
            }
            result.append(remainingAttr)
        }
        
        return result
    }
}

// MARK: - Markdown Preview Helper

/// Strips markdown syntax for preview text
struct MarkdownPreviewHelper {
    /// Strips markdown formatting from text for preview display
    static func stripMarkdown(_ text: String) -> String {
        var result = text
        
        // Remove headings markers
        result = result.replacingOccurrences(of: "## ", with: "")
        result = result.replacingOccurrences(of: "# ", with: "")
        
        // Remove bold markers
        result = result.replacingOccurrences(of: "**", with: "")
        
        // Remove italic markers (but be careful not to remove ** markers)
        // Simple approach: replace single * that aren't part of **
        let pattern = "(?<!\\*)\\*(?!\\*)"
        if let regex = try? NSRegularExpression(pattern: pattern, options: []) {
            let range = NSRange(result.startIndex..., in: result)
            result = regex.stringByReplacingMatches(in: result, options: [], range: range, withTemplate: "")
        }
        
        // Remove underline tags
        result = result.replacingOccurrences(of: "<u>", with: "")
        result = result.replacingOccurrences(of: "</u>", with: "")
        
        // Remove strikethrough markers
        result = result.replacingOccurrences(of: "~~", with: "")
        
        // Collapse multiple spaces and newlines
        result = result.replacingOccurrences(of: "\n\n+", with: " ", options: .regularExpression)
        result = result.replacingOccurrences(of: "\n", with: " ")
        result = result.replacingOccurrences(of: "  +", with: " ", options: .regularExpression)
        
        return result.trimmingCharacters(in: .whitespaces)
    }
}

// MARK: - Preview

#Preview("Full Markdown") {
    ZStack {
        Color(red: 0.05, green: 0.06, blue: 0.08)
            .ignoresSafeArea()
        
        VStack(alignment: .leading, spacing: 16) {
            MarkdownRenderer("""
            # Welcome to My Journal
            ## Today's Reflections
            
            I've been thinking about **important things** and *new ideas* lately.
            
            This is regular text with **bold** and *italic* mixed in.
            
            Some ***bold italic*** text as well!
            """)
        }
        .padding()
    }
}

#Preview("Preview Text") {
    ZStack {
        Color(red: 0.05, green: 0.06, blue: 0.08)
            .ignoresSafeArea()
        
        VStack(alignment: .leading, spacing: 16) {
            Text("Original:")
                .foregroundColor(.gray)
            
            Text("# Title\n## Subtitle\n**Bold** and *italic* text")
                .foregroundColor(.white.opacity(0.5))
            
            Text("Stripped:")
                .foregroundColor(.gray)
                .padding(.top)
            
            Text(MarkdownPreviewHelper.stripMarkdown("# Title\n## Subtitle\n**Bold** and *italic* text"))
                .foregroundColor(.white)
        }
        .padding()
    }
}
