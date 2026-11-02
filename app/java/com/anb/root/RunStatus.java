package com.anb.root;

/** A loaded module alone never proves loader completion or restored enforcement. */
public final class RunStatus {
    private RunStatus() { }
    public static boolean isComplete(String output) {
        String[] lines=header(output);
        return lines!=null&&lines[1].equals("MODULE=1")&&lines[2].equals("FINISHED=1")&&lines[3].equals("ENFORCE=Enforcing");
    }
    public static boolean modulePresent(String output){String[] lines=header(output);return lines!=null&&lines[1].equals("MODULE=1");}
    public static boolean securityNotRestored(String output){String[] lines=header(output);return lines!=null&&lines[3].equals("ENFORCE=Permissive");}
    private static String[] header(String output) {
        if(output==null)return null;
        String[] lines=output.replace("\r","").split("\n",6);
        if(lines.length<5||!lines[0].equals("ANB_STATUS=1")||!lines[4].equals("DETAILS"))return null;
        if(!lines[1].matches("MODULE=[01]")||!lines[2].matches("FINISHED=[01]"))return null;
        if(!lines[3].equals("ENFORCE=Enforcing")&&!lines[3].equals("ENFORCE=Permissive"))return null;
        return lines;
    }
}
