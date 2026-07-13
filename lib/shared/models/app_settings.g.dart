// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'app_settings.dart';

// **************************************************************************
// TypeAdapterGenerator
// **************************************************************************

class AppSettingsAdapter extends TypeAdapter<AppSettings> {
  @override
  final int typeId = 10;

  @override
  AppSettings read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return AppSettings(
      autoRotateVideo: fields[0] as bool,
      landscapeMode: fields[1] as bool,
      showFrameNumbers: fields[2] as bool,
      hapticFeedback: fields[3] as bool,
      buttonSize: fields[4] as double,
      autoSaveProgress: fields[5] as bool,
    );
  }

  @override
  void write(BinaryWriter writer, AppSettings obj) {
    writer
      ..writeByte(6)
      ..writeByte(0)
      ..write(obj.autoRotateVideo)
      ..writeByte(1)
      ..write(obj.landscapeMode)
      ..writeByte(2)
      ..write(obj.showFrameNumbers)
      ..writeByte(3)
      ..write(obj.hapticFeedback)
      ..writeByte(4)
      ..write(obj.buttonSize)
      ..writeByte(5)
      ..write(obj.autoSaveProgress);
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is AppSettingsAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}
