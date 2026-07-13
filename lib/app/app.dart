// Responsibility: App root widget with theme and routing.

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../features/projects/presentation/screens/home_screen.dart';

class PhysicsTrackerApp extends ConsumerWidget {
  const PhysicsTrackerApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return MaterialApp(
      title: 'PhysicsTracker',
      themeMode: ThemeMode.system,
      theme: ThemeData(
        colorSchemeSeed: const Color(0xFF1B5E20),
        useMaterial3: true,
        brightness: Brightness.light,
      ),
      darkTheme: ThemeData(
        colorSchemeSeed: const Color(0xFF1B5E20),
        useMaterial3: true,
        brightness: Brightness.dark,
      ),
      home: const HomeScreen(),
    );
  }
}
