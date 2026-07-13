// GENERATED CODE - DO NOT MODIFY BY HAND
// Manually corrected: hive_generator 2.0.1 does not resolve freezed factory fields.

part of 'track_point.dart';

// **************************************************************************
// TypeAdapterGenerator
// **************************************************************************

class TrackPointAdapter extends TypeAdapter<TrackPoint> {
  @override
  final int typeId = 2;

  @override
  TrackPoint read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return TrackPoint(
      frameIndex: fields[0] as int,
      pixelX: fields[1] as double,
      pixelY: fields[2] as double,
      worldX: fields[3] as double?,
      worldY: fields[4] as double?,
      normalizedX: fields[5] as double?,
      normalizedY: fields[6] as double?,
    );
  }

  @override
  void write(BinaryWriter writer, TrackPoint obj) {
    writer
      ..writeByte(7)
      ..writeByte(0)
      ..write(obj.frameIndex)
      ..writeByte(1)
      ..write(obj.pixelX)
      ..writeByte(2)
      ..write(obj.pixelY)
      ..writeByte(3)
      ..write(obj.worldX)
      ..writeByte(4)
      ..write(obj.worldY)
      ..writeByte(5)
      ..write(obj.normalizedX)
      ..writeByte(6)
      ..write(obj.normalizedY);
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is TrackPointAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}
