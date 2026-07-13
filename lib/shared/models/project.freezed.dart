// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'project.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

/// @nodoc
mixin _$Project {
  @HiveField(0)
  String get id => throw _privateConstructorUsedError;
  @HiveField(1)
  String get name => throw _privateConstructorUsedError;
  @HiveField(2)
  String get videoPath => throw _privateConstructorUsedError;
  @HiveField(3)
  DateTime get createdAt => throw _privateConstructorUsedError;
  @HiveField(4)
  CalibrationData? get calibration => throw _privateConstructorUsedError;
  @HiveField(5)
  List<Track> get tracks => throw _privateConstructorUsedError;
  @HiveField(6)
  int get lastFrameIndex => throw _privateConstructorUsedError;
  @HiveField(7)
  int get startFrame => throw _privateConstructorUsedError;
  @HiveField(8)
  int get endFrame => throw _privateConstructorUsedError;

  @JsonKey(ignore: true)
  $ProjectCopyWith<Project> get copyWith => throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $ProjectCopyWith<$Res> {
  factory $ProjectCopyWith(Project value, $Res Function(Project) then) =
      _$ProjectCopyWithImpl<$Res, Project>;
  @useResult
  $Res call(
      {@HiveField(0) String id,
      @HiveField(1) String name,
      @HiveField(2) String videoPath,
      @HiveField(3) DateTime createdAt,
      @HiveField(4) CalibrationData? calibration,
      @HiveField(5) List<Track> tracks,
      @HiveField(6) int lastFrameIndex,
      @HiveField(7) int startFrame,
      @HiveField(8) int endFrame});

  $CalibrationDataCopyWith<$Res>? get calibration;
}

/// @nodoc
class _$ProjectCopyWithImpl<$Res, $Val extends Project>
    implements $ProjectCopyWith<$Res> {
  _$ProjectCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? videoPath = null,
    Object? createdAt = null,
    Object? calibration = freezed,
    Object? tracks = null,
    Object? lastFrameIndex = null,
    Object? startFrame = null,
    Object? endFrame = null,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      videoPath: null == videoPath
          ? _value.videoPath
          : videoPath // ignore: cast_nullable_to_non_nullable
              as String,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      calibration: freezed == calibration
          ? _value.calibration
          : calibration // ignore: cast_nullable_to_non_nullable
              as CalibrationData?,
      tracks: null == tracks
          ? _value.tracks
          : tracks // ignore: cast_nullable_to_non_nullable
              as List<Track>,
      lastFrameIndex: null == lastFrameIndex
          ? _value.lastFrameIndex
          : lastFrameIndex // ignore: cast_nullable_to_non_nullable
              as int,
      startFrame: null == startFrame
          ? _value.startFrame
          : startFrame // ignore: cast_nullable_to_non_nullable
              as int,
      endFrame: null == endFrame
          ? _value.endFrame
          : endFrame // ignore: cast_nullable_to_non_nullable
              as int,
    ) as $Val);
  }

  @override
  @pragma('vm:prefer-inline')
  $CalibrationDataCopyWith<$Res>? get calibration {
    if (_value.calibration == null) {
      return null;
    }

    return $CalibrationDataCopyWith<$Res>(_value.calibration!, (value) {
      return _then(_value.copyWith(calibration: value) as $Val);
    });
  }
}

/// @nodoc
abstract class _$$ProjectImplCopyWith<$Res> implements $ProjectCopyWith<$Res> {
  factory _$$ProjectImplCopyWith(
          _$ProjectImpl value, $Res Function(_$ProjectImpl) then) =
      __$$ProjectImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {@HiveField(0) String id,
      @HiveField(1) String name,
      @HiveField(2) String videoPath,
      @HiveField(3) DateTime createdAt,
      @HiveField(4) CalibrationData? calibration,
      @HiveField(5) List<Track> tracks,
      @HiveField(6) int lastFrameIndex,
      @HiveField(7) int startFrame,
      @HiveField(8) int endFrame});

  @override
  $CalibrationDataCopyWith<$Res>? get calibration;
}

