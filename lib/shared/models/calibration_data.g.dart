// GENERATED CODE - DO NOT MODIFY BY HAND
// Manually corrected: hive_generator 2.0.1 does not resolve freezed factory fields.

part of 'calibration_data.dart';

// **************************************************************************
// TypeAdapterGenerator
// **************************************************************************

class CalibrationDataAdapter extends TypeAdapter<CalibrationData> {
  @override
  final int typeId = 1;

  @override
  CalibrationData read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return CalibrationData(
      pixelLength: fields[0] as double,
      realLength: fields[1] as double,
      originPixelX: fields[2] as double,
      originPixelY: fields[3] as double,
      angleDegrees: fields[4] as double,
    );
  }

  @override
  void write(BinaryWriter writer, CalibrationData obj) {
    writer
      ..writeByte(5)
      ..writeByte(0)
      ..write(obj.pixelLength)
      ..writeByte(1)
      ..write(obj.realLength)
      ..writeByte(2)
      ..write(obj.originPixelX)
      ..writeByte(3)
      ..write(obj.originPixelY)
      ..writeByte(4)
      ..write(obj.angleDegrees);
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is CalibrationDataAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}
