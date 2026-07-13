// Responsibility: Validate pixel-to-world conversion math.

import 'dart:math';

import 'package:flutter_test/flutter_test.dart';

import 'package:physics_tracker/shared/models/calibration_data.dart';
import 'package:physics_tracker/shared/utils/calibration_math.dart';

void main() {
  test('converts pixels to meters with zero rotation', () {
    // Happy path: no rotation, simple scaling and translation.
    const CalibrationData calibration = CalibrationData(
      pixelLength: 100,
      realLength: 2,
      originPixelX: 10,
      originPixelY: 20,
      angleDegrees: 0,
    );

    final Point<double> result = pixelToWorld(
      calibration: calibration,
      pixelX: 60,
      pixelY: 70,
    );

    expect(result.x, closeTo(1.0, 0.0001));
    expect(result.y, closeTo(1.0, 0.0001));
  });

  test('rotates coordinates by 90 degrees counter-clockwise', () {
    // Edge case: validate axis rotation handling.
    const CalibrationData calibration = CalibrationData(
      pixelLength: 100,
      realLength: 1,
      originPixelX: 0,
      originPixelY: 0,
      angleDegrees: 90,
    );

    final Point<double> result = pixelToWorld(
      calibration: calibration,
      pixelX: 100,
      pixelY: 0,
    );

    expect(result.x, closeTo(0.0, 0.0001));
    expect(result.y, closeTo(1.0, 0.0001));
  });

  test('throws when calibration lengths are invalid', () {
    // Error case: avoid division by zero or negative scales.
    const CalibrationData calibration = CalibrationData(
      pixelLength: 0,
      realLength: 1,
      originPixelX: 0,
      originPixelY: 0,
      angleDegrees: 0,
    );

    expect(
      () => pixelToWorld(
        calibration: calibration,
        pixelX: 10,
        pixelY: 10,
      ),
      throwsA(isA<ArgumentError>()),
    );
  });
}
