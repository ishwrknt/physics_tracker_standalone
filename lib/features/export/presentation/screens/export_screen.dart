// Responsibility: Export analysis files (CSV and annotated PNG) for sharing.

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:share_plus/share_plus.dart';

import '../../../tracking/presentation/providers/tracking_controller.dart';
import '../../data/export_service.dart';

class ExportScreen extends ConsumerWidget {
  const ExportScreen({super.key, required this.projectId});

  final String projectId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tracking = ref.watch(trackingControllerProvider(projectId));

    return Scaffold(
      appBar: AppBar(title: const Text('Export')),
      body: tracking.when(
        data: (state) {
          final exportService = ExportService();
          final track = state.project.tracks.first;
          final points = track.points;
          final fps = state.metadata.fps > 0 ? state.metadata.fps : 30.0;

          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              ListTile(
                leading: const Icon(Icons.table_view),
                title: const Text('Export CSV'),
                subtitle: const Text(
                  'Saves frame, t, x, y, v, a, and point-mass analysis values.',
                ),
                trailing: const Icon(Icons.chevron_right),
                onTap: points.isEmpty
                    ? null
                    : () async {
                        final String path = await exportService.exportCsv(
                          projectName: state.project.name,
                          points: points,
                          fps: fps,
                          calibration: state.project.calibration,
                          massKg: track.mass,
                        );
                        if (!context.mounted) {
                          return;
                        }
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(content: Text('CSV exported: $path')),
                        );
                        await SharePlus.instance.share(
                          ShareParams(
                            files: [XFile(path)],
                            text: 'PhysicsTracker CSV export',
                          ),
                        );
                      },
              ),
              ListTile(
                leading: const Icon(Icons.image),
                title: const Text('Export Annotated PNG'),
                subtitle: const Text(
                  'Saves current frame with track path, points, and calibration overlay.',
                ),
                trailing: const Icon(Icons.chevron_right),
                onTap: state.frameBytes == null
                    ? null
                    : () async {
                        final String path =
                            await exportService.exportAnnotatedPng(
                          projectName: state.project.name,
                          frameBytes: state.frameBytes!,
                          points: points,
                          calibration: state.project.calibration,
                        );
                        if (!context.mounted) {
                          return;
                        }
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(content: Text('PNG exported: $path')),
                        );
                        await SharePlus.instance.share(
                          ShareParams(
                            files: [XFile(path)],
                            text: 'PhysicsTracker PNG export',
                          ),
                        );
                      },
              ),
              const SizedBox(height: 12),
              const Text(
                'Files are stored in app documents under exports/<project_name>/',
              ),
            ],
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text('Failed to load export data.'),
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
