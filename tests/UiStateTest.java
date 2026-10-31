package com.anb.root;

/** JVM-only presentation and action routing regression coverage. */
public final class UiStateTest {
    private static void check(boolean result,String message) {
        if(!result)throw new AssertionError(message);
    }
    private static UiState state(Readiness.Gate gate,boolean setup,boolean checking,
                                 boolean busy,boolean module,String phase) {
        return UiState.resolve(gate,setup,checking,busy,module,phase,"进行任务","正在处理");
    }
    public static void main(String[] args) {
        Readiness.Gate[] gates={
            Readiness.Gate.DEVICE,
            Readiness.Gate.SHIZUKU_INSTALL,
            Readiness.Gate.KSU_INSTALL,
            Readiness.Gate.SHIZUKU_START,
            Readiness.Gate.SHIZUKU_VERSION,
            Readiness.Gate.SHIZUKU_PERMISSION,
            Readiness.Gate.SHELL_MODE,
            Readiness.Gate.ENVIRONMENT,
            Readiness.Gate.READY
        };
        UiState.Action[] actions={
            UiState.Action.NONE,
            UiState.Action.GET_SHIZUKU,
            UiState.Action.GET_KSU,
            UiState.Action.OPEN_SHIZUKU,
            UiState.Action.GET_SHIZUKU,
            UiState.Action.GRANT_SHIZUKU,
            UiState.Action.OPEN_SHIZUKU,
            UiState.Action.CHECK_ENVIRONMENT,
            UiState.Action.RECOVER_OR_VERIFY
        };
        for(int i=0;i<gates.length;i++) {
            UiState resolved=state(gates[i],true,false,false,false,"IDLE");
            check(resolved.action==actions[i],"gate action "+gates[i]);
            check(resolved.canAct()==(actions[i]!=UiState.Action.NONE),"action enabled "+gates[i]);
            check(!resolved.title.isEmpty()&&!resolved.primaryLabel.isEmpty(),"visible copy "+gates[i]);
        }
        check(state(Readiness.Gate.READY,false,false,false,false,"IDLE").action
                ==UiState.Action.COMPLETE_SETUP,"setup is a separate step");
        check(state(Readiness.Gate.READY,true,false,true,false,"RUNNING").action
                ==UiState.Action.NONE,"busy cannot start second recovery");
        check(state(Readiness.Gate.READY,true,true,false,false,"IDLE").action
                ==UiState.Action.NONE,"checking disables start");
        check(state(Readiness.Gate.DEVICE,true,false,true,false,"RUNNING").action
                ==UiState.Action.NONE,"unsupported device always fails closed");
        check(state(Readiness.Gate.SHIZUKU_START,true,false,false,true,"SUCCESS").action
                ==UiState.Action.OPEN_SHIZUKU,"stale success does not hide broken prerequisites");
        check(state(Readiness.Gate.READY,true,false,false,true,"WAIT_AUTH").action
                ==UiState.Action.RECOVER_OR_VERIFY,"post-recovery authorization");
        check(state(Readiness.Gate.READY,true,false,false,true,"SUCCESS").tone
                ==UiState.Tone.GOOD,"verified success color");
        check(state(Readiness.Gate.READY,true,false,false,false,"FAILED").action
                ==UiState.Action.CHECK_ENVIRONMENT,"failure never launches retry when checking");
        check(state(Readiness.Gate.READY,true,false,false,true,"IDLE").action
                ==UiState.Action.RECOVER_OR_VERIFY,"present module uses verification path");
        check(state(Readiness.Gate.READY,true,false,false,false,"IDLE").action
                ==UiState.Action.RECOVER_OR_VERIFY,"ready uses recovery path");
        UiState active=UiState.resolve(Readiness.Gate.READY,true,false,true,false,"RUNNING",
                "自定义标题","自定义信息");
        check(active.title.equals("自定义标题")&&active.message.equals("自定义信息"),
                "running stage from engine is shown");
        System.out.println("PASS: presentation matrix, recovery gates, setup, stale success, fail-closed, safe retry");
    }
}
