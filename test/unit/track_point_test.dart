// Responsibility: Validate TrackPoint Hive serialization and JSON parsing.

import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:hive/hive.dart';

import 'package:physics_tracker/shared/models/color_adapter.dart';
import 'package:physics_tracker/shared/models/calibration_data.dart';
import 'package:physics_tracker/shared/models/hive_type_ids.dart';
import 'package:physics_tracker/shared/models/project.dart';
import 'package:physics_tracker/shared/models/project_summary.dart';
import 'package:physics_tracker/shared/models/track.dart';
import 'package:physics_tracker/shared/models/track_point.dart';

void main() {
  setUp(() async {
    final Directory dir = await Directory.systemTemp.createTemp();
    Hive.init(dir.path);
    if (!Hive.isAdapterRegistered(kColorTypeId)) {
      Hive.registerAdapter(ColorAdapter());
    }
    if (!Hive.isAdapterRegistered(kCalibrationDataTypeId)) {
      Hive.registerAdapter(CalibrationDataAdapter());
    }
    if (!Hive.isAdapterRegistered(kTrackPointTypeId)) {
      Hive.registerAdapter(TrackPointAdapter());
    }
    if (!Hive.isAdapterRegistered(kTrackTypeId)) {
      Hive.registerAdapter(TrackAdapter());
    }
    if (!Hive.isAdapterRegistered(kProjectTypeId)) {
      Hive.registerAdapter(ProjectAdapter());
    }
    if (!Hive.isAdapterRegistered(kProjectSummaryTypeId)) {
      Hive.registerAdapter(ProjectSummaryAdapter());
    }
  });

  tearDown(() async {
    await Hive.close();
  });

  test('stores and restores TrackPoint values', () async {
    // Happy path: ensure legacy and normalized coordinates are preserved.
    final Box<TrackPoint> box =
        await Hive.openBox<TrackPoint>('track_point_box');
    const TrackPoint point = TrackPoint(
      frameIndex: 12,
      pixelX: 100.5,
      pixelY: 200.25,
      worldX: 1.2,
      worldY: -3.4,
      normalizedX: 0.25,
      normalizedY: 0.5,
    );
    await box.put('point', point);

    final TrackPoint? restored = box.get('point');
    expect(restored, isNotNull);
    expect(restored!.frameIndex, 12);
    expect(restored.pixelX, 100.5);
    expect(restored.pixelY, 200.25);
    expect(restored.worldX, 1.2);
    expect(restored.worldY, -3.4);
    expect(restored.normalizedX, 0.25);
    expect(restored.normalizedY, 0.5);
  });

  test('persists null world coordinates', () async {
    // Edge case: optional derived coordinates should remain null until set.
    final Box<TrackPoint> box =
        await Hive.openBox<TrackPoint>('track_point_box_2');
    const TrackPoint point = TrackPoint(
      frameIndex: 1,
      pixelX: 10,
      pixelY: 20,
      worldX: null,
      worldY: null,
      normalizedX: null,
      normalizedY: null,
    );
    await box.put('point', point);

    final TrackPoint? restored = box.get('point');
    expect(restored, isNotNull);
    expect(restored!.worldX, isNull);
    expect(restored.worldY, isNull);
    expect(restored.normalizedX, isNull);
    expect(restored.normalizedY, isNull);
  });

  test('copyWith updates point values', () {
    // Regression guard: normalized coordinates are the editable source of truth.
    const TrackPoint point = TrackPoint(
      frameIndex: 1,
      pixelX: 10,
      pixelY: 20,
      worldX: null,
      worldY: null,
      normalizedX: 0.1,
      normalizedY: 0.2,
    );
    final TrackPoint updated = point.copyWith(normalizedX: 0.4, worldX: 0.42);
    expect(updated.frameIndex, 1);
    expect(updated.pixelX, 10);
    expect(updated.pixelY, 20);
    expect(updated.worldX, 0.42);
    expect(updated.worldY, isNull);
    expect(updated.normalizedX, 0.4);
    expect(updated.normalizedY, 0.2);
  });
}
