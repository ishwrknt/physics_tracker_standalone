// Responsibility: Record video in-app with optional grid and level overlays.

import 'dart:async';
import 'dart:io';
import 'dart:math';

import 'package:camera/camera.dart';
import 'package:flutter/material.dart';
import 'package:sensors_plus/sensors_plus.dart';

import '../../../../shared/utils/app_storage.dart';

class CameraRecordingScreen extends StatefulWidget {
  const CameraRecordingScreen({super.key});

  @override
  State<CameraRecordingScreen> createState() => _CameraRecordingScreenState();
}

class _CameraRecordingScreenState extends State<CameraRecordingScreen> {
  CameraController? _controller;
  bool _isInitializing = true;
  bool _isRecording = false;
  bool _showGrid = true;
  bool _showLevel = true;
  double _rollDegrees = 0;
  bool _recordActionInProgress = false;
  StreamSubscription<AccelerometerEvent>? _accelSub;

  @override
  void initState() {
    super.initState();
    _initCamera();
    _listenLevel();
  }

  Future<void> _initCamera() async {
    try {
      final List<CameraDescription> cameras = await availableCameras();
      if (cameras.isEmpty) {
        if (!mounted) {
          return;
        }
        setState(() => _isInitializing = false);
        return;
      }
      final CameraDescription selected = cameras.firstWhere(
        (c) => c.lensDirection == CameraLensDirection.back,
        orElse: () => cameras.first,
      );

      final CameraController controller = CameraController(
        selected,
        ResolutionPreset.medium,
        enableAudio: false,
      );
      await controller.initialize();
      if (!mounted) {
        await controller.dispose();
        return;
      }
      setState(() {
        _controller = controller;
        _isInitializing = false;
      });
    } catch (_) {
      if (!mounted) {
        return;
      }
      setState(() => _isInitializing = false);
    }
  }

  void _listenLevel() {
    _accelSub = accelerometerEventStream().listen((event) {
      final double roll = atan2(event.x, event.y) * 180 / pi;
      if (!mounted) {
        return;
      }
      setState(() {
        _rollDegrees = roll.clamp(-45, 45);
      });
    });
  }

  Future<void> _toggleRecording() async {
    final CameraController? controller = _controller;
    if (controller == null ||
        !controller.value.isInitialized ||
        _recordActionInProgress) {
      return;
    }
    _recordActionInProgress = true;

    try {
      if (!_isRecording) {
        if (controller.value.isRecordingVideo) {
          return;
        }
        await controller.startVideoRecording();
        if (!mounted) {
          return;
        }
        setState(() => _isRecording = true);
        return;
      }

      if (!controller.value.isRecordingVideo) {
        if (mounted) {
          setState(() => _isRecording = false);
        }
        return;
      }

      final XFile file = await controller.stopVideoRecording();
      final String safePath = await _persistRecording(file.path);
      if (!mounted) {
        return;
      }
      setState(() => _isRecording = false);
      Navigator.of(context).pop(safePath);
    } on CameraException catch (e) {
      if (!mounted) {
        return;
      }
      setState(() => _isRecording = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Camera error: ${e.description ?? e.code}')),
      );
    } catch (e) {
      if (!mounted) {
        return;
      }
      setState(() => _isRecording = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Recording failed: $e')),
      );
    } finally {
      _recordActionInProgress = false;
    }
  }

  @override
  void dispose() {
    _accelSub?.cancel();
    _controller?.dispose();
    super.dispose();
  }

  Future<String> _persistRecording(String sourcePath) async {
    final File source = File(sourcePath);
    if (!await source.exists()) {
      throw StateError('Recorded file missing after stop.');
    }
    final Directory docs = await getWritableAppDirectory();
    final Directory dir = Directory('${docs.path}/recordings');
    await dir.create(recursive: true);
    final String outPath =
        '${dir.path}/rec_${DateTime.now().millisecondsSinceEpoch}.mp4';
    final File copied = await source.copy(outPath);
    return copied.path;
  }

  @override
  Widget build(BuildContext context) {
    final CameraController? controller = _controller;

    return PopScope(
      canPop: !_isRecording && !_recordActionInProgress,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Record Video'),
          actions: [
            IconButton(
              onPressed: () => setState(() => _showGrid = !_showGrid),
              icon: Icon(_showGrid ? Icons.grid_on : Icons.grid_off),
              tooltip: 'Grid',
            ),
            IconButton(
              onPressed: () => setState(() => _showLevel = !_showLevel),
              icon: Icon(
                  _showLevel ? Icons.straighten : Icons.straighten_outlined),
              tooltip: 'Level',
            ),
          ],
        ),
        body: _isInitializing
            ? const Center(child: CircularProgressIndicator())
            : controller == null || !controller.value.isInitialized
                ? const Center(
                    child: Text('Camera unavailable on this device.'))
                : Stack(
                    children: [
                      Positioned.fill(
                        child: CameraPreview(controller),
                      ),
                      if (_showGrid)
                        const Positioned.fill(
                          child: IgnorePointer(child: _GridOverlay()),
                        ),
                      if (_showLevel)
                        Positioned(
                          top: 16,
                          left: 0,
                          right: 0,
                          child: Center(
                            child: _LevelIndicator(rollDegrees: _rollDegrees),
                          ),
                        ),
                      Positioned(
                        left: 0,
                        right: 0,
                        bottom: 24,
                        child: Center(
                          child: FilledButton.icon(
                            onPressed: _toggleRecording,
                            style: FilledButton.styleFrom(
                              backgroundColor:
                                  _isRecording ? Colors.red : Colors.green,
                            ),
                            icon: Icon(
                              _isRecording
                                  ? Icons.stop
                                  : Icons.fiber_manual_record,
                            ),
                            label: Text(_isRecording ? 'Stop' : 'Record'),
                          ),
                        ),
                      ),
                    ],
                  ),
      ),
    );
  }
}

class _GridOverlay extends StatelessWidget {
  const _GridOverlay();

  @override
  Widget build(BuildContext context) {
    return CustomPaint(
      painter: _GridPainter(),
    );
  }
}

class _GridPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final Paint paint = Paint()
      ..color = const Color(0x88FFFFFF)
      ..strokeWidth = 1.0;

    for (int i = 1; i < 3; i++) {
      final double dx = size.width * i / 3;
      final double dy = size.height * i / 3;
      canvas.drawLine(Offset(dx, 0), Offset(dx, size.height), paint);
      canvas.drawLine(Offset(0, dy), Offset(size.width, dy), paint);
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}

class _LevelIndicator extends StatelessWidget {
  const _LevelIndicator({required this.rollDegrees});

  final double rollDegrees;

  @override
  Widget build(BuildContext context) {
    final double normalized = (rollDegrees / 45).clamp(-1.0, 1.0);
    final double offset = normalized * 52.0;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: const Color(0xAA000000),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Text(
            'Level',
            style: TextStyle(color: Colors.white),
          ),
          const SizedBox(height: 6),
          SizedBox(
            width: 140,
            height: 18,
            child: Stack(
              children: [
                Align(
                  alignment: Alignment.center,
                  child: Container(
                    width: 120,
                    height: 2,
                    color: Colors.white70,
                  ),
                ),
                Positioned(
                  left: 70 + offset - 8,
                  top: 1,
                  child: Container(
                    width: 16,
                    height: 16,
                    decoration: const BoxDecoration(
                      color: Colors.amber,
                      shape: BoxShape.circle,
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 4),
          Text(
            '${rollDegrees.toStringAsFixed(1)}°',
            style: const TextStyle(color: Colors.white70, fontSize: 12),
          ),
        ],
      ),
    );
  }
}
