package com.anb.root;

/** Presentation rules shared by the Activity and its regression tests. */
public final class UiState {
    private UiState() { }
    public static boolean showProgress(TaskGate.Task task,String phase) {
        if(task!=null)return task==TaskGate.Task.RECOVERY;
        return "SUCCESS".equals(phase)||"WAIT_AUTH".equals(phase)||"FAILED".equals(phase);
    }
    public static String busyLabel(TaskGate.Task task) {
        if(task==TaskGate.Task.CHECK)return "正在检查环境…";
        if(task==TaskGate.Task.MANAGER)return "正在刷新 KernelSU…";
        if(task==TaskGate.Task.SETTINGS)return "正在保存选项…";
        return "正在处理，请稍候";
    }
    public static String stageState(int index,int step,boolean recovering,String phase) {
        int current=Math.max(0,Math.min(5,step));
        if(index<current)return "完成";
        if(index!=current)return "等待";
        if(recovering)return "进行中";
        if("WAIT_AUTH".equals(phase))return "待授权";
        if("FAILED".equals(phase))return "未完成";
        return "等待";
    }
    public static String logText(String log) {
        return log.isEmpty()?"暂无记录。完成检查或恢复后，会显示在这里。":log;
    }
}
