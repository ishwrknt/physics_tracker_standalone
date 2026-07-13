// GENERATED CODE - DO NOT MODIFY BY HAND
// Manually corrected: hive_generator 2.0.1 does not resolve freezed factory fields.

part of 'project.dart';

// **************************************************************************
// TypeAdapterGenerator
// **************************************************************************

class ProjectAdapter extends TypeAdapter<Project> {
  @override
  final int typeId = 4;

  @override
  Project read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return Project(
      id: fields[0] as String,
      name: fields[1] as String,
      videoPath: fields[2] as String,
      createdAt: fields[3] as DateTime,
      calibration: fields[4] as CalibrationData?,
      tracks: (fields[5] as List).cast<Track>(),
      lastFrameIndex: fields[6] as int? ?? 0,
      startFrame: fields[7] as int? ?? 0,
      endFrame: fields[8] as int? ?? 0,
    );
  }

  @override
  void write(BinaryWriter writer, Project obj) {
    writer
      ..writeByte(9)
      ..writeByte(0)
      ..write(obj.id)
      ..writeByte(1)
      ..write(obj.name)
      ..writeByte(2)
      ..write(obj.videoPath)
      ..writeByte(3)
      ..write(obj.createdAt)
      ..writeByte(4)
      ..write(obj.calibration)
      ..writeByte(5)
      ..write(obj.tracks)
      ..writeByte(6)
      ..write(obj.lastFrameIndex)
      ..writeByte(7)
      ..write(obj.startFrame)
      ..writeByte(8)
      ..write(obj.endFrame);
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ProjectAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}
