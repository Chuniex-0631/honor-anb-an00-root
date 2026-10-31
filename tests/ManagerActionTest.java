package com.anb.root;

/** Full truth table for KernelSU's single combined open/refresh button. */
public final class ManagerActionTest {
    private static void check(boolean okay,String label) {
        if(!okay)throw new AssertionError(label);
    }
    public static void main(String[] args) {
        int scenarios=0;
        for(int mask=0;mask<32;mask++) {
            boolean installed=(mask&1)!=0;
            boolean busy=(mask&2)!=0;
            boolean checking=(mask&4)!=0;
            boolean module=(mask&8)!=0;
            boolean ready=(mask&16)!=0;
            ManagerAction.Action got=ManagerAction.resolve(installed,busy,checking,module,ready);
            ManagerAction.Action expected=!installed?ManagerAction.Action.DOWNLOAD
                    :(!busy&&!checking&&module&&ready)?
                        ManagerAction.Action.REFRESH_AND_OPEN:ManagerAction.Action.OPEN;
            check(got==expected,"manager shortcut decision for mask "+mask);
            scenarios++;
        }
        check(ManagerAction.resolve(true,true,false,true,true)==ManagerAction.Action.OPEN,
                "busy must never stop manager for refresh");
        check(ManagerAction.resolve(true,false,false,true,false)==ManagerAction.Action.OPEN,
                "stale module with missing prerequisites is not enough");
        check(ManagerAction.resolve(true,false,false,true,true)==ManagerAction.Action.REFRESH_AND_OPEN,
                "verified existing module uses one combined refresh");
        System.out.println("PASS: "+scenarios+" KernelSU shortcut scenarios, no duplicate recovery or manager restart");
    }
}
