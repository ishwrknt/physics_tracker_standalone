// GENERATED CODE - DO NOT MODIFY BY HAND
// Manually corrected: hive_generator 2.0.1 does not resolve freezed factory fields.

part of 'track.dart';

// **************************************************************************
// TypeAdapterGenerator
// **************************************************************************

class TrackAdapter extends TypeAdapter<Track> {
  @override
  final int typeId = 3;

  @override
  Track read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return Track(
      id: fields[0] as String,
      label: fields[1] as String,
      mass: fields[2] as double?,
      color: fields[3] as Color,
      points: (fields[4] as List).cast<TrackPoint>(),
    );
  }

  @override
  void write(BinaryWriter writer, Track obj) {
    writer
      ..writeByte(5)
      ..writeByte(0)
      ..write(obj.id)
      ..writeByte(1)
      ..write(obj.label)
      ..writeByte(2)
      ..write(obj.mass)
      ..writeByte(3)
      ..write(obj.color)
      ..writeByte(4)
      ..write(obj.points);
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is TrackAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}
