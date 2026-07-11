import AppKit

func color(_ hex: UInt32, _ alpha: CGFloat = 1.0) -> NSColor {
    let r = CGFloat((hex >> 16) & 0xff) / 255.0
    let g = CGFloat((hex >> 8) & 0xff) / 255.0
    let b = CGFloat(hex & 0xff) / 255.0
    return NSColor(calibratedRed: r, green: g, blue: b, alpha: alpha)
}

func roundedRect(_ rect: CGRect, radius: CGFloat, fill: NSColor) {
    fill.setFill()
    NSBezierPath(roundedRect: rect, xRadius: radius, yRadius: radius).fill()
}

func drawText(_ text: String, rect: CGRect, size: CGFloat, weight: NSFont.Weight, color textColor: NSColor) {
    let paragraph = NSMutableParagraphStyle()
    paragraph.alignment = .left
    let attrs: [NSAttributedString.Key: Any] = [
        .font: NSFont.systemFont(ofSize: size, weight: weight),
        .foregroundColor: textColor,
        .paragraphStyle: paragraph
    ]
    text.draw(in: rect, withAttributes: attrs)
}

func savePNG(_ image: NSImage, to path: String) throws {
    guard
        let tiff = image.tiffRepresentation,
        let bitmap = NSBitmapImageRep(data: tiff),
        let png = bitmap.representation(using: .png, properties: [:])
    else {
        throw NSError(domain: "asset", code: 1)
    }
    try png.write(to: URL(fileURLWithPath: path))
}

func drawPowerMark(center: CGPoint, radius: CGFloat, lineWidth: CGFloat) {
    color(0x14B8A6).setFill()
    NSBezierPath(ovalIn: CGRect(x: center.x - radius, y: center.y - radius, width: radius * 2, height: radius * 2)).fill()
    color(0x99F6E4, 0.30).setFill()
    NSBezierPath(ovalIn: CGRect(x: center.x - radius * 0.87, y: center.y - radius * 0.02, width: radius * 1.74, height: radius * 1.08)).fill()

    let stem = NSBezierPath()
    stem.lineWidth = lineWidth
    stem.lineCapStyle = .round
    stem.lineJoinStyle = .round
    color(0xFFFFFF).setStroke()
    stem.move(to: CGPoint(x: center.x, y: center.y + radius * 0.58))
    stem.line(to: CGPoint(x: center.x, y: center.y - radius * 0.20))
    stem.stroke()

    let arc = NSBezierPath()
    arc.lineWidth = lineWidth
    arc.lineCapStyle = .round
    arc.appendArc(withCenter: CGPoint(x: center.x, y: center.y - radius * 0.28), radius: radius * 0.76, startAngle: 138, endAngle: 402)
    arc.stroke()
}

func drawFeatureGraphic() throws {
    let output = "/Users/sangchan/Desktop/oneclick_store_assets/feature_graphic_1024x500.png"
    let image = NSImage(size: NSSize(width: 1024, height: 500))
    image.lockFocus()

    color(0x0B1220).setFill()
    CGRect(x: 0, y: 0, width: 1024, height: 500).fill()

    roundedRect(CGRect(x: 54, y: 58, width: 916, height: 384), radius: 32, fill: color(0x111827))
    roundedRect(CGRect(x: 54, y: 58, width: 916, height: 140), radius: 32, fill: color(0x020617, 0.22))
    drawPowerMark(center: CGPoint(x: 206, y: 250), radius: 108, lineWidth: 22)

    drawText("oneclick free vpn", rect: CGRect(x: 360, y: 268, width: 540, height: 70), size: 54, weight: .bold, color: color(0xF8FAFC))
    drawText("Free VPN for Android TV, phone and tablet", rect: CGRect(x: 364, y: 222, width: 610, height: 40), size: 25, weight: .semibold, color: color(0x99F6E4))
    drawText("One-tap country connection", rect: CGRect(x: 364, y: 176, width: 420, height: 34), size: 24, weight: .medium, color: color(0xCBD5E1))

    roundedRect(CGRect(x: 364, y: 130, width: 236, height: 14), radius: 7, fill: color(0x14B8A6))
    roundedRect(CGRect(x: 620, y: 130, width: 164, height: 14), radius: 7, fill: color(0x334155))

    image.unlockFocus()
    try savePNG(image, to: output)
}

func drawTvBanner() throws {
    let output = "/Users/sangchan/Desktop/oneclick_store_assets/tv_banner_1280x720.png"
    let image = NSImage(size: NSSize(width: 1280, height: 720))
    image.lockFocus()

    color(0x0B1220).setFill()
    CGRect(x: 0, y: 0, width: 1280, height: 720).fill()
    roundedRect(CGRect(x: 84, y: 84, width: 1112, height: 552), radius: 42, fill: color(0x111827))
    roundedRect(CGRect(x: 84, y: 84, width: 1112, height: 178), radius: 42, fill: color(0x020617, 0.24))
    drawPowerMark(center: CGPoint(x: 278, y: 360), radius: 126, lineWidth: 26)

    drawText("oneclick free vpn", rect: CGRect(x: 472, y: 390, width: 640, height: 80), size: 62, weight: .bold, color: color(0xF8FAFC))
    drawText("Free VPN for TV, phone and tablet", rect: CGRect(x: 478, y: 334, width: 620, height: 44), size: 30, weight: .semibold, color: color(0x99F6E4))
    drawText("One-tap country connection", rect: CGRect(x: 478, y: 284, width: 470, height: 40), size: 29, weight: .medium, color: color(0xCBD5E1))

    roundedRect(CGRect(x: 478, y: 222, width: 276, height: 16), radius: 8, fill: color(0x14B8A6))
    roundedRect(CGRect(x: 784, y: 222, width: 188, height: 16), radius: 8, fill: color(0x334155))

    image.unlockFocus()
    try savePNG(image, to: output)
}

try drawFeatureGraphic()
try drawTvBanner()
