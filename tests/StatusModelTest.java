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

        int combinations=0;
        String[] phases={"IDLE","CHECKING","PREPARING","RUNNING","LOADING","VERIFYING","WAIT_AUTH","SUCCESS","FAILED"};
        for(Readiness.Gate gate:Readiness.Gate.values())for(String phase:phases)
            for(FailureText.Kind failure:FailureText.Kind.values())for(int flags=0;flags<16;flags++){
                boolean busy=(flags&1)!=0,checking=(flags&2)!=0,setup=(flags&4)!=0,module=(flags&8)!=0;
                s=StatusModel.of(gate,phase,busy,checking,setup,module,failure);combinations++;
                check(s.headline!=null&&s.badge!=null&&s.action!=null&&s.tone!=null,"all states render");
                if(gate==Readiness.Gate.DEVICE)check(s.action==StatusModel.Action.NONE,"unsupported never acts");
                if(gate!=Readiness.Gate.READY)check(s.action!=StatusModel.Action.RESTORE&&s.action!=StatusModel.Action.VERIFY
                        &&s.action!=StatusModel.Action.CHECK_PROGRESS,"prerequisites precede recovery actions");
                if(gate==Readiness.Gate.READY&&!busy&&!checking&&!setup)
                    check(s.action==StatusModel.Action.FINISH_SETUP,"setup cannot be bypassed by historical state");
                if(busy&&!checking&&gate!=Readiness.Gate.DEVICE)check(s.action==StatusModel.Action.NONE,"busy cannot start a second task");
                if(module)check(s.action!=StatusModel.Action.RESTORE,"loaded module never offers another restore");
                if(gate==Readiness.Gate.READY&&!busy&&!checking&&setup&&phase.equals("FAILED")
                        &&(failure==FailureText.Kind.SECURITY_NOT_RESTORED||failure==FailureText.Kind.REBOOT_REQUIRED))
                    check(s.action==StatusModel.Action.SAVE_DIAGNOSTICS,"unsafe failure requires diagnostics");
                if(s.badge==StatusModel.Badge.VERIFIED)check(gate==Readiness.Gate.READY&&module&&setup&&!busy&&!checking&&phase.equals("SUCCESS"),"no false verified badge");
            }
        System.out.println("PASS: prerequisite precedence, actions, badges and "+combinations+" complete state combinations");
    }
}
