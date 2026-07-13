// Responsibility: Ultra-stable video tracking with a professional coordinate mapping engine.
// This implementation ensures that point selection is pixel-perfect even when zoomed or letterboxed.

import 'dart:async';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:video_player/video_player.dart';

import '../../../../shared/widgets/enhanced_video_controls.dart';
import '../../domain/tracking_state.dart';
import '../providers/tracking_controller.dart';
import '../widgets/track_overlay.dart';

class VideoTrackingScreen extends ConsumerWidget {
  const VideoTrackingScreen({super.key, required this.projectId});

  final String projectId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tracking = ref.watch(trackingControllerProvider(projectId));

    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        title: const Text('Physics Tracker'),
        elevation: 0,
        backgroundColor: Colors.black,
        foregroundColor: Colors.white,
      ),
      body: tracking.when(
        data: (state) =>
            _VideoTrackingContent(state: state, projectId: projectId),
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => _ErrorDisplay(projectId: projectId),
      ),
    );
  }
}

class _VideoTrackingContent extends ConsumerStatefulWidget {
  const _VideoTrackingContent({required this.state, required this.projectId});

  final TrackingState state;
  final String projectId;

  @override
  ConsumerState<_VideoTrackingContent> createState() =>
      _VideoTrackingContentState();
}

class _VideoTrackingContentState extends ConsumerState<_VideoTrackingContent> {
  static const double kTopBarHeight = 64.0;
  static const double kBottomBarHeight = 230.0;

  VideoPlayerController? _videoController;
  late TransformationController _transformationController;
  bool _isInitialized = false;

  bool _fasterModeEnabled = false;
  bool _magnifierEnabled = true;
  bool _debugOverlayEnabled = false;
  bool _isTrackingActive = false;

  final ValueNotifier<Offset?> _previewNotifier = ValueNotifier<Offset?>(null);

  @override
  void initState() {
    super.initState();
    _transformationController = TransformationController();
    _initController();
  }

  Future<void> _initController() async {
    // AUTOMATED TEST COMPATIBILITY
    if (kIsWeb || (!Platform.isAndroid && !Platform.isIOS)) {
      if (mounted) setState(() => _isInitialized = true);
      return;
    }

    _videoController =
        VideoPlayerController.file(File(widget.state.project.videoPath));
    try {
      await _videoController!.initialize();
      if (mounted) {
        setState(() {
          _isInitialized = true;
        });
      }
    } catch (e) {
      debugPrint('Video init failed: $e');
      if (mounted) setState(() => _isInitialized = true);
    }
  }

