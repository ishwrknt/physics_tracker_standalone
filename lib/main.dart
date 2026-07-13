// Responsibility: App entry point with Hive initialization and adapter registration.

import 'dart:io';
import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:hive_flutter/hive_flutter.dart';

import 'app/app.dart';
import 'shared/models/calibration_data.dart';
import 'shared/models/color_adapter.dart';
import 'shared/models/hive_type_ids.dart';
import 'shared/models/project.dart';
import 'shared/models/project_summary.dart';
import 'shared/models/track.dart';
import 'shared/models/track_point.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  try {
    DartPluginRegistrant.ensureInitialized();
    await _initHive();
    _registerAdaptersSafely();
    runApp(const ProviderScope(child: PhysicsTrackerApp()));
  } catch (error, stackTrace) {
    // Release-safe fallback so startup exceptions don't leave a blank launch screen.
    runApp(
      MaterialApp(
        home: Scaffold(
          body: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'PhysicsTracker failed to start.',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
                ),
                const SizedBox(height: 12),
                Text(error.toString()),
                const SizedBox(height: 12),
                SingleChildScrollView(
                  child: Text(stackTrace.toString()),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

Future<void> _initHive() async {
  try {
    await Hive.initFlutter();
    return;
  } catch (_) {
    if (!Platform.isAndroid) {
      rethrow;
    }
  }

  // Fallback for devices where path_provider channel isn't available at startup.
  const List<String> appIds = <String>[
    'com.example.physics_tracker',
    'com.physics.tracker',
    'physics_tracker',
  ];
  for (final String appId in appIds) {
    final String base = '/data/user/0/$appId/files';
    final Directory dir = Directory('$base/hive');
    try {
      await dir.create(recursive: true);
      Hive.init(dir.path);
      return;
    } catch (_) {
      continue;
    }
  }
  throw StateError(
    'Hive initialization failed: path_provider unavailable and Android fallback paths not writable.',
  );
}

void _registerAdaptersSafely() {
  if (!Hive.isAdapterRegistered(kColorTypeId)) {
    Hive.registerAdapter(ColorAdapter());
  }
  if (!Hive.isAdapterRegistered(kCalibrationDataTypeId)) {
    Hive.registerAdapter(CalibrationDataAdapter());
  }
  if (!Hive.isAdapterRegistered(kTrackPointTypeId)) {
    Hive.registerAdapter(TrackPointAdapter());
  }
  if (!Hive.isAdapterRegistered(kTrackTypeId)) {
    Hive.registerAdapter(TrackAdapter());
  }
  if (!Hive.isAdapterRegistered(kProjectTypeId)) {
    Hive.registerAdapter(ProjectAdapter());
  }
  if (!Hive.isAdapterRegistered(kProjectSummaryTypeId)) {
    Hive.registerAdapter(ProjectSummaryAdapter());
  }
}
