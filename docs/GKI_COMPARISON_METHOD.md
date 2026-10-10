# ANB-AN00 public GKI comparison methodology

**This is deliberately NOT the HONOR factory kernel.**

The reference is Google's **android12-5.10-2025-05_r1**, Linux **5.10.236**, commit
`b97c62c4e7d1e80fb6a2cb0cb381f03bbcd26a4e`.

See Google's [official deprecated GKI builds](https://source.android.com/docs/core/architecture/kernel/gki-android12-5_10-deprecated-builds) and [tag](https://android.googlesource.com/kernel/common/+/refs/tags/android12-5.10-2025-05_r1).

We import the **real owner's ANB-AN00 /proc/config.gz** and run the candidate public tree's `olddefconfig` with runner LLVM, preserving device original and candidate-resolved text, compiler version, source commit, and all differences.

**This does not use the owner's exact Clang 12.0.5** for the first trial. Compiler version drift is therefore not considered evidence of OEM divergence. This is a **Kconfig vocabulary coverage** test only; all enabled settings absent from the public tree require separate investigation.

No real handset is accessed. No images are built for flashing, no kernel vulnerabilities tested, no symbol offsets claimed.