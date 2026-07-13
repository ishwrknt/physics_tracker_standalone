// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'track.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

/// @nodoc
mixin _$Track {
  @HiveField(0)
  String get id => throw _privateConstructorUsedError;
  @HiveField(1)
  String get label => throw _privateConstructorUsedError;
  @HiveField(2)
  double? get mass => throw _privateConstructorUsedError;
  @HiveField(3)
  Color get color => throw _privateConstructorUsedError;
  @HiveField(4)
  List<TrackPoint> get points => throw _privateConstructorUsedError;

  @JsonKey(ignore: true)
  $TrackCopyWith<Track> get copyWith => throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $TrackCopyWith<$Res> {
  factory $TrackCopyWith(Track value, $Res Function(Track) then) =
      _$TrackCopyWithImpl<$Res, Track>;
  @useResult
  $Res call(
      {@HiveField(0) String id,
      @HiveField(1) String label,
      @HiveField(2) double? mass,
      @HiveField(3) Color color,
      @HiveField(4) List<TrackPoint> points});
}

/// @nodoc
class _$TrackCopyWithImpl<$Res, $Val extends Track>
    implements $TrackCopyWith<$Res> {
  _$TrackCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? label = null,
    Object? mass = freezed,
    Object? color = null,
    Object? points = null,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      label: null == label
          ? _value.label
          : label // ignore: cast_nullable_to_non_nullable
              as String,
      mass: freezed == mass
          ? _value.mass
          : mass // ignore: cast_nullable_to_non_nullable
              as double?,
      color: null == color
          ? _value.color
          : color // ignore: cast_nullable_to_non_nullable
              as Color,
      points: null == points
          ? _value.points
          : points // ignore: cast_nullable_to_non_nullable
              as List<TrackPoint>,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$TrackImplCopyWith<$Res> implements $TrackCopyWith<$Res> {
  factory _$$TrackImplCopyWith(
          _$TrackImpl value, $Res Function(_$TrackImpl) then) =
      __$$TrackImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {@HiveField(0) String id,
      @HiveField(1) String label,
      @HiveField(2) double? mass,
      @HiveField(3) Color color,
      @HiveField(4) List<TrackPoint> points});
}

/// @nodoc
class __$$TrackImplCopyWithImpl<$Res>
    extends _$TrackCopyWithImpl<$Res, _$TrackImpl>
    implements _$$TrackImplCopyWith<$Res> {
  __$$TrackImplCopyWithImpl(
      _$TrackImpl _value, $Res Function(_$TrackImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? label = null,
    Object? mass = freezed,
    Object? color = null,
    Object? points = null,
  }) {
    return _then(_$TrackImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      label: null == label
          ? _value.label
          : label // ignore: cast_nullable_to_non_nullable
              as String,
      mass: freezed == mass
          ? _value.mass
          : mass // ignore: cast_nullable_to_non_nullable
              as double?,
      color: null == color
          ? _value.color
          : color // ignore: cast_nullable_to_non_nullable
              as Color,
      points: null == points
          ? _value._points
          : points // ignore: cast_nullable_to_non_nullable
              as List<TrackPoint>,
    ));
  }
}

/// @nodoc

class _$TrackImpl implements _Track {
  const _$TrackImpl(
      {@HiveField(0) required this.id,
      @HiveField(1) required this.label,
      @HiveField(2) this.mass,
      @HiveField(3) required this.color,
      @HiveField(4) required final List<TrackPoint> points})
      : _points = points;

  @override
  @HiveField(0)
  final String id;
  @override
  @HiveField(1)
  final String label;
  @override
  @HiveField(2)
  final double? mass;
  @override
  @HiveField(3)
  final Color color;
  final List<TrackPoint> _points;
  @override
  @HiveField(4)
  List<TrackPoint> get points {
    if (_points is EqualUnmodifiableListView) return _points;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(_points);
  }

  @override
  String toString() {
    return 'Track(id: $id, label: $label, mass: $mass, color: $color, points: $points)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$TrackImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.label, label) || other.label == label) &&
            (identical(other.mass, mass) || other.mass == mass) &&
            (identical(other.color, color) || other.color == color) &&
            const DeepCollectionEquality().equals(other._points, _points));
  }

  @override
  int get hashCode => Object.hash(runtimeType, id, label, mass, color,
      const DeepCollectionEquality().hash(_points));

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$TrackImplCopyWith<_$TrackImpl> get copyWith =>
      __$$TrackImplCopyWithImpl<_$TrackImpl>(this, _$identity);
}

abstract class _Track implements Track {
  const factory _Track(
      {@HiveField(0) required final String id,
      @HiveField(1) required final String label,
      @HiveField(2) final double? mass,
      @HiveField(3) required final Color color,
      @HiveField(4) required final List<TrackPoint> points}) = _$TrackImpl;

  @override
  @HiveField(0)
  String get id;
  @override
  @HiveField(1)
  String get label;
  @override
  @HiveField(2)
  double? get mass;
  @override
  @HiveField(3)
  Color get color;
  @override
  @HiveField(4)
  List<TrackPoint> get points;
  @override
  @JsonKey(ignore: true)
  _$$TrackImplCopyWith<_$TrackImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
