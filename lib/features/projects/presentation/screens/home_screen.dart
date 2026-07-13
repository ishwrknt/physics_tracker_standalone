// Responsibility: Display project list and entry point for creating projects.

import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

import '../../../tracking/presentation/screens/project_screen.dart';
import '../providers/project_creation_controller.dart';
import '../providers/project_list_provider.dart';
import '../../../../shared/utils/constants.dart';

class HomeScreen extends ConsumerWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final projectList = ref.watch(projectListControllerProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('PhysicsTracker')),
      body: projectList.when(
        data: (projects) {
          if (projects.isEmpty) {
            return const Center(
              child: Padding(
                padding: EdgeInsets.all(24),
                child: Text(
                  'No projects yet. Create one with a local video to start tracking.',
                  textAlign: TextAlign.center,
                ),
              ),
            );
          }
          return ListView.builder(
            itemCount: projects.length,
            itemBuilder: (context, index) {
              final project = projects[index];
              return Dismissible(
                key: ValueKey(project.id),
                direction: DismissDirection.endToStart,
                background: Container(
                  color: Theme.of(context).colorScheme.error,
                  alignment: Alignment.centerRight,
                  padding:
                      const EdgeInsets.symmetric(horizontal: kScrubberPadding),
                  child: const Icon(Icons.delete, color: Colors.white),
                ),
                onDismissed: (_) async {
                  await ref
                      .read(projectListControllerProvider.notifier)
                      .deleteProject(project.id);
                },
                child: ListTile(
                  leading: _Thumbnail(path: project.thumbnailPath),
                  title: Text(project.name),
                  subtitle: Text(_formatDate(project.createdAt)),
                  onTap: () {
                    Navigator.of(context).push(
                      MaterialPageRoute(
                        builder: (_) => ProjectScreen(projectId: project.id),
                      ),
                    );
                  },
                ),
              );
            },
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text('Failed to load projects.'),
              const SizedBox(height: 12),
              ElevatedButton(
                onPressed: () =>
                    ref.read(projectListControllerProvider.notifier).refresh(),
                child: const Text('Retry'),
              ),
            ],
          ),
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _showCreateDialog(context, ref),
        child: const Icon(Icons.add),
      ),
    );
  }

  Future<void> _showCreateDialog(BuildContext context, WidgetRef ref) async {
    final TextEditingController controller = TextEditingController();
    String? selectedVideoPath;

    final String? projectId = await showDialog<String>(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            final bool canSave =
                controller.text.trim().isNotEmpty && selectedVideoPath != null;
            return AlertDialog(
              title: const Text('New Project'),
              content: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  TextField(
                    controller: controller,
                    decoration: const InputDecoration(
                      labelText: 'Project name',
                    ),
                    onChanged: (_) => setDialogState(() {}),
                  ),
                  const SizedBox(height: 12),
                  OutlinedButton.icon(
                    onPressed: () async {
                      final String? videoPath = await _pickVideoPath();
                      if (videoPath == null) {
                        if (context.mounted) {
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(
                              content: Text(
                                'Could not access selected video. Choose a local video file.',
                              ),
                            ),
                          );
                        }
                        return;
                      }
                      setDialogState(() {
                        selectedVideoPath = videoPath;
                      });
                    },
                    icon: const Icon(Icons.video_library_outlined),
                    label: Text(
                      selectedVideoPath == null
                          ? 'Pick Video'
                          : 'Video Selected',
                    ),
                  ),
                  const SizedBox(height: 8),
                  const Align(
                    alignment: Alignment.centerLeft,
                    child: Text(
                      'A video is required for the MVP tracking workflow.',
                    ),
                  ),
                ],
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.of(context).pop(),
                  child: const Text('Cancel'),
                ),
                FilledButton(
                  onPressed: !canSave
                      ? null
                      : () async {
                          final String name = controller.text.trim();
                          final String videoPath = selectedVideoPath!;
                          try {
                            final String id = await ref
                                .read(
                                    projectCreationControllerProvider.notifier)
                                .createProject(
                                    name: name, videoPath: videoPath);
                            if (context.mounted) {
                              Navigator.of(context).pop(id);
                            }
                          } catch (e) {
                            if (context.mounted) {
                              ScaffoldMessenger.of(context).showSnackBar(
                                SnackBar(content: Text('Save failed: $e')),
                              );
                            }
                          }
                        },
                  child: const Text('Save Project'),
                ),
              ],
            );
          },
        );
      },
    );

    if (projectId != null && context.mounted) {
      await ref.read(projectListControllerProvider.notifier).refresh();
      if (!context.mounted) {
        return;
      }
      Navigator.of(context).push(
        MaterialPageRoute(
          builder: (_) => ProjectScreen(projectId: projectId),
        ),
      );
    }
  }

  Future<String?> _pickVideoPath() async {
    final ImagePicker picker = ImagePicker();
    final XFile? picked = await picker.pickVideo(source: ImageSource.gallery);
    return picked?.path;
  }
}

class _Thumbnail extends StatelessWidget {
  const _Thumbnail({required this.path});

  final String? path;

  @override
  Widget build(BuildContext context) {
    final double size = kMinTapTarget;
    if (path == null || path!.isEmpty) {
      return SizedBox(
        width: size,
        height: size,
        child: const Icon(Icons.videocam),
      );
    }
    final File file = File(path!);
    return ClipRRect(
      borderRadius: BorderRadius.circular(8),
      child: Image.file(
        file,
        width: size,
        height: size,
        fit: BoxFit.cover,
        errorBuilder: (_, __, ___) {
          return SizedBox(
            width: size,
            height: size,
            child: const Icon(Icons.videocam_off),
          );
        },
      ),
    );
  }
}

String _formatDate(DateTime date) {
  return '${date.year.toString().padLeft(4, '0')}-'
      '${date.month.toString().padLeft(2, '0')}-'
      '${date.day.toString().padLeft(2, '0')}';
}
