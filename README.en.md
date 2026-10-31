# ANB Root — HONOR 80 Pro Flat / ANB-AN00 Temporary Root

[简体中文](README.md) · [Download APK](https://github.com/Chuniex-0631/honor-anb-an00-root/releases/latest) · [Compatibility](docs/COMPATIBILITY.md)

ANB Root restores **session-only root** on a verified HONOR ANB-AN00 firmware using **Shizuku UserService, GhostLock and an Honor-specific KernelSU LKM**. Start Shizuku through Android wireless debugging, grant this app permission, and run recovery on the phone. No computer is required for the established phone-only workflow.

## Supported target

- HONOR 80 Pro **Flat**, ANB-AN00 / HNANB; not the ANP curved variant.
- Android 15 / MagicOS `9.0.0.230(CHNC00E230R2P5)`.
- Kernel `5.10.236-android12-9-gd671cf8c2b02`.
- Official Shizuku 13+ started in **shell mode through wireless debugging**.
- KernelSU Manager v3.3.0 and the bundled device-specific module.

This does not unlock the bootloader or provide persistent root. A full reboot clears the temporary kernel state. Start Shizuku again after reboot, then run recovery. Pairing or developer settings may need to be restored if revoked.

## Usage

1. Download the APK from [Releases](https://github.com/Chuniex-0631/honor-anb-an00-root/releases/latest) and compare its [SHA-256](release/SHA256SUMS.txt).
2. Install official Shizuku and KernelSU v3.3.0; pair and start Shizuku via wireless debugging.
3. Grant Shizuku access, pass the prerequisite checks, and complete initial setup.
4. After the phone has been up for about 90 seconds, run recovery. Grant this app root access in KernelSU when requested.
5. Wait for the verified-root result. If the manager shows stale state, use manager refresh; do not repeat the exploit.

The app uses no fixed TCP 5555 client, imports no desktop ADB private key, and requests no Internet permission. Recovery is serialized and does not automatically re-inject after timeout or disconnect. The race may fail or reboot the phone; support is not a universal reliability claim.

[Build](docs/BUILD.md) · [Architecture](docs/ARCHITECTURE.md) · [Validation](docs/VALIDATION.md) · [History](docs/RESEARCH_HISTORY.md) · [Privacy](docs/PRIVACY.md)

Public research retains model, firmware, kernel and reproducibility hashes. It excludes device identifiers, private mail IDs, credentials, raw private logs, signing keys and operator-specific paths. Historical snapshots are labeled with their original dates.

App-owned source is Apache-2.0. Third-party components retain their terms, including GPL-2.0 KernelSU components. See [NOTICE](NOTICE) and [third-party source links](docs/THIRD_PARTY.md).
