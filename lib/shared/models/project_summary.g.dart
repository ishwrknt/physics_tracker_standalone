// GENERATED CODE - DO NOT MODIFY BY HAND
// Manually corrected: hive_generator 2.0.1 does not resolve freezed factory fields.

part of 'project_summary.dart';

// **************************************************************************
// TypeAdapterGenerator
// **************************************************************************

class ProjectSummaryAdapter extends TypeAdapter<ProjectSummary> {
  @override
  final int typeId = 5;

  @override
  ProjectSummary read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return ProjectSummary(
      id: fields[0] as String,
      name: fields[1] as String,
      videoPath: fields[2] as String,
      createdAt: fields[3] as DateTime,
      thumbnailPath: fields[4] as String?,
    );
  }

  @override
  void write(BinaryWriter writer, ProjectSummary obj) {
    writer
      ..writeByte(5)
      ..writeByte(0)
      ..write(obj.id)
      ..writeByte(1)
      ..write(obj.name)
      ..writeByte(2)
      ..write(obj.videoPath)
      ..writeByte(3)
      ..write(obj.createdAt)
      ..writeByte(4)
      ..write(obj.thumbnailPath);
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ProjectSummaryAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}
