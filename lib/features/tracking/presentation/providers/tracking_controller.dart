// Responsibility: Coordinate project loading, frame extraction, and point updates.

import 'dart:async';
import 'dart:io';
import 'dart:math' as math;
import 'dart:typed_data';

import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../projects/presentation/providers/project_repository_provider.dart';
import '../../../../shared/models/project.dart';
import '../../../../shared/models/track.dart';
import '../../../../shared/models/track_point.dart';
import '../../../../shared/utils/app_storage.dart';
import '../../data/frame_extractor.dart';
import '../../domain/tracking_state.dart';
import '../../domain/video_metadata.dart';
import 'frame_extractor_provider.dart';

part 'tracking_controller.g.dart';

@riverpod
class TrackingController extends _$TrackingController {
  final Map<int, Uint8List> _frameCache = <int, Uint8List>{};
  final int _maxCacheSize = 100;

  @override
  Future<TrackingState> build(String projectId) async {
    ref.onDispose(() {
      _frameCache.clear();
    });

    final repo = ref.read(projectRepositoryProvider);
    final FrameExtractor extractor = ref.read(frameExtractorProvider);
    final Project project = await repo.loadProject(projectId);
    return _loadTrackingState(project: project, extractor: extractor);
  }

  void togglePlay() {
    final TrackingState? current = state.valueOrNull;
    if (current == null) {
      return;
    }

    if (current.isPlaying) {
      _stopPlayback();
    } else {
      _startPlayback();
    }
  }

  void clearWarning() {
    final TrackingState? current = state.valueOrNull;
    if (current != null) {
      state = AsyncData(current.copyWith(warningMessage: null));
    }
  }

  void _startPlayback() {
    final TrackingState? current = state.valueOrNull;
    if (current == null || current.isPlaying) {
      return;
    }
    state = AsyncData(current.copyWith(isPlaying: true, warningMessage: null));
  }

  void _stopPlayback() {
    final TrackingState? current = state.valueOrNull;
    if (current != null) {
      state = AsyncData(current.copyWith(isPlaying: false));
    }
  }

  Future<void> setStartFrame(int frameIndex) async {
    final TrackingState? current = state.valueOrNull;
    if (current == null) {
      return;
    }
    final Project updatedProject = current.project.copyWith(startFrame: frameIndex);
    await _persistProject(updatedProject);
    state = AsyncData(current.copyWith(project: updatedProject));
  }

  Future<void> setEndFrame(int frameIndex) async {
    final TrackingState? current = state.valueOrNull;
    if (current == null) {
      return;
    }
    final Project updatedProject = current.project.copyWith(endFrame: frameIndex);
    await _persistProject(updatedProject);
    state = AsyncData(current.copyWith(project: updatedProject));
  }

  Future<void> goToFrame(int frameIndex) async {
    final TrackingState? current = state.valueOrNull;
    if (current == null) {
      return;
    }
    final int clampedIndex = _clampFrame(
      frameIndex,
      current.metadata.totalFrames,
    );

    // Check Cache first
    if (_frameCache.containsKey(clampedIndex)) {
      final Project updatedProject = await _saveProjectFrameIndex(
        current.project,
        clampedIndex,
      );
      state = AsyncData(current.copyWith(
        project: updatedProject,
        frameIndex: clampedIndex,
        frameBytes: _frameCache[clampedIndex],
        isFrameLoading: false,
        warningMessage: null,
      ));
      return;
    }

    if (clampedIndex == current.frameIndex && current.frameBytes != null) {
      return;
    }

    // Only show loading spinner if we're not playing or if it's the first load
    final bool shouldShowSpinner = !current.isPlaying || current.frameBytes == null;

    if (shouldShowSpinner) {
      state = AsyncData(current.copyWith(
        isFrameLoading: true,
        warningMessage: null,
      ));
    }

    try {
      final Uint8List frameBytes = await ref.read(frameExtractorProvider).extractFrame(
            videoPath: current.project.videoPath,
            frameIndex: clampedIndex,
            fps: current.metadata.fps,
            rotation: current.metadata.rotation,
          );
      
      // Update Cache
      if (_frameCache.length >= _maxCacheSize) {
        _frameCache.remove(_frameCache.keys.first);
      }
      _frameCache[clampedIndex] = frameBytes;

      final Project updatedProject = await _saveProjectFrameIndex(
            current.project,
            clampedIndex,
          );

      final TrackingState? latest = state.valueOrNull;
      if (latest == null) return;

      state = AsyncData(latest.copyWith(
        project: updatedProject,
        frameIndex: clampedIndex,
        frameBytes: frameBytes,
        isFrameLoading: false,
      ));
    } catch (e) {
      final TrackingState? latest = state.valueOrNull;
      if (latest == null) return;
      state = AsyncData(latest.copyWith(
        isFrameLoading: false,
        warningMessage: 'Could not load frame ${clampedIndex + 1}.',
      ));
      rethrow;
    }
  }

  Future<void> nextFrame() async {
    final TrackingState? current = state.valueOrNull;
    if (current == null) {
      return;
    }
    int next = current.frameIndex + 1;
    if (next > current.project.endFrame) {
      next = current.project.startFrame;
      if (current.isPlaying) {
        _stopPlayback();
        return;
      }
    }
    await goToFrame(next);
  }

