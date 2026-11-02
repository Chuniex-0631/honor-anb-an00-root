package com.anb.root.uitest;

import android.app.*;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.anb.root.*;
import java.lang.reflect.*;

/** Exercises the production Activity with presentation-only task leases. Never calls engine.start. */
public final class UiAuditInstrumentation extends Instrumentation {
    private MainActivity activity;
    private RecoveryEngine engine;
    private TaskGate tasks;
    private TaskGate.Lease lease;
    private int assertions;
    private static Object field(Object target,String name){
        try{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
        catch(Exception e){throw new RuntimeException(e);}
    }
    private static void call(Object target,String name,Class<?>[] types,Object... args){
        try{Method m=target.getClass().getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(target,args);}
        catch(Exception e){throw new RuntimeException(e);}
    }
    private void call(String name){call(activity,name,new Class<?>[0]);}
    private void refresh(){call("refresh");}
    private void ui(Runnable action){
        final java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();
        runOnMainSync(()->{try{action.run();}catch(Throwable error){failure.set(error);}});
        if(failure.get()!=null)throw new RuntimeException(failure.get());
    }
    private void check(boolean ok,String reason){assertions++;if(!ok)throw new AssertionError(reason);}
    private View view(String name){return (View)field(activity,name);}
    private String text(String name){return ((TextView)view(name)).getText().toString();}
    private void checkFullWidthRow(View row){
        View parent=(View)row.getParent();
        check(row.getLeft()==0&&row.getRight()==parent.getWidth(),"row spans its entire card");
        check(row.getBackground() instanceof android.graphics.drawable.StateListDrawable,"uniform list highlight instead of rounded ripple");
        boolean enabled=row.isEnabled();row.setEnabled(true);row.setPressed(true);
        android.graphics.drawable.GradientDrawable fill=(android.graphics.drawable.GradientDrawable)row.getBackground().getCurrent();
        check(fill.getCornerRadius()==0,"pressed row has square edges");
        check(fill.getColor().getDefaultColor()==(Integer)field(activity,"cRipple"),"original press color preserved");
        row.setPressed(false);row.setEnabled(enabled);
    }
    private TextView findText(View node,String label){
        if(node instanceof TextView&&((TextView)node).getText().toString().equals(label))return (TextView)node;
        if(node instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)node;
            for(int i=0;i<group.getChildCount();i++){TextView found=findText(group.getChildAt(i),label);if(found!=null)return found;}}
        return null;
    }
    private void checkTextLayout(View node){
        if(node.getVisibility()!=View.VISIBLE)return;
        if(node instanceof TextView){
            TextView text=(TextView)node;android.text.Layout layout=text.getLayout();
            if(layout!=null&&text.getWidth()>0&&text.getText().length()>0){
                int width=text.getWidth()-text.getCompoundPaddingLeft()-text.getCompoundPaddingRight();
                int height=text.getHeight()-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom();
                check(layout.getLineBottom(layout.getLineCount()-1)<=height+2,"text height: "+text.getText());
                for(int i=0;i<layout.getLineCount();i++)check(layout.getLineWidth(i)<=width+2,"text width: "+text.getText());
            }
        }
        if(node instanceof android.view.ViewGroup){
            android.view.ViewGroup group=(android.view.ViewGroup)node;
            for(int i=0;i<group.getChildCount();i++)checkTextLayout(group.getChildAt(i));
        }
    }
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        RecoveryEngine.Phase savedPhase=null;FailureText.Kind savedFailure=null;
        int savedStep=0;String savedTitle=null,savedMessage=null;
        try{
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            engine=RecoveryEngine.get(getTargetContext());
            long end=System.nanoTime()+20_000_000_000L;
            while(engine.isBusy()&&System.nanoTime()<end)Thread.sleep(50);
            if(engine.isBusy())throw new IllegalStateException("Wait for the current task before UI tests");
            tasks=(TaskGate)field(engine,"tasks");
            savedPhase=engine.phase;savedFailure=engine.failureKind;savedStep=engine.step;savedTitle=engine.title;savedMessage=engine.message;
            for(int page=0;page<3;page++){
                final int index=page;
                ui(()->call(activity,"showPage",new Class<?>[]{int.class},index));waitForIdleSync();
                ui(()->{
                    checkTextLayout(activity.getWindow().getDecorView());
                    if(index==0){for(View row:(View[])field(activity,"statusRows"))checkFullWidthRow(row);checkFullWidthRow(view("allowShellRow"));}
                    if(index==2)for(String faq:new String[]{"Shizuku 未运行或无法授权","KernelSU 显示“未安装”","恢复超时，或手机重启了","升级系统后还能使用吗？","卸载 App 会立即解除 Root 吗？","允许调试工具使用 Root 是什么？"})
                        checkFullWidthRow((View)findText(activity.getWindow().getDecorView(),faq).getParent());
                });
            }
            ui(()->{
                check(engine.gate()==Readiness.Gate.READY,"device prerequisites ready for state matrix");
                for(int page=0;page<3;page++){
                    call(activity,"showPage",new Class<?>[]{int.class},page);
                    ScrollView[] scrolls=(ScrollView[])field(activity,"scrolls");
                    check(scrolls[page].getVisibility()==View.VISIBLE,"selected page visible");
                    for(int other=0;other<3;other++)if(other!=page)check(scrolls[other].getVisibility()==View.GONE,"hidden page cannot intercept input");
                }
                call(activity,"showPage",new Class<?>[]{int.class},0);
                for(TaskGate.Task task:new TaskGate.Task[]{TaskGate.Task.CHECK,TaskGate.Task.MANAGER,TaskGate.Task.RECOVERY,TaskGate.Task.RECOVERY}){
                    lease=tasks.acquire(task);check(lease!=null,"UI task acquired");
                    engine.phase=RecoveryEngine.Phase.RUNNING;engine.step=2;refresh();
                    check(!view("primary").isEnabled(),"busy primary disabled");
                    check(!view("managerAction").isEnabled(),"busy manager action disabled");
                    check(!((View[])field(activity,"statusRows"))[3].isEnabled(),"busy environment action disabled");
                    check(view("progressCard").getVisibility()==(task==TaskGate.Task.RECOVERY?View.VISIBLE:View.GONE),"task-specific progress visibility");
                    check((Boolean)field(activity,"ticking"),"ticker starts for each task including repeated recovery");
                    if(task==TaskGate.Task.RECOVERY){
                        TextView[] stages=(TextView[])field(activity,"stageStates");
                        check(stages[0].getText().equals("完成")&&stages[2].getText().equals("进行中"),"timeline state");
                    }
                    lease.close();lease=null;engine.phase=RecoveryEngine.Phase.IDLE;refresh();
                    check(!(Boolean)field(activity,"ticking"),"completion clears ticker ownership");
                }
                engine.phase=RecoveryEngine.Phase.FAILED;engine.failureKind=FailureText.Kind.TIMEOUT;refresh();
                check(text("primary").equals("检查现有进度"),"timeout cannot offer a fresh recovery");
                engine.failureKind=FailureText.Kind.SECURITY_NOT_RESTORED;refresh();
                check(text("primary").equals("保存诊断记录"),"security failure offers diagnostics");
                check(text("errorGuidance").contains("完整重启"),"security guidance visible");
                call(activity,"showPage",new Class<?>[]{int.class},2);
                for(String faq:new String[]{"Shizuku 未运行或无法授权","KernelSU 显示“未安装”","恢复超时，或手机重启了","升级系统后还能使用吗？","卸载 App 会立即解除 Root 吗？","允许调试工具使用 Root 是什么？"}){
                    TextView label=findText(activity.getWindow().getDecorView(),faq);
                    check(label!=null,"FAQ entry exists");((View)label.getParent()).performClick();
                    AlertDialog faqDialog=(AlertDialog)field(activity,"activeDialog");
                    check(faqDialog!=null&&faqDialog.isShowing(),"FAQ opens its dialog");faqDialog.dismiss();
                }
                call("confirmAllowShell");AlertDialog option=(AlertDialog)field(activity,"activeDialog");
                check(option!=null&&option.isShowing(),"option confirmation opens");
                boolean selected=engine.allowShellSelected();option.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();option.dismiss();
                check(engine.allowShellSelected()==selected,"cancel keeps saved choice");
                call(activity,"showPage",new Class<?>[]{int.class},0);
                call("showAbout");AlertDialog dialog=(AlertDialog)field(activity,"activeDialog");
                check(dialog!=null&&dialog.isShowing(),"modal visible");
                call("showAbout");check(field(activity,"activeDialog")==dialog,"rapid taps do not stack dim layers");
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).performClick();
            });
            waitForIdleSync();
            ui(()->{
                AlertDialog licenses=(AlertDialog)field(activity,"activeDialog");
                check(licenses!=null&&licenses.isShowing(),"about opens licenses after dismissing old modal");
                licenses.dismiss();check(!licenses.isShowing(),"dismiss removes modal window");
                call("showAbout");
                callActivityOnPause(activity);
                AlertDialog paused=(AlertDialog)field(activity,"activeDialog");
                check(paused==null||!paused.isShowing(),"pause dismisses modal and dim layer");
                check(!(Boolean)field(activity,"ticking"),"pause releases ticker");
                callActivityOnResume(activity);
            });
            result.putString("stream","PASS: "+assertions+" production Activity assertions; navigation, task states, repeated ticker, safe actions, modal handoff and pause cleanup. No root/reboot executed.\n");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable error){result.putString("stream","FAIL: "+error+"\n");finish(Activity.RESULT_CANCELED,result);}
        finally{
            if(lease!=null)lease.close();
            if(engine!=null&&savedPhase!=null){engine.phase=savedPhase;engine.failureKind=savedFailure;engine.step=savedStep;engine.title=savedTitle;engine.message=savedMessage;}
        }
    }
}
