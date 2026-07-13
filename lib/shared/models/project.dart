// Responsibility: Immutable project aggregate containing video and tracking data.

import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:hive/hive.dart';

import 'calibration_data.dart';
import 'hive_type_ids.dart';
import 'track.dart';

part 'project.freezed.dart';
part 'project.g.dart';

@freezed
@HiveType(typeId: kProjectTypeId)
class Project with _$Project {
  const factory Project({
    @HiveField(0) required String id,
    @HiveField(1) required String name,
    @HiveField(2) required String videoPath,
    @HiveField(3) required DateTime createdAt,
    @HiveField(4) CalibrationData? calibration,
    @HiveField(5) required List<Track> tracks,
    @HiveField(6) @Default(0) int lastFrameIndex,
    @HiveField(7) @Default(0) int startFrame,
    @HiveField(8) @Default(0) int endFrame,
  }) = _Project;
}
