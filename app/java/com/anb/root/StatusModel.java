package com.anb.root;

/**
 * Pure presentation model: turns the engine's (gate, phase, flags) into the screen's
 * headline, badge, primary action and tone. No Android types are referenced so the
 * mapping can be unit tested on a plain JVM, and the activity stays a thin renderer.
 */
public final class StatusModel {
    public enum Headline { UNSUPPORTED, INSTALL_SHIZUKU, INSTALL_KSU, START_SHIZUKU, UPDATE_SHIZUKU, GRANT_SHIZUKU, SHELL_MODE, CHECK_ENV, FIRST_SETUP, READY, WORKING, WAIT_AUTH, SUCCESS, FAILED }
    public enum Badge { UNSUPPORTED, FIRST_RUN, STATUS, CHECKING, WORKING, WAIT_AUTH, VERIFIED, ATTENTION }
    public enum Action { NONE, SHIZUKU_DOWNLOAD, KSU_DOWNLOAD, OPEN_SHIZUKU, REQUEST_SHIZUKU, CHECK_ENVIRONMENT, FINISH_SETUP, RESTORE, VERIFY, CHECK_PROGRESS, SAVE_DIAGNOSTICS }
    public enum Tone { NEUTRAL, PROGRESS, SUCCESS, WARNING }

    public static final class Snapshot {
        public final Headline headline;
        public final Badge badge;
        public final Action action;
        public final Tone tone;
        Snapshot(Headline headline, Badge badge, Action action, Tone tone) {
            this.headline = headline; this.badge = badge; this.action = action; this.tone = tone;
        }
    }

    private StatusModel() { }

    /**
     * Precedence: an unsupported device can never act; a running check or recovery owns the
     * screen; prerequisites precede historical results. Failures offer a typed, safe next action.
     */
    public static Snapshot of(Readiness.Gate gate, String phase, boolean busy, boolean checking,
                              boolean setup, boolean modulePresent) {
        return of(gate,phase,busy,checking,setup,modulePresent,FailureText.Kind.UNKNOWN);
    }
    public static Snapshot of(Readiness.Gate gate, String phase, boolean busy, boolean checking,
                              boolean setup, boolean modulePresent,FailureText.Kind failure) {
        if (gate == Readiness.Gate.DEVICE) return new Snapshot(Headline.UNSUPPORTED, Badge.UNSUPPORTED, Action.NONE, Tone.WARNING);
        if (checking) return new Snapshot(Headline.CHECK_ENV, Badge.CHECKING, Action.CHECK_ENVIRONMENT, Tone.PROGRESS);
        if (busy) return new Snapshot(Headline.WORKING, Badge.WORKING, Action.NONE, Tone.PROGRESS);
        Badge idle = setup ? Badge.STATUS : Badge.FIRST_RUN;
        switch (gate) {
            case SHIZUKU_INSTALL: return new Snapshot(Headline.INSTALL_SHIZUKU, idle, Action.SHIZUKU_DOWNLOAD, Tone.NEUTRAL);
            case KSU_INSTALL: return new Snapshot(Headline.INSTALL_KSU, idle, Action.KSU_DOWNLOAD, Tone.NEUTRAL);
            case SHIZUKU_START: return new Snapshot(Headline.START_SHIZUKU, idle, Action.OPEN_SHIZUKU, Tone.NEUTRAL);
            case SHIZUKU_VERSION: return new Snapshot(Headline.UPDATE_SHIZUKU, idle, Action.SHIZUKU_DOWNLOAD, Tone.NEUTRAL);
            case SHIZUKU_PERMISSION: return new Snapshot(Headline.GRANT_SHIZUKU, idle, Action.REQUEST_SHIZUKU, Tone.NEUTRAL);
            case SHELL_MODE: return new Snapshot(Headline.SHELL_MODE, idle, Action.OPEN_SHIZUKU, Tone.NEUTRAL);
            case ENVIRONMENT: return new Snapshot(Headline.CHECK_ENV, idle, Action.CHECK_ENVIRONMENT, Tone.NEUTRAL);
            case READY:
            default:
                if (!setup) return new Snapshot(Headline.FIRST_SETUP, Badge.FIRST_RUN, Action.FINISH_SETUP, Tone.SUCCESS);
                if ("SUCCESS".equals(phase)&&modulePresent) return new Snapshot(Headline.SUCCESS, Badge.VERIFIED, Action.VERIFY, Tone.SUCCESS);
                if ("WAIT_AUTH".equals(phase)&&modulePresent) return new Snapshot(Headline.WAIT_AUTH, Badge.WAIT_AUTH, Action.VERIFY, Tone.WARNING);
                if ("FAILED".equals(phase)) {
                    Action action=Action.SAVE_DIAGNOSTICS;
                    if(failure==FailureText.Kind.BOOT_WAIT)action=Action.RESTORE;
                    else if(failure==FailureText.Kind.CONCURRENT_RUN||failure==FailureText.Kind.TIMEOUT)action=Action.CHECK_PROGRESS;
                    else if(failure==FailureText.Kind.PREPARE||failure==FailureText.Kind.SHIZUKU||failure==FailureText.Kind.PERMISSION)action=Action.CHECK_ENVIRONMENT;
                    return new Snapshot(Headline.FAILED,Badge.ATTENTION,action,Tone.WARNING);
                }
                return modulePresent
                        ? new Snapshot(Headline.READY, Badge.STATUS, Action.VERIFY, Tone.SUCCESS)
                        : new Snapshot(Headline.READY, Badge.STATUS, Action.RESTORE, Tone.SUCCESS);
        }
    }
}
