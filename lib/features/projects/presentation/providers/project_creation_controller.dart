// Responsibility: Create new projects with initial data and thumbnails.

import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import 'package:uuid/uuid.dart';

import '../../../../shared/models/project.dart';
import '../../../../shared/models/project_summary.dart';
import '../../../../shared/models/track.dart';
import '../../../../shared/models/track_point.dart';
import '../../../../shared/utils/app_storage.dart';
import '../../../../shared/utils/constants.dart';
import '../../../tracking/presentation/providers/frame_extractor_provider.dart';
import '../../../tracking/domain/video_metadata.dart';
import '../../../tracking/data/frame_extractor.dart';
import 'project_repository_provider.dart';

part 'project_creation_controller.g.dart';

const Color _defaultTrackColor = Color(0xFFEF6C00);
const String _defaultTrackLabel = 'Object';

@riverpod
class ProjectCreationController extends _$ProjectCreationController {
  @override
  Future<void> build() async {}

  Future<String> createProject({
    required String name,
    required String videoPath,
  }) async {
    if (videoPath.trim().isEmpty) {
      throw StateError('A local video is required for a new project.');
    }

    final repo = ref.read(projectRepositoryProvider);
    final FrameExtractor extractor = ref.read(frameExtractorProvider);
    final String id = const Uuid().v4();
    final String storedVideoPath = await persistVideoToAppStorage(videoPath);

    final Track defaultTrack = Track(
      id: const Uuid().v4(),
      label: _defaultTrackLabel,
      color: _defaultTrackColor,
      points: const <TrackPoint>[],
    );

    final VideoMetadata metadata =
        await extractor.loadMetadata(storedVideoPath);

    String? thumbnailPath;
    try {
      final Uint8List bytes = await extractor.extractFrame(
        videoPath: storedVideoPath,
        frameIndex: kThumbnailFrameIndex,
        fps: metadata.fps,
      );
      final Directory docsDir = await getWritableAppDirectory();
      final File output = File('${docsDir.path}/thumb_$id.png');
      await output.writeAsBytes(bytes, flush: true);
      thumbnailPath = output.path;
    } catch (_) {
      thumbnailPath = null;
    }

    final Project project = Project(
      id: id,
      name: name,
      videoPath: storedVideoPath,
      createdAt: DateTime.now(),
      calibration: null,
      tracks: <Track>[defaultTrack],
      lastFrameIndex: 0,
      startFrame: 0,
      endFrame: metadata.totalFrames > 0 ? metadata.totalFrames - 1 : 0,
    );

    final ProjectSummary summary = ProjectSummary(
      id: id,
      name: name,
      videoPath: storedVideoPath,
      createdAt: project.createdAt,
      thumbnailPath: thumbnailPath,
    );

    await repo.createProject(project, summary);
    return id;
  }
}
