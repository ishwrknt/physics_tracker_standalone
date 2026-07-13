// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'tracking_controller.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$trackingControllerHash() =>
    r'18b059ea9f0d4c94f6c2228b25ac57e6bc22705c';

/// Copied from Dart SDK
class _SystemHash {
  _SystemHash._();

  static int combine(int hash, int value) {
    // ignore: parameter_assignments
    hash = 0x1fffffff & (hash + value);
    // ignore: parameter_assignments
    hash = 0x1fffffff & (hash + ((0x0007ffff & hash) << 10));
    return hash ^ (hash >> 6);
  }

  static int finish(int hash) {
    // ignore: parameter_assignments
    hash = 0x1fffffff & (hash + ((0x03ffffff & hash) << 3));
    // ignore: parameter_assignments
    hash = hash ^ (hash >> 11);
    return 0x1fffffff & (hash + ((0x00003fff & hash) << 15));
  }
}

abstract class _$TrackingController
    extends BuildlessAutoDisposeAsyncNotifier<TrackingState> {
  late final String projectId;

  FutureOr<TrackingState> build(
    String projectId,
  );
}

/// See also [TrackingController].
@ProviderFor(TrackingController)
const trackingControllerProvider = TrackingControllerFamily();

/// See also [TrackingController].
class TrackingControllerFamily extends Family {
  /// See also [TrackingController].
  const TrackingControllerFamily();

  static const Iterable<ProviderOrFamily>? _dependencies = null;

  static const Iterable<ProviderOrFamily>? _allTransitiveDependencies = null;

  @override
  Iterable<ProviderOrFamily>? get dependencies => _dependencies;

  @override
  Iterable<ProviderOrFamily>? get allTransitiveDependencies =>
      _allTransitiveDependencies;

  @override
  String? get name => r'trackingControllerProvider';

  /// See also [TrackingController].
  TrackingControllerProvider call(
    String projectId,
  ) {
    return TrackingControllerProvider(
      projectId,
    );
  }

  @visibleForOverriding
  @override
  TrackingControllerProvider getProviderOverride(
    covariant TrackingControllerProvider provider,
  ) {
    return call(
      provider.projectId,
    );
  }

  /// Enables overriding the behavior of this provider, no matter the parameters.
  Override overrideWith(TrackingController Function() create) {
    return _$TrackingControllerFamilyOverride(this, create);
  }
}

class _$TrackingControllerFamilyOverride implements FamilyOverride {
  _$TrackingControllerFamilyOverride(this.overriddenFamily, this.create);

  final TrackingController Function() create;

  @override
  final TrackingControllerFamily overriddenFamily;

  @override
  TrackingControllerProvider getProviderOverride(
    covariant TrackingControllerProvider provider,
  ) {
    return provider._copyWith(create);
  }
}

/// See also [TrackingController].
class TrackingControllerProvider extends AutoDisposeAsyncNotifierProviderImpl<
    TrackingController, TrackingState> {
  /// See also [TrackingController].
  TrackingControllerProvider(
    String projectId,
  ) : this._internal(
          () => TrackingController()..projectId = projectId,
          from: trackingControllerProvider,
          name: r'trackingControllerProvider',
          debugGetCreateSourceHash:
              const bool.fromEnvironment('dart.vm.product')
                  ? null
                  : _$trackingControllerHash,
          dependencies: TrackingControllerFamily._dependencies,
          allTransitiveDependencies:
              TrackingControllerFamily._allTransitiveDependencies,
          projectId: projectId,
        );

  TrackingControllerProvider._internal(
    super.create, {
    required super.name,
    required super.dependencies,
    required super.allTransitiveDependencies,
    required super.debugGetCreateSourceHash,
    required super.from,
    required this.projectId,
  }) : super.internal();

  final String projectId;

  @override
  FutureOr<TrackingState> runNotifierBuild(
    covariant TrackingController notifier,
  ) {
    return notifier.build(
      projectId,
    );
  }

  @override
  Override overrideWith(TrackingController Function() create) {
    return ProviderOverride(
      origin: this,
      override: TrackingControllerProvider._internal(
        () => create()..projectId = projectId,
        from: from,
        name: null,
        dependencies: null,
        allTransitiveDependencies: null,
        debugGetCreateSourceHash: null,
        projectId: projectId,
      ),
    );
  }

  @override
  (String,) get argument {
    return (projectId,);
  }

  @override
  AutoDisposeAsyncNotifierProviderElement<TrackingController, TrackingState>
      createElement() {
    return _TrackingControllerProviderElement(this);
  }

  TrackingControllerProvider _copyWith(
    TrackingController Function() create,
  ) {
    return TrackingControllerProvider._internal(
      () => create()..projectId = projectId,
      name: name,
      dependencies: dependencies,
      allTransitiveDependencies: allTransitiveDependencies,
      debugGetCreateSourceHash: debugGetCreateSourceHash,
      from: from,
      projectId: projectId,
    );
  }

  @override
  bool operator ==(Object other) {
    return other is TrackingControllerProvider && other.projectId == projectId;
  }

  @override
  int get hashCode {
    var hash = _SystemHash.combine(0, runtimeType.hashCode);
    hash = _SystemHash.combine(hash, projectId.hashCode);

    return _SystemHash.finish(hash);
  }
}

mixin TrackingControllerRef
    on AutoDisposeAsyncNotifierProviderRef<TrackingState> {
  /// The parameter `projectId` of this provider.
  String get projectId;
}

class _TrackingControllerProviderElement
    extends AutoDisposeAsyncNotifierProviderElement<TrackingController,
        TrackingState> with TrackingControllerRef {
  _TrackingControllerProviderElement(super.provider);

  @override
  String get projectId => (origin as TrackingControllerProvider).projectId;
}
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member, inference_failure_on_uninitialized_variable, inference_failure_on_function_return_type, inference_failure_on_untyped_parameter, deprecated_member_use_from_same_package
