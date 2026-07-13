// Responsibility: Enhanced video controls with rewind, forward, restart, and improved reliability

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:video_player/video_player.dart';

class EnhancedVideoControls extends StatelessWidget {
  const EnhancedVideoControls({
    super.key,
    required this.videoController,
    required this.frameIndex,
    required this.totalFrames,
    required this.fps,
    required this.isPlaying,
    this.onPreviousFrame,
    this.onNextFrame,
    required this.onPlayPause,
    required this.onRewind,
    required this.onForward,
    required this.onRestart,
    required this.onGoToFrame,
    this.buttonSize = 1.0,
    this.hapticFeedback = true,
  });

  final VideoPlayerController? videoController;
  final int frameIndex;
  final int totalFrames;
  final double fps;
  final bool isPlaying;
  final VoidCallback? onPreviousFrame;
  final VoidCallback? onNextFrame;
  final VoidCallback onPlayPause;
  final VoidCallback onRewind;
  final VoidCallback onForward;
  final VoidCallback onRestart;
  final ValueChanged<int> onGoToFrame;
  final double buttonSize;
  final bool hapticFeedback;

  void _triggerHaptic() {
    if (hapticFeedback) {
      HapticFeedback.lightImpact();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // Main video controls
        Padding(
          padding: const EdgeInsets.fromLTRB(8, 8, 8, 0),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              // Rewind button (10 seconds back)
              Expanded(
                child: _ControlButton(
                  icon: Icons.fast_rewind,
                  size: 32 * buttonSize,
                  onPressed: frameIndex > (fps * 10).round() ? () {
                    _triggerHaptic();
                    onRewind();
                  } : null,
                  tooltip: 'Rewind 10 seconds',
                ),
              ),
              
              // Previous frame button
              Expanded(
                child: _ControlButton(
                  icon: Icons.skip_previous,
                  size: 36 * buttonSize,
                  onPressed: (frameIndex > 0 && onPreviousFrame != null) ? () {
                    _triggerHaptic();
                    onPreviousFrame!();
                  } : null,
                  tooltip: 'Previous frame',
                ),
              ),
              
              // Play/Pause button
              Expanded(
                child: _ControlButton(
                  icon: isPlaying ? Icons.pause_circle_filled : Icons.play_circle_filled,
                  size: 48 * buttonSize,
                  onPressed: () {
                    _triggerHaptic();
                    onPlayPause();
                  },
                  tooltip: isPlaying ? 'Pause' : 'Play',
                  isPrimary: true,
                ),
              ),
              
              // Next frame button
              Expanded(
                child: _ControlButton(
                  icon: Icons.skip_next,
                  size: 36 * buttonSize,
                  onPressed: (frameIndex < totalFrames - 1 && onNextFrame != null) ? () {
                    _triggerHaptic();
                    onNextFrame!();
                  } : null,
                  tooltip: 'Next frame',
                ),
              ),
              
              // Forward button (10 seconds forward)
              Expanded(
                child: _ControlButton(
                  icon: Icons.fast_forward,
                  size: 32 * buttonSize,
                  onPressed: frameIndex < totalFrames - (fps * 10).round() ? () {
                    _triggerHaptic();
                    onForward();
                  } : null,
                  tooltip: 'Forward 10 seconds',
                ),
              ),
            ],
          ),
        ),
        
        const SizedBox(height: 12),
        
        // Frame scrubber with enhanced features
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: _EnhancedFrameScrubber(
            frameIndex: frameIndex,
            totalFrames: totalFrames,
            fps: fps,
            onGoToFrame: onGoToFrame,
            buttonSize: buttonSize,
          ),
        ),
        
        const SizedBox(height: 12),
        
        // Additional controls row
        Row(
          children: [
            // Restart button
            Expanded(
              child: _TextButton(
                icon: Icons.restart_alt,
                text: 'Restart',
                onPressed: () {
                  _triggerHaptic();
                  onRestart();
                },
              ),
            ),
            
            // Set start frame
            Expanded(
              child: _TextButton(
                icon: Icons.keyboard_double_arrow_left,
                text: 'Set Start',
                onPressed: () {
                  _triggerHaptic();
                  // TODO: Implement set start frame
                },
              ),
            ),
            
            // Set end frame
            Expanded(
              child: _TextButton(
                icon: Icons.keyboard_double_arrow_right,
                text: 'Set End',
                onPressed: () {
                  _triggerHaptic();
                  // TODO: Implement set end frame
                },
              ),
            ),
          ],
        ),
      ],
    );
  }
}

