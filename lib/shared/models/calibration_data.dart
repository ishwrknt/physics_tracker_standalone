// Responsibility: Immutable calibration data for pixel-to-world conversion.

import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:hive/hive.dart';

import 'hive_type_ids.dart';

part 'calibration_data.freezed.dart';
part 'calibration_data.g.dart';

@freezed
@HiveType(typeId: kCalibrationDataTypeId)
class CalibrationData with _$CalibrationData {
  const factory CalibrationData({
    @HiveField(0) required double pixelLength,
    @HiveField(1) required double realLength,
    @HiveField(2) required double originPixelX,
    @HiveField(3) required double originPixelY,
    @HiveField(4) required double angleDegrees,
  }) = _CalibrationData;
}
