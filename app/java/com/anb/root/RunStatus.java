package com.anb.root;

/** A loaded module alone never proves loader completion or restored enforcement. */
public final class RunStatus {
    private RunStatus() { }
    public static boolean isComplete(String output) {
        String[] lines=header(output);
        return lines!=null&&lines[1].equals("MODULE=1")&&lines[2].equals("FINISHED=1")&&lines[3].equals("ENFORCE=Enforcing")&&lines[4].equals("CLEANUP=1")&&lines[5].equals("STAGES=1");
    }
    public static boolean modulePresent(String output){String[] lines=header(output);return lines!=null&&lines[1].equals("MODULE=1");}
    public static boolean cleanupFailed(String output){String[] lines=header(output);return lines!=null&&lines[2].equals("FINISHED=1")&&lines[4].equals("CLEANUP=0");}
    public static boolean stagesFailed(String output){String[] lines=header(output);return lines!=null&&lines[2].equals("FINISHED=1")&&lines[5].equals("STAGES=0");}
    public static boolean securityNotRestored(String output){String[] lines=header(output);return lines!=null&&(lines[3].equals("ENFORCE=Permissive")||cleanupFailed(output));}
    public static boolean protectionVerified(String output){
        if(output==null)return false;
        int proofs=0;
        for(String line:output.replace("\r","").split("\n"))if(line.startsWith("ANB_PROTECTION=")){
            if(!line.equals("ANB_PROTECTION=1"))return false;proofs++;
        }
        return proofs==1;
    }
    private static String[] header(String output) {
        if(output==null)return null;
        String[] lines=output.replace("\r","").split("\n",8);
        if(lines.length<7||!lines[0].equals("ANB_STATUS=2")||!lines[6].equals("DETAILS"))return null;
        if(!lines[1].matches("MODULE=[01]")||!lines[2].matches("FINISHED=[01]"))return null;
        if(!lines[3].equals("ENFORCE=Enforcing")&&!lines[3].equals("ENFORCE=Permissive"))return null;
        if(!lines[4].matches("CLEANUP=[01]")||!lines[5].matches("STAGES=[01]"))return null;
        return lines;
    }
}
