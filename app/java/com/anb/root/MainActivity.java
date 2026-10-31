package com.anb.root;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;
import rikka.shizuku.Shizuku;

/**
 * Material-style, dependency-free UI inspired by KernelSU's clean status cards.
 * All recovery actions are delegated to the existing, verified RecoveryEngine.
 */
public final class MainActivity extends Activity {
    private static final int HOME=0, SETTINGS=1, RECORDS=2;
    private static final int SHIZUKU_PERMISSION=51, NOTIFICATION_PERMISSION=52, EXPORT_LOG=53;
    private static final String SHIZUKU_URL="https://shizuku.rikka.app/download/";
    private static final String KSU_URL="https://github.com/tiann/KernelSU/releases/tag/v3.3.0";
    private RecoveryEngine engine;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final LinearLayout[] pages=new LinearLayout[3];
    private final ScrollView[] scrolls=new ScrollView[3];
    private final TextView[] tabs=new TextView[3];
    private LinearLayout actionFooter, progressCard;
    private TextView statusIcon, statusTitle, statusMessage, statusBadge;
    private TextView deviceValue, shizukuValue, ksuValue, environmentValue;
    private TextView progressText, recordTitle, recordMessage, logText, shellDescription, managerMessage;
    private Button primary, logsToggle, refreshManagerButton;
    private Switch allowShellSwitch;
    private boolean syncingSwitch, expandedLog;
    private int selectedPage=HOME;
    private String shownLog="";
    private UiState lastUi;

    // Warm, subtle surfaces and a single accent, with native dark-mode support.
    private int background, surface, ink, secondary, outline, accent, onAccent, accentTint;
    private int success, warning, successTint, warningTint;

