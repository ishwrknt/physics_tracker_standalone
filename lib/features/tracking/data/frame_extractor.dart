// Responsibility: Extract single video frames and metadata using FFmpeg.

import 'dart:io';
import 'dart:math' as math;
import 'dart:typed_data';

import 'package:ffmpeg_kit_flutter_new/ffmpeg_kit.dart';
import 'package:ffmpeg_kit_flutter_new/ffprobe_kit.dart';
import 'package:ffmpeg_kit_flutter_new/return_code.dart';
import 'package:path_provider/path_provider.dart';

import '../domain/video_metadata.dart';
import '../../../../shared/utils/constants.dart';

class FrameExtractor {
  Future<VideoMetadata> loadMetadata(String videoPath) async {
    final File videoFile = File(videoPath);
    if (!await videoFile.exists()) {
      throw StateError('The selected video file is missing.');
    }

    final session = await FFprobeKit.executeWithArguments(<String>[
      '-v',
      'error',
      '-select_streams',
      'v:0',
      '-show_entries',
      'stream=width,height,avg_frame_rate:format=duration:stream_tags=rotate',
      '-of',
      'default=noprint_wrappers=1',
      videoPath,
    ]);
    final returnCode = await session.getReturnCode();
    final String output = (await session.getOutput())?.trim() ?? '';
    if (!ReturnCode.isSuccess(returnCode) || output.isEmpty) {
      throw StateError(
        await _buildProbeError(
          session: session,
          fallback: 'Could not read video metadata.',
        ),
      );
    }

    final Map<String, String> values = _parseKeyValueOutput(output);
    final double durationSeconds =
        double.tryParse(values['duration'] ?? '') ?? 0.0;

    final double? parsedFps = _parseFps(values['avg_frame_rate']);
    final double fps = parsedFps != null && parsedFps > 0 ? parsedFps : 30.0;
    
    int width = _parseInt(values['width']) ?? 0;
    int height = _parseInt(values['height']) ?? 0;
    
    // Check multiple possible rotation keys (TAG:rotate, side_data, etc)
    int rotation = _parseInt(values['TAG:rotate']) ?? 0;
    if (rotation == 0) {
      // Sometimes it's in a different case or format
      rotation = _parseInt(values['rotate']) ?? 0;
    }

    // Handle rotation: swap dimensions if rotated 90 or 270 degrees
    if (rotation == 90 || rotation == 270 || rotation == -90 || rotation == -270) {
      final int temp = width;
      width = height;
      height = temp;
    }

    final int totalFrames =
        durationSeconds <= 0 ? 0 : math.max(1, (durationSeconds * fps).round());
    return VideoMetadata(
      fps: fps,
      duration:
          Duration(milliseconds: (durationSeconds * kMsPerSecond).round()),
      totalFrames: totalFrames,
      width: width <= 0 ? 16 : width,
      height: height <= 0 ? 9 : height,
      rotation: rotation,
    );
  }

