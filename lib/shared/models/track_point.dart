// Responsibility: Immutable track point data stored per frame.

import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:hive/hive.dart';

import 'hive_type_ids.dart';

part 'track_point.freezed.dart';
part 'track_point.g.dart';

@freezed
@HiveType(typeId: kTrackPointTypeId)
class TrackPoint with _$TrackPoint {
  const factory TrackPoint({
    @HiveField(0) required int frameIndex,
    @HiveField(1) required double pixelX,
    @HiveField(2) required double pixelY,
    @HiveField(3) double? worldX,
    @HiveField(4) double? worldY,
    @HiveField(5) double? normalizedX,
    @HiveField(6) double? normalizedY,
  }) = _TrackPoint;
}
