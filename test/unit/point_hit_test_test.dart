import 'dart:ui';

import 'package:flutter_test/flutter_test.dart';
import 'package:physics_tracker/features/tracking/presentation/utils/point_hit_test.dart';
import 'package:physics_tracker/shared/models/track_point.dart';

void main() {
  group('findNearestTrackPoint', () {
    const Size frameSize = Size(200, 100);
    final TrackPoint nearPoint = TrackPoint(
      frameIndex: 4,
      pixelX: 0,
      pixelY: 0,
      normalizedX: 0.25,
      normalizedY: 0.5,
    );
    final TrackPoint farPoint = TrackPoint(
      frameIndex: 9,
      pixelX: 0,
      pixelY: 0,
      normalizedX: 0.8,
      normalizedY: 0.5,
    );

    test('returns nearest point within scaled hit radius', () {
      final TrackPoint? hit = findNearestTrackPoint(
        points: [nearPoint, farPoint],
        sceneOffset: const Offset(53, 51),
        frameSize: frameSize,
        viewportScale: 1,
      );

      expect(hit?.frameIndex, nearPoint.frameIndex);
    });

    test('shrinks scene hit radius as zoom increases', () {
      final TrackPoint? hit = findNearestTrackPoint(
        points: [nearPoint],
        sceneOffset: const Offset(60, 50),
        frameSize: frameSize,
        viewportScale: 4,
      );

      expect(hit, isNull);
    });

    test('returns null when touch misses all points', () {
      final TrackPoint? hit = findNearestTrackPoint(
        points: [nearPoint, farPoint],
        sceneOffset: const Offset(10, 10),
        frameSize: frameSize,
        viewportScale: 1,
      );

      expect(hit, isNull);
    });
  });

  test('trackPointSceneOffset resolves normalized coordinates', () {
    final TrackPoint point = TrackPoint(
      frameIndex: 2,
      pixelX: 10,
      pixelY: 12,
      normalizedX: 0.4,
      normalizedY: 0.25,
    );

    expect(trackPointSceneOffset(point, const Size(300, 200)),
        const Offset(120, 50));
  });
}
