// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'calibration_data.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

/// @nodoc
mixin _$CalibrationData {
  @HiveField(0)
  double get pixelLength => throw _privateConstructorUsedError;
  @HiveField(1)
  double get realLength => throw _privateConstructorUsedError;
  @HiveField(2)
  double get originPixelX => throw _privateConstructorUsedError;
  @HiveField(3)
  double get originPixelY => throw _privateConstructorUsedError;
  @HiveField(4)
  double get angleDegrees => throw _privateConstructorUsedError;

  @JsonKey(ignore: true)
  $CalibrationDataCopyWith<CalibrationData> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $CalibrationDataCopyWith<$Res> {
  factory $CalibrationDataCopyWith(
          CalibrationData value, $Res Function(CalibrationData) then) =
      _$CalibrationDataCopyWithImpl<$Res, CalibrationData>;
  @useResult
  $Res call(
      {@HiveField(0) double pixelLength,
      @HiveField(1) double realLength,
      @HiveField(2) double originPixelX,
      @HiveField(3) double originPixelY,
      @HiveField(4) double angleDegrees});
}

/// @nodoc
class _$CalibrationDataCopyWithImpl<$Res, $Val extends CalibrationData>
    implements $CalibrationDataCopyWith<$Res> {
  _$CalibrationDataCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? pixelLength = null,
    Object? realLength = null,
    Object? originPixelX = null,
    Object? originPixelY = null,
    Object? angleDegrees = null,
  }) {
    return _then(_value.copyWith(
      pixelLength: null == pixelLength
          ? _value.pixelLength
          : pixelLength // ignore: cast_nullable_to_non_nullable
              as double,
      realLength: null == realLength
          ? _value.realLength
          : realLength // ignore: cast_nullable_to_non_nullable
              as double,
      originPixelX: null == originPixelX
          ? _value.originPixelX
          : originPixelX // ignore: cast_nullable_to_non_nullable
              as double,
      originPixelY: null == originPixelY
          ? _value.originPixelY
          : originPixelY // ignore: cast_nullable_to_non_nullable
              as double,
      angleDegrees: null == angleDegrees
          ? _value.angleDegrees
          : angleDegrees // ignore: cast_nullable_to_non_nullable
              as double,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$CalibrationDataImplCopyWith<$Res>
    implements $CalibrationDataCopyWith<$Res> {
  factory _$$CalibrationDataImplCopyWith(_$CalibrationDataImpl value,
          $Res Function(_$CalibrationDataImpl) then) =
      __$$CalibrationDataImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {@HiveField(0) double pixelLength,
      @HiveField(1) double realLength,
      @HiveField(2) double originPixelX,
      @HiveField(3) double originPixelY,
      @HiveField(4) double angleDegrees});
}

/// @nodoc
class __$$CalibrationDataImplCopyWithImpl<$Res>
    extends _$CalibrationDataCopyWithImpl<$Res, _$CalibrationDataImpl>
    implements _$$CalibrationDataImplCopyWith<$Res> {
  __$$CalibrationDataImplCopyWithImpl(
      _$CalibrationDataImpl _value, $Res Function(_$CalibrationDataImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? pixelLength = null,
    Object? realLength = null,
    Object? originPixelX = null,
    Object? originPixelY = null,
    Object? angleDegrees = null,
  }) {
    return _then(_$CalibrationDataImpl(
      pixelLength: null == pixelLength
          ? _value.pixelLength
          : pixelLength // ignore: cast_nullable_to_non_nullable
              as double,
      realLength: null == realLength
          ? _value.realLength
          : realLength // ignore: cast_nullable_to_non_nullable
              as double,
      originPixelX: null == originPixelX
          ? _value.originPixelX
          : originPixelX // ignore: cast_nullable_to_non_nullable
              as double,
      originPixelY: null == originPixelY
          ? _value.originPixelY
          : originPixelY // ignore: cast_nullable_to_non_nullable
              as double,
      angleDegrees: null == angleDegrees
          ? _value.angleDegrees
          : angleDegrees // ignore: cast_nullable_to_non_nullable
              as double,
    ));
  }
}

/// @nodoc

class _$CalibrationDataImpl implements _CalibrationData {
  const _$CalibrationDataImpl(
      {@HiveField(0) required this.pixelLength,
      @HiveField(1) required this.realLength,
      @HiveField(2) required this.originPixelX,
      @HiveField(3) required this.originPixelY,
      @HiveField(4) required this.angleDegrees});

  @override
  @HiveField(0)
  final double pixelLength;
  @override
  @HiveField(1)
  final double realLength;
  @override
  @HiveField(2)
  final double originPixelX;
  @override
  @HiveField(3)
  final double originPixelY;
  @override
  @HiveField(4)
  final double angleDegrees;

  @override
  String toString() {
    return 'CalibrationData(pixelLength: $pixelLength, realLength: $realLength, originPixelX: $originPixelX, originPixelY: $originPixelY, angleDegrees: $angleDegrees)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$CalibrationDataImpl &&
            (identical(other.pixelLength, pixelLength) ||
                other.pixelLength == pixelLength) &&
            (identical(other.realLength, realLength) ||
                other.realLength == realLength) &&
            (identical(other.originPixelX, originPixelX) ||
                other.originPixelX == originPixelX) &&
            (identical(other.originPixelY, originPixelY) ||
                other.originPixelY == originPixelY) &&
            (identical(other.angleDegrees, angleDegrees) ||
                other.angleDegrees == angleDegrees));
  }

  @override
  int get hashCode => Object.hash(runtimeType, pixelLength, realLength,
      originPixelX, originPixelY, angleDegrees);

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$CalibrationDataImplCopyWith<_$CalibrationDataImpl> get copyWith =>
      __$$CalibrationDataImplCopyWithImpl<_$CalibrationDataImpl>(
          this, _$identity);
}

abstract class _CalibrationData implements CalibrationData {
  const factory _CalibrationData(
          {@HiveField(0) required final double pixelLength,
          @HiveField(1) required final double realLength,
          @HiveField(2) required final double originPixelX,
          @HiveField(3) required final double originPixelY,
          @HiveField(4) required final double angleDegrees}) =
      _$CalibrationDataImpl;

  @override
  @HiveField(0)
  double get pixelLength;
  @override
  @HiveField(1)
  double get realLength;
  @override
  @HiveField(2)
  double get originPixelX;
  @override
  @HiveField(3)
  double get originPixelY;
  @override
  @HiveField(4)
  double get angleDegrees;
  @override
  @JsonKey(ignore: true)
  _$$CalibrationDataImplCopyWith<_$CalibrationDataImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
