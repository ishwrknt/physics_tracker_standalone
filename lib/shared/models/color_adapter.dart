// Responsibility: Hive adapter for Flutter Color values.

import 'package:flutter/material.dart';
import 'package:hive/hive.dart';

import 'hive_type_ids.dart';

class ColorAdapter extends TypeAdapter<Color> {
  @override
  final int typeId = kColorTypeId;

  @override
  Color read(BinaryReader reader) {
    final int value = reader.readInt();
    return Color(value);
  }

  @override
  void write(BinaryWriter writer, Color obj) {
    writer.writeInt(obj.toARGB32());
  }
}
