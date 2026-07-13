// Responsibility: Screen to set up and confirm orientation preferences

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

class OrientationSetupScreen extends ConsumerStatefulWidget {
  const OrientationSetupScreen({super.key});

  @override
  ConsumerState<OrientationSetupScreen> createState() => _OrientationSetupScreenState();
}

class _OrientationSetupScreenState extends ConsumerState<OrientationSetupScreen> {
  bool _isLoading = false;
  bool _landscapeMode = false;
  bool _autoRotate = true;

  @override
  void initState() {
    super.initState();
    _loadCurrentSettings();
  }

  Future<void> _loadCurrentSettings() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      _landscapeMode = prefs.getBool('landscape_mode') ?? false;
      _autoRotate = prefs.getBool('auto_rotate') ?? true;
    });
  }

  Future<void> _saveSettings() async {
    setState(() => _isLoading = true);
    
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('landscape_mode', _landscapeMode);
      await prefs.setBool('auto_rotate', _autoRotate);
      await prefs.setBool('orientation_setup_complete', true);
      
      if (mounted) {
        Navigator.of(context).pop(true);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error saving settings: $e')),
        );
      }
    } finally {
      setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        title: const Text('Display Settings'),
        backgroundColor: Colors.black,
        foregroundColor: Colors.white,
      ),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Choose Your Preferred Orientation',
              style: TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.bold,
                color: Colors.white,
              ),
            ),
            const SizedBox(height: 8),
            const Text(
              'This setting will be saved and used every time you use PhysicsTracker.',
              style: TextStyle(
                fontSize: 16,
                color: Colors.white70,
              ),
            ),
            const SizedBox(height: 32),
            
            // Orientation selection
            const Text(
              'Video Analysis Mode',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w600,
                color: Colors.white,
              ),
            ),
            const SizedBox(height: 16),
            
            Row(
              children: [
                Expanded(
                  child: _OrientationOption(
                    icon: Icons.stay_current_portrait,
                    title: 'Portrait',
                    description: 'Better for handheld use',
                    isSelected: !_landscapeMode,
                    onTap: () => setState(() => _landscapeMode = false),
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: _OrientationOption(
                    icon: Icons.stay_current_landscape,
                    title: 'Landscape',
                    description: 'Better for video analysis',
                    isSelected: _landscapeMode,
                    onTap: () => setState(() => _landscapeMode = true),
                  ),
                ),
              ],
            ),
            
            const SizedBox(height: 32),
            
            // Auto-rotate toggle
            SwitchListTile(
              title: const Text(
                'Auto-rotate videos',
                style: TextStyle(color: Colors.white, fontSize: 16),
              ),
              subtitle: const Text(
                'Automatically match video orientation',
                style: TextStyle(color: Colors.white70, fontSize: 14),
              ),
              value: _autoRotate,
              onChanged: (value) => setState(() => _autoRotate = value),
              activeThumbColor: Theme.of(context).colorScheme.primary,
            ),
            
            const Spacer(),
            
            // Action buttons
            Row(
              children: [
                TextButton(
                  onPressed: _isLoading ? null : () => Navigator.of(context).pop(false),
                  child: const Text('Maybe Later'),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: ElevatedButton(
                    onPressed: _isLoading ? null : _saveSettings,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Theme.of(context).colorScheme.primary,
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 16),
                    ),
                    child: _isLoading
                        ? const SizedBox(
                            height: 20,
                            width: 20,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Text('Save & Continue'),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _OrientationOption extends StatelessWidget {
  const _OrientationOption({
    required this.icon,
    required this.title,
    required this.description,
    required this.isSelected,
    required this.onTap,
  });

  final IconData icon;
  final String title;
  final String description;
  final bool isSelected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: isSelected 
              ? Theme.of(context).colorScheme.primary.withValues(alpha: 0.2)
              : Colors.white.withValues(alpha: 0.05),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(
            color: isSelected 
                ? Theme.of(context).colorScheme.primary
                : Colors.white.withValues(alpha: 0.2),
            width: 2,
          ),
        ),
        child: Column(
          children: [
            Icon(
              icon,
              size: 48,
              color: isSelected 
                  ? Theme.of(context).colorScheme.primary
                  : Colors.white70,
            ),
            const SizedBox(height: 12),
            Text(
              title,
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
                color: isSelected 
                    ? Theme.of(context).colorScheme.primary
                    : Colors.white,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              description,
              style: const TextStyle(
                fontSize: 12,
                color: Colors.white70,
              ),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }
}
