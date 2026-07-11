import AppKit
import AVFoundation

struct Slide {
    let path: String
    let seconds: Int
}

let slides = [
    Slide(path: "/tmp/oneclick_v33_frame_6.png", seconds: 5),
    Slide(path: "/tmp/oneclick_tv_disclosure_v33.png", seconds: 12),
    Slide(path: "/tmp/oneclick_tv_after_continue_probe.png", seconds: 8),
]

let outputPath = "/Users/sangchan/Documents/New project/android_tv_app/play_submission/oneclick_vpn_review_video_v33.mp4"
let outputURL = URL(fileURLWithPath: outputPath)
try? FileManager.default.removeItem(at: outputURL)

let width = 1280
let height = 720
let fps: Int32 = 30

let writer = try AVAssetWriter(outputURL: outputURL, fileType: .mp4)
let input = AVAssetWriterInput(mediaType: .video, outputSettings: [
    AVVideoCodecKey: AVVideoCodecType.h264,
    AVVideoWidthKey: width,
    AVVideoHeightKey: height,
    AVVideoCompressionPropertiesKey: [
        AVVideoAverageBitRateKey: 2_000_000
    ]
])
input.expectsMediaDataInRealTime = false

let adaptor = AVAssetWriterInputPixelBufferAdaptor(assetWriterInput: input, sourcePixelBufferAttributes: [
    kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA,
    kCVPixelBufferWidthKey as String: width,
    kCVPixelBufferHeightKey as String: height,
])

guard writer.canAdd(input) else {
    fatalError("Cannot add video input")
}
writer.add(input)

func pixelBuffer(for imagePath: String) throws -> CVPixelBuffer {
    guard let image = NSImage(contentsOfFile: imagePath),
          let cgImage = image.cgImage(forProposedRect: nil, context: nil, hints: nil) else {
        fatalError("Cannot load image: \(imagePath)")
    }

    var maybeBuffer: CVPixelBuffer?
    let attrs = [
        kCVPixelBufferCGImageCompatibilityKey as String: true,
        kCVPixelBufferCGBitmapContextCompatibilityKey as String: true,
    ] as CFDictionary
    let status = CVPixelBufferCreate(kCFAllocatorDefault, width, height, kCVPixelFormatType_32BGRA, attrs, &maybeBuffer)
    guard status == kCVReturnSuccess, let buffer = maybeBuffer else {
        fatalError("Cannot create pixel buffer")
    }

    CVPixelBufferLockBaseAddress(buffer, [])
    defer { CVPixelBufferUnlockBaseAddress(buffer, []) }

    guard let base = CVPixelBufferGetBaseAddress(buffer),
          let context = CGContext(
            data: base,
            width: width,
            height: height,
            bitsPerComponent: 8,
            bytesPerRow: CVPixelBufferGetBytesPerRow(buffer),
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGBitmapInfo.byteOrder32Little.rawValue | CGImageAlphaInfo.premultipliedFirst.rawValue
          ) else {
        fatalError("Cannot create bitmap context")
    }

    context.setFillColor(NSColor.black.cgColor)
    context.fill(CGRect(x: 0, y: 0, width: width, height: height))
    context.draw(cgImage, in: CGRect(x: 0, y: 0, width: width, height: height))
    return buffer
}

writer.startWriting()
writer.startSession(atSourceTime: .zero)

var frame: Int64 = 0
for slide in slides {
    let buffer = try pixelBuffer(for: slide.path)
    for _ in 0..<(slide.seconds * Int(fps)) {
        while !input.isReadyForMoreMediaData {
            Thread.sleep(forTimeInterval: 0.01)
        }
        let time = CMTime(value: frame, timescale: fps)
        adaptor.append(buffer, withPresentationTime: time)
        frame += 1
    }
}

input.markAsFinished()
let semaphore = DispatchSemaphore(value: 0)
writer.finishWriting {
    semaphore.signal()
}
semaphore.wait()

if writer.status != .completed {
    fatalError("Video writer failed: \(writer.error?.localizedDescription ?? "unknown error")")
}

print(outputPath)
