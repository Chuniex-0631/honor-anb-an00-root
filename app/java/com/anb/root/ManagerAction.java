package com.anb.root;

/** Android-independent routing for the combined KernelSU shortcut. */
public final class ManagerAction {
    public enum Action { DOWNLOAD, OPEN, REFRESH_AND_OPEN }

    private ManagerAction() {}

    /**
     * Manager refresh is NOT an exploit retry. Only refresh when Shizuku is
     * verified and no foreground environment check or recovery is underway.
     */
    public static Action resolve(boolean installed,boolean busy,boolean checking,
                                 boolean modulePresent,boolean environmentReady) {
        if(!installed)return Action.DOWNLOAD;
        if(busy||checking||!modulePresent||!environmentReady)return Action.OPEN;
        return Action.REFRESH_AND_OPEN;
    }
}
