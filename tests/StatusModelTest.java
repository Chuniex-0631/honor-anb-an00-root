package com.anb.root;

/** Guards the screen's decision table: which headline, badge and primary action each state gets. */
public final class StatusModelTest {
    static void check(boolean ok, String label) { if (!ok) throw new AssertionError(label); }

    static StatusModel.Snapshot snap(Readiness.Gate gate, String phase, boolean busy, boolean checking,
                                     boolean setup, boolean module) {
        return StatusModel.of(gate, phase, busy, checking, setup, module);
    }

    public static void main(String[] args) {
        StatusModel.Snapshot s;

        s = snap(Readiness.Gate.DEVICE, "SUCCESS", true, true, true, true);
        check(s.headline == StatusModel.Headline.UNSUPPORTED, "unsupported device always wins");
        check(s.action == StatusModel.Action.NONE, "unsupported device offers no action");
        check(s.tone == StatusModel.Tone.WARNING, "unsupported device warns");

        s = snap(Readiness.Gate.READY, "IDLE", false, true, true, false);
        check(s.headline == StatusModel.Headline.CHECK_ENV, "a running check owns the screen");
        check(s.badge == StatusModel.Badge.CHECKING, "a running check shows the checking badge");
        check(s.action == StatusModel.Action.CHECK_ENVIRONMENT, "checking action re-runs the check");

        s = snap(Readiness.Gate.READY, "RUNNING", true, false, true, false);
        check(s.headline == StatusModel.Headline.WORKING, "busy shows working");
        check(s.action == StatusModel.Action.NONE, "busy disables the primary action");

        s = snap(Readiness.Gate.READY, "SUCCESS", false, false, true, true);
        check(s.headline == StatusModel.Headline.SUCCESS, "success is sticky");
        check(s.badge == StatusModel.Badge.VERIFIED, "success is verified");
        check(s.action == StatusModel.Action.VERIFY, "success can be re-verified");

        s = snap(Readiness.Gate.READY, "WAIT_AUTH", false, false, true, true);
        check(s.headline == StatusModel.Headline.WAIT_AUTH, "waiting for authorization");
        check(s.badge == StatusModel.Badge.WAIT_AUTH, "waiting badge");
        check(s.action == StatusModel.Action.VERIFY, "waiting action is verify");

        s = snap(Readiness.Gate.READY, "FAILED", false, false, true, false);
        check(s.headline == StatusModel.Headline.FAILED, "failure headline");
        check(s.badge == StatusModel.Badge.ATTENTION, "failure badge");
        check(s.action == StatusModel.Action.SAVE_DIAGNOSTICS, "failure offers diagnostics, not retry");

        Readiness.Gate[] gates = {Readiness.Gate.SHIZUKU_INSTALL, Readiness.Gate.KSU_INSTALL,
                Readiness.Gate.SHIZUKU_START, Readiness.Gate.SHIZUKU_VERSION,
                Readiness.Gate.SHIZUKU_PERMISSION, Readiness.Gate.SHELL_MODE, Readiness.Gate.ENVIRONMENT};
        StatusModel.Headline[] headlines = {StatusModel.Headline.INSTALL_SHIZUKU, StatusModel.Headline.INSTALL_KSU,
                StatusModel.Headline.START_SHIZUKU, StatusModel.Headline.UPDATE_SHIZUKU,
                StatusModel.Headline.GRANT_SHIZUKU, StatusModel.Headline.SHELL_MODE, StatusModel.Headline.CHECK_ENV};
        StatusModel.Action[] actions = {StatusModel.Action.SHIZUKU_DOWNLOAD, StatusModel.Action.KSU_DOWNLOAD,
                StatusModel.Action.OPEN_SHIZUKU, StatusModel.Action.SHIZUKU_DOWNLOAD,
                StatusModel.Action.REQUEST_SHIZUKU, StatusModel.Action.OPEN_SHIZUKU,
                StatusModel.Action.CHECK_ENVIRONMENT};
        for (int i = 0; i < gates.length; i++) {
            s = snap(gates[i], "IDLE", false, false, true, false);
            check(s.headline == headlines[i], "gate " + gates[i] + " headline");
            check(s.action == actions[i], "gate " + gates[i] + " action");
        }

        s = snap(Readiness.Gate.READY, "IDLE", false, false, false, false);
        check(s.headline == StatusModel.Headline.FIRST_SETUP, "ready but unset asks for first setup");
        check(s.action == StatusModel.Action.FINISH_SETUP, "first setup action");
        check(s.badge == StatusModel.Badge.FIRST_RUN, "first run badge");

        s = snap(Readiness.Gate.READY, "IDLE", false, false, true, false);
        check(s.headline == StatusModel.Headline.READY, "ready after setup");
        check(s.action == StatusModel.Action.RESTORE, "ready restores");
        check(s.badge == StatusModel.Badge.STATUS, "ready badge is the plain status chip");

        s = snap(Readiness.Gate.READY, "IDLE", false, false, true, true);
        check(s.action == StatusModel.Action.VERIFY, "an already loaded module is only verified");

        System.out.println("PASS: 8 gates, precedence (device > checking > busy > current gate > phase), setup/module actions, badges");
    }
}
