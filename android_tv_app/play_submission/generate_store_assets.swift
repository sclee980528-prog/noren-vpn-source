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
    NSGraphicsContext.saveGraphicsState()
    defer { NSGraphicsContext.restoreGraphicsState() }
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
    let url = URL(fileURLWithPath: path)
    try FileManager.default.createDirectory(
        at: url.deletingLastPathComponent(),
        withIntermediateDirectories: true
    )
    try png.write(to: url)
}

func drawPowerMark(center: CGPoint, radius: CGFloat, lineWidth: CGFloat) {
    color(0x0F8B8D).setFill()
    NSBezierPath(ovalIn: CGRect(x: center.x - radius, y: center.y - radius, width: radius * 2, height: radius * 2)).fill()
    color(0x64D8CB).setFill()
    NSBezierPath(ovalIn: CGRect(x: center.x - radius * 0.86, y: center.y + radius * 0.15, width: radius * 1.72, height: radius * 0.54)).fill()

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

func drawRoute(from start: CGPoint, to end: CGPoint, lineWidth: CGFloat) {
    let route = NSBezierPath()
    route.lineWidth = lineWidth
    route.lineCapStyle = .round
    color(0xF4B942).setStroke()
    route.move(to: start)
    route.line(to: CGPoint(x: (start.x + end.x) * 0.52, y: start.y))
    route.line(to: CGPoint(x: (start.x + end.x) * 0.52, y: end.y))
    route.line(to: end)
    route.stroke()

    color(0xF4B942).setFill()
    let dot = lineWidth * 2.5
    NSBezierPath(ovalIn: CGRect(x: end.x - dot / 2, y: end.y - dot / 2, width: dot, height: dot)).fill()
}

func drawFeatureGraphic(locale: String, headline: String, detail: String) throws {
    let output = "play_submission/store_assets/\(locale)/feature_graphic_1024x500.png"
    let image = NSImage(size: NSSize(width: 1024, height: 500))
    image.lockFocus()

    color(0x111315).setFill()
    CGRect(x: 0, y: 0, width: 1024, height: 500).fill()
    color(0x202526).setFill()
    CGRect(x: 0, y: 0, width: 326, height: 500).fill()
    color(0x0F8B8D).setFill()
    CGRect(x: 0, y: 0, width: 12, height: 500).fill()

    drawPowerMark(center: CGPoint(x: 166, y: 250), radius: 106, lineWidth: 21)
    drawRoute(from: CGPoint(x: 274, y: 250), to: CGPoint(x: 366, y: 214), lineWidth: 7)

    drawText("Noren VPN", rect: CGRect(x: 384, y: 270, width: 570, height: 72), size: 56, weight: .bold, color: color(0xF5F7F4))
    drawText(headline, rect: CGRect(x: 388, y: 218, width: 560, height: 42), size: 27, weight: .semibold, color: color(0x64D8CB))
    drawText(detail, rect: CGRect(x: 388, y: 174, width: 560, height: 38), size: 22, weight: .medium, color: color(0xC3C9C5))

    roundedRect(CGRect(x: 388, y: 128, width: 172, height: 12), radius: 6, fill: color(0x0F8B8D))
    roundedRect(CGRect(x: 574, y: 128, width: 92, height: 12), radius: 6, fill: color(0xF4B942))
    roundedRect(CGRect(x: 680, y: 128, width: 214, height: 12), radius: 6, fill: color(0x3A4140))

    image.unlockFocus()
    try savePNG(image, to: output)
}

func drawTvBanner(locale: String, headline: String) throws {
    let output = "play_submission/store_assets/\(locale)/tv_banner_1280x720.png"
    let image = NSImage(size: NSSize(width: 1280, height: 720))
    image.lockFocus()

    color(0x111315).setFill()
    CGRect(x: 0, y: 0, width: 1280, height: 720).fill()
    color(0x202526).setFill()
    CGRect(x: 0, y: 0, width: 420, height: 720).fill()
    color(0x0F8B8D).setFill()
    CGRect(x: 0, y: 0, width: 16, height: 720).fill()
    drawPowerMark(center: CGPoint(x: 216, y: 360), radius: 132, lineWidth: 26)
    drawRoute(from: CGPoint(x: 350, y: 360), to: CGPoint(x: 500, y: 300), lineWidth: 9)

    drawText("Noren VPN", rect: CGRect(x: 532, y: 374, width: 650, height: 84), size: 66, weight: .bold, color: color(0xF5F7F4))
    drawText(headline, rect: CGRect(x: 538, y: 316, width: 650, height: 48), size: 31, weight: .semibold, color: color(0x64D8CB))
    drawText("Android TV · Google TV · Phone · Tablet", rect: CGRect(x: 538, y: 262, width: 650, height: 42), size: 26, weight: .medium, color: color(0xC3C9C5))

    roundedRect(CGRect(x: 538, y: 206, width: 228, height: 14), radius: 7, fill: color(0x0F8B8D))
    roundedRect(CGRect(x: 786, y: 206, width: 110, height: 14), radius: 7, fill: color(0xF4B942))
    roundedRect(CGRect(x: 916, y: 206, width: 210, height: 14), radius: 7, fill: color(0x3A4140))

    image.unlockFocus()
    try savePNG(image, to: output)
}

func drawAppBanner() throws {
    let image = NSImage(size: NSSize(width: 320, height: 180))
    image.lockFocus()

    color(0x111315).setFill()
    CGRect(x: 0, y: 0, width: 320, height: 180).fill()
    color(0x0F8B8D).setFill()
    CGRect(x: 0, y: 0, width: 6, height: 180).fill()
    drawPowerMark(center: CGPoint(x: 74, y: 90), radius: 48, lineWidth: 10)
    drawRoute(from: CGPoint(x: 123, y: 90), to: CGPoint(x: 146, y: 74), lineWidth: 3)
    drawText("Noren", rect: CGRect(x: 158, y: 94, width: 148, height: 32), size: 25, weight: .bold, color: color(0xF5F7F4))
    drawText("VPN", rect: CGRect(x: 158, y: 66, width: 88, height: 28), size: 22, weight: .semibold, color: color(0x64D8CB))
    roundedRect(CGRect(x: 158, y: 47, width: 62, height: 5), radius: 2.5, fill: color(0x0F8B8D))
    roundedRect(CGRect(x: 228, y: 47, width: 30, height: 5), radius: 2.5, fill: color(0xF4B942))

    image.unlockFocus()
    let outputs = [
        "app/src/main/res/mipmap-xhdpi/app_banner.png",
        "app/src/tv/res/mipmap-xhdpi/app_banner_tv.png"
    ]
    for output in outputs {
        try savePNG(image, to: output)
    }
}

func drawAppIcons() throws {
    let outputs: [(String, Int)] = [
        ("app/src/main/res/mipmap-mdpi/app_icon.png", 80),
        ("app/src/main/res/mipmap-hdpi/app_icon.png", 120),
        ("app/src/main/res/mipmap-xhdpi/app_icon.png", 160),
        ("app/src/main/res/mipmap-xxhdpi/app_icon.png", 240),
        ("app/src/main/res/mipmap-xxxhdpi/app_icon.png", 320)
    ]

    for (output, pixels) in outputs {
        let size = CGFloat(pixels)
        let image = NSImage(size: NSSize(width: size, height: size))
        image.lockFocus()

        color(0x0B1320).setFill()
        CGRect(x: 0, y: 0, width: size, height: size).fill()
        let radius = size / 3
        drawPowerMark(
            center: CGPoint(x: size / 2, y: size / 2),
            radius: radius,
            lineWidth: radius * 0.21
        )

        image.unlockFocus()
        try savePNG(image, to: output)
    }
}

try drawFeatureGraphic(locale: "en-US", headline: "One tap. Clear controls.", detail: "Public volunteer VPN connections")
try drawFeatureGraphic(locale: "ja-JP", headline: "ワンタップ。迷わない操作。", detail: "公開ボランティアVPNへ接続")
try drawTvBanner(locale: "en-US", headline: "One-tap public VPN connection")
try drawTvBanner(locale: "ja-JP", headline: "公開VPNへワンタップ接続")
try drawAppBanner()
try drawAppIcons()
