// Responsibility: Video metadata used for frame navigation.

import 'package:freezed_annotation/freezed_annotation.dart';

part 'video_metadata.freezed.dart';

@freezed
class VideoMetadata with _$VideoMetadata {
  const factory VideoMetadata({
    required double fps,
    required Duration duration,
    required int totalFrames,
    @Default(16) int width,
    @Default(9) int height,
    @Default(0) int rotation,
  }) = _VideoMetadata;
}
