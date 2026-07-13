// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'video_metadata.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

/// @nodoc
mixin _$VideoMetadata {
  double get fps => throw _privateConstructorUsedError;
  Duration get duration => throw _privateConstructorUsedError;
  int get totalFrames => throw _privateConstructorUsedError;
  int get width => throw _privateConstructorUsedError;
  int get height => throw _privateConstructorUsedError;
  int get rotation => throw _privateConstructorUsedError;

  @JsonKey(ignore: true)
  $VideoMetadataCopyWith<VideoMetadata> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $VideoMetadataCopyWith<$Res> {
  factory $VideoMetadataCopyWith(
          VideoMetadata value, $Res Function(VideoMetadata) then) =
      _$VideoMetadataCopyWithImpl<$Res, VideoMetadata>;
  @useResult
  $Res call(
      {double fps,
      Duration duration,
      int totalFrames,
      int width,
      int height,
      int rotation});
}

/// @nodoc
class _$VideoMetadataCopyWithImpl<$Res, $Val extends VideoMetadata>
    implements $VideoMetadataCopyWith<$Res> {
  _$VideoMetadataCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? fps = null,
    Object? duration = null,
    Object? totalFrames = null,
    Object? width = null,
    Object? height = null,
    Object? rotation = null,
  }) {
    return _then(_value.copyWith(
      fps: null == fps
          ? _value.fps
          : fps // ignore: cast_nullable_to_non_nullable
              as double,
      duration: null == duration
          ? _value.duration
          : duration // ignore: cast_nullable_to_non_nullable
              as Duration,
      totalFrames: null == totalFrames
          ? _value.totalFrames
          : totalFrames // ignore: cast_nullable_to_non_nullable
              as int,
      width: null == width
          ? _value.width
          : width // ignore: cast_nullable_to_non_nullable
              as int,
      height: null == height
          ? _value.height
          : height // ignore: cast_nullable_to_non_nullable
              as int,
      rotation: null == rotation
          ? _value.rotation
          : rotation // ignore: cast_nullable_to_non_nullable
              as int,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$VideoMetadataImplCopyWith<$Res>
    implements $VideoMetadataCopyWith<$Res> {
  factory _$$VideoMetadataImplCopyWith(
          _$VideoMetadataImpl value, $Res Function(_$VideoMetadataImpl) then) =
      __$$VideoMetadataImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {double fps,
      Duration duration,
      int totalFrames,
      int width,
      int height,
      int rotation});
}

/// @nodoc
class __$$VideoMetadataImplCopyWithImpl<$Res>
    extends _$VideoMetadataCopyWithImpl<$Res, _$VideoMetadataImpl>
    implements _$$VideoMetadataImplCopyWith<$Res> {
  __$$VideoMetadataImplCopyWithImpl(
      _$VideoMetadataImpl _value, $Res Function(_$VideoMetadataImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? fps = null,
    Object? duration = null,
    Object? totalFrames = null,
    Object? width = null,
    Object? height = null,
    Object? rotation = null,
  }) {
    return _then(_$VideoMetadataImpl(
      fps: null == fps
          ? _value.fps
          : fps // ignore: cast_nullable_to_non_nullable
              as double,
      duration: null == duration
          ? _value.duration
          : duration // ignore: cast_nullable_to_non_nullable
              as Duration,
      totalFrames: null == totalFrames
          ? _value.totalFrames
          : totalFrames // ignore: cast_nullable_to_non_nullable
              as int,
      width: null == width
          ? _value.width
          : width // ignore: cast_nullable_to_non_nullable
              as int,
      height: null == height
          ? _value.height
          : height // ignore: cast_nullable_to_non_nullable
              as int,
      rotation: null == rotation
          ? _value.rotation
          : rotation // ignore: cast_nullable_to_non_nullable
              as int,
    ));
  }
}

/// @nodoc

class _$VideoMetadataImpl implements _VideoMetadata {
  const _$VideoMetadataImpl(
      {required this.fps,
      required this.duration,
      required this.totalFrames,
      this.width = 16,
      this.height = 9,
      this.rotation = 0});

  @override
  final double fps;
  @override
  final Duration duration;
  @override
  final int totalFrames;
  @override
  @JsonKey()
  final int width;
  @override
  @JsonKey()
  final int height;
  @override
  @JsonKey()
  final int rotation;

  @override
  String toString() {
    return 'VideoMetadata(fps: $fps, duration: $duration, totalFrames: $totalFrames, width: $width, height: $height, rotation: $rotation)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$VideoMetadataImpl &&
            (identical(other.fps, fps) || other.fps == fps) &&
            (identical(other.duration, duration) ||
                other.duration == duration) &&
            (identical(other.totalFrames, totalFrames) ||
                other.totalFrames == totalFrames) &&
            (identical(other.width, width) || other.width == width) &&
            (identical(other.height, height) || other.height == height) &&
            (identical(other.rotation, rotation) ||
                other.rotation == rotation));
  }

  @override
  int get hashCode => Object.hash(
      runtimeType, fps, duration, totalFrames, width, height, rotation);

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$VideoMetadataImplCopyWith<_$VideoMetadataImpl> get copyWith =>
      __$$VideoMetadataImplCopyWithImpl<_$VideoMetadataImpl>(this, _$identity);
}

abstract class _VideoMetadata implements VideoMetadata {
  const factory _VideoMetadata(
      {required final double fps,
      required final Duration duration,
      required final int totalFrames,
      final int width,
      final int height,
      final int rotation}) = _$VideoMetadataImpl;

  @override
  double get fps;
  @override
  Duration get duration;
  @override
  int get totalFrames;
  @override
  int get width;
  @override
  int get height;
  @override
  int get rotation;
  @override
  @JsonKey(ignore: true)
  _$$VideoMetadataImplCopyWith<_$VideoMetadataImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
