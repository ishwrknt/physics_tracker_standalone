// Responsibility: Estimate next-frame object position from a user-selected point.

import 'dart:math';
import 'dart:typed_data';
import 'dart:ui';

import 'package:image/image.dart' as img;

class ObjectAutoTracker {
  const ObjectAutoTracker();

  Offset? detectNextPoint({
    required Uint8List currentFrameBytes,
    required Uint8List nextFrameBytes,
    required Offset previousPoint,
    required Size viewportSize,
    int templateRadius = 12,
    int searchRadius = 32,
  }) {
    final int width = max(1, viewportSize.width.round());
    final int height = max(1, viewportSize.height.round());

    final img.Image? currentRaw = img.decodeImage(currentFrameBytes);
    final img.Image? nextRaw = img.decodeImage(nextFrameBytes);
    if (currentRaw == null || nextRaw == null) {
      return null;
    }

    final img.Image currentFrame = img.copyResize(
      currentRaw,
      width: width,
      height: height,
      interpolation: img.Interpolation.linear,
    );
    final img.Image nextFrame = img.copyResize(
      nextRaw,
      width: width,
      height: height,
      interpolation: img.Interpolation.linear,
    );

    final int centerX = previousPoint.dx.round().clamp(0, width - 1);
    final int centerY = previousPoint.dy.round().clamp(0, height - 1);

    final int left = max(0, centerX - templateRadius);
    final int right = min(width - 1, centerX + templateRadius);
    final int top = max(0, centerY - templateRadius);
    final int bottom = min(height - 1, centerY + templateRadius);

    if (right <= left || bottom <= top) {
      return null;
    }

    double bestScore = double.infinity;
    int bestX = centerX;
    int bestY = centerY;

    for (int dy = -searchRadius; dy <= searchRadius; dy++) {
      for (int dx = -searchRadius; dx <= searchRadius; dx++) {
        final int candidateX = centerX + dx;
        final int candidateY = centerY + dy;

        final int cLeft = candidateX - (centerX - left);
        final int cRight = candidateX + (right - centerX);
        final int cTop = candidateY - (centerY - top);
        final int cBottom = candidateY + (bottom - centerY);

        if (cLeft < 0 || cRight >= width || cTop < 0 || cBottom >= height) {
          continue;
        }

        final double score = _sad(
          currentFrame: currentFrame,
          nextFrame: nextFrame,
          left: left,
          top: top,
          cLeft: cLeft,
          cTop: cTop,
          width: right - left + 1,
          height: bottom - top + 1,
        );

        if (score < bestScore) {
          bestScore = score;
          bestX = candidateX;
          bestY = candidateY;
        }
      }
    }

    return Offset(bestX.toDouble(), bestY.toDouble());
  }

  double _sad({
    required img.Image currentFrame,
    required img.Image nextFrame,
    required int left,
    required int top,
    required int cLeft,
    required int cTop,
    required int width,
    required int height,
  }) {
    double sum = 0.0;

    // Sample every other pixel to keep tracking responsive on phones.
    for (int y = 0; y < height; y += 2) {
      for (int x = 0; x < width; x += 2) {
        final img.Pixel a = currentFrame.getPixel(left + x, top + y);
        final img.Pixel b = nextFrame.getPixel(cLeft + x, cTop + y);
        final int la = _luma(a);
        final int lb = _luma(b);
        sum += (la - lb).abs();
      }
    }
    return sum;
  }

  int _luma(img.Pixel px) {
    return ((px.r.toInt() * 299) +
            (px.g.toInt() * 587) +
            (px.b.toInt() * 114)) ~/
        1000;
  }
}
