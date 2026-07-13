// Responsibility: Manage project summary list state for the home screen.

import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../../shared/models/project_summary.dart';
import 'project_repository_provider.dart';

part 'project_list_provider.g.dart';

@riverpod
class ProjectListController extends _$ProjectListController {
  @override
  Future<List<ProjectSummary>> build() async {
    final repo = ref.read(projectRepositoryProvider);
    return repo.loadSummaries();
  }

  Future<void> refresh() async {
    state = const AsyncLoading();
    state = AsyncData(await ref.read(projectRepositoryProvider).loadSummaries());
  }

  Future<void> deleteProject(String projectId) async {
    await ref.read(projectRepositoryProvider).deleteProject(projectId);
    await refresh();
  }
}
