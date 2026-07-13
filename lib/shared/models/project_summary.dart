// Responsibility: Lightweight project summary for the home screen list.

import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:hive/hive.dart';

import 'hive_type_ids.dart';

part 'project_summary.freezed.dart';
part 'project_summary.g.dart';

@freezed
@HiveType(typeId: kProjectSummaryTypeId)
class ProjectSummary with _$ProjectSummary {
  const factory ProjectSummary({
    @HiveField(0) required String id,
    @HiveField(1) required String name,
    @HiveField(2) required String videoPath,
    @HiveField(3) required DateTime createdAt,
    @HiveField(4) String? thumbnailPath,
  }) = _ProjectSummary;
}
