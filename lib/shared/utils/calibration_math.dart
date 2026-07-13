// Responsibility: Pixel-to-world coordinate conversion utilities.

import 'dart:math';

import '../models/calibration_data.dart';

const double _degToRad = pi / 180.0;

Point<double> pixelToWorld({
  required CalibrationData calibration,
  required double pixelX,
  required double pixelY,
}) {
  if (calibration.pixelLength <= 0 || calibration.realLength <= 0) {
    throw ArgumentError('Calibration lengths must be positive.');
  }
  // Scale is real-world meters per pixel.
  final double metersPerPixel = calibration.realLength / calibration.pixelLength;

  // Translate pixels so the origin is at the calibration origin.
  final double dxPixels = pixelX - calibration.originPixelX;
  final double dyPixels = pixelY - calibration.originPixelY;

  // Convert to meters before rotating.
  final double dxMeters = dxPixels * metersPerPixel;
  final double dyMeters = dyPixels * metersPerPixel;

  // Rotate by the calibration angle (degrees counter-clockwise).
  final double theta = calibration.angleDegrees * _degToRad;
  final double cosTheta = cos(theta);
  final double sinTheta = sin(theta);

  final double worldX = dxMeters * cosTheta - dyMeters * sinTheta;
  final double worldY = dxMeters * sinTheta + dyMeters * cosTheta;

  return Point<double>(worldX, worldY);
}