class _ControlButton extends StatelessWidget {
  const _ControlButton({
    required this.icon,
    required this.size,
    required this.onPressed,
    required this.tooltip,
    this.isPrimary = false,
  });

  final IconData icon;
  final double size;
  final VoidCallback? onPressed;
  final String tooltip;
  final bool isPrimary;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 4),
      child: Tooltip(
        message: tooltip,
        child: IconButton(
          icon: Icon(icon),
          iconSize: size,
          onPressed: onPressed,
          style: IconButton.styleFrom(
            backgroundColor: isPrimary 
                ? Theme.of(context).colorScheme.primary.withValues(alpha: 0.2)
                : onPressed != null 
                    ? Colors.white.withValues(alpha: 0.1)
                    : Colors.transparent,
            foregroundColor: isPrimary 
                ? Theme.of(context).colorScheme.primary
                : Colors.white,
            padding: EdgeInsets.all(size * 0.3),
          ),
        ),
      ),
    );
  }
}

class _TextButton extends StatelessWidget {
  const _TextButton({
    required this.icon,
    required this.text,
    required this.onPressed,
  });

  final IconData icon;
  final String text;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return TextButton.icon(
      onPressed: onPressed,
      icon: Icon(icon, size: 16),
      label: Text(
        text,
        style: const TextStyle(fontSize: 11),
        overflow: TextOverflow.ellipsis,
      ),
      style: TextButton.styleFrom(
        foregroundColor: Colors.white70,
        padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 8),
        tapTargetSize: MaterialTapTargetSize.shrinkWrap,
      ),
    );
  }
}

class _EnhancedFrameScrubber extends StatefulWidget {
  const _EnhancedFrameScrubber({
    required this.frameIndex,
    required this.totalFrames,
    required this.fps,
    required this.onGoToFrame,
    required this.buttonSize,
  });

  final int frameIndex;
  final int totalFrames;
  final double fps;
  final ValueChanged<int> onGoToFrame;
  final double buttonSize;

  @override
  State<_EnhancedFrameScrubber> createState() => _EnhancedFrameScrubberState();
}

class _EnhancedFrameScrubberState extends State<_EnhancedFrameScrubber> {

  String _formatTime(int frame) {
    final seconds = frame / widget.fps;
    final minutes = seconds ~/ 60;
    final remainingSeconds = (seconds % 60).toStringAsFixed(1).padLeft(4, '0');
    return '$minutes:$remainingSeconds';
  }

  @override
  Widget build(BuildContext context) {
    final maxIndex = widget.totalFrames > 0 ? widget.totalFrames - 1 : 0;
    final currentValue = widget.frameIndex.clamp(0, maxIndex).toDouble();
    
    return Column(
      children: [
        // Time display
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              _formatTime(widget.frameIndex),
              style: TextStyle(
                color: Colors.white70,
                fontSize: 12 * widget.buttonSize,
                fontFamily: 'monospace',
              ),
            ),
            Text(
              '${widget.frameIndex + 1} / ${widget.totalFrames}',
              style: TextStyle(
                color: Colors.white70,
                fontSize: 12 * widget.buttonSize,
                fontWeight: FontWeight.bold,
              ),
            ),
            Text(
              _formatTime(widget.totalFrames - 1),
              style: TextStyle(
                color: Colors.white70,
                fontSize: 12 * widget.buttonSize,
                fontFamily: 'monospace',
              ),
            ),
          ],
        ),
        
        const SizedBox(height: 8),
        
        // Enhanced slider
        SliderTheme(
          data: SliderTheme.of(context).copyWith(
            activeTrackColor: Theme.of(context).colorScheme.primary,
            inactiveTrackColor: Colors.white.withValues(alpha: 0.2),
            thumbColor: Colors.white,
            overlayColor: Colors.white.withValues(alpha: 0.2),
            thumbShape: RoundSliderThumbShape(
              enabledThumbRadius: 8 * widget.buttonSize,
            ),
            trackHeight: 4 * widget.buttonSize,
          ),
          child: Slider(
            value: currentValue,
            min: 0,
            max: maxIndex.toDouble(),
            onChanged: widget.totalFrames <= 0
                ? null
                : (value) {
                    widget.onGoToFrame(value.round());
                  },
          ),
        ),
      ],
    );
  }
}
