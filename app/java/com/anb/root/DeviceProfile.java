package com.anb.root;

/** One device-verified firmware. Unknown or partially matching systems fail closed. */
public final class DeviceProfile {
    public static final String MODEL="ANB-AN00";
    public static final String DISPLAY="ANB-AN00 9.0.0.230(CHNC00E230R2P5)";
    public static final String KERNEL="5.10.236-android12-9-gd671cf8c2b02";
    public static final int SDK=35;
    private DeviceProfile() { }
    public static boolean supports(String model,int sdk,String display,String kernel) {
        return MODEL.equals(model)&&sdk==SDK&&DISPLAY.equals(display)&&KERNEL.equals(kernel);
    }
    public static boolean supportsProbe(String output) {
        if(output==null)return false;
        String[] fields=output.replace("\r","").trim().split("\n",-1);
        return fields.length==4&&MODEL.equals(fields[0])&&"35".equals(fields[1])
                &&DISPLAY.equals(fields[2])&&KERNEL.equals(fields[3]);
    }
}
