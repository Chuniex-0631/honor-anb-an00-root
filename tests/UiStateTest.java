package com.anb.root;

public final class UiStateTest {
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void main(String[] args){
        String[] phases={"IDLE","PREPARING","RUNNING","LOADING","VERIFYING","WAIT_AUTH","SUCCESS","FAILED"};
        for(String phase:phases){
            for(TaskGate.Task task:TaskGate.Task.values()){
                check(UiState.showProgress(task,phase)==(task==TaskGate.Task.RECOVERY),"only recovery owns the timeline: "+task+phase);
                check(!UiState.busyLabel(task).isEmpty(),"every task has an accurate busy label");
            }
        }
        check(!UiState.showProgress(null,"IDLE"),"fresh environment has no recovery progress");
        check(UiState.showProgress(null,"SUCCESS"),"retain completed recovery");
        check(UiState.showProgress(null,"FAILED"),"retain failed recovery");
        check(!UiState.busyLabel(TaskGate.Task.MANAGER).equals(UiState.busyLabel(TaskGate.Task.RECOVERY)),"manager does not pretend to recover");
        for(int step=0;step<=5;step++)for(int row=0;row<5;row++){
            String state=UiState.stageState(row,step,true,"RUNNING");
            check(state.equals(row<step?"完成":row==step?"进行中":"等待"),"timeline order");
        }
        check(UiState.stageState(4,4,false,"WAIT_AUTH").equals("待授权"),"authorization step");
        check(UiState.stageState(2,2,false,"FAILED").equals("未完成"),"failure does not claim the background was interrupted");
        check(!UiState.logText("").isEmpty(),"empty log is explained");
        check(UiState.logText("one\ntwo").equals("one\ntwo"),"diagnostic text preserved");
        TaskGate gate=new TaskGate();
        check(gate.current()==null,"idle owner");
        TaskGate.Lease first=gate.acquire(TaskGate.Task.MANAGER);
        check(gate.current()==TaskGate.Task.MANAGER,"task visible to presentation");
        first.close();TaskGate.Lease second=gate.acquire(TaskGate.Task.RECOVERY);first.close();
        check(gate.current()==TaskGate.Task.RECOVERY,"stale close preserves active presentation owner");second.close();
        System.out.println("PASS: UI task/phase matrix, progress order, accurate failures, empty logs and task identity");
    }
}