  Future<void> previousFrame() async {
    final TrackingState? current = state.valueOrNull;
    if (current == null) {
      return;
    }
    int prev = current.frameIndex - 1;
    if (prev < current.project.startFrame) {
      prev = current.project.endFrame;
    }
    await goToFrame(prev);
  }

  Future<void> addPoint({
    required double normalizedX,
    required double normalizedY,
  }) async {
    final TrackingState? current = state.valueOrNull;
    if (current == null) {
      return;
    }

    final Track activeTrack = _activeTrack(current.project);
    final double clampedX = normalizedX.clamp(0.0, 1.0);
    final double clampedY = normalizedY.clamp(0.0, 1.0);
    final TrackPoint newPoint = TrackPoint(
      frameIndex: current.frameIndex,
      pixelX: clampedX * current.metadata.width,
      pixelY: clampedY * current.metadata.height,
      normalizedX: clampedX,
      normalizedY: clampedY,
    );

    final List<TrackPoint> updatedPoints = List<TrackPoint>.from(activeTrack.points);
    final int existingIndex = updatedPoints.indexWhere((p) => p.frameIndex == current.frameIndex);
    if (existingIndex >= 0) {
      updatedPoints[existingIndex] = newPoint;
    } else {
      updatedPoints.add(newPoint);
    }
    updatedPoints.sort((a, b) => a.frameIndex.compareTo(b.frameIndex));

    final Track updatedTrack = activeTrack.copyWith(points: updatedPoints);
    final Project updatedProject = _replaceTrack(current.project, updatedTrack);
    await _persistProject(updatedProject);

    state = AsyncData(current.copyWith(
      project: updatedProject,
      warningMessage: null,
      errorMessage: null,
    ));
  }

  Future<void> replaceVideo(String videoPath) async {
    final TrackingState? current = state.valueOrNull;
    if (current == null) {
      return;
    }
    state = AsyncData(current.copyWith(
      isFrameLoading: true,
      warningMessage: null,
      errorMessage: null,
    ));

    try {
      final String storedVideoPath = await persistVideoToAppStorage(videoPath);
      final Project updatedProject = current.project.copyWith(
        videoPath: storedVideoPath,
      );
      await _persistProject(updatedProject);

      final TrackingState loaded = await _loadTrackingState(
        project: updatedProject,
        extractor: ref.read(frameExtractorProvider),
      );
      state = AsyncData(loaded);
    } catch (error) {
      state = AsyncData(current.copyWith(
        isFrameLoading: false,
        errorMessage: 'Could not open the selected video.',
        warningMessage: error.toString(),
      ));
    }
  }

  Track _activeTrack(Project project) {
    if (project.tracks.isEmpty) {
      throw StateError('Project has no tracks');
    }
    return project.tracks.first;
  }

  Project _replaceTrack(Project project, Track track) {
    final List<Track> tracks = List<Track>.from(project.tracks);
    tracks[0] = track;
    return project.copyWith(tracks: tracks);
  }

  Future<void> _persistProject(Project project) async {
    final repo = ref.read(projectRepositoryProvider);
    await repo.saveProject(project);
  }

  int _clampFrame(int value, int totalFrames) {
    if (totalFrames <= 0) {
      return 0;
    }
    if (value < 0) {
      return 0;
    }
    if (value >= totalFrames) {
      return totalFrames - 1;
    }
    return value;
  }

  Future<TrackingState> _loadTrackingState({
    required Project project,
    required FrameExtractor extractor,
  }) async {
    final File videoFile = File(project.videoPath);
    if (!await videoFile.exists()) {
      return TrackingState(
        project: project,
        metadata: const VideoMetadata(
          fps: 0,
          duration: Duration.zero,
          totalFrames: 0,
        ),
        frameIndex: 0,
        frameBytes: null,
        errorMessage: 'Video file missing. Pick the video again to continue.',
      );
    }

    try {
      final VideoMetadata metadata = await extractor.loadMetadata(project.videoPath);

      // Fix endFrame for legacy projects or if it's 0 incorrectly
      Project currentProject = project;
      if (currentProject.endFrame == 0 && metadata.totalFrames > 1) {
        currentProject = currentProject.copyWith(endFrame: math.max(0, metadata.totalFrames - 1));
        await _persistProject(currentProject);
      }

      final int safeFrameIndex = _clampFrame(currentProject.lastFrameIndex, metadata.totalFrames);
      final Uint8List frameBytes = await extractor.extractFrame(
        videoPath: currentProject.videoPath,
        frameIndex: safeFrameIndex,
        fps: metadata.fps,
      );

      final Project syncedProject = currentProject.lastFrameIndex == safeFrameIndex
          ? currentProject
          : await _saveProjectFrameIndex(currentProject, safeFrameIndex);

      return TrackingState(
        project: syncedProject,
        metadata: metadata,
        frameIndex: safeFrameIndex,
        frameBytes: frameBytes,
      );
    } catch (error) {
      return TrackingState(
        project: project,
        metadata: const VideoMetadata(
          fps: 0,
          duration: Duration.zero,
          totalFrames: 0,
        ),
        frameIndex: 0,
        frameBytes: null,
        errorMessage: 'Could not load this video for analysis.',
        warningMessage: error.toString(),
      );
    }
  }

  Future<Project> _saveProjectFrameIndex(
      Project project, int frameIndex) async {
    if (project.lastFrameIndex == frameIndex) {
      return project;
    }

    final Project updatedProject = project.copyWith(lastFrameIndex: frameIndex);
    await _persistProject(updatedProject);
    return updatedProject;
  }
}
