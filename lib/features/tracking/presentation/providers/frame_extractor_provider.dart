// Responsibility: Provide a shared FrameExtractor instance.

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/frame_extractor.dart';

final frameExtractorProvider = Provider<FrameExtractor>((ref) {
  return FrameExtractor();
});
