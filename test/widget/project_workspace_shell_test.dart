import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:physics_tracker/features/tracking/presentation/screens/project_screen.dart';

void main() {
  testWidgets('project workspace exposes video, data, and graphs tabs',
      (WidgetTester tester) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: ProjectWorkspaceShell(
          screens: <Widget>[
            Scaffold(body: Text('Video Screen')),
            Scaffold(body: Text('Data Screen')),
            Scaffold(body: Text('Graphs Screen')),
            Scaffold(body: Text('Profile Screen')),
            Scaffold(body: Text('Settings Screen')),
          ],
        ),
      ),
    );

    expect(find.text('Video Screen'), findsOneWidget);
    expect(find.text('Video'), findsOneWidget);
    expect(find.text('Data'), findsOneWidget);
    expect(find.text('Graphs'), findsOneWidget);
    expect(find.text('Profile'), findsOneWidget);

    await tester.tap(find.text('Data'));
    await tester.pumpAndSettle();
    expect(find.text('Data Screen'), findsOneWidget);

    await tester.tap(find.text('Graphs'));
    await tester.pumpAndSettle();
    expect(find.text('Graphs Screen'), findsOneWidget);
  });
}
