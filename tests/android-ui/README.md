# UI-only device regression

This instrumentation package runs against the real `MainActivity`. It verifies the three pages, text layout bounds, task-specific button and progress states, repeated ticker ownership, typed-failure actions, all six FAQ entries, cancellation of the shell-option confirmation, and modal handoff/pause cleanup.

Task and phase transitions are **presentation simulations** under a task lease. The runner never calls `RecoveryEngine.start`, taps the restore/verify button, changes saved recovery options, reboots, or executes the payload. Starting the Activity can perform its ordinary read-only environment probe. Run only when no real recovery task is active and the prerequisites are ready.

1. Build the main app with `build.ps1`.
2. Run `tools/build_ui_tests.ps1` with the same SDK tool paths.
3. Sign both APKs with the same development certificate; private release keys are not included here.
4. Install the main APK and the test APK, then run:

```text
adb shell am instrument -w com.anb.root.uitest/.UiAuditInstrumentation
```

Repeat in light/dark mode, with font scale 1.0/2.0, and in portrait/landscape. Restore device display settings afterward. Uninstall `com.anb.root.uitest` when finished. The test package is never part of the production APK or release ZIP.

Pressed-state animation and drag cancellation require actual touch input and inspection of multiple frames or a recording; passing this test does not substitute for those visual checks. System document pickers, external app links and real root recovery are separate checks.
