// Responsibility: Render track points and paths on top of the video frame.

import 'package:flutter/material.dart';

import '../../../../shared/models/track_point.dart';
import '../../../../shared/utils/constants.dart';
import '../utils/point_hit_test.dart';

class TrackOverlay extends StatelessWidget {
  const TrackOverlay({
    super.key,
    required this.points,
    required this.color,
    required this.currentFrameIndex,
    this.previewNormalizedOffset,
  });

  final List<TrackPoint> points;
  final Color color;
  final int currentFrameIndex;
  final Offset? previewNormalizedOffset;

  @override
  Widget build(BuildContext context) {
    return CustomPaint(
      painter: _TrackOverlayPainter(
        points: points,
        color: color,
        currentFrameIndex: currentFrameIndex,
        previewNormalizedOffset: previewNormalizedOffset,
      ),
      child: const SizedBox.expand(),
    );
  }
}

class _TrackOverlayPainter extends CustomPainter {
  _TrackOverlayPainter({
    required this.points,
    required this.color,
    required this.currentFrameIndex,
    required this.previewNormalizedOffset,
  });

  final List<TrackPoint> points;
  final Color color;
  final int currentFrameIndex;
  final Offset? previewNormalizedOffset;

  @override
  void paint(Canvas canvas, Size size) {
    if (points.isEmpty) {
      return;
    }

    final List<TrackPoint> ordered = List<TrackPoint>.from(points)
      ..sort((a, b) => a.frameIndex.compareTo(b.frameIndex));

    final Paint pathPaint = Paint()
      ..color = color
      ..strokeWidth = kOverlayStrokeWidth
      ..style = PaintingStyle.stroke;

    final Paint pointPaint = Paint()
      ..color = color
      ..style = PaintingStyle.fill;

    final Paint currentPointPaint = Paint()
      ..color = Colors.white
      ..style = PaintingStyle.fill;

    final Paint previewRingPaint = Paint()
      ..color = Colors.white
      ..strokeWidth = 2
      ..style = PaintingStyle.stroke;

    final Path path = Path();
    for (int i = 0; i < ordered.length; i++) {
      final TrackPoint point = ordered[i];
      final Offset offset = _toCanvasOffset(point, size);
      if (i == 0) {
        path.moveTo(offset.dx, offset.dy);
      } else {
        path.lineTo(offset.dx, offset.dy);
      }
    }
    canvas.drawPath(path, pathPaint);

    for (final TrackPoint point in ordered) {
      final Offset offset = _toCanvasOffset(point, size);
      canvas.drawCircle(
        offset,
        kPointRadius,
        pointPaint,
      );
      if (point.frameIndex == currentFrameIndex) {
        canvas.drawCircle(
          offset,
          kPointRadius + 2,
          currentPointPaint,
        );
        canvas.drawCircle(
          offset,
          kPointRadius,
          pointPaint,
        );
      }
    }

    if (previewNormalizedOffset != null) {
      final Offset previewOffset = Offset(
        previewNormalizedOffset!.dx.clamp(0.0, 1.0) * size.width,
        previewNormalizedOffset!.dy.clamp(0.0, 1.0) * size.height,
      );
      canvas.drawCircle(
        previewOffset,
        kPointRadius + 4,
        previewRingPaint,
      );
      canvas.drawCircle(
        previewOffset,
        kPointRadius,
        pointPaint,
      );
      canvas.drawLine(
        Offset(previewOffset.dx - 10, previewOffset.dy),
        Offset(previewOffset.dx + 10, previewOffset.dy),
        previewRingPaint,
      );
      canvas.drawLine(
        Offset(previewOffset.dx, previewOffset.dy - 10),
        Offset(previewOffset.dx, previewOffset.dy + 10),
        previewRingPaint,
      );
    }
  }

  Offset _toCanvasOffset(TrackPoint point, Size size) {
    return trackPointSceneOffset(point, size);
  }

  @override
  bool shouldRepaint(covariant _TrackOverlayPainter oldDelegate) {
    return oldDelegate.points != points ||
        oldDelegate.color != color ||
        oldDelegate.currentFrameIndex != currentFrameIndex ||
        oldDelegate.previewNormalizedOffset != previewNormalizedOffset;
  }
}
