// Responsibility: Display kinematics data table with derived velocity/acceleration.

import 'dart:math';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../shared/models/calibration_data.dart';
import '../../../../shared/models/track_point.dart';
import '../../../../shared/utils/calibration_math.dart';
import '../../../tracking/presentation/providers/tracking_controller.dart';

class DataTableScreen extends ConsumerWidget {
  const DataTableScreen({super.key, required this.projectId});

  final String projectId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tracking = ref.watch(trackingControllerProvider(projectId));

    return Scaffold(
      appBar: AppBar(title: const Text('Data Table')),
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
          final List<_KinematicsRow> rows = _buildRows(
            points: points,
            fps: fps,
            calibration: calibration,
            massKg: state.project.tracks.first.mass,
          );
          final String positionUnit = calibration == null ? 'px' : 'm';
          final String velocityUnit = calibration == null ? 'px/s' : 'm/s';
          final String accelerationUnit =
              calibration == null ? 'px/s²' : 'm/s²';
          const String momentumUnit = 'kg·m/s';
          const String energyUnit = 'J';
          const String forceUnit = 'N';

          return SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: SingleChildScrollView(
              child: DataTable(
                columns: [
                  const DataColumn(label: Text('Frame')),
                  const DataColumn(label: Text('t (s)')),
                  DataColumn(label: Text('x ($positionUnit)')),
                  DataColumn(label: Text('y ($positionUnit)')),
                  DataColumn(label: Text('vx ($velocityUnit)')),
                  DataColumn(label: Text('vy ($velocityUnit)')),
                  DataColumn(label: Text('ax ($accelerationUnit)')),
                  DataColumn(label: Text('ay ($accelerationUnit)')),
                  DataColumn(label: Text('speed ($velocityUnit)')),
                  DataColumn(label: Text('p ($momentumUnit)')),
                  DataColumn(label: Text('KE ($energyUnit)')),
                  DataColumn(label: Text('Fx ($forceUnit)')),
                  DataColumn(label: Text('Fy ($forceUnit)')),
                ],
                rows: rows
                    .map(
                      (row) => DataRow(cells: [
                        DataCell(Text(row.frameIndex.toString())),
                        DataCell(Text(row.timeSeconds.toStringAsFixed(3))),
                        DataCell(Text(row.x.toStringAsFixed(4))),
                        DataCell(Text(row.y.toStringAsFixed(4))),
                        DataCell(Text(_fmt(row.vx))),
                        DataCell(Text(_fmt(row.vy))),
                        DataCell(Text(_fmt(row.ax))),
                        DataCell(Text(_fmt(row.ay))),
                        DataCell(Text(_fmt(row.speed))),
                        DataCell(Text(_fmt(row.momentum))),
                        DataCell(Text(_fmt(row.kineticEnergy))),
                        DataCell(Text(_fmt(row.forceX))),
                        DataCell(Text(_fmt(row.forceY))),
                      ]),
                    )
                    .toList(growable: false),
              ),
            ),
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text('Failed to load data.'),
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

class _KinematicsRow {
  const _KinematicsRow({
    required this.frameIndex,
    required this.timeSeconds,
    required this.x,
    required this.y,
    required this.vx,
    required this.vy,
    required this.ax,
    required this.ay,
    required this.speed,
    required this.momentum,
    required this.kineticEnergy,
    required this.forceX,
    required this.forceY,
  });

  final int frameIndex;
  final double timeSeconds;
  final double x;
  final double y;
  final double? vx;
  final double? vy;
  final double? ax;
  final double? ay;
  final double? speed;
  final double? momentum;
  final double? kineticEnergy;
  final double? forceX;
  final double? forceY;
}

List<_KinematicsRow> _buildRows({
  required List<TrackPoint> points,
  required double fps,
  required CalibrationData? calibration,
  required double? massKg,
}) {
  final List<_KinematicsRow> rows = <_KinematicsRow>[];

  double? prevTime;
  double? prevX;
  double? prevY;
  double? prevVx;
  double? prevVy;

  for (final TrackPoint point in points) {
    final double t = point.frameIndex / fps;
    final double x;
    final double y;
    if (calibration == null) {
      x = point.pixelX;
      y = point.pixelY;
    } else {
      final world = pixelToWorld(
        calibration: calibration,
        pixelX: point.pixelX,
        pixelY: point.pixelY,
      );
      x = world.x;
      y = world.y;
    }

    double? vx;
    double? vy;
    double? ax;
    double? ay;
    double? speed;
    double? momentum;
    double? kineticEnergy;
    double? forceX;
    double? forceY;

    if (prevTime != null && prevX != null && prevY != null) {
      final double dt = t - prevTime;
      if (dt > 0) {
        vx = (x - prevX) / dt;
        vy = (y - prevY) / dt;
        speed = sqrt((vx * vx) + (vy * vy));
        if (prevVx != null && prevVy != null) {
          ax = (vx - prevVx) / dt;
          ay = (vy - prevVy) / dt;
        }
        if (massKg != null) {
          momentum = massKg * speed;
          kineticEnergy = 0.5 * massKg * speed * speed;
          if (ax != null && ay != null) {
            forceX = massKg * ax;
            forceY = massKg * ay;
          }
        }
      }
    }

    rows.add(
      _KinematicsRow(
        frameIndex: point.frameIndex,
        timeSeconds: t,
        x: x,
        y: y,
        vx: vx,
        vy: vy,
        ax: ax,
        ay: ay,
        speed: speed,
        momentum: momentum,
        kineticEnergy: kineticEnergy,
        forceX: forceX,
        forceY: forceY,
      ),
    );

    prevTime = t;
    prevX = x;
    prevY = y;
    prevVx = vx;
    prevVy = vy;
  }

  return rows;
}

String _fmt(double? value) {
  if (value == null) {
    return '—';
  }
  return value.toStringAsFixed(4);
}
