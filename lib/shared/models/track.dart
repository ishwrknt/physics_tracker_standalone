// Responsibility: Immutable track data containing visual and physical properties.

import 'package:flutter/material.dart';
import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:hive/hive.dart';

import 'hive_type_ids.dart';
import 'track_point.dart';

part 'track.freezed.dart';
part 'track.g.dart';

@freezed
@HiveType(typeId: kTrackTypeId)
class Track with _$Track {
  const factory Track({
    @HiveField(0) required String id,
    @HiveField(1) required String label,
    @HiveField(2) double? mass,
    @HiveField(3) required Color color,
    @HiveField(4) required List<TrackPoint> points,
  }) = _Track;
}