  @override
  void dispose() {
    _videoController?.dispose();
    _transformationController.dispose();
    _previewNotifier.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (!_isInitialized) {
      return const Center(child: CircularProgressIndicator());
    }

    final double videoWidth = widget.state.metadata.width.toDouble();
    final double videoHeight = widget.state.metadata.height.toDouble();
    final double aspectRatio = videoWidth / (videoHeight > 0 ? videoHeight : 1);

    return Column(
      children: [
        // 1. TOP CONTROLS (Chips)
        SizedBox(
          height: kTopBarHeight,
          child: _TopControls(
            fasterMode: _fasterModeEnabled,
            magnifier: _magnifierEnabled,
            debug: _debugOverlayEnabled,
            isTracking: _isTrackingActive,
            onFasterChanged: (v) => setState(() => _fasterModeEnabled = v),
            onMagnifierChanged: (v) => setState(() => _magnifierEnabled = v),
            onDebugChanged: (v) => setState(() => _debugOverlayEnabled = v),
            onTrackingToggle: () =>
                setState(() => _isTrackingActive = !_isTrackingActive),
          ),
        ),

        // 2. STATUS BAR
        _StatusBar(state: widget.state),

        // 3. THE WORKSPACE
        Expanded(
          child: Container(
            color: Colors.black,
            child: LayoutBuilder(
              builder: (context, constraints) {
                // Calculate the exactly letterboxed video area
                final double viewWidth = constraints.maxWidth;
                final double viewHeight = constraints.maxHeight;
                final double viewAspectRatio = viewWidth / viewHeight;

                double drawWidth, drawHeight;
                if (aspectRatio > viewAspectRatio) {
                  drawWidth = viewWidth;
                  drawHeight = viewWidth / aspectRatio;
                } else {
                  drawHeight = viewHeight;
                  drawWidth = viewHeight * aspectRatio;
                }

                final Size drawSize = Size(drawWidth, drawHeight);

                return Center(
                  child: InteractiveViewer(
                    transformationController: _transformationController,
                    minScale: 1.0,
                    maxScale: 20.0,
                    child: SizedBox(
                      width: drawWidth,
                      height: drawHeight,
                      child: Stack(
                        fit: StackFit.expand,
                        children: [
                          // LAYER 1: VIDEO (Hardware or Image fallback)
                          RepaintBoundary(
                            child: _VideoLayer(
                              controller: _videoController,
                              state: widget.state,
                              size: drawSize,
                            ),
                          ),

                          // LAYER 2: TRACKING OVERLAY (Paths and historical points)
                          RepaintBoundary(
                            child: TrackOverlay(
                              points: widget.state.project.tracks.first.points,
                              color: widget.state.project.tracks.first.color,
                              currentFrameIndex: widget.state.frameIndex,
                            ),
                          ),

                          // LAYER 3: PREVIEW/CROSSHAIR (Instant response)
                          ValueListenableBuilder<Offset?>(
                            valueListenable: _previewNotifier,
                            builder: (context, preview, _) {
                              if (preview == null) {
                                return const SizedBox.shrink();
                              }
                              return TrackOverlay(
                                points: const [],
                                color: Colors.white,
                                currentFrameIndex: -1,
                                previewNormalizedOffset: preview,
                              );
                            },
                          ),

                          // LAYER 4: GESTURES (Opaque detector covering ONLY the video area)
                          GestureDetector(
                            key: const Key('video_gesture_detector'),
                            behavior: HitTestBehavior.opaque,
                            onTapDown: (details) {
                              final Offset normalized = Offset(
                                (details.localPosition.dx / drawWidth)
                                    .clamp(0.0, 1.0),
                                (details.localPosition.dy / drawHeight)
                                    .clamp(0.0, 1.0),
                              );
                              _handlePointAddition(normalized);
                            },
                            onPanUpdate: (details) {
                              final Offset normalized = Offset(
                                (details.localPosition.dx / drawWidth)
                                    .clamp(0.0, 1.0),
                                (details.localPosition.dy / drawHeight)
                                    .clamp(0.0, 1.0),
                              );
                              _previewNotifier.value = normalized;
                            },
                            onPanEnd: (_) => _previewNotifier.value = null,
                            child: Container(color: Colors.transparent),
                          ),
                        ],
                      ),
                    ),
                  ),
                );
              },
            ),
          ),
        ),

        // 4. BOTTOM PANEL
        SizedBox(
          height: kBottomBarHeight,
          child: _BottomPanel(
            projectId: widget.projectId,
            state: widget.state,
            videoController: _videoController,
          ),
        ),
      ],
    );
  }

  void _handlePointAddition(Offset normalized) {
    ref.read(trackingControllerProvider(widget.projectId).notifier).addPoint(
          normalizedX: normalized.dx,
          normalizedY: normalized.dy,
        );
    if (_fasterModeEnabled || _isTrackingActive) {
      ref
          .read(trackingControllerProvider(widget.projectId).notifier)
          .nextFrame();
    }
  }
}

class _VideoLayer extends StatelessWidget {
  const _VideoLayer(
      {required this.controller, required this.state, required this.size});
  final VideoPlayerController? controller;
  final TrackingState state;
  final Size size;

  @override
  Widget build(BuildContext context) {
    if (controller != null && controller!.value.isInitialized) {
      return VideoPlayer(controller!);
    }
    if (state.frameBytes != null) {
      return Image.memory(
        state.frameBytes!,
        width: size.width,
        height: size.height,
        fit: BoxFit
            .fill, // Using fill because parent SizedBox is already aspect-correct
      );
    }
    return const ColoredBox(color: Colors.black);
  }
}

class _TopControls extends StatelessWidget {
  const _TopControls({
    required this.fasterMode,
    required this.magnifier,
    required this.debug,
    required this.isTracking,
    required this.onFasterChanged,
    required this.onMagnifierChanged,
    required this.onDebugChanged,
    required this.onTrackingToggle,
  });

