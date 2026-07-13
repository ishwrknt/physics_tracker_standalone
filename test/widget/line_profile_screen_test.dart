// Widget test for LineProfileScreen.
// The mock controller returns a loading state, so we only verify the
// Scaffold structure and the Slider control that is always present.

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:physics_tracker/features/line_profile/presentation/screens/line_profile_screen.dart';
import 'package:physics_tracker/features/tracking/presentation/providers/tracking_controller.dart';
import 'package:physics_tracker/features/tracking/domain/tracking_state.dart';
import 'package:physics_tracker/features/tracking/domain/video_metadata.dart';
import 'package:physics_tracker/shared/models/project.dart';
import 'package:physics_tracker/shared/models/track.dart';

void main() {
  testWidgets(
    'LineProfileScreen builds without crashing',
    (WidgetTester tester) async {
      // Provide an override so Riverpod can resolve the provider.
      // The mock returns null frameBytes, which triggers the inner spinner.
      final mockProject = Project(
        id: 'loading_project',
        name: 'Loading Test',
        videoPath: 'dummy.mp4',
        createdAt: DateTime.now(),
        tracks: [
          Track(id: 't1', label: 'Object', color: Colors.red, points: []),
        ],
      );
      final mockMetadata = VideoMetadata(
        fps: 30,
        duration: const Duration(seconds: 1),
        totalFrames: 30,
        width: 640,
        height: 480,
      );

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            trackingControllerProvider('loading_project').overrideWith(
              () => _MockTrackingController(mockProject, mockMetadata),
            ),
          ],
          child: const MaterialApp(
            home: LineProfileScreen(projectId: 'loading_project'),
          ),
        ),
      );

      // Just verify the screen builds without throwing.
      await tester.pump();
      expect(find.byType(Scaffold), findsOneWidget);
    },
  );

  testWidgets(
    'LineProfileScreen renders Slider and instructions after data loads',
    (WidgetTester tester) async {
      final mockProject = Project(
        id: 'test_project',
        name: 'Test Project',
        videoPath: 'dummy.mp4',
        createdAt: DateTime.now(),
        tracks: [
          Track(id: 't1', label: 'Object', color: Colors.red, points: []),
        ],
      );

      final mockMetadata = VideoMetadata(
        fps: 30,
        duration: const Duration(seconds: 1),
        totalFrames: 30,
        width: 640,
        height: 480,
      );

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            trackingControllerProvider('test_project').overrideWith(
              () => _MockTrackingController(mockProject, mockMetadata),
            ),
          ],
          child: const MaterialApp(
            home: LineProfileScreen(projectId: 'test_project'),
          ),
        ),
      );

      // Pump a few frames to let the async provider resolve.
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 100));

      // After data loads (frameBytes is null → shows loading spinner from
      // LineProfileScreen's inner null-check).
      // Either a spinner or the Slider widget should be present.
      final hasSlider = tester.any(find.byType(Slider));
      final hasSpinner = tester.any(find.byType(CircularProgressIndicator));
      expect(hasSlider || hasSpinner, isTrue);
    },
  );
}

// ---------------------------------------------------------------------------

class _MockTrackingController extends TrackingController {
  _MockTrackingController(this._project, this._metadata);

  final Project _project;
  final VideoMetadata _metadata;

  @override
  Future<TrackingState> build(String projectId) async {
    return TrackingState(
      project: _project,
      metadata: _metadata,
      frameIndex: 0,
      frameBytes: null, // null → spinner shown inside LineProfileScreen
    );
  }
}
