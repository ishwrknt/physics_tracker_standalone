// Responsibility: Fit common curve models and report equation + R^2.

import 'dart:math';

enum CurveFitType {
  linear,
  quadratic,
  sinusoidal,
  exponential,
}

class CurveFitResult {
  const CurveFitResult({
    required this.type,
    required this.coefficients,
    required this.equation,
    required this.rSquared,
  });

  final CurveFitType type;
  final List<double> coefficients;
  final String equation;
  final double rSquared;
}

CurveFitResult? fitCurve({
  required CurveFitType type,
  required List<double> x,
  required List<double> y,
}) {
  if (x.length != y.length || x.isEmpty) {
    return null;
  }
  switch (type) {
    case CurveFitType.linear:
      return _fitLinear(x, y);
    case CurveFitType.quadratic:
      return _fitQuadratic(x, y);
    case CurveFitType.sinusoidal:
      return _fitSinusoidal(x, y);
    case CurveFitType.exponential:
      return _fitExponential(x, y);
  }
}

double evaluateCurve(CurveFitResult fit, double x) {
  switch (fit.type) {
    case CurveFitType.linear:
      final double m = fit.coefficients[0];
      final double b = fit.coefficients[1];
      return (m * x) + b;
    case CurveFitType.quadratic:
      final double a = fit.coefficients[0];
      final double b = fit.coefficients[1];
      final double c = fit.coefficients[2];
      return (a * x * x) + (b * x) + c;
    case CurveFitType.sinusoidal:
      final double amp = fit.coefficients[0];
      final double omega = fit.coefficients[1];
      final double phase = fit.coefficients[2];
      final double c = fit.coefficients[3];
      return (amp * sin((omega * x) + phase)) + c;
    case CurveFitType.exponential:
      final double a = fit.coefficients[0];
      final double b = fit.coefficients[1];
      return a * exp(b * x);
  }
}

CurveFitResult? _fitLinear(List<double> x, List<double> y) {
  if (x.length < 2) {
    return null;
  }
  final _LinearFit? fit = _linearRegression(x, y);
  if (fit == null) {
    return null;
  }
  final List<double> predicted =
      x.map((xi) => (fit.slope * xi) + fit.intercept).toList();
  final double r2 = _rSquared(y, predicted);
  return CurveFitResult(
    type: CurveFitType.linear,
    coefficients: [fit.slope, fit.intercept],
    equation:
        'y = ${fit.slope.toStringAsFixed(4)}x + ${fit.intercept.toStringAsFixed(4)}',
    rSquared: r2,
  );
}

CurveFitResult? _fitQuadratic(List<double> x, List<double> y) {
  if (x.length < 3) {
    return null;
  }
  final int n = x.length;
  double sx = 0;
  double sx2 = 0;
  double sx3 = 0;
  double sx4 = 0;
  double sy = 0;
  double sxy = 0;
  double sx2y = 0;

  for (int i = 0; i < n; i++) {
    final double xi = x[i];
    final double yi = y[i];
    final double xi2 = xi * xi;
    sx += xi;
    sx2 += xi2;
    sx3 += xi2 * xi;
    sx4 += xi2 * xi2;
    sy += yi;
    sxy += xi * yi;
    sx2y += xi2 * yi;
  }

  final List<double>? sol = _solve3x3(
    <List<double>>[
      [sx4, sx3, sx2],
      [sx3, sx2, sx],
      [sx2, sx, n.toDouble()],
    ],
    <double>[sx2y, sxy, sy],
  );
  if (sol == null) {
    return null;
  }
  final double a = sol[0];
  final double b = sol[1];
  final double c = sol[2];
  final List<double> predicted =
      x.map((xi) => (a * xi * xi) + (b * xi) + c).toList();
  final double r2 = _rSquared(y, predicted);
  return CurveFitResult(
    type: CurveFitType.quadratic,
    coefficients: [a, b, c],
    equation:
        'y = ${a.toStringAsFixed(4)}x^2 + ${b.toStringAsFixed(4)}x + ${c.toStringAsFixed(4)}',
    rSquared: r2,
  );
}

CurveFitResult? _fitExponential(List<double> x, List<double> y) {
  if (x.length < 2) {
    return null;
  }
  if (y.any((v) => v <= 0)) {
    return null;
  }
  final List<double> ly = y.map(log).toList(growable: false);
  final _LinearFit? logFit = _linearRegression(x, ly);
  if (logFit == null) {
    return null;
  }
  final double a = exp(logFit.intercept);
  final double b = logFit.slope;
  final List<double> predicted =
      x.map((xi) => a * exp(b * xi)).toList(growable: false);
  final double r2 = _rSquared(y, predicted);
  return CurveFitResult(
    type: CurveFitType.exponential,
    coefficients: [a, b],
    equation: 'y = ${a.toStringAsFixed(4)}e^(${b.toStringAsFixed(4)}x)',
    rSquared: r2,
  );
}

