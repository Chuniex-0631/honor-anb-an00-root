package com.anb.root;

/** Immutable values for one module load; no runtime module reconfiguration. */
public final class RecoveryOptions {
    static final String PREFERENCES="recovery_options", ALLOW_SHELL="allow_shell";
    static final boolean DEFAULT_ALLOW_SHELL=false;
    private RecoveryOptions() { }

    static String renderLoader(String template,String directory,String stamp,boolean allowShell) {
        return template.replace("@RUNDIR@",directory).replace("@STAMP@",stamp)
                .replace("@FREEZE_HISECD@","1").replace("@ALLOW_SHELL@",allowShell?"1":"0")
                .replace("@STAGES@","1").replace("@ENFORCE@","1");
    }
}
