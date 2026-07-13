// Responsibility: Render interactive kinematics graphs and jump to frame on tap.

import 'dart:math';

import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/curve_fitting.dart';
import '../../../tracking/presentation/providers/tracking_controller.dart';
import '../../../../../shared/models/calibration_data.dart';
import '../../../../../shared/models/track_point.dart';
import '../../../../../shared/utils/calibration_math.dart';

class GraphsScreen extends ConsumerWidget {
  const GraphsScreen({super.key, required this.projectId});

  final String projectId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tracking = ref.watch(trackingControllerProvider(projectId));
    final controller = ref.read(trackingControllerProvider(projectId).notifier);

    return Scaffold(
      appBar: AppBar(title: const Text('Graphs')),
      body: tracking.when(
        data: (state) {
          final List<TrackPoint> points = List<TrackPoint>.from(
            state.project.tracks.first.points,
          )..sort((a, b) => a.frameIndex.compareTo(b.frameIndex));
          if (points.isEmpty) {
            return const Center(
                child: Text('No points yet. Tap on the video.'));
          }

          final double fps = state.metadata.fps > 0 ? state.metadata.fps : 30.0;
          final CalibrationData? calibration = state.project.calibration;
          final _KinematicsData data = _buildKinematics(
            points: points,
            fps: fps,
            calibration: calibration,
          );

          final String positionUnit = calibration == null ? 'px' : 'm';
          final String velocityUnit = calibration == null ? 'px/s' : 'm/s';
          final String accelerationUnit =
              calibration == null ? 'px/s²' : 'm/s²';

          return DefaultTabController(
            length: 4,
            child: Column(
              children: [
                const TabBar(
                  tabs: [
                    Tab(text: 'x-t'),
                    Tab(text: 'y-t'),
                    Tab(text: 'v-t'),
                    Tab(text: 'a-t'),
                  ],
                ),
                Expanded(
                  child: TabBarView(
                    children: [
                      _GraphPanel(
                        title: 'x vs t',
                        yLabel: 'x ($positionUnit)',
                        samples: data.xSamples,
                        color: Colors.blue,
                        onFrameSelected: (frame) => controller.goToFrame(frame),
                      ),
                      _GraphPanel(
                        title: 'y vs t',
                        yLabel: 'y ($positionUnit)',
                        samples: data.ySamples,
                        color: Colors.teal,
                        onFrameSelected: (frame) => controller.goToFrame(frame),
                      ),
                      _GraphPanel(
                        title: 'v vs t',
                        yLabel: 'v ($velocityUnit)',
                        samples: data.vSamples,
                        color: Colors.orange,
                        onFrameSelected: (frame) => controller.goToFrame(frame),
                      ),
                      _GraphPanel(
                        title: 'a vs t',
                        yLabel: 'a ($accelerationUnit)',
                        samples: data.aSamples,
                        color: Colors.red,
                        onFrameSelected: (frame) => controller.goToFrame(frame),
                      ),
                    ],
                  ),
                ),
                const Padding(
                  padding: EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  child: Text(
                    'Tap any graph point to jump to that frame in Video tab.',
                  ),
                ),
              ],
            ),
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text('Failed to load graphs.'),
              const SizedBox(height: 12),
              ElevatedButton(
                onPressed: () =>
                    ref.invalidate(trackingControllerProvider(projectId)),
                child: const Text('Retry'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _GraphPanel extends StatefulWidget {
  const _GraphPanel({
    required this.title,
    required this.yLabel,
    required this.samples,
    required this.color,
    required this.onFrameSelected,
  });

  final String title;
  final String yLabel;
  final List<_GraphSample> samples;
  final Color color;
  final ValueChanged<int> onFrameSelected;

  @override
  State<_GraphPanel> createState() => _GraphPanelState();
}

class _GraphPanelState extends State<_GraphPanel> {
  CurveFitType _fitType = CurveFitType.linear;

  @override
  Widget build(BuildContext context) {
    if (widget.samples.isEmpty) {
      return Center(child: Text('Need more points for ${widget.title}.'));
    }

    final List<FlSpot> spots = widget.samples
        .map((s) => FlSpot(s.timeSeconds, s.value))
        .toList(growable: false);
    final List<double> x =
        widget.samples.map((e) => e.timeSeconds).toList(growable: false);
    final List<double> y =
        widget.samples.map((e) => e.value).toList(growable: false);
    final CurveFitResult? fit = fitCurve(type: _fitType, x: x, y: y);

    double minY = widget.samples.first.value;
    double maxY = widget.samples.first.value;
    for (final _GraphSample s in widget.samples) {
      minY = min(minY, s.value);
      maxY = max(maxY, s.value);
    }
    List<FlSpot> fitSpots = const <FlSpot>[];
    if (fit != null) {
      fitSpots = _buildFitSpots(
        fit: fit,
        minX: widget.samples.first.timeSeconds,
        maxX: widget.samples.last.timeSeconds,
      );
      for (final FlSpot fs in fitSpots) {
        minY = min(minY, fs.y);
        maxY = max(maxY, fs.y);
      }
    }
    if ((maxY - minY).abs() < 1e-9) {
      minY -= 1;
      maxY += 1;
    }

    return Padding(
      padding: const EdgeInsets.all(12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  widget.title,
                  style: Theme.of(context).textTheme.titleMedium,
                ),
              ),
              DropdownButton<CurveFitType>(
                value: _fitType,
                onChanged: (value) {
                  if (value == null) {
                    return;
                  }
                  setState(() => _fitType = value);
                },
                items: const [
                  DropdownMenuItem(
                    value: CurveFitType.linear,
                    child: Text('Linear'),
                  ),
                  DropdownMenuItem(
                    value: CurveFitType.quadratic,
                    child: Text('Quadratic'),
                  ),
                  DropdownMenuItem(
                    value: CurveFitType.sinusoidal,
                    child: Text('Sinusoidal'),
                  ),
                  DropdownMenuItem(
                    value: CurveFitType.exponential,
                    child: Text('Exponential'),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 8),
          Text(widget.yLabel, style: Theme.of(context).textTheme.bodySmall),
          const SizedBox(height: 4),
          Text(
            fit == null
                ? 'Fit unavailable for current data'
                : '${fit.equation}    R² = ${fit.rSquared.toStringAsFixed(4)}',
            style: Theme.of(context).textTheme.bodySmall,
          ),
          const SizedBox(height: 8),
          Expanded(
            child: LineChart(
              LineChartData(
                minX: widget.samples.first.timeSeconds,
                maxX: widget.samples.last.timeSeconds,
                minY: minY,
                maxY: maxY,
                gridData: const FlGridData(show: true),
                borderData: FlBorderData(show: true),
                titlesData: FlTitlesData(
                  bottomTitles: AxisTitles(
                    sideTitles: SideTitles(
                      showTitles: true,
                      reservedSize: 28,
                      getTitlesWidget: (value, _) {
                        return Text(value.toStringAsFixed(1));
                      },
                    ),
                    axisNameWidget: const Text('t (s)'),
                  ),
                  leftTitles: AxisTitles(
                    sideTitles: SideTitles(
                      showTitles: true,
                      reservedSize: 44,
                      getTitlesWidget: (value, _) {
                        return Text(value.toStringAsFixed(1));
                      },
                    ),
                  ),
                  rightTitles: const AxisTitles(
                    sideTitles: SideTitles(showTitles: false),
                  ),
                  topTitles: const AxisTitles(
                    sideTitles: SideTitles(showTitles: false),
                  ),
                ),
                lineBarsData: [
                  LineChartBarData(
                    spots: spots,
                    isCurved: false,
                    barWidth: 2,
                    color: widget.color,
                    dotData: const FlDotData(show: true),
                  ),
                  if (fitSpots.isNotEmpty)
                    LineChartBarData(
                      spots: fitSpots,
                      isCurved: false,
                      barWidth: 2,
                      color: widget.color.withValues(alpha: 0.45),
                      dotData: const FlDotData(show: false),
                    ),
                ],
                lineTouchData: LineTouchData(
                  handleBuiltInTouches: true,
                  touchCallback: (event, response) {
                    final LineBarSpot? spot =
                        response?.lineBarSpots?.isNotEmpty == true
                            ? response!.lineBarSpots!.first
                            : null;
                    if (spot == null) {
                      return;
                    }
                    final int index = spot.spotIndex;
                    if (index < 0 || index >= widget.samples.length) {
                      return;
                    }
                    widget.onFrameSelected(widget.samples[index].frameIndex);
                  },
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

List<FlSpot> _buildFitSpots({
  required CurveFitResult fit,
  required double minX,
  required double maxX,
}) {
  if (maxX <= minX) {
    return <FlSpot>[FlSpot(minX, evaluateCurve(fit, minX))];
  }
  const int count = 80;
  final List<FlSpot> spots = <FlSpot>[];
  for (int i = 0; i <= count; i++) {
    final double x = minX + ((maxX - minX) * i / count);
    final double y = evaluateCurve(fit, x);
    if (y.isFinite) {
      spots.add(FlSpot(x, y));
    }
  }
  return spots;
}

class _GraphSample {
  const _GraphSample({
    required this.frameIndex,
    required this.timeSeconds,
    required this.value,
  });

  final int frameIndex;
  final double timeSeconds;
  final double value;
}

class _KinematicsData {
  const _KinematicsData({
    required this.xSamples,
    required this.ySamples,
    required this.vSamples,
    required this.aSamples,
  });

  final List<_GraphSample> xSamples;
  final List<_GraphSample> ySamples;
  final List<_GraphSample> vSamples;
  final List<_GraphSample> aSamples;
}

class _FramePoint {
  const _FramePoint({
    required this.frameIndex,
    required this.timeSeconds,
    required this.x,
    required this.y,
  });

  final int frameIndex;
  final double timeSeconds;
  final double x;
  final double y;
}

_KinematicsData _buildKinematics({
  required List<TrackPoint> points,
  required double fps,
  required CalibrationData? calibration,
}) {
  final List<_FramePoint> framePoints = points.map((p) {
    if (calibration == null) {
      return _FramePoint(
        frameIndex: p.frameIndex,
        timeSeconds: p.frameIndex / fps,
        x: p.pixelX,
        y: p.pixelY,
      );
    }
    final world = pixelToWorld(
      calibration: calibration,
      pixelX: p.pixelX,
      pixelY: p.pixelY,
    );
    return _FramePoint(
      frameIndex: p.frameIndex,
      timeSeconds: p.frameIndex / fps,
      x: world.x,
      y: world.y,
    );
  }).toList(growable: false);

  final List<_GraphSample> xSamples = framePoints
      .map((p) => _GraphSample(
            frameIndex: p.frameIndex,
            timeSeconds: p.timeSeconds,
            value: p.x,
          ))
      .toList(growable: false);
  final List<_GraphSample> ySamples = framePoints
      .map((p) => _GraphSample(
            frameIndex: p.frameIndex,
            timeSeconds: p.timeSeconds,
            value: p.y,
          ))
      .toList(growable: false);

  final List<_GraphSample> vSamples = <_GraphSample>[];
  for (int i = 1; i < framePoints.length; i++) {
    final _FramePoint prev = framePoints[i - 1];
    final _FramePoint curr = framePoints[i];
    final double dt = curr.timeSeconds - prev.timeSeconds;
    if (dt <= 0) {
      continue;
    }
    final double vx = (curr.x - prev.x) / dt;
    final double vy = (curr.y - prev.y) / dt;
    final double speed = sqrt(vx * vx + vy * vy);
    vSamples.add(
      _GraphSample(
        frameIndex: curr.frameIndex,
        timeSeconds: curr.timeSeconds,
        value: speed,
      ),
    );
  }

  final List<_GraphSample> aSamples = <_GraphSample>[];
  for (int i = 1; i < vSamples.length; i++) {
    final _GraphSample prev = vSamples[i - 1];
    final _GraphSample curr = vSamples[i];
    final double dt = curr.timeSeconds - prev.timeSeconds;
    if (dt <= 0) {
      continue;
    }
    final double a = (curr.value - prev.value) / dt;
    aSamples.add(
      _GraphSample(
        frameIndex: curr.frameIndex,
        timeSeconds: curr.timeSeconds,
        value: a,
      ),
    );
  }

  return _KinematicsData(
    xSamples: xSamples,
    ySamples: ySamples,
    vSamples: vSamples,
    aSamples: aSamples,
  );
}
