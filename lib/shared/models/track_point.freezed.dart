// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'track_point.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

/// @nodoc
mixin _$TrackPoint {
  @HiveField(0)
  int get frameIndex => throw _privateConstructorUsedError;
  @HiveField(1)
  double get pixelX => throw _privateConstructorUsedError;
  @HiveField(2)
  double get pixelY => throw _privateConstructorUsedError;
  @HiveField(3)
  double? get worldX => throw _privateConstructorUsedError;
  @HiveField(4)
  double? get worldY => throw _privateConstructorUsedError;
  @HiveField(5)
  double? get normalizedX => throw _privateConstructorUsedError;
  @HiveField(6)
  double? get normalizedY => throw _privateConstructorUsedError;

  @JsonKey(ignore: true)
  $TrackPointCopyWith<TrackPoint> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $TrackPointCopyWith<$Res> {
  factory $TrackPointCopyWith(
          TrackPoint value, $Res Function(TrackPoint) then) =
      _$TrackPointCopyWithImpl<$Res, TrackPoint>;
  @useResult
  $Res call(
      {@HiveField(0) int frameIndex,
      @HiveField(1) double pixelX,
      @HiveField(2) double pixelY,
      @HiveField(3) double? worldX,
      @HiveField(4) double? worldY,
      @HiveField(5) double? normalizedX,
      @HiveField(6) double? normalizedY});
}

/// @nodoc
class _$TrackPointCopyWithImpl<$Res, $Val extends TrackPoint>
    implements $TrackPointCopyWith<$Res> {
  _$TrackPointCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? frameIndex = null,
    Object? pixelX = null,
    Object? pixelY = null,
    Object? worldX = freezed,
    Object? worldY = freezed,
    Object? normalizedX = freezed,
    Object? normalizedY = freezed,
  }) {
    return _then(_value.copyWith(
      frameIndex: null == frameIndex
          ? _value.frameIndex
          : frameIndex // ignore: cast_nullable_to_non_nullable
              as int,
      pixelX: null == pixelX
          ? _value.pixelX
          : pixelX // ignore: cast_nullable_to_non_nullable
              as double,
      pixelY: null == pixelY
          ? _value.pixelY
          : pixelY // ignore: cast_nullable_to_non_nullable
              as double,
      worldX: freezed == worldX
          ? _value.worldX
          : worldX // ignore: cast_nullable_to_non_nullable
              as double?,
      worldY: freezed == worldY
          ? _value.worldY
          : worldY // ignore: cast_nullable_to_non_nullable
              as double?,
      normalizedX: freezed == normalizedX
          ? _value.normalizedX
          : normalizedX // ignore: cast_nullable_to_non_nullable
              as double?,
      normalizedY: freezed == normalizedY
          ? _value.normalizedY
          : normalizedY // ignore: cast_nullable_to_non_nullable
              as double?,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$TrackPointImplCopyWith<$Res>
    implements $TrackPointCopyWith<$Res> {
  factory _$$TrackPointImplCopyWith(
          _$TrackPointImpl value, $Res Function(_$TrackPointImpl) then) =
      __$$TrackPointImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {@HiveField(0) int frameIndex,
      @HiveField(1) double pixelX,
      @HiveField(2) double pixelY,
      @HiveField(3) double? worldX,
      @HiveField(4) double? worldY,
      @HiveField(5) double? normalizedX,
      @HiveField(6) double? normalizedY});
}

/// @nodoc
class __$$TrackPointImplCopyWithImpl<$Res>
    extends _$TrackPointCopyWithImpl<$Res, _$TrackPointImpl>
    implements _$$TrackPointImplCopyWith<$Res> {
  __$$TrackPointImplCopyWithImpl(
      _$TrackPointImpl _value, $Res Function(_$TrackPointImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? frameIndex = null,
    Object? pixelX = null,
    Object? pixelY = null,
    Object? worldX = freezed,
    Object? worldY = freezed,
    Object? normalizedX = freezed,
    Object? normalizedY = freezed,
  }) {
    return _then(_$TrackPointImpl(
      frameIndex: null == frameIndex
          ? _value.frameIndex
          : frameIndex // ignore: cast_nullable_to_non_nullable
              as int,
      pixelX: null == pixelX
          ? _value.pixelX
          : pixelX // ignore: cast_nullable_to_non_nullable
              as double,
      pixelY: null == pixelY
          ? _value.pixelY
          : pixelY // ignore: cast_nullable_to_non_nullable
              as double,
      worldX: freezed == worldX
          ? _value.worldX
          : worldX // ignore: cast_nullable_to_non_nullable
              as double?,
      worldY: freezed == worldY
          ? _value.worldY
          : worldY // ignore: cast_nullable_to_non_nullable
              as double?,
      normalizedX: freezed == normalizedX
          ? _value.normalizedX
          : normalizedX // ignore: cast_nullable_to_non_nullable
              as double?,
      normalizedY: freezed == normalizedY
          ? _value.normalizedY
          : normalizedY // ignore: cast_nullable_to_non_nullable
              as double?,
    ));
  }
}

/// @nodoc

class _$TrackPointImpl implements _TrackPoint {
  const _$TrackPointImpl(
      {@HiveField(0) required this.frameIndex,
      @HiveField(1) required this.pixelX,
      @HiveField(2) required this.pixelY,
      @HiveField(3) this.worldX,
      @HiveField(4) this.worldY,
      @HiveField(5) this.normalizedX,
      @HiveField(6) this.normalizedY});

  @override
  @HiveField(0)
  final int frameIndex;
  @override
  @HiveField(1)
  final double pixelX;
  @override
  @HiveField(2)
  final double pixelY;
  @override
  @HiveField(3)
  final double? worldX;
  @override
  @HiveField(4)
  final double? worldY;
  @override
  @HiveField(5)
  final double? normalizedX;
  @override
  @HiveField(6)
  final double? normalizedY;

  @override
  String toString() {
    return 'TrackPoint(frameIndex: $frameIndex, pixelX: $pixelX, pixelY: $pixelY, worldX: $worldX, worldY: $worldY, normalizedX: $normalizedX, normalizedY: $normalizedY)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$TrackPointImpl &&
            (identical(other.frameIndex, frameIndex) ||
                other.frameIndex == frameIndex) &&
            (identical(other.pixelX, pixelX) || other.pixelX == pixelX) &&
            (identical(other.pixelY, pixelY) || other.pixelY == pixelY) &&
            (identical(other.worldX, worldX) || other.worldX == worldX) &&
            (identical(other.worldY, worldY) || other.worldY == worldY) &&
            (identical(other.normalizedX, normalizedX) ||
                other.normalizedX == normalizedX) &&
            (identical(other.normalizedY, normalizedY) ||
                other.normalizedY == normalizedY));
  }

  @override
  int get hashCode => Object.hash(runtimeType, frameIndex, pixelX, pixelY,
      worldX, worldY, normalizedX, normalizedY);

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$TrackPointImplCopyWith<_$TrackPointImpl> get copyWith =>
      __$$TrackPointImplCopyWithImpl<_$TrackPointImpl>(this, _$identity);
}

abstract class _TrackPoint implements TrackPoint {
  const factory _TrackPoint(
      {@HiveField(0) required final int frameIndex,
      @HiveField(1) required final double pixelX,
      @HiveField(2) required final double pixelY,
      @HiveField(3) final double? worldX,
      @HiveField(4) final double? worldY,
      @HiveField(5) final double? normalizedX,
      @HiveField(6) final double? normalizedY}) = _$TrackPointImpl;

  @override
  @HiveField(0)
  int get frameIndex;
  @override
  @HiveField(1)
  double get pixelX;
  @override
  @HiveField(2)
  double get pixelY;
  @override
  @HiveField(3)
  double? get worldX;
  @override
  @HiveField(4)
  double? get worldY;
  @override
  @HiveField(5)
  double? get normalizedX;
  @override
  @HiveField(6)
  double? get normalizedY;
  @override
  @JsonKey(ignore: true)
  _$$TrackPointImplCopyWith<_$TrackPointImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