    private final Shizuku.OnBinderReceivedListener received=()->{
        engine.invalidate();autoCheck();render();
    };
    private final Shizuku.OnBinderDeadListener dead=()->{
        engine.invalidate();engine.record("Shizuku 服务已停止。");render();
    };
    private final Shizuku.OnRequestPermissionResultListener permission=(request,grant)->{
        if(request!=SHIZUKU_PERMISSION)return;
        if(grant==PackageManager.PERMISSION_GRANTED)engine.checkEnvironment();
        else engine.record("Shizuku 权限未授予。");
        render();
    };
    private final Runnable tick=new Runnable() {
        @Override public void run() {
            render();
            main.postDelayed(this,1200);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        engine=RecoveryEngine.get(this);
        initPalette();
        buildUi();
        if(state!=null) {
            expandedLog=state.getBoolean("expanded_log",false);
            selectPage(state.getInt("selected_page",HOME));
        }
        Shizuku.addBinderReceivedListenerSticky(received);
        Shizuku.addBinderDeadListener(dead);
        Shizuku.addRequestPermissionResultListener(permission);
        render();
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("selected_page",selectedPage);
        out.putBoolean("expanded_log",expandedLog);
    }
    @Override protected void onResume() {
        super.onResume();
        autoCheck();
        main.removeCallbacks(tick);
        main.post(tick);
    }
    @Override protected void onPause() {
        main.removeCallbacks(tick);
        super.onPause();
    }
    @Override protected void onDestroy() {
        main.removeCallbacks(tick);
        Shizuku.removeBinderReceivedListener(received);
        Shizuku.removeBinderDeadListener(dead);
        Shizuku.removeRequestPermissionResultListener(permission);
        super.onDestroy();
    }
    @Override public void onBackPressed() {
        if(selectedPage!=HOME)selectPage(HOME);
        else super.onBackPressed();
    }
    private boolean setupComplete() {
        return getSharedPreferences("setup",0).getBoolean("completed",false);
    }
    private void autoCheck() {
        if(engine.gate()==Readiness.Gate.ENVIRONMENT)engine.checkEnvironment();
    }

    private void initPalette() {
        boolean dark=(getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                ==Configuration.UI_MODE_NIGHT_YES;
        background=dark?0xff101512:0xffF5F7F5;
        surface=dark?0xff1B241F:Color.WHITE;
        ink=dark?0xffEBF2EC:0xff192C23;
        secondary=dark?0xffA9BDB1:0xff617368;
        outline=dark?0xff34483C:0xffE4EBE5;
        accent=dark?0xff80D9AA:0xff16744B;
        onAccent=dark?0xff103C27:Color.WHITE;
        accentTint=dark?0xff254B37:0xffE4F3E8;
        success=dark?0xff83DBA7:0xff14764A;
        warning=dark?0xffFFD18B:0xff946015;
        successTint=dark?0xff294938:0xffE4F3E8;
        warningTint=dark?0xff483A24:0xffFFF2DA;
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);
        getWindow().getDecorView().setSystemUiVisibility(dark?0:
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }
    private int dp(float value) {
        return Math.round(value*getResources().getDisplayMetrics().density);
    }
    private LinearLayout column() {
        LinearLayout l=new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }
    private LinearLayout row() {
        LinearLayout l=new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }
    private TextView label(String text,int sp,int color,boolean bold) {
        TextView view=new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setLineSpacing(dp(2),1.03f);
        if(bold)view.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        return view;
    }
    private GradientDrawable rounded(int color,int radius) {
        GradientDrawable g=new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }
    private GradientDrawable stroked(int color,int radius) {
        GradientDrawable g=rounded(color,radius);
        g.setStroke(dp(1),outline);
        return g;
    }
    private void gap(LinearLayout parent,int height) {
        View space=new View(this);
        parent.addView(space,new LinearLayout.LayoutParams(1,dp(height)));
    }
    private LinearLayout card(LinearLayout parent) {
        LinearLayout c=column();
        c.setPadding(dp(18),dp(18),dp(18),dp(18));
        c.setBackground(stroked(surface,22));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.bottomMargin=dp(14);
        parent.addView(c,p);
        return c;
    }
    private void divider(LinearLayout parent) {
        View line=new View(this);line.setBackgroundColor(outline);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(1));
        p.topMargin=dp(10);p.bottomMargin=dp(10);
        parent.addView(line,p);
    }
    private Button button(String title,boolean filled,View.OnClickListener click) {
        Button b=new Button(this);
        b.setAllCaps(false);
        b.setText(title);
        b.setTextSize(15);
        b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        b.setTextColor(filled?onAccent:accent);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33000000),
                rounded(filled?accent:accentTint,16),null));
        b.setStateListAnimator(null);
        b.setMinHeight(dp(50));
        b.setMinimumHeight(dp(50));
        b.setOnClickListener(click);
        return b;
    }
    private void addButton(LinearLayout parent,Button b) {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.topMargin=dp(10);parent.addView(b,p);
    }
    private void heading(LinearLayout parent,String title,String subtitle) {
        parent.addView(label(title,23,ink,true));
        gap(parent,6);
        parent.addView(label(subtitle,14,secondary,false));
        gap(parent,18);
    }
    private void buildUi() {
        LinearLayout frame=column();
        frame.setBackgroundColor(background);
        frame.setOnApplyWindowInsetsListener((v,insets)->{
            int top,bottom;
            if(Build.VERSION.SDK_INT>=30) {
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
                top=bars.top;bottom=bars.bottom;
            } else {
                top=insets.getSystemWindowInsetTop();bottom=insets.getSystemWindowInsetBottom();
            }
            v.setPadding(0,top,0,bottom);
            return insets;
        });

        LinearLayout header=row();
        header.setPadding(dp(20),dp(14),dp(20),dp(12));
        TextView mark=label("A",20,onAccent,true);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(rounded(accent,14));
        header.addView(mark,new LinearLayout.LayoutParams(dp(44),dp(44)));
        LinearLayout branding=column();
        branding.addView(label("ANB Root",21,ink,true));
        branding.addView(label("临时 Root 恢复",12,secondary,false));
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,-2,1);
        bp.leftMargin=dp(12);header.addView(branding,bp);
        TextView help=label("帮助",14,accent,true);
        help.setGravity(Gravity.CENTER);
        help.setMinHeight(dp(48));help.setMinWidth(dp(54));
        help.setContentDescription("打开使用帮助");
        help.setOnClickListener(v->showGuide());
        header.addView(help);
        frame.addView(header);

        FrameLayout content=new FrameLayout(this);
        frame.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        for(int i=0;i<3;i++) {
            ScrollView scroll=new ScrollView(this);
            scroll.setFillViewport(true);
            scroll.setClipToPadding(false);
            scroll.setVerticalScrollBarEnabled(false);
            LinearLayout body=column();
            body.setPadding(dp(18),dp(6),dp(18),dp(14));
            scroll.addView(body);
            pages[i]=body;scrolls[i]=scroll;
            content.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        }
        buildHome(pages[HOME]);
        buildSettings(pages[SETTINGS]);
        buildRecords(pages[RECORDS]);

        actionFooter=column();
        actionFooter.setPadding(dp(20),dp(8),dp(20),dp(10));
        actionFooter.setBackgroundColor(background);
        primary=button("检查设备",true,v->primaryAction());
        actionFooter.addView(primary,new LinearLayout.LayoutParams(-1,-2));
        frame.addView(actionFooter);

        LinearLayout navigation=row();
        navigation.setPadding(dp(12),dp(7),dp(12),dp(7));
        navigation.setBackgroundColor(surface);
        String[] tabNames={"首页","设置","记录"};
        for(int i=0;i<3;i++) {
            final int index=i;
            TextView nav=label(tabNames[i],14,secondary,true);
            nav.setGravity(Gravity.CENTER);
            nav.setMinHeight(dp(48));
            nav.setContentDescription(tabNames[i]+"页面");
            nav.setOnClickListener(v->selectPage(index));
            LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(0,-2,1);
            np.leftMargin=i==0?0:dp(6);
            navigation.addView(nav,np);
            tabs[i]=nav;
        }
        frame.addView(navigation);
        setContentView(frame);
        selectPage(HOME);
    }

    private TextView statusRow(LinearLayout parent,String name,View.OnClickListener click) {
        LinearLayout item=row();
        item.setMinimumHeight(dp(49));
        TextView label=label(name,14,ink,false);
        item.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        TextView value=label("待检查",14,secondary,true);
        value.setGravity(Gravity.END);
        item.addView(value);
        item.setContentDescription(name+"状态");
        item.setOnClickListener(click);
        parent.addView(item,new LinearLayout.LayoutParams(-1,-2));
        return value;
    }

    private void buildHome(LinearLayout root) {
        LinearLayout summary=card(root);
        LinearLayout top=row();
        statusIcon=label("•",23,success,true);
        statusIcon.setGravity(Gravity.CENTER);
        statusIcon.setBackground(rounded(successTint,16));
        top.addView(statusIcon,new LinearLayout.LayoutParams(dp(52),dp(52)));
        TextView name=label("ROOT 状态",14,secondary,true);
        LinearLayout.LayoutParams nameParams=new LinearLayout.LayoutParams(0,-2,1);
        nameParams.leftMargin=dp(14);top.addView(name,nameParams);
        statusBadge=label("检查中",12,success,true);
        statusBadge.setGravity(Gravity.CENTER);
        statusBadge.setPadding(dp(10),dp(5),dp(10),dp(5));
        statusBadge.setBackground(rounded(successTint,20));
        top.addView(statusBadge);
        summary.addView(top);
        gap(summary,16);
        statusTitle=label("正在检查",25,ink,true);
        summary.addView(statusTitle);
        gap(summary,8);
        statusMessage=label("连接 Shizuku，确认设备及执行环境。",14,secondary,false);
        summary.addView(statusMessage);

        LinearLayout checks=card(root);
        checks.addView(label("设备与依赖",17,ink,true));
        gap(checks,10);
        deviceValue=statusRow(checks,"设备与固件",v->dialog("适配范围",
                "仅支持 ANB-AN00 · Android 15 · MagicOS 9.0.0.230(CHNC00E230R2P5)，必须同时符合精确内核与固件指纹。"));
        divider(checks);
        shizukuValue=statusRow(checks,"Shizuku",v->openPackage(RecoveryEngine.SHIZUKU,SHIZUKU_URL));
        divider(checks);
        ksuValue=statusRow(checks,"KernelSU",v->openPackage(RecoveryEngine.KSU,KSU_URL));
        divider(checks);
        environmentValue=statusRow(checks,"执行环境",v->refreshEnvironment());
        addButton(checks,button("重新检查环境",false,v->refreshEnvironment()));

        progressCard=card(root);
        progressCard.addView(label("恢复进度",17,ink,true));
        gap(progressCard,12);
        progressText=label("",14,secondary,false);
        progressCard.addView(progressText);

        LinearLayout hint=card(root);
        hint.addView(label("仅在本次开机有效",15,ink,true));
        gap(hint,7);
        hint.addView(label("完整重启后：启动 Shizuku → 返回 ANB Root 恢复。Root 授权与模块管理请使用官方 KernelSU。",14,secondary,false));
    }

    private void buildSettings(LinearLayout root) {
        heading(root,"设置","只保留恢复需要的选项，不重复 KernelSU 的权限管理。");
        LinearLayout permissions=card(root);
        permissions.addView(label("调试工具 Root 授权",17,ink,true));
        gap(permissions,10);
        allowShellSwitch=new Switch(this);
        allowShellSwitch.setText("允许调试工具使用 Root");
        allowShellSwitch.setTextSize(15);
        allowShellSwitch.setTextColor(ink);
        allowShellSwitch.setMinHeight(dp(56));
        permissions.addView(allowShellSwitch);
        gap(permissions,6);
        permissions.addView(label("默认关闭。开启后，以 shell 身份运行的 ADB / Shizuku 工具可直接获得 Root，不再逐项请求授权。",14,secondary,false));
        gap(permissions,9);
        shellDescription=label("",13,warning,false);
        permissions.addView(shellDescription);
        allowShellSwitch.setOnCheckedChangeListener((v,enabled)->{
            if(syncingSwitch || enabled==engine.allowShellSelected())return;
            if(!enabled) {
                if(!engine.selectAllowShell(false))toast("无法保存选项或本次状态已锁定");
                render();return;
            }
            syncingSwitch=true;allowShellSwitch.setChecked(false);syncingSwitch=false;
            new AlertDialog.Builder(this)
                    .setTitle("允许 shell 调试工具使用 Root？")
                    .setMessage("开启后，所有以 shell 身份运行的调试工具都可自动获得 Root，包括已授权电脑的 ADB。\n\n该选项只在新一次恢复加载内核模块时生效。若要撤销当前模式，必须完整重启手机。")
                    .setPositiveButton("开启", (d,w)->{
                        if(!engine.selectAllowShell(true))toast("状态已变化，未修改设置");
                        render();
                    })
                    .setNegativeButton("取消",(d,w)->render())
                    .show();
        });

        LinearLayout manager=card(root);
        manager.addView(label("KernelSU 管理器",17,ink,true));
        gap(manager,8);
        manager.addView(label("应用 Root 授权、模块安装与管理均在官方 KernelSU 中完成。",14,secondary,false));
        addButton(manager,button("打开 KernelSU",false,v->openPackage(RecoveryEngine.KSU,KSU_URL)));
        refreshManagerButton=button("刷新管理器状态",false,v->{
            engine.refreshManagerOnly();
            toast("正在检查管理器状态，不会再次执行 Root 恢复");
        });
        addButton(manager,refreshManagerButton);
        gap(manager,8);
        managerMessage=label("",13,secondary,false);manager.addView(managerMessage);

        LinearLayout help=card(root);
        help.addView(label("使用与安全",17,ink,true));
        gap(help,8);
        help.addView(label("查看首次准备、重启后恢复和故障处理说明。",14,secondary,false));
        addButton(help,button("使用指南",false,v->showGuide()));
        addButton(help,button("关于与开源许可",false,v->showAbout()));
    }

    private void buildRecords(LinearLayout root) {
        heading(root,"运行记录","确认本次执行结果；失败或超时不会自动再次提权。");
        LinearLayout info=card(root);
        info.addView(label("最新状态",14,secondary,true));
        gap(info,8);
        recordTitle=label("",19,ink,true);info.addView(recordTitle);
        gap(info,6);
        recordMessage=label("",14,secondary,false);info.addView(recordMessage);

        LinearLayout logs=card(root);
        logs.addView(label("诊断日志",17,ink,true));
        gap(logs,7);
        logs.addView(label("日志保存在 App 私有目录；环境与验证摘要也可能保存在 App 专属外部目录。不会自动上传。",13,secondary,false));
        addButton(logs,button("保存日志到文件",false,v->exportLogs()));
        logsToggle=button("展开最近日志",false,v->{
            expandedLog=!expandedLog;
            renderLogs();
        });
        addButton(logs,logsToggle);
        logText=label("",12,secondary,false);
        logText.setTypeface(Typeface.MONOSPACE);
        logText.setTextIsSelectable(true);
        logText.setPadding(0,dp(10),0,0);
        logs.addView(logText);
        addButton(logs,button("恢复失败怎么办？",false,v->dialog("失败和超时",
                "请先保存诊断，不要连续重复运行恢复。若同次开机已有尝试，应用只会读取既有状态；无法确认时完整重启，重新启动 Shizuku 后再操作。")));
    }

    private void selectPage(int page) {
        selectedPage=Math.max(HOME,Math.min(RECORDS,page));
        for(int i=0;i<3;i++) {
            boolean selected=i==selectedPage;
            scrolls[i].setVisibility(selected?View.VISIBLE:View.GONE);
            tabs[i].setTextColor(selected?accent:secondary);
            tabs[i].setBackground(selected?rounded(accentTint,16):null);
            tabs[i].setSelected(selected);
        }
        actionFooter.setVisibility(selectedPage==HOME?View.VISIBLE:View.GONE);
        if(selectedPage==RECORDS)renderLogs();
        render();
    }

    private void refreshEnvironment() {
        if(engine.busy.get()||engine.checking) return;
        engine.invalidate();
        if(engine.gate()==Readiness.Gate.ENVIRONMENT)engine.checkEnvironment();
        render();
    }
    private static String progressLine(int index,int active,boolean working) {
        return (index<active?"✓  ":index==active&&working?"●  ":"○  ");
    }
    private void renderProgress() {
        boolean active=engine.busy.get();
        boolean visible=active||engine.phase==RecoveryEngine.Phase.FAILED
                ||engine.phase==RecoveryEngine.Phase.WAIT_AUTH
                ||engine.phase==RecoveryEngine.Phase.SUCCESS;
        progressCard.setVisibility(visible?View.VISIBLE:View.GONE);
        if(!visible)return;
        String[] steps={"检查环境","准备载荷","恢复权限","完成加载","验证 Root"};
        StringBuilder s=new StringBuilder();
        for(int i=0;i<steps.length;i++) {
            s.append(progressLine(i,engine.step,active)).append(steps[i]);
            if(i==engine.step&&active)s.append(" · 正在进行");
            if(i<steps.length-1)s.append('\n');
        }
        progressText.setText(s.toString());
    }
    private void renderLogs() {
        if(logsToggle==null || logText==null)return;
        logsToggle.setText(expandedLog?"收起日志":"展开最近日志");
        logText.setVisibility(expandedLog?View.VISIBLE:View.GONE);
        if(!expandedLog||selectedPage!=RECORDS)return;
        String all=engine.logs();
        String shown=all.length()>32000?"（仅展示最后 32,000 字符，完整内容请保存到文件）\n\n"+
                all.substring(all.length()-32000):all;
        if(shown.isEmpty())shown="暂无记录";
        if(!shownLog.equals(shown)) {
            logText.setText(shown);shownLog=shown;
        }
    }
    private UiState currentState() {
        return UiState.resolve(engine.gate(),setupComplete(),engine.checking,engine.busy.get(),
                engine.modulePresent,engine.phase.name(),engine.title,engine.message);
    }
    private void render() {
        if(primary==null)return;
        UiState state=currentState();
        lastUi=state;
        statusTitle.setText(state.title);
        statusMessage.setText(state.message);
        statusBadge.setText(state.badge);
        boolean good=state.tone==UiState.Tone.GOOD;
        boolean bad=state.tone==UiState.Tone.WARNING;
        int color=good?success:bad?warning:accent;
        int tint=good?successTint:bad?warningTint:accentTint;
        statusIcon.setText(good?"✓":bad?"!":"•");
        statusIcon.setTextColor(color);
        statusIcon.setBackground(rounded(tint,16));
        statusBadge.setTextColor(color);
        statusBadge.setBackground(rounded(tint,18));

        boolean compatible=engine.compatible();
        setValue(deviceValue,compatible?"已适配":"不匹配",compatible);
        Readiness.Gate gate=engine.gate();
        boolean shizukuReady=gate==Readiness.Gate.READY||gate==Readiness.Gate.ENVIRONMENT
                ||gate==Readiness.Gate.KSU_INSTALL;
        String shizukuText=engine.installed(RecoveryEngine.SHIZUKU)?
                (shizukuReady?"已连接":gate==Readiness.Gate.SHIZUKU_START?"待启动":
                        gate==Readiness.Gate.SHIZUKU_PERMISSION?"待授权":
                        gate==Readiness.Gate.SHELL_MODE?"模式不符":
                        gate==Readiness.Gate.SHIZUKU_VERSION?"待更新":"待检查"):"未安装";
        setValue(shizukuValue,shizukuText,shizukuReady);
        boolean ksuInstalled=engine.installed(RecoveryEngine.KSU);
        setValue(ksuValue,ksuInstalled?"已安装":"未安装",ksuInstalled);
        setValue(environmentValue,engine.checking?"检查中":engine.environmentReady?"已通过":"待检查",
                engine.environmentReady&&!engine.checking);
        primary.setText(state.primaryLabel);
        primary.setEnabled(state.canAct());
        primary.setAlpha(state.canAct()?1f:.45f);
        renderProgress();
        recordTitle.setText(engine.title);
        recordMessage.setText(engine.message);
        renderLogs();

        boolean locked=engine.checking||engine.busy.get()||engine.modulePresent;
        syncingSwitch=true;
        allowShellSwitch.setChecked(engine.allowShellSelected());
        syncingSwitch=false;
        allowShellSwitch.setEnabled(!locked);
        shellDescription.setText(locked?
                (engine.modulePresent?"本次已加载内核模块；需要完整重启才能切换此模式。":"正在检查或恢复，选项暂时锁定。"):
                "该选择在下一次新恢复时生效。已有运行不会重新加载；切换本次 Root 模式需要完整重启。");
        refreshManagerButton.setEnabled(!engine.busy.get()&&!engine.checking);
        managerMessage.setText(engine.phase==RecoveryEngine.Phase.SUCCESS
                ||engine.phase==RecoveryEngine.Phase.WAIT_AUTH?engine.message:"刷新只会重新打开 KernelSU，不会重启恢复链。");
        if(engine.busy.get())getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    private void setValue(TextView view,String text,boolean ok) {
        view.setText(text);
        view.setTextColor(ok?success:secondary);
    }

    private void primaryAction() {
        UiState state=currentState();
        switch(state.action) {
            case GET_SHIZUKU: openUri(SHIZUKU_URL);return;
            case GET_KSU: openUri(KSU_URL);return;
            case OPEN_SHIZUKU: openPackage(RecoveryEngine.SHIZUKU,SHIZUKU_URL);return;
            case GRANT_SHIZUKU:
                try {
                    if(Shizuku.shouldShowRequestPermissionRationale())
                        dialog("授予 Shizuku 权限",
                                "请到 Shizuku 的已授权应用中开启 ANB Root 权限，然后返回检查。");
                    else Shizuku.requestPermission(SHIZUKU_PERMISSION);
                } catch(Exception e) { toast("Shizuku 已断开，请重新检查"); }
                return;
            case CHECK_ENVIRONMENT: refreshEnvironment();return;
            case COMPLETE_SETUP:
                if(engine.gate()==Readiness.Gate.READY
                        &&getSharedPreferences("setup",0).edit().putBoolean("completed",true).commit())
                    engine.record("用户已确认首次准备。");
                else toast("尚未满足准备条件或保存失败");
                render();return;
            case RECOVER_OR_VERIFY:
                if(!setupComplete()||engine.gate()!=Readiness.Gate.READY) {
                    render();return;
                }
                if(Build.VERSION.SDK_INT>=33
                        &&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        !=PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},NOTIFICATION_PERMISSION);
                } else startRecovery();
                return;
            case NONE:
            default: return;
        }
    }
    private void startRecovery() {
        if(engine.busy.get()||engine.checking||engine.gate()!=Readiness.Gate.READY
                ||!setupComplete()) { render();return; }
        try { startForegroundService(new Intent(this,RecoveryService.class)); }
        catch(Exception e) {toast("无法启动恢复服务，请检查系统后台限制");}
        render();
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants) {
        super.onRequestPermissionsResult(request,permissions,grants);
        if(request==NOTIFICATION_PERMISSION) {
            // Notification denial must not leave the primary button inert.
            if(grants.length==0 || grants[0]!=PackageManager.PERMISSION_GRANTED)
                toast("通知未授权；系统仍可能显示必要的恢复前台服务提示");
            startRecovery();
        }
    }
    private void showGuide() {
        new AlertDialog.Builder(this).setTitle("使用 ANB Root")
                .setMessage("首次使用\n1. 安装 KernelSU v3.3.0 与官方 Shizuku。\n2. 用无线调试启动 Shizuku 并授予权限。\n3. 首页检查通过后确认准备并恢复 Root。\n4. 若需要，在 KernelSU 中授权 ANB Root，再返回验证。\n\n每次完整重启\n重新启动 Shizuku → 返回首页恢复。\n\n异常情况\n不要并发或连续重新提权；先在“记录”保存日志。")
                .setPositiveButton("明白",null).show();
    }
    private void showAbout() {
        String version="2.2.0";
        try {version=getPackageManager().getPackageInfo(getPackageName(),0).versionName;}
        catch(Exception ignored) { }
        new AlertDialog.Builder(this).setTitle("ANB Root "+version)
                .setMessage("ANB-AN00 · MagicOS 9.0.0.230\nShizuku + GhostLock + KernelSU LKM\n\n这是临时 Root 恢复工具，不是 Bootloader 解锁工具。\n\n界面采用简洁状态卡片布局，参考 KernelSU 的信息层级；KernelSU 及其标识归原项目所有。")
                .setPositiveButton("关闭",null)
                .setNeutralButton("开源许可",(d,w)->showLicenses()).show();
    }
    private void showLicenses() {
        try(InputStream in=getAssets().open("THIRD_PARTY_NOTICES.txt");
                ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buf=new byte[4096];int n;
            while((n=in.read(buf))!=-1)out.write(buf,0,n);
            TextView text=label(new String(out.toByteArray(),"UTF-8"),13,ink,false);
            text.setTextIsSelectable(true);
            text.setPadding(dp(20),dp(16),dp(20),dp(16));
            ScrollView scroller=new ScrollView(this);scroller.addView(text);
            new AlertDialog.Builder(this).setTitle("开源与第三方许可")
                    .setView(scroller).setPositiveButton("关闭",null).show();
        }catch(Exception e){toast("暂时无法显示许可说明");}
    }
    private void dialog(String title,String message) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(message)
                .setPositiveButton("知道了",null).show();
    }
    private void openPackage(String pkg,String url) {
        Intent launch=getPackageManager().getLaunchIntentForPackage(pkg);
        if(launch!=null)try{startActivity(launch);return;}catch(Exception ignored){}
        openUri(url);
    }
    private void openUri(String url) {
        try {startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}
        catch(Exception e){toast("没有可打开网页的应用");}
    }
    private void toast(String value) {Toast.makeText(this,value,Toast.LENGTH_LONG).show();}
    private void exportLogs() {
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        String date=new SimpleDateFormat("yyyyMMdd-HHmmss",Locale.US).format(new Date());
        intent.putExtra(Intent.EXTRA_TITLE,"ANB-Root-诊断-"+date+".txt");
        try {startActivityForResult(intent,EXPORT_LOG);}
        catch(Exception e){toast("无法打开系统文件保存页面");}
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==EXPORT_LOG&&result==RESULT_OK&&data!=null&&data.getData()!=null) {
            try(OutputStream out=getContentResolver().openOutputStream(data.getData())) {
                if(out==null)throw new IOException("No output stream");
                out.write(engine.logs().getBytes("UTF-8"));
                toast("诊断记录已保存");
            }catch(Exception e){toast("保存失败，请重试");}
        }
    }
}
