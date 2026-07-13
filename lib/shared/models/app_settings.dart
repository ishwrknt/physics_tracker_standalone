// Responsibility: Persistent app settings including orientation preferences

import 'package:hive/hive.dart';

part 'app_settings.g.dart';

@HiveType(typeId: 10)
class AppSettings {
  @HiveField(0)
  final bool autoRotateVideo;
  
  @HiveField(1)
  final bool landscapeMode;
  
  @HiveField(2)
  final bool showFrameNumbers;
  
  @HiveField(3)
  final bool hapticFeedback;
  
  @HiveField(4)
  final double buttonSize;
  
  @HiveField(5)
  final bool autoSaveProgress;

  const AppSettings({
    this.autoRotateVideo = true,
    this.landscapeMode = false,
    this.showFrameNumbers = true,
    this.hapticFeedback = true,
    this.buttonSize = 1.0,
    this.autoSaveProgress = true,
  });

  factory AppSettings.fromJson(Map<String, dynamic> json) {
    return AppSettings(
      autoRotateVideo: json['autoRotateVideo'] as bool? ?? true,
      landscapeMode: json['landscapeMode'] as bool? ?? false,
      showFrameNumbers: json['showFrameNumbers'] as bool? ?? true,
      hapticFeedback: json['hapticFeedback'] as bool? ?? true,
      buttonSize: (json['buttonSize'] as num?)?.toDouble() ?? 1.0,
      autoSaveProgress: json['autoSaveProgress'] as bool? ?? true,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'autoRotateVideo': autoRotateVideo,
      'landscapeMode': landscapeMode,
      'showFrameNumbers': showFrameNumbers,
      'hapticFeedback': hapticFeedback,
      'buttonSize': buttonSize,
      'autoSaveProgress': autoSaveProgress,
    };
  }

  AppSettings copyWith({
    bool? autoRotateVideo,
    bool? landscapeMode,
    bool? showFrameNumbers,
    bool? hapticFeedback,
    double? buttonSize,
    bool? autoSaveProgress,
  }) {
    return AppSettings(
      autoRotateVideo: autoRotateVideo ?? this.autoRotateVideo,
      landscapeMode: landscapeMode ?? this.landscapeMode,
      showFrameNumbers: showFrameNumbers ?? this.showFrameNumbers,
      hapticFeedback: hapticFeedback ?? this.hapticFeedback,
      buttonSize: buttonSize ?? this.buttonSize,
      autoSaveProgress: autoSaveProgress ?? this.autoSaveProgress,
    );
  }
}
