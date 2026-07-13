// Responsibility: Sample and plot pixel intensity (RGB + Luma) along a user-defined line.
// Adapts responsively to Portrait (stacked Column) and Landscape (split Row) layouts.

import 'dart:io';
import 'dart:math';
import 'dart:typed_data';

import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image/image.dart' as img;
import 'package:share_plus/share_plus.dart';

import '../../../../shared/utils/app_storage.dart';
import '../../../tracking/presentation/providers/tracking_controller.dart';

class ProfilePoint {
  final int index;
  final double x;
  final double y;
  final double r;
  final double g;
  final double b;
  final double luma;

  ProfilePoint({
    required this.index,
    required this.x,
    required this.y,
    required this.r,
    required this.g,
    required this.b,
    required this.luma,
  });
}

class LineProfileScreen extends ConsumerStatefulWidget {
  const LineProfileScreen({super.key, required this.projectId});

  final String projectId;

  @override
  ConsumerState<LineProfileScreen> createState() => _LineProfileScreenState();
}

class _LineProfileScreenState extends ConsumerState<LineProfileScreen> {
  // Coordinates are stored in normalized coordinate space (0.0 to 1.0)
  Offset? _start;
  Offset? _end;
  double _lineWidth = 3;
  List<ProfilePoint> _sampledPoints = const <ProfilePoint>[];

  static const double sidePanelWidth = 320.0;