  final bool fasterMode, magnifier, debug, isTracking;
  final ValueChanged<bool> onFasterChanged, onMagnifierChanged, onDebugChanged;
  final VoidCallback onTrackingToggle;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      children: [
        _ControlIcon(
            icon: Icons.bolt,
            active: fasterMode,
            onToggle: () => onFasterChanged(!fasterMode),
            color: Colors.yellow),
        _ControlIcon(
            icon: isTracking ? Icons.stop_circle : Icons.play_circle_fill,
            active: isTracking,
            onToggle: onTrackingToggle,
            color: isTracking ? Colors.red : Colors.green),
        _ControlIcon(
            icon: Icons.zoom_in,
            active: magnifier,
            onToggle: () => onMagnifierChanged(!magnifier),
            color: Colors.blue),
        _ControlIcon(
            icon: Icons.bug_report,
            active: debug,
            onToggle: () => onDebugChanged(!debug),
            color: Colors.purple),
      ],
    );
  }
}

class _ControlIcon extends StatelessWidget {
  const _ControlIcon(
      {required this.icon,
      required this.active,
      required this.onToggle,
      required this.color});
  final IconData icon;
  final bool active;
  final VoidCallback onToggle;
  final Color color;
  @override
  Widget build(BuildContext context) {
    return IconButton(
      icon: Icon(icon, color: active ? color : Colors.white70),
      onPressed: onToggle,
      style: IconButton.styleFrom(
          backgroundColor: active ? color.withValues(alpha: 0.1) : null),
    );
  }
}

class _StatusBar extends StatelessWidget {
  const _StatusBar({required this.state});
  final TrackingState state;
  @override
  Widget build(BuildContext context) {
    final track = state.project.tracks.first;
    return Container(
      height: 40,
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      padding: const EdgeInsets.symmetric(horizontal: 16),
      decoration: BoxDecoration(
          color: Colors.white.withValues(alpha: 0.05),
          borderRadius: BorderRadius.circular(20)),
      child: Row(
        children: [
          const Icon(Icons.gps_fixed, color: Colors.white70, size: 16),
          const SizedBox(width: 8),
          Text('${track.points.length} points',
              style: const TextStyle(color: Colors.white70, fontSize: 11)),
          const Spacer(),
          Text('FRAME ${state.frameIndex + 1}',
              style: const TextStyle(
                  color: Colors.white70,
                  fontSize: 11,
                  fontWeight: FontWeight.bold)),
        ],
      ),
    );
  }
}

class _BottomPanel extends ConsumerWidget {
  const _BottomPanel(
      {required this.projectId, required this.state, this.videoController});
  final String projectId;
  final TrackingState state;
  final VideoPlayerController? videoController;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final notifier = ref.read(trackingControllerProvider(projectId).notifier);

    return EnhancedVideoControls(
      videoController: videoController,
      frameIndex: state.frameIndex,
      totalFrames: state.metadata.totalFrames,
      fps: state.metadata.fps > 0 ? state.metadata.fps : 30.0,
      isPlaying: state.isPlaying,
      onPreviousFrame:
          state.frameIndex > 0 ? () => notifier.previousFrame() : null,
      onNextFrame: state.frameIndex < state.metadata.totalFrames - 1
          ? () => notifier.nextFrame()
          : null,
      onPlayPause: () => notifier.togglePlay(),
      onRewind: () {
        final int targetFrame = (state.frameIndex - (state.metadata.fps * 10))
            .clamp(0, state.metadata.totalFrames - 1)
            .round();
        notifier.goToFrame(targetFrame);
        videoController?.seekTo(Duration(
            milliseconds: (targetFrame / state.metadata.fps * 1000).round()));
      },
      onForward: () {
        final int targetFrame = (state.frameIndex + (state.metadata.fps * 10))
            .clamp(0, state.metadata.totalFrames - 1)
            .round();
        notifier.goToFrame(targetFrame);
        videoController?.seekTo(Duration(
            milliseconds: (targetFrame / state.metadata.fps * 1000).round()));
      },
      onRestart: () {
        notifier.goToFrame(0);
        videoController?.seekTo(Duration.zero);
      },
      onGoToFrame: (idx) {
        notifier.goToFrame(idx);
        final fps = state.metadata.fps > 0 ? state.metadata.fps : 30.0;
        videoController
            ?.seekTo(Duration(milliseconds: (idx / fps * 1000).round()));
      },
    );
  }
}

class _ErrorDisplay extends ConsumerWidget {
  const _ErrorDisplay({required this.projectId});
  final String projectId;
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.error_outline, color: Colors.red, size: 48),
          const SizedBox(height: 16),
          const Text('Video data missing or corrupt',
              style: TextStyle(color: Colors.white70)),
          const SizedBox(height: 24),
          ElevatedButton(
              onPressed: () =>
                  ref.invalidate(trackingControllerProvider(projectId)),
              child: const Text('Retry')),
        ],
      ),
    );
  }
}
