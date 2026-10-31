#!/usr/bin/env python3
"""Static UI regression guard for the v2.2.2 single-screen refactor.

This complements (does not replace) Android compilation and device interaction tests.
"""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parent.parent
activity = (root / "app/java/com/anb/root/MainActivity.java").read_text(encoding="utf-8")
engine = (root / "app/java/com/anb/root/RecoveryEngine.java").read_text(encoding="utf-8")
screen = activity[activity.index("private void buildUi() {"):activity.index("private TextView statusRow(")]
manager = activity[activity.index("private void openKernelSuSmart() {"):activity.index("private void buildRecords(")]
manifest = ET.parse(root / "app/AndroidManifest.xml").getroot()
ns = "{http://schemas.android.com/apk/res/android}"

assert screen.count("new ScrollView(this)") == 1, "Single scroll screen required"
for element in ("buildHome(body)", "buildSettings(body)", "buildRecords(body)"):
    assert screen.count(element) == 1, f"Missing single-page section: {element}"
assert screen.index("buildHome(body)") < screen.index("buildSettings(body)") < screen.index("buildRecords(body)")
for forbidden in ("selectPage(", "selectedPage", "navItems", "navIcons", "mark=label(\"A\"", "navigation.addView"):
    assert forbidden not in activity, f"Legacy tab or A-logo: {forbidden}"
assert "contentScroll.post(()->contentScroll.scrollTo(0,y))" in activity
assert "expandedLog" in activity and "out.putBoolean(\"expanded_log\"" in activity
assert "buildUi();" in activity and "onConfigurationChanged(Configuration configuration)" in activity

assert "button(\"打开 Shizuku\"" in activity
assert "button(\"打开 KernelSU\"" in activity
assert "ManagerAction.resolve(" in manager
assert "engine.refreshManagerOnly()" in manager
assert "openPackage(RecoveryEngine.KSU,KSU_URL)" in manager
assert "openUri(KSU_URL)" in manager
assert "startRecovery(" not in manager, "Manager action must never start exploit"
assert "public void refreshManagerOnly()" in engine and "am force-stop " in engine, "Existing manager refresh must remain"
assert "engine.selectAllowShell(true)" in activity and "new AlertDialog.Builder(this)" in activity
assert "if(!expandedLog)return;" in activity
assert "all.length()>32000" in activity
assert "startActivityForResult(intent,EXPORT_LOG)" in activity
assert "case RECOVER_OR_VERIFY:" in activity
assert "if(engine.busy.get()||engine.checking||engine.gate()!=Readiness.Gate.READY" in activity

assert manifest.attrib.get("package") == "com.anb.root"
app = manifest.find("application")
assert app is not None
assert app.attrib[ns+"icon"] == "@mipmap/ic_launcher"
assert app.attrib[ns+"roundIcon"] == "@mipmap/ic_launcher"
assert app.attrib[ns+"debuggable"] == "false"
assert not any(x.attrib.get(ns+"name") == "android.permission.INTERNET" for x in manifest.findall("uses-permission"))

launcher = ET.parse(root / "app/res/mipmap-anydpi-v26/ic_launcher.xml").getroot()
assert launcher.find("foreground") is not None
assert launcher.find("monochrome") is not None
assert launcher.find("background") is not None
assert launcher.find("background").attrib[ns+"drawable"] == "@color/icon_background"
for icon in ("ic_launcher", "ic_launcher_foreground", "ic_launcher_monochrome"):
    path = root / f"app/res/drawable/{icon}.xml"
    tree = ET.parse(path).getroot()
    assert len(tree.findall("path")) >= 3, f"Missing shield/key geometry in {icon}"

for path in ("nav_home.xml", "nav_settings.xml", "nav_records.xml"):
    assert not (root / "app/res/drawable" / path).exists(), f"Unused tab icon {path}"
print("PASS: v2.2.2 single-scroll layout, side-by-side app shortcuts, action guards, icon resources")
