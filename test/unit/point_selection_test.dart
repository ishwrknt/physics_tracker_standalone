import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:physics_tracker/features/tracking/presentation/screens/video_tracking_screen.dart';
import 'package:physics_tracker/features/tracking/presentation/providers/tracking_controller.dart';
import 'package:physics_tracker/features/tracking/domain/tracking_state.dart';
import 'package:physics_tracker/features/tracking/domain/video_metadata.dart';
import 'package:physics_tracker/shared/models/project.dart';
import 'package:physics_tracker/shared/models/track.dart';

void main() {
  testWidgets('End-to-end: Pixel selection on video overlay works', (WidgetTester tester) async {
    final mockProject = Project(
      id: 'test_project',
      name: 'Test',
      videoPath: 'dummy.mp4',
      createdAt: DateTime.now(),
      tracks: [Track(id: 't1', label: 'Object', color: Colors.red, points: [])],
    );

    final mockMetadata = VideoMetadata(
      fps: 30,
      duration: const Duration(seconds: 1),
      totalFrames: 30,
      width: 1920,
      height: 1080,
    );

    final mockController = MockTrackingController(mockProject, mockMetadata);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          trackingControllerProvider('test_project').overrideWith(
            () => mockController,
          ),
        ],
        child: const MaterialApp(
          home: VideoTrackingScreen(projectId: 'test_project'),
        ),
      ),
    );

    await tester.pump();
    await tester.pump(); 
    
    // VERIFY: Check if circular progress indicator is gone (init finished)
    expect(find.byType(CircularProgressIndicator), findsNothing);

    // VERIFY: Find the gesture detector
    final gestureDetector = find.byKey(const Key('video_gesture_detector'));
    expect(gestureDetector, findsOneWidget);

    // ACTION: Tap using a simulated Down/Up sequence for maximum reliability
    final center = tester.getCenter(gestureDetector);
    final gesture = await tester.startGesture(center);
    await gesture.up();
    
    // Pump to handle the tap event
    await tester.pump();

    // Verify point addition
    expect(mockController.addedPoints.length, 1, reason: 'Tap should add exactly one point to controller');
    expect(mockController.addedPoints.first.dx, closeTo(0.5, 0.05));
    expect(mockController.addedPoints.first.dy, closeTo(0.5, 0.05));
  });
}

class MockTrackingController extends TrackingController {
  final Project initialProject;
  final VideoMetadata metadata;
  final List<Offset> addedPoints = [];

  MockTrackingController(this.initialProject, this.metadata);

  @override
  Future<TrackingState> build(String projectId) async {
    return TrackingState(
      project: initialProject,
      metadata: metadata,
      frameIndex: 0,
      frameBytes: null,
    );
  }

  @override
  Future<void> addPoint({required double normalizedX, required double normalizedY}) async {
    addedPoints.add(Offset(normalizedX, normalizedY));
  }
  
  @override
  void clearWarning() {}
}