  @override
  Widget build(BuildContext context) {
    final tracking = ref.watch(trackingControllerProvider(widget.projectId));
    final isLandscape = MediaQuery.orientationOf(context) == Orientation.landscape;

    return Scaffold(
      backgroundColor: Colors.black,
      appBar: isLandscape
          ? null
          : AppBar(
              title: const Text('Line Profile'),
              backgroundColor: Colors.black,
              foregroundColor: Colors.white,
              elevation: 0,
            ),
      body: tracking.when(
        data: (state) {
          final Uint8List? frameBytes = state.frameBytes;
          if (frameBytes == null) {
            return const Center(child: CircularProgressIndicator());
          }

          final double videoWidth = state.metadata.width.toDouble();
          final double videoHeight = state.metadata.height.toDouble();
          final double aspectRatio = videoWidth / (videoHeight > 0 ? videoHeight : 1);

          return LayoutBuilder(
            builder: (context, constraints) {
              final double availableWidth = isLandscape
                  ? max(100.0, constraints.maxWidth - sidePanelWidth)
                  : constraints.maxWidth;
              final double availableHeight = isLandscape
                  ? constraints.maxHeight
                  : max(100.0, constraints.maxHeight * 0.50);

              final double viewAspectRatio = availableWidth / availableHeight;

              double drawWidth, drawHeight;
              if (aspectRatio > viewAspectRatio) {
                drawWidth = availableWidth;
                drawHeight = availableWidth / aspectRatio;
              } else {
                drawHeight = availableHeight;
                drawWidth = availableHeight * aspectRatio;
              }

              if (isLandscape) {
                return _buildLandscapeLayout(
                  frameBytes,
                  drawWidth,
                  drawHeight,
                  availableWidth,
                  availableHeight,
                  state.project.name,
                );
              } else {
                return _buildPortraitLayout(
                  frameBytes,
                  drawWidth,
                  drawHeight,
                  availableWidth,
                  availableHeight,
                  state.project.name,
                );
              }
            },
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text(
                'Failed to load frame for line profiling.',
                style: TextStyle(color: Colors.white),
              ),
              const SizedBox(height: 12),
              ElevatedButton(
                onPressed: () => ref.invalidate(
                  trackingControllerProvider(widget.projectId),
                ),
                child: const Text('Retry'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  // ─── Portrait Layout ───────────────────────────────────────────────────────
  Widget _buildPortraitLayout(
    Uint8List frameBytes,
    double drawWidth,
    double drawHeight,
    double availableWidth,
    double availableHeight,
    String projectName,
  ) {
    return Column(
      children: [
        // Video Canvas Section
        Container(
          width: availableWidth,
          height: availableHeight,
          alignment: Alignment.center,
          color: Colors.black,
          child: GestureDetector(
            onTapDown: (details) =>
                _handleTap(details.localPosition, drawWidth, drawHeight, frameBytes),
            child: SizedBox(
              width: drawWidth,
              height: drawHeight,
              child: Stack(
                children: [
                  Positioned.fill(
                    child: Image.memory(
                      frameBytes,
                      fit: BoxFit.fill,
                      errorBuilder: (context, error, stackTrace) => const SizedBox(),
                    ),
                  ),
                  Positioned.fill(
                    child: CustomPaint(
                      painter: _LineOverlayPainter(
                        start: _start,
                        end: _end,
                        width: _lineWidth,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
        // Line Width Slider
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: _buildWidthSlider(frameBytes),
        ),
        // Export Action
        if (_sampledPoints.isNotEmpty)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
            child: ElevatedButton.icon(
              onPressed: () => _exportCsv(projectName),
              icon: const Icon(Icons.download),
              label: const Text('Export CSV'),
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFFFFB300),
                foregroundColor: Colors.black,
                minimumSize: const Size.fromHeight(40),
              ),
            ),
          ),
        // Chart View
        Expanded(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 4, 16, 12),
            child: _buildChartSection(),
          ),
        ),
      ],
    );
  }

  // ─── Landscape Layout ──────────────────────────────────────────────────────
  Widget _buildLandscapeLayout(
    Uint8List frameBytes,
    double drawWidth,
    double drawHeight,
    double availableWidth,
    double availableHeight,
    String projectName,
  ) {
    return Row(
      children: [
        // Left: Video Canvas
        Expanded(
          child: Container(
            color: Colors.black,
            alignment: Alignment.center,
            child: GestureDetector(
              onTapDown: (details) =>
                  _handleTap(details.localPosition, drawWidth, drawHeight, frameBytes),
              child: SizedBox(
                width: drawWidth,
                height: drawHeight,
                child: Stack(
                  children: [
                    Positioned.fill(
                      child: Image.memory(
                        frameBytes,
                        fit: BoxFit.fill,
                        errorBuilder: (context, error, stackTrace) => const SizedBox(),
                      ),
                    ),
                    Positioned.fill(
                      child: CustomPaint(
                        painter: _LineOverlayPainter(
                          start: _start,
                          end: _end,
                          width: _lineWidth,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
        // Divider
        const VerticalDivider(width: 1, thickness: 1, color: Colors.white24),
        // Right: Control & Chart Sidebar
        SizedBox(
          width: sidePanelWidth,
          child: Container(
            color: const Color(0xFF121212),
            child: SafeArea(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const Text(
                      'Line Profiler',
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 4),
                    const Text(
                      'Tap twice on the video frame to analyze intensity (RGB & Brightness) along the line.',
                      style: TextStyle(color: Colors.white70, fontSize: 12),
                    ),
                    const SizedBox(height: 16),
                    _buildWidthSlider(frameBytes),
                    const SizedBox(height: 16),
                    if (_sampledPoints.isNotEmpty) ...[
                      ElevatedButton.icon(
                        onPressed: () => _exportCsv(projectName),
                        icon: const Icon(Icons.download),
                        label: const Text('Export CSV'),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: const Color(0xFFFFB300),
                          foregroundColor: Colors.black,
                        ),
                      ),
                      const SizedBox(height: 16),
                    ],
                    _buildChartSection(),
                  ],
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }

  // ─── Gesture Tap Handler ───────────────────────────────────────────────────
  void _handleTap(Offset localPos, double drawWidth, double drawHeight, Uint8List frameBytes) {
    final Offset normalized = Offset(
      (localPos.dx / drawWidth).clamp(0.0, 1.0),
      (localPos.dy / drawHeight).clamp(0.0, 1.0),
    );

    setState(() {
      if (_start == null || (_start != null && _end != null)) {
        _start = normalized;
        _end = null;
        _sampledPoints = const <ProfilePoint>[];
      } else {
        _end = normalized;
        _sampledPoints = _sampleLine(
          frameBytes: frameBytes,
          start: _start!,
          end: _end!,
          width: _lineWidth.round(),
        );
      }
    });
  }

  // ─── Slider UI ─────────────────────────────────────────────────────────────
  Widget _buildWidthSlider(Uint8List frameBytes) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text(
              'Sample Path Width',
              style: TextStyle(color: Colors.white, fontSize: 13),
            ),
            Text(
              '${_lineWidth.round()} px',
              style: const TextStyle(
                color: Color(0xFFFFB300),
                fontSize: 13,
                fontWeight: FontWeight.bold,
              ),
            ),
          ],
        ),
        SliderTheme(
          data: SliderTheme.of(context).copyWith(
            activeTrackColor: const Color(0xFFFFB300),
            inactiveTrackColor: Colors.white24,
            thumbColor: const Color(0xFFFFB300),
            overlayColor: const Color(0xFFFFB300).withValues(alpha: 0.2),
          ),
          child: Slider(
            min: 1,
            max: 15,
            divisions: 14,
            value: _lineWidth,
            onChanged: (v) {
              setState(() => _lineWidth = v);
              if (_start != null && _end != null) {
                setState(() {
                  _sampledPoints = _sampleLine(
                    frameBytes: frameBytes,
                    start: _start!,
                    end: _end!,
                    width: _lineWidth.round(),
                  );
                });
              }
            },
          ),
        ),
      ],
    );
  }

  // ─── Chart UI Section ──────────────────────────────────────────────────────
  Widget _buildChartSection() {
    if (_sampledPoints.isEmpty) {
      return Container(
        height: 200,
        decoration: BoxDecoration(
          color: Colors.white.withValues(alpha: 0.05),
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: Colors.white10),
        ),
        child: const Center(
          child: Text(
            'Tap two points on the frame\nto profile intensity.',
            textAlign: TextAlign.center,
            style: TextStyle(color: Colors.white54, fontSize: 13),
          ),
        ),
      );
    }

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            _LegendItem(color: Colors.white, label: 'Luma'),
            const SizedBox(width: 12),
            _LegendItem(color: Colors.redAccent, label: 'Red'),
            const SizedBox(width: 12),
            _LegendItem(color: Colors.greenAccent, label: 'Green'),
            const SizedBox(width: 12),
            _LegendItem(color: Colors.blueAccent, label: 'Blue'),
          ],
        ),
        const SizedBox(height: 12),
        SizedBox(
          height: 200,
          child: LineChart(
            LineChartData(
              gridData: const FlGridData(
                show: true,
                drawVerticalLine: true,
                drawHorizontalLine: true,
              ),
              borderData: FlBorderData(
                show: true,
                border: Border.all(color: Colors.white24),
              ),
              lineBarsData: [
                LineChartBarData(
                  spots: _sampledPoints
                      .map((p) => FlSpot(p.index.toDouble(), p.r))
                      .toList(),
                  color: Colors.redAccent,
                  barWidth: 1.5,
                  dotData: const FlDotData(show: false),
                ),
                LineChartBarData(
                  spots: _sampledPoints
                      .map((p) => FlSpot(p.index.toDouble(), p.g))
                      .toList(),
                  color: Colors.greenAccent,
                  barWidth: 1.5,
                  dotData: const FlDotData(show: false),
                ),
                LineChartBarData(
                  spots: _sampledPoints
                      .map((p) => FlSpot(p.index.toDouble(), p.b))
                      .toList(),
                  color: Colors.blueAccent,
                  barWidth: 1.5,
                  dotData: const FlDotData(show: false),
                ),
                LineChartBarData(
                  spots: _sampledPoints
                      .map((p) => FlSpot(p.index.toDouble(), p.luma))
                      .toList(),
                  color: Colors.white,
                  barWidth: 2.5,
                  dotData: const FlDotData(show: false),
                ),
              ],
              titlesData: FlTitlesData(
                bottomTitles: AxisTitles(
                  axisNameWidget: const Text(
                    'Distance (px sample)',
                    style: TextStyle(color: Colors.white70, fontSize: 10),
                  ),
                  sideTitles: const SideTitles(
                    showTitles: true,
                    reservedSize: 22,
                  ),
                ),
                leftTitles: AxisTitles(
                  axisNameWidget: const Text(
                    'Intensity (0-255)',
                    style: TextStyle(color: Colors.white70, fontSize: 10),
                  ),
                  sideTitles: const SideTitles(
                    showTitles: true,
                    reservedSize: 30,
                  ),
                ),
                rightTitles: const AxisTitles(
                  sideTitles: SideTitles(showTitles: false),
                ),
                topTitles: const AxisTitles(
                  sideTitles: SideTitles(showTitles: false),
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }

  // ─── CSV Exporter & Share ──────────────────────────────────────────────────
  Future<void> _exportCsv(String projectName) async {
    if (_sampledPoints.isEmpty) return;

    final StringBuffer buffer = StringBuffer();
    buffer.writeln('index,pixel_x,pixel_y,red,green,blue,brightness');
    for (final p in _sampledPoints) {
      buffer.writeln([
        p.index,
        p.x.toStringAsFixed(2),
        p.y.toStringAsFixed(2),
        p.r.toStringAsFixed(1),
        p.g.toStringAsFixed(1),
        p.b.toStringAsFixed(1),
        p.luma.toStringAsFixed(1),
      ].join(','));
    }

    final Directory docs = await getWritableAppDirectory();
    final String safeName = projectName.replaceAll(RegExp(r'[^a-zA-Z0-9_-]'), '_');
    final Directory exportDir = Directory('${docs.path}/exports/$safeName');
    await exportDir.create(recursive: true);

    final String timestamp = DateTime.now()
        .toIso8601String()
        .replaceAll(':', '-')
        .replaceAll('.', '-');
    final File file = File('${exportDir.path}/line_profile_$timestamp.csv');
    await file.writeAsString(buffer.toString(), flush: true);

    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('CSV exported: ${file.path}')),
    );

    await SharePlus.instance.share(
      ShareParams(
        files: [XFile(file.path)],
        text: 'PhysicsTracker Line Profile CSV export',
      ),
    );
  }
}

class _LineOverlayPainter extends CustomPainter {
  _LineOverlayPainter({
    required this.start,
    required this.end,
    required this.width,
  });

  final Offset? start;
  final Offset? end;
  final double width;

  @override
  void paint(Canvas canvas, Size size) {
    if (start == null) return;

    final Offset startPt = Offset(start!.dx * size.width, start!.dy * size.height);

    final Paint p = Paint()
      ..color = const Color(0xFFFFB300)
      ..strokeWidth = max(1, width / 2)
      ..style = PaintingStyle.stroke;
    final Paint dot = Paint()
      ..color = const Color(0xFFFFB300)
      ..style = PaintingStyle.fill;

    canvas.drawCircle(startPt, 4, dot);
    if (end != null) {
      final Offset endPt = Offset(end!.dx * size.width, end!.dy * size.height);
      canvas.drawLine(startPt, endPt, p);
      canvas.drawCircle(endPt, 4, dot);
    }
  }

  @override
  bool shouldRepaint(covariant _LineOverlayPainter oldDelegate) {
    return oldDelegate.start != start ||
        oldDelegate.end != end ||
        oldDelegate.width != width;
  }
}

class _LegendItem extends StatelessWidget {
  const _LegendItem({required this.color, required this.label});

  final Color color;
  final String label;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 10,
          height: 10,
          decoration: BoxDecoration(
            color: color,
            shape: BoxShape.circle,
          ),
        ),
        const SizedBox(width: 4),
        Text(
          label,
          style: const TextStyle(color: Colors.white70, fontSize: 11),
        ),
      ],
    );
  }
}

// ─── High-Resolution Pixel Line Sampling Math ────────────────────────────────
List<ProfilePoint> _sampleLine({
  required Uint8List frameBytes,
  required Offset start,
  required Offset end,
  required int width,
}) {
  final img.Image? frame = img.decodeImage(frameBytes);
  if (frame == null) {
    return const <ProfilePoint>[];
  }

  final double x1 = start.dx * frame.width;
  final double y1 = start.dy * frame.height;
  final double x2 = end.dx * frame.width;
  final double y2 = end.dy * frame.height;

  final double dx = x2 - x1;
  final double dy = y2 - y1;
  final double dist = sqrt((dx * dx) + (dy * dy));
  if (dist <= 0) {
    return const <ProfilePoint>[];
  }

  final int samples = max(2, dist.round());
  final double nx = -dy / dist;
  final double ny = dx / dist;
  final int half = max(0, width ~/ 2);

  final List<ProfilePoint> out = <ProfilePoint>[];
  for (int i = 0; i < samples; i++) {
    final double t = i / (samples - 1);
    final double px = x1 + (dx * t);
    final double py = y1 + (dy * t);

    double sumR = 0;
    double sumG = 0;
    double sumB = 0;
    double sumLuma = 0;
    int count = 0;

    for (int w = -half; w <= half; w++) {
      final int sx = (px + (nx * w)).round().clamp(0, frame.width - 1);
      final int sy = (py + (ny * w)).round().clamp(0, frame.height - 1);

      final img.Pixel pixel = frame.getPixel(sx, sy);
      final double r = pixel.r.toDouble();
      final double g = pixel.g.toDouble();
      final double b = pixel.b.toDouble();
      final double luma = ((r * 299) + (g * 587) + (b * 114)) / 1000.0;

      sumR += r;
      sumG += g;
      sumB += b;
      sumLuma += luma;
      count++;
    }

    out.add(ProfilePoint(
      index: i,
      x: px,
      y: py,
      r: count > 0 ? sumR / count : 0,
      g: count > 0 ? sumG / count : 0,
      b: count > 0 ? sumB / count : 0,
      luma: count > 0 ? sumLuma / count : 0,
    ));
  }
  return out;
}