  Future<Uint8List> extractFrame({
    required String videoPath,
    required int frameIndex,
    required double fps,
    int rotation = 0,
  }) async {
    final File videoFile = File(videoPath);
    if (!await videoFile.exists()) {
      throw StateError('The selected video file is missing.');
    }
    if (fps <= 0) {
      throw StateError('Video metadata is invalid for frame extraction.');
    }

    final Directory tempDir = await getTemporaryDirectory();
    final String timestamp = DateTime.now().microsecondsSinceEpoch.toString();
    final String outputPath = '${tempDir.path}/f_${frameIndex}_$timestamp.jpg';
    
    // Calculate a slightly early seek time for better accuracy on compressed streams
    final double targetTime = frameIndex / fps;
    final double preSeekTime = math.max(0.0, targetTime - 0.5);
    final double postSeekOffset = targetTime - preSeekTime;

    // Use transpose filter to match visual rotation if needed
    String? transpose;
    if (rotation == 90 || rotation == -270) transpose = 'transpose=1';
    if (rotation == 180 || rotation == -180) transpose = 'transpose=2,transpose=2';
    if (rotation == 270 || rotation == -90) transpose = 'transpose=2';

    // Dual-seek strategy for maximum precision:
    // 1. Fast seek to before the frame (-ss before -i)
    // 2. Slow/Accurate seek to the exact frame (-ss after -i)
    final List<String> args = <String>[
      '-y',
      '-loglevel', 'error',
      '-ss', preSeekTime.toStringAsFixed(4),
      '-i', videoPath,
      '-ss', postSeekOffset.toStringAsFixed(4),
      if (transpose != null) ...['-vf', transpose],
      '-frames:v', '1',
      '-f', 'image2',
      '-vcodec', 'mjpeg',
      '-q:v', '2',
      outputPath,
    ];

    int attempts = 0;
    const int maxAttempts = 3;
    dynamic lastError;

    while (attempts < maxAttempts) {
      final session = await FFmpegKit.executeWithArguments(args);
      final returnCode = await session.getReturnCode();

      if (ReturnCode.isSuccess(returnCode)) {
        final File file = File(outputPath);
        if (await file.exists()) {
          final Uint8List bytes = await file.readAsBytes();
          try {
            await file.delete();
          } catch (_) {}
          return bytes;
        }
      }
      
      lastError = await _buildFfmpegError(
        session: session,
        fallback: 'Failed to extract frame $frameIndex.',
      );
      attempts++;
      if (attempts < maxAttempts) {
        // Simple linear backoff
        await Future.delayed(Duration(milliseconds: 50 * attempts));
      }
    }

    throw StateError(lastError.toString());
  }

  double? _parseFps(String? raw) {
    if (raw == null || raw.isEmpty) {
      return null;
    }
    if (raw.contains('/')) {
      final parts = raw.split('/');
      if (parts.length == 2) {
        final double? numerator = double.tryParse(parts[0]);
        final double? denominator = double.tryParse(parts[1]);
        if (numerator != null && denominator != null && denominator != 0) {
          return numerator / denominator;
        }
      }
      return null;
    }
    return double.tryParse(raw);
  }

  int? _parseInt(dynamic value) {
    if (value == null) {
      return null;
    }
    return int.tryParse(value.toString());
  }

  Map<String, String> _parseKeyValueOutput(String output) {
    final Map<String, String> values = <String, String>{};
    for (final String line in output.split('\n')) {
      final int separator = line.indexOf('=');
      if (separator <= 0) {
        continue;
      }
      final String key = line.substring(0, separator).trim();
      final String value = line.substring(separator + 1).trim();
      if (key.isNotEmpty && value.isNotEmpty) {
        values[key] = value;
      }
    }
    return values;
  }

  Future<String> _buildProbeError({
    required dynamic session,
    required String fallback,
  }) async {
    final String? output = await session.getOutput();
    final String? failStackTrace = await session.getFailStackTrace();
    final String details = _cleanErrorDetails(output, failStackTrace);
    return details.isEmpty ? fallback : '$fallback $details';
  }

  Future<String> _buildFfmpegError({
    required dynamic session,
    required String fallback,
  }) async {
    final String? output = await session.getOutput();
    final String? failStackTrace = await session.getFailStackTrace();
    final String details = _cleanErrorDetails(output, failStackTrace);
    return details.isEmpty ? fallback : '$fallback $details';
  }

  String _cleanErrorDetails(String? output, String? failStackTrace) {
    final List<String> parts = <String>[
      if (output != null && output.trim().isNotEmpty) output.trim(),
      if (failStackTrace != null && failStackTrace.trim().isNotEmpty)
        failStackTrace.trim(),
    ];
    if (parts.isEmpty) {
      return '';
    }
    final String combined = parts.join(' ').replaceAll(RegExp(r'\s+'), ' ');
    return combined.length <= 240
        ? combined
        : '${combined.substring(0, 240)}...';
  }
}