/// @nodoc
class __$$ProjectImplCopyWithImpl<$Res>
    extends _$ProjectCopyWithImpl<$Res, _$ProjectImpl>
    implements _$$ProjectImplCopyWith<$Res> {
  __$$ProjectImplCopyWithImpl(
      _$ProjectImpl _value, $Res Function(_$ProjectImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? videoPath = null,
    Object? createdAt = null,
    Object? calibration = freezed,
    Object? tracks = null,
    Object? lastFrameIndex = null,
    Object? startFrame = null,
    Object? endFrame = null,
  }) {
    return _then(_$ProjectImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      videoPath: null == videoPath
          ? _value.videoPath
          : videoPath // ignore: cast_nullable_to_non_nullable
              as String,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      calibration: freezed == calibration
          ? _value.calibration
          : calibration // ignore: cast_nullable_to_non_nullable
              as CalibrationData?,
      tracks: null == tracks
          ? _value._tracks
          : tracks // ignore: cast_nullable_to_non_nullable
              as List<Track>,
      lastFrameIndex: null == lastFrameIndex
          ? _value.lastFrameIndex
          : lastFrameIndex // ignore: cast_nullable_to_non_nullable
              as int,
      startFrame: null == startFrame
          ? _value.startFrame
          : startFrame // ignore: cast_nullable_to_non_nullable
              as int,
      endFrame: null == endFrame
          ? _value.endFrame
          : endFrame // ignore: cast_nullable_to_non_nullable
              as int,
    ));
  }
}

/// @nodoc

class _$ProjectImpl implements _Project {
  const _$ProjectImpl(
      {@HiveField(0) required this.id,
      @HiveField(1) required this.name,
      @HiveField(2) required this.videoPath,
      @HiveField(3) required this.createdAt,
      @HiveField(4) this.calibration,
      @HiveField(5) required final List<Track> tracks,
      @HiveField(6) this.lastFrameIndex = 0,
      @HiveField(7) this.startFrame = 0,
      @HiveField(8) this.endFrame = 0})
      : _tracks = tracks;

  @override
  @HiveField(0)
  final String id;
  @override
  @HiveField(1)
  final String name;
  @override
  @HiveField(2)
  final String videoPath;
  @override
  @HiveField(3)
  final DateTime createdAt;
  @override
  @HiveField(4)
  final CalibrationData? calibration;
  final List<Track> _tracks;
  @override
  @HiveField(5)
  List<Track> get tracks {
    if (_tracks is EqualUnmodifiableListView) return _tracks;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(_tracks);
  }

  @override
  @JsonKey()
  @HiveField(6)
  final int lastFrameIndex;
  @override
  @JsonKey()
  @HiveField(7)
  final int startFrame;
  @override
  @JsonKey()
  @HiveField(8)
  final int endFrame;

  @override
  String toString() {
    return 'Project(id: $id, name: $name, videoPath: $videoPath, createdAt: $createdAt, calibration: $calibration, tracks: $tracks, lastFrameIndex: $lastFrameIndex, startFrame: $startFrame, endFrame: $endFrame)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$ProjectImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.videoPath, videoPath) ||
                other.videoPath == videoPath) &&
            (identical(other.createdAt, createdAt) ||
                other.createdAt == createdAt) &&
            (identical(other.calibration, calibration) ||
                other.calibration == calibration) &&
            const DeepCollectionEquality().equals(other._tracks, _tracks) &&
            (identical(other.lastFrameIndex, lastFrameIndex) ||
                other.lastFrameIndex == lastFrameIndex) &&
            (identical(other.startFrame, startFrame) ||
                other.startFrame == startFrame) &&
            (identical(other.endFrame, endFrame) ||
                other.endFrame == endFrame));
  }

  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      name,
      videoPath,
      createdAt,
      calibration,
      const DeepCollectionEquality().hash(_tracks),
      lastFrameIndex,
      startFrame,
      endFrame);

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$ProjectImplCopyWith<_$ProjectImpl> get copyWith =>
      __$$ProjectImplCopyWithImpl<_$ProjectImpl>(this, _$identity);
}

abstract class _Project implements Project {
  const factory _Project(
      {@HiveField(0) required final String id,
      @HiveField(1) required final String name,
      @HiveField(2) required final String videoPath,
      @HiveField(3) required final DateTime createdAt,
      @HiveField(4) final CalibrationData? calibration,
      @HiveField(5) required final List<Track> tracks,
      @HiveField(6) final int lastFrameIndex,
      @HiveField(7) final int startFrame,
      @HiveField(8) final int endFrame}) = _$ProjectImpl;

  @override
  @HiveField(0)
  String get id;
  @override
  @HiveField(1)
  String get name;
  @override
  @HiveField(2)
  String get videoPath;
  @override
  @HiveField(3)
  DateTime get createdAt;
  @override
  @HiveField(4)
  CalibrationData? get calibration;
  @override
  @HiveField(5)
  List<Track> get tracks;
  @override
  @HiveField(6)
  int get lastFrameIndex;
  @override
  @HiveField(7)
  int get startFrame;
  @override
  @HiveField(8)
  int get endFrame;
  @override
  @JsonKey(ignore: true)
  _$$ProjectImplCopyWith<_$ProjectImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
