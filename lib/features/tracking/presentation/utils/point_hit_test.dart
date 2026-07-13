// Responsibility: Resolve rendered track points from scene-space touch input.

import 'dart:ui';

import '../../../../shared/models/track_point.dart';
import '../../../../shared/utils/constants.dart';

Offset trackPointSceneOffset(TrackPoint point, Size frameSize) {
  final double dx = point.normalizedX == null
      ? point.pixelX
      : point.normalizedX!.clamp(0.0, 1.0) * frameSize.width;
  final double dy = point.normalizedY == null
      ? point.pixelY
      : point.normalizedY!.clamp(0.0, 1.0) * frameSize.height;
  return Offset(dx, dy);
}

TrackPoint? findNearestTrackPoint({
  required Iterable<TrackPoint> points,
  required Offset sceneOffset,
  required Size frameSize,
  required double viewportScale,
  double viewportHitRadius = kPointHitRadius,
}) {
  if (frameSize.isEmpty) {
    return null;
  }

  final double safeScale = viewportScale <= 0 ? 1.0 : viewportScale;
  final double sceneHitRadius = viewportHitRadius / safeScale;

  TrackPoint? bestMatch;
  double bestDistance = double.infinity;

  for (final TrackPoint point in points) {
    final double distance =
        (trackPointSceneOffset(point, frameSize) - sceneOffset).distance;
    if (distance <= sceneHitRadius && distance < bestDistance) {
      bestMatch = point;
      bestDistance = distance;
    }
  }

  return bestMatch;
}
