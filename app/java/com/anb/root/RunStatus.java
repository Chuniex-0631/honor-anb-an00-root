package com.anb.root;

/** A loaded module alone never proves loader completion or restored enforcement. */
public final class RunStatus {
    private RunStatus() { }
    public static boolean isComplete(String output) {
        boolean module=false, finished=false, enforcing=false;
        for (String line : output.replace("\r", "").split("\n")) {
            if (line.equals("MODULE=1")) module=true;
            if (line.equals("FINISHED=1")) finished=true;
            if (line.equals("ENFORCE=Enforcing")) enforcing=true;
        }
        return module && finished && enforcing;
    }
}
