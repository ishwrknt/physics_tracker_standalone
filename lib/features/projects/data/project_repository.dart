// Responsibility: Persist and load projects and summaries from Hive storage.

import 'dart:io';

import 'package:hive/hive.dart';

import '../../../../shared/models/project.dart';
import '../../../../shared/models/project_summary.dart';

const String _projectsIndexBox = 'projects_index';
const String _projectBoxPrefix = 'project_';
const String _projectKey = 'project';

class ProjectRepository {
  Future<Box<ProjectSummary>> _openIndexBox() async {
    return Hive.openBox<ProjectSummary>(_projectsIndexBox);
  }

  Future<Box<Project>> _openProjectBox(String projectId) async {
    return Hive.openBox<Project>('$_projectBoxPrefix$projectId');
  }

  Future<List<ProjectSummary>> loadSummaries() async {
    final Box<ProjectSummary> box = await _openIndexBox();
    return box.values.toList()..sort((a, b) => b.createdAt.compareTo(a.createdAt));
  }

  Future<Project> loadProject(String projectId) async {
    final Box<Project> box = await _openProjectBox(projectId);
    final Project? project = box.get(_projectKey);
    if (project == null) {
      throw StateError('Project not found');
    }
    return project;
  }

  Future<void> saveProject(Project project) async {
    final Box<Project> projectBox = await _openProjectBox(project.id);
    await projectBox.put(_projectKey, project);

    final Box<ProjectSummary> indexBox = await _openIndexBox();
    final ProjectSummary? existing = indexBox.get(project.id);
    if (existing != null) {
      await indexBox.put(
        project.id,
        existing.copyWith(
          name: project.name,
          videoPath: project.videoPath,
        ),
      );
    }
  }

  Future<Project> createProject(Project project, ProjectSummary summary) async {
    final Box<Project> projectBox = await _openProjectBox(project.id);
    await projectBox.put(_projectKey, project);

    final Box<ProjectSummary> indexBox = await _openIndexBox();
    await indexBox.put(summary.id, summary);

    return project;
  }

  Future<void> deleteProject(String projectId) async {
    final Box<Project> projectBox = await _openProjectBox(projectId);
    final Project? project = projectBox.get(_projectKey);

    final Box<ProjectSummary> indexBox = await _openIndexBox();
    final ProjectSummary? summary = indexBox.get(projectId);
    if (summary != null) {
      await indexBox.delete(projectId);
      if (summary.thumbnailPath != null) {
        final File thumbnail = File(summary.thumbnailPath!);
        if (await thumbnail.exists()) {
          await thumbnail.delete();
        }
      }
    }

    if (project != null && project.videoPath.isNotEmpty) {
      final File videoFile = File(project.videoPath);
      if (await videoFile.exists()) {
        await videoFile.delete();
      }
    }

    await projectBox.delete(_projectKey);
    await projectBox.close();
    await Hive.deleteBoxFromDisk('$_projectBoxPrefix$projectId');
  }
}