CurveFitResult? _fitSinusoidal(List<double> x, List<double> y) {
  if (x.length < 4) {
    return null;
  }
  double minX = x.first;
  double maxX = x.first;
  for (final double xi in x) {
    minX = min(minX, xi);
    maxX = max(maxX, xi);
  }
  final double span = maxX - minX;
  if (span <= 0) {
    return null;
  }

  final double minOmega = (2 * pi / span) * 0.25;
  final double maxOmega = (2 * pi / span) * 6.0;
  const int steps = 80;

  double bestSse = double.infinity;
  double? bestOmega;
  List<double>? bestCoeff;

  for (int i = 0; i <= steps; i++) {
    final double omega = minOmega + ((maxOmega - minOmega) * i / steps);
    final List<double>? coeff = _fitSinCosOffset(x, y, omega);
    if (coeff == null) {
      continue;
    }
    final double a = coeff[0];
    final double b = coeff[1];
    final double c = coeff[2];
    double sse = 0;
    for (int j = 0; j < x.length; j++) {
      final double yp = (a * sin(omega * x[j])) + (b * cos(omega * x[j])) + c;
      final double e = y[j] - yp;
      sse += e * e;
    }
    if (sse < bestSse) {
      bestSse = sse;
      bestOmega = omega;
      bestCoeff = coeff;
    }
  }

  if (bestOmega == null || bestCoeff == null) {
    return null;
  }
  final double omega = bestOmega;

  final double a = bestCoeff[0];
  final double b = bestCoeff[1];
  final double c = bestCoeff[2];
  final double amplitude = sqrt((a * a) + (b * b));
  final double phase = atan2(b, a);
  final List<double> predicted = x
      .map((xi) => (amplitude * sin((omega * xi) + phase)) + c)
      .toList(growable: false);
  final double r2 = _rSquared(y, predicted);
  return CurveFitResult(
    type: CurveFitType.sinusoidal,
    coefficients: [amplitude, omega, phase, c],
    equation:
        'y = ${amplitude.toStringAsFixed(4)}sin(${omega.toStringAsFixed(4)}x + ${phase.toStringAsFixed(4)}) + ${c.toStringAsFixed(4)}',
    rSquared: r2,
  );
}

List<double>? _fitSinCosOffset(List<double> x, List<double> y, double omega) {
  double ss = 0;
  double cc = 0;
  double oo = x.length.toDouble();
  double sc = 0;
  double so = 0;
  double co = 0;
  double sy = 0;
  double cy = 0;
  double oy = 0;

  for (int i = 0; i < x.length; i++) {
    final double s = sin(omega * x[i]);
    final double c = cos(omega * x[i]);
    final double yi = y[i];
    ss += s * s;
    cc += c * c;
    sc += s * c;
    so += s;
    co += c;
    sy += s * yi;
    cy += c * yi;
    oy += yi;
  }

  return _solve3x3(
    <List<double>>[
      [ss, sc, so],
      [sc, cc, co],
      [so, co, oo],
    ],
    <double>[sy, cy, oy],
  );
}

class _LinearFit {
  const _LinearFit(this.slope, this.intercept);
  final double slope;
  final double intercept;
}

_LinearFit? _linearRegression(List<double> x, List<double> y) {
  if (x.length != y.length || x.length < 2) {
    return null;
  }
  final int n = x.length;
  double sx = 0;
  double sy = 0;
  double sxx = 0;
  double sxy = 0;
  for (int i = 0; i < n; i++) {
    sx += x[i];
    sy += y[i];
    sxx += x[i] * x[i];
    sxy += x[i] * y[i];
  }
  final double denom = (n * sxx) - (sx * sx);
  if (denom.abs() < 1e-12) {
    return null;
  }
  final double m = ((n * sxy) - (sx * sy)) / denom;
  final double b = (sy - (m * sx)) / n;
  return _LinearFit(m, b);
}

List<double>? _solve3x3(List<List<double>> a, List<double> b) {
  final List<List<double>> m = <List<double>>[
    [a[0][0], a[0][1], a[0][2], b[0]],
    [a[1][0], a[1][1], a[1][2], b[1]],
    [a[2][0], a[2][1], a[2][2], b[2]],
  ];

  for (int i = 0; i < 3; i++) {
    int pivot = i;
    for (int r = i + 1; r < 3; r++) {
      if (m[r][i].abs() > m[pivot][i].abs()) {
        pivot = r;
      }
    }
    if (m[pivot][i].abs() < 1e-12) {
      return null;
    }
    if (pivot != i) {
      final List<double> tmp = m[i];
      m[i] = m[pivot];
      m[pivot] = tmp;
    }

    final double div = m[i][i];
    for (int c = i; c < 4; c++) {
      m[i][c] /= div;
    }

    for (int r = 0; r < 3; r++) {
      if (r == i) {
        continue;
      }
      final double factor = m[r][i];
      for (int c = i; c < 4; c++) {
        m[r][c] -= factor * m[i][c];
      }
    }
  }

  return <double>[m[0][3], m[1][3], m[2][3]];
}

double _rSquared(List<double> actual, List<double> predicted) {
  if (actual.length != predicted.length || actual.isEmpty) {
    return 0;
  }
  double mean = 0;
  for (final double v in actual) {
    mean += v;
  }
  mean /= actual.length;

  double ssRes = 0;
  double ssTot = 0;
  for (int i = 0; i < actual.length; i++) {
    final double e = actual[i] - predicted[i];
    ssRes += e * e;
    final double d = actual[i] - mean;
    ssTot += d * d;
  }
  if (ssTot.abs() < 1e-12) {
    return 1;
  }
  return 1 - (ssRes / ssTot);
}
