// Responsibility: Host the project workspace and expose phase analysis tools.
// Adapts navigation layout between Portrait (bottom NavigationBar)
// and Landscape (side NavigationRail) to maximise usable screen area.

import 'package:flutter/material.dart';

import '../../../data_table/presentation/screens/data_table_screen.dart';
import '../../../graphs/presentation/screens/graphs_screen.dart';
import '../../../line_profile/presentation/screens/line_profile_screen.dart';
import 'video_tracking_screen.dart';
import 'settings_screen.dart';

class ProjectScreen extends StatefulWidget {
  const ProjectScreen({super.key, required this.projectId});

  final String projectId;

  @override
  State<ProjectScreen> createState() => _ProjectScreenState();
}

class _ProjectScreenState extends State<ProjectScreen> {
  @override
  Widget build(BuildContext context) {
    return ProjectWorkspaceShell(
      screens: <Widget>[
        VideoTrackingScreen(projectId: widget.projectId),
        DataTableScreen(projectId: widget.projectId),
        GraphsScreen(projectId: widget.projectId),
        LineProfileScreen(projectId: widget.projectId),
        const SettingsScreen(),
      ],
    );
  }
}

class ProjectWorkspaceShell extends StatefulWidget {
  const ProjectWorkspaceShell({super.key, required this.screens});

  final List<Widget> screens;

  @override
  State<ProjectWorkspaceShell> createState() => _ProjectWorkspaceShellState();
}

class _ProjectWorkspaceShellState extends State<ProjectWorkspaceShell> {
  int _selectedIndex = 0;

  static const List<({IconData icon, IconData selectedIcon, String label})> _destinations = [
    (icon: Icons.my_location_outlined, selectedIcon: Icons.my_location, label: 'Video'),
    (icon: Icons.table_chart_outlined, selectedIcon: Icons.table_chart, label: 'Data'),
    (icon: Icons.show_chart_outlined, selectedIcon: Icons.show_chart, label: 'Graphs'),
    (icon: Icons.blur_linear_outlined, selectedIcon: Icons.blur_linear, label: 'Profile'),
    (icon: Icons.settings_outlined, selectedIcon: Icons.settings, label: 'Settings'),
  ];

  @override
  Widget build(BuildContext context) {
    assert(widget.screens.isNotEmpty, 'Project workspace requires at least one screen.');

    final isLandscape = MediaQuery.orientationOf(context) == Orientation.landscape;

    return isLandscape ? _buildLandscapeShell() : _buildPortraitShell();
  }

  // ─── Portrait: standard bottom NavigationBar ─────────────────────────────
  Widget _buildPortraitShell() {
    return Scaffold(
      body: IndexedStack(
        index: _selectedIndex,
        children: widget.screens,
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _selectedIndex,
        onDestinationSelected: (index) => setState(() => _selectedIndex = index),
        destinations: _destinations
            .map((d) => NavigationDestination(
                  icon: Icon(d.icon),
                  selectedIcon: Icon(d.selectedIcon),
                  label: d.label,
                ))
            .toList(),
      ),
    );
  }

  // ─── Landscape: side NavigationRail + body ────────────────────────────────
  Widget _buildLandscapeShell() {
    return Scaffold(
      body: Row(
        children: [
          // Compact side rail — saves ~80 px of vertical space vs bottom bar
          NavigationRail(
            selectedIndex: _selectedIndex,
            onDestinationSelected: (index) => setState(() => _selectedIndex = index),
            labelType: NavigationRailLabelType.all,
            minWidth: 72,
            destinations: _destinations
                .map((d) => NavigationRailDestination(
                      icon: Icon(d.icon),
                      selectedIcon: Icon(d.selectedIcon),
                      label: Text(
                        d.label,
                        style: const TextStyle(fontSize: 10),
                      ),
                    ))
                .toList(),
          ),
          // Thin divider between rail and content
          const VerticalDivider(thickness: 1, width: 1),
          // Main content area
          Expanded(
            child: IndexedStack(
              index: _selectedIndex,
              children: widget.screens,
            ),
          ),
        ],
      ),
    );
  }
}
