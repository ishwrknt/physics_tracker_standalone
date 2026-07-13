// Responsibility: Handle orientation preferences and setup flow

import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';

class OrientationService {
  static const String _orientationSetupCompleteKey = 'orientation_setup_complete';
  static const String _landscapeModeKey = 'landscape_mode';
  static const String _autoRotateKey = 'auto_rotate';

  static Future<bool> shouldShowOrientationSetup() async {
    final prefs = await SharedPreferences.getInstance();
    return !(prefs.getBool(_orientationSetupCompleteKey) ?? false);
  }

  static Future<void> setOrientationSetupComplete() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_orientationSetupCompleteKey, true);
  }

  static Future<bool> isLandscapeMode() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getBool(_landscapeModeKey) ?? false;
  }

  static Future<bool> isAutoRotateEnabled() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getBool(_autoRotateKey) ?? true;
  }

  static Future<void> applyOrientationSettings() async {
    final landscapeMode = await isLandscapeMode();
    final autoRotate = await isAutoRotateEnabled();

    if (autoRotate) {
      // Let the system handle rotation based on video orientation
      await SystemChrome.setPreferredOrientations([
        DeviceOrientation.portraitUp,
        DeviceOrientation.portraitDown,
        DeviceOrientation.landscapeLeft,
        DeviceOrientation.landscapeRight,
      ]);
    } else {
      // Force the preferred orientation
      if (landscapeMode) {
        await SystemChrome.setPreferredOrientations([
          DeviceOrientation.landscapeLeft,
          DeviceOrientation.landscapeRight,
        ]);
      } else {
        await SystemChrome.setPreferredOrientations([
          DeviceOrientation.portraitUp,
          DeviceOrientation.portraitDown,
        ]);
      }
    }
  }

  static Future<void> resetOrientationSetup() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_orientationSetupCompleteKey);
  }
}
