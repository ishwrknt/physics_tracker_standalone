// Responsibility: Verify the frame scrubber renders and responds to input.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:physics_tracker/features/tracking/presentation/widgets/frame_scrubber.dart';

void main() {
  testWidgets('renders with provided frame index', (tester) async {
    // Happy path: initial value matches the provided frame index.
    await tester.pumpWidget(
      const MaterialApp(
        home: Scaffold(
          body: FrameScrubber(
            frameIndex: 5,
            totalFrames: 10,
            onChanged: _noop,
          ),
        ),
      ),
    );

    final Slider slider = tester.widget(find.byType(Slider));
    expect(slider.value, 5);
  });

  testWidgets('calls onChanged when dragged', (tester) async {
    // Edge case: user interaction updates the frame index.
    int changedValue = -1;
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: FrameScrubber(
            frameIndex: 0,
            totalFrames: 10,
            onChanged: (value) => changedValue = value,
          ),
        ),
      ),
    );

    await tester.drag(find.byType(Slider), const Offset(200, 0));
    await tester.pumpAndSettle();

    expect(changedValue, isNot(-1));
  });

  testWidgets('disables when totalFrames is zero', (tester) async {
    // Error case: scrubber should be disabled when no frames exist.
    await tester.pumpWidget(
      const MaterialApp(
        home: Scaffold(
          body: FrameScrubber(
            frameIndex: 0,
            totalFrames: 0,
            onChanged: _noop,
          ),
        ),
      ),
    );

    final Slider slider = tester.widget(find.byType(Slider));
    expect(slider.onChanged, isNull);
  });
}

void _noop(int _) {}
