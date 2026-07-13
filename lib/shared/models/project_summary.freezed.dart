// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'project_summary.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

/// @nodoc
mixin _$ProjectSummary {
  @HiveField(0)
  String get id => throw _privateConstructorUsedError;
  @HiveField(1)
  String get name => throw _privateConstructorUsedError;
  @HiveField(2)
  String get videoPath => throw _privateConstructorUsedError;
  @HiveField(3)
  DateTime get createdAt => throw _privateConstructorUsedError;
  @HiveField(4)
  String? get thumbnailPath => throw _privateConstructorUsedError;

  @JsonKey(ignore: true)
  $ProjectSummaryCopyWith<ProjectSummary> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $ProjectSummaryCopyWith<$Res> {
  factory $ProjectSummaryCopyWith(
          ProjectSummary value, $Res Function(ProjectSummary) then) =
      _$ProjectSummaryCopyWithImpl<$Res, ProjectSummary>;
  @useResult
  $Res call(
      {@HiveField(0) String id,
      @HiveField(1) String name,
      @HiveField(2) String videoPath,
      @HiveField(3) DateTime createdAt,
      @HiveField(4) String? thumbnailPath});
}

/// @nodoc
class _$ProjectSummaryCopyWithImpl<$Res, $Val extends ProjectSummary>
    implements $ProjectSummaryCopyWith<$Res> {
  _$ProjectSummaryCopyWithImpl(this._value, this._then);

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
    Object? thumbnailPath = freezed,
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
      thumbnailPath: freezed == thumbnailPath
          ? _value.thumbnailPath
          : thumbnailPath // ignore: cast_nullable_to_non_nullable
              as String?,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$ProjectSummaryImplCopyWith<$Res>
    implements $ProjectSummaryCopyWith<$Res> {
  factory _$$ProjectSummaryImplCopyWith(_$ProjectSummaryImpl value,
          $Res Function(_$ProjectSummaryImpl) then) =
      __$$ProjectSummaryImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {@HiveField(0) String id,
      @HiveField(1) String name,
      @HiveField(2) String videoPath,
      @HiveField(3) DateTime createdAt,
      @HiveField(4) String? thumbnailPath});
}

/// @nodoc
class __$$ProjectSummaryImplCopyWithImpl<$Res>
    extends _$ProjectSummaryCopyWithImpl<$Res, _$ProjectSummaryImpl>
    implements _$$ProjectSummaryImplCopyWith<$Res> {
  __$$ProjectSummaryImplCopyWithImpl(
      _$ProjectSummaryImpl _value, $Res Function(_$ProjectSummaryImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? videoPath = null,
    Object? createdAt = null,
    Object? thumbnailPath = freezed,
  }) {
    return _then(_$ProjectSummaryImpl(
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
      thumbnailPath: freezed == thumbnailPath
          ? _value.thumbnailPath
          : thumbnailPath // ignore: cast_nullable_to_non_nullable
              as String?,
    ));
  }
}

/// @nodoc

class _$ProjectSummaryImpl implements _ProjectSummary {
  const _$ProjectSummaryImpl(
      {@HiveField(0) required this.id,
      @HiveField(1) required this.name,
      @HiveField(2) required this.videoPath,
      @HiveField(3) required this.createdAt,
      @HiveField(4) this.thumbnailPath});

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
  final String? thumbnailPath;

  @override
  String toString() {
    return 'ProjectSummary(id: $id, name: $name, videoPath: $videoPath, createdAt: $createdAt, thumbnailPath: $thumbnailPath)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$ProjectSummaryImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.videoPath, videoPath) ||
                other.videoPath == videoPath) &&
            (identical(other.createdAt, createdAt) ||
                other.createdAt == createdAt) &&
            (identical(other.thumbnailPath, thumbnailPath) ||
                other.thumbnailPath == thumbnailPath));
  }

  @override
  int get hashCode =>
      Object.hash(runtimeType, id, name, videoPath, createdAt, thumbnailPath);

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$ProjectSummaryImplCopyWith<_$ProjectSummaryImpl> get copyWith =>
      __$$ProjectSummaryImplCopyWithImpl<_$ProjectSummaryImpl>(
          this, _$identity);
}

abstract class _ProjectSummary implements ProjectSummary {
  const factory _ProjectSummary(
      {@HiveField(0) required final String id,
      @HiveField(1) required final String name,
      @HiveField(2) required final String videoPath,
      @HiveField(3) required final DateTime createdAt,
      @HiveField(4) final String? thumbnailPath}) = _$ProjectSummaryImpl;

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
  String? get thumbnailPath;
  @override
  @JsonKey(ignore: true)
  _$$ProjectSummaryImplCopyWith<_$ProjectSummaryImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
