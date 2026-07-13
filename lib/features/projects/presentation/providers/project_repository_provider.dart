// Responsibility: Provide a singleton ProjectRepository instance.

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/project_repository.dart';

final projectRepositoryProvider = Provider<ProjectRepository>((ref) {
  return ProjectRepository();
});
