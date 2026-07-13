// Responsibility: Slider control for selecting a specific frame index.

import 'package:flutter/material.dart';

import '../../../../shared/utils/constants.dart';

class FrameScrubber extends StatelessWidget {
  const FrameScrubber({
    super.key,
    required this.frameIndex,
    required this.totalFrames,
    required this.onChanged,
  });

  final int frameIndex;
  final int totalFrames;
  final ValueChanged<int> onChanged;

  @override
  Widget build(BuildContext context) {
    final int maxIndex = totalFrames > 0 ? totalFrames - 1 : 0;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: kScrubberPadding),
      child: Slider(
        value: frameIndex.clamp(0, maxIndex).toDouble(),
        min: 0,
        max: maxIndex.toDouble(),
        onChanged: totalFrames <= 0
            ? null
            : (value) => onChanged(value.round()),
      ),
    );
  }
}
