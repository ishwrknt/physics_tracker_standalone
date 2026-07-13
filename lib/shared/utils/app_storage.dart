// Responsibility: Resolve writable app-local directories with path_provider fallback.

import 'dart:io';

import 'package:path_provider/path_provider.dart';

const List<String> _androidAppIds = <String>[
  'com.example.physics_tracker',
  'com.physics.tracker',
  'physics_tracker',
];

Future<Directory> getWritableAppDirectory() async {
  try {
    return await getApplicationDocumentsDirectory();
  } catch (_) {
    // Fall through to platform-specific fallback.
  }

  if (Platform.isAndroid) {
    for (final String appId in _androidAppIds) {
      final Directory dir = Directory('/data/user/0/$appId/files');
      try {
        await dir.create(recursive: true);
        return dir;
      } catch (_) {
        continue;
      }
    }
  }

  final Directory fallback =
      Directory('${Directory.systemTemp.path}/physics_tracker');
  await fallback.create(recursive: true);
  return fallback;
}

Future<String> persistVideoToAppStorage(String sourcePath) async {
  final File source = File(sourcePath);
  if (!await source.exists()) {
    throw StateError('Selected video is no longer available.');
  }

  final Directory docs = await getWritableAppDirectory();
  final Directory importDir = Directory('${docs.path}/imported_videos');
  await importDir.create(recursive: true);

  final String fallbackName = source.uri.pathSegments.isEmpty
      ? 'video.mp4'
      : source.uri.pathSegments.last;
  final String outputPath =
      '${importDir.path}/${DateTime.now().millisecondsSinceEpoch}_$fallbackName';

  try {
    final File copied = await source.copy(outputPath);
    return copied.path;
  } catch (error) {
    throw StateError(
        'Could not copy the selected video into app storage: $error');
  }
}
