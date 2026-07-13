// Responsibility: Immutable state for tracking screen data and frame cache.

import 'dart:typed_data';

import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../../shared/models/project.dart';
import 'video_metadata.dart';

part 'tracking_state.freezed.dart';

@freezed
class TrackingState with _$TrackingState {
  const factory TrackingState({
    required Project project,
    required VideoMetadata metadata,
    required int frameIndex,
    Uint8List? frameBytes,
    @Default(false) bool isFrameLoading,
    @Default(false) bool isPlaying,
    String? errorMessage,
    String? warningMessage,
  }) = _TrackingState;
}
