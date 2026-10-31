package com.anb.root;
public final class ShizukuOnlyProvider extends rikka.shizuku.ShizukuProvider {
    @Override public boolean onCreate() {
        disableAutomaticSuiInitialization();
        return super.onCreate();
    }
}
