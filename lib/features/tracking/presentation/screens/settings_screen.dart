// Responsibility: App settings and preferences management

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

class SettingsScreen extends ConsumerStatefulWidget {
  const SettingsScreen({super.key});

  @override
  ConsumerState<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends ConsumerState<SettingsScreen> {
  bool _landscapeMode = false;
  bool _autoRotate = true;
  bool _showFrameNumbers = true;
  bool _hapticFeedback = true;
  double _buttonSize = 1.0;
  bool _autoSaveProgress = true;

  @override
  void initState() {
    super.initState();
    _loadSettings();
  }

  Future<void> _loadSettings() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      _landscapeMode = prefs.getBool('landscape_mode') ?? false;
      _autoRotate = prefs.getBool('auto_rotate') ?? true;
      _showFrameNumbers = prefs.getBool('show_frame_numbers') ?? true;
      _hapticFeedback = prefs.getBool('haptic_feedback') ?? true;
      _buttonSize = prefs.getDouble('button_size') ?? 1.0;
      _autoSaveProgress = prefs.getBool('auto_save_progress') ?? true;
    });
  }

  Future<void> _saveSettings() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('landscape_mode', _landscapeMode);
    await prefs.setBool('auto_rotate', _autoRotate);
    await prefs.setBool('show_frame_numbers', _showFrameNumbers);
    await prefs.setBool('haptic_feedback', _hapticFeedback);
    await prefs.setDouble('button_size', _buttonSize);
    await prefs.setBool('auto_save_progress', _autoSaveProgress);

    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Settings saved successfully')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        title: const Text('Settings'),
        backgroundColor: Colors.black,
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            icon: const Icon(Icons.save),
            onPressed: _saveSettings,
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Display Settings Section
          _SectionHeader(title: 'Display Settings', icon: Icons.display_settings),
          _SwitchTile(
            title: 'Landscape Mode',
            subtitle: 'Use landscape orientation for video analysis',
            value: _landscapeMode,
            onChanged: (value) => setState(() => _landscapeMode = value),
          ),
          _SwitchTile(
            title: 'Auto-rotate Videos',
            subtitle: 'Automatically match video orientation',
            value: _autoRotate,
            onChanged: (value) => setState(() => _autoRotate = value),
          ),
          _SwitchTile(
            title: 'Show Frame Numbers',
            subtitle: 'Display frame numbers in controls',
            value: _showFrameNumbers,
            onChanged: (value) => setState(() => _showFrameNumbers = value),
          ),

          const SizedBox(height: 24),

          // Interaction Settings Section
          _SectionHeader(title: 'Interaction', icon: Icons.touch_app),
          _SwitchTile(
            title: 'Haptic Feedback',
            subtitle: 'Vibrate on button interactions',
            value: _hapticFeedback,
            onChanged: (value) => setState(() => _hapticFeedback = value),
          ),
          _SliderTile(
            title: 'Button Size',
            subtitle: 'Adjust control button sizes',
            value: _buttonSize,
            min: 0.8,
            max: 1.5,
            divisions: 7,
            onChanged: (value) => setState(() => _buttonSize = value),
            formatter: (value) => '${(value * 100).round()}%',
          ),

          const SizedBox(height: 24),

          // Data Settings Section
          _SectionHeader(title: 'Data Management', icon: Icons.storage),
          _SwitchTile(
            title: 'Auto-save Progress',
            subtitle: 'Automatically save tracking progress',
            value: _autoSaveProgress,
            onChanged: (value) => setState(() => _autoSaveProgress = value),
          ),

          const SizedBox(height: 24),

          // About Section
          _SectionHeader(title: 'About', icon: Icons.info),
          ListTile(
            title: const Text('Version', style: TextStyle(color: Colors.white)),
            subtitle: const Text('0.1.0', style: TextStyle(color: Colors.white70)),
            leading: const Icon(Icons.info_outline, color: Colors.white70),
          ),
          ListTile(
            title: const Text('Reset Orientation Setup', style: TextStyle(color: Colors.white)),
            subtitle: const Text('Show orientation setup on next launch', style: TextStyle(color: Colors.white70)),
            leading: const Icon(Icons.refresh, color: Colors.white70),
            onTap: () async {
              final messenger = ScaffoldMessenger.of(context);
              final prefs = await SharedPreferences.getInstance();
              await prefs.remove('orientation_setup_complete');
              messenger.showSnackBar(
                const SnackBar(content: Text('Orientation setup will show on next launch')),
              );
            },
          ),
        ],
      ),
    );
  }
}

class _SectionHeader extends StatelessWidget {
  const _SectionHeader({required this.title, required this.icon});

  final String title;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: Row(
        children: [
          Icon(icon, color: Theme.of(context).colorScheme.primary, size: 20),
          const SizedBox(width: 8),
          Text(
            title,
            style: TextStyle(
              color: Theme.of(context).colorScheme.primary,
              fontSize: 18,
              fontWeight: FontWeight.bold,
            ),
          ),
        ],
      ),
    );
  }
}

class _SwitchTile extends StatelessWidget {
  const _SwitchTile({
    required this.title,
    required this.subtitle,
    required this.value,
    required this.onChanged,
  });

  final String title;
  final String subtitle;
  final bool value;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) {
    return SwitchListTile(
      title: Text(title, style: const TextStyle(color: Colors.white)),
      subtitle: Text(subtitle, style: const TextStyle(color: Colors.white70)),
      value: value,
      onChanged: onChanged,
      activeThumbColor: Theme.of(context).colorScheme.primary,
    );
  }
}

class _SliderTile extends StatelessWidget {
  const _SliderTile({
    required this.title,
    required this.subtitle,
    required this.value,
    required this.min,
    required this.max,
    required this.divisions,
    required this.onChanged,
    required this.formatter,
  });

  final String title;
  final String subtitle;
  final double value;
  final double min;
  final double max;
  final int divisions;
  final ValueChanged<double> onChanged;
  final String Function(double) formatter;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        ListTile(
          title: Text(title, style: const TextStyle(color: Colors.white)),
          subtitle: Text(subtitle, style: const TextStyle(color: Colors.white70)),
          trailing: Text(
            formatter(value),
            style: TextStyle(
              color: Theme.of(context).colorScheme.primary,
              fontWeight: FontWeight.bold,
            ),
          ),
        ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: Slider(
            value: value,
            min: min,
            max: max,
            divisions: divisions,
            onChanged: onChanged,
            activeColor: Theme.of(context).colorScheme.primary,
          ),
        ),
      ],
    );
  }
}
