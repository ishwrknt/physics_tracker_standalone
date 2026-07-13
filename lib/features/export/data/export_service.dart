// Responsibility: Export analysis outputs as CSV and annotated PNG.

import 'dart:io';
import 'dart:math';
import 'dart:typed_data';

import 'package:image/image.dart' as img;

import '../../../shared/models/calibration_data.dart';
import '../../../shared/models/track_point.dart';
import '../../../shared/utils/app_storage.dart';
import '../../../shared/utils/calibration_math.dart';

class ExportService {
  Future<String> exportCsv({
    required String projectName,
    required List<TrackPoint> points,
    required double fps,
    required CalibrationData? calibration,
    required double? massKg,
  }) async {
    final List<TrackPoint> ordered = List<TrackPoint>.from(points)
      ..sort((a, b) => a.frameIndex.compareTo(b.frameIndex));

    final StringBuffer out = StringBuffer();
    out.writeln('frame,t,x,y,vx,vy,ax,ay,speed,p,ke,fx,fy');

    double? prevTime;
    double? prevX;
    double? prevY;
    double? prevVx;
    double? prevVy;

    for (final TrackPoint p in ordered) {
      final double t = p.frameIndex / fps;
      final double x;
      final double y;
      if (calibration == null) {
        x = p.pixelX;
        y = p.pixelY;
      } else {
        final world = pixelToWorld(
          calibration: calibration,
          pixelX: p.pixelX,
          pixelY: p.pixelY,
        );
        x = world.x;
        y = world.y;
      }

      double? vx;
      double? vy;
      double? ax;
      double? ay;
      double? speed;
      double? momentum;
      double? kineticEnergy;
      double? forceX;
      double? forceY;

      if (prevTime != null && prevX != null && prevY != null) {
        final double dt = t - prevTime;
        if (dt > 0) {
          vx = (x - prevX) / dt;
          vy = (y - prevY) / dt;
          speed = sqrt((vx * vx) + (vy * vy));
          if (prevVx != null && prevVy != null) {
            ax = (vx - prevVx) / dt;
            ay = (vy - prevVy) / dt;
          }
          if (massKg != null) {
            momentum = massKg * speed;
            kineticEnergy = 0.5 * massKg * speed * speed;
            if (ax != null && ay != null) {
              forceX = massKg * ax;
              forceY = massKg * ay;
            }
          }
        }
      }

      out.writeln([
        p.frameIndex,
        _f(t),
        _f(x),
        _f(y),
        _f(vx),
        _f(vy),
        _f(ax),
        _f(ay),
        _f(speed),
        _f(momentum),
        _f(kineticEnergy),
        _f(forceX),
        _f(forceY),
      ].join(','));

      prevTime = t;
      prevX = x;
      prevY = y;
      prevVx = vx;
      prevVy = vy;
    }

    final Directory dir = await _ensureExportDirectory(projectName);
    final String timestamp = DateTime.now()
        .toIso8601String()
        .replaceAll(':', '-')
        .replaceAll('.', '-');
    final File file = File('${dir.path}/analysis_$timestamp.csv');
    await file.writeAsString(out.toString(), flush: true);
    return file.path;
  }

  Future<String> exportAnnotatedPng({
    required String projectName,
    required Uint8List frameBytes,
    required List<TrackPoint> points,
    required CalibrationData? calibration,
  }) async {
    final img.Image? decoded = img.decodeImage(frameBytes);
    if (decoded == null) {
      throw StateError('Failed to decode frame for PNG export.');
    }
    final img.Image frame = img.Image.from(decoded);
    final List<TrackPoint> ordered = List<TrackPoint>.from(points)
      ..sort((a, b) => a.frameIndex.compareTo(b.frameIndex));

    final img.Color trackColor = img.ColorRgb8(239, 108, 0);
    for (int i = 1; i < ordered.length; i++) {
      final TrackPoint a = ordered[i - 1];
      final TrackPoint b = ordered[i];
      img.drawLine(
        frame,
        x1: a.pixelX.round(),
        y1: a.pixelY.round(),
        x2: b.pixelX.round(),
        y2: b.pixelY.round(),
        color: trackColor,
      );
    }
    for (final TrackPoint p in ordered) {
      img.drawCircle(
        frame,
        x: p.pixelX.round(),
        y: p.pixelY.round(),
        radius: 4,
        color: trackColor,
      );
    }

    if (calibration != null) {
      final img.Color calColor = img.ColorRgb8(255, 179, 0);
      final int ox = calibration.originPixelX.round();
      final int oy = calibration.originPixelY.round();
      final double theta = calibration.angleDegrees * pi / 180.0;
      final int ax = (calibration.originPixelX + (60 * cos(theta))).round();
      final int ay = (calibration.originPixelY + (60 * sin(theta))).round();

      img.drawLine(
        frame,
        x1: ox - 10,
        y1: oy,
        x2: ox + 10,
        y2: oy,
        color: calColor,
      );
      img.drawLine(
        frame,
        x1: ox,
        y1: oy - 10,
        x2: ox,
        y2: oy + 10,
        color: calColor,
      );
      img.drawLine(frame, x1: ox, y1: oy, x2: ax, y2: ay, color: calColor);
    }

    final Directory dir = await _ensureExportDirectory(projectName);
    final String timestamp = DateTime.now()
        .toIso8601String()
        .replaceAll(':', '-')
        .replaceAll('.', '-');
    final File file = File('${dir.path}/annotated_$timestamp.png');
    await file.writeAsBytes(img.encodePng(frame), flush: true);
    return file.path;
  }

  Future<Directory> _ensureExportDirectory(String projectName) async {
    final Directory docs = await getWritableAppDirectory();
    final String safe = projectName.replaceAll(RegExp(r'[^a-zA-Z0-9_-]'), '_');
    final Directory dir = Directory('${docs.path}/exports/$safe');
    await dir.create(recursive: true);
    return dir;
  }
}

String _f(num? value) {
  if (value == null) {
    return '';
  }
  return value.toStringAsFixed(6);
}
