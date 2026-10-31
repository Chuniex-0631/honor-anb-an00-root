package com.anb.root;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import rikka.shizuku.Shizuku;

/** Native onboarding and status UI. Shell execution is exclusively Shizuku UserService. */
public final class MainActivity extends Activity {
    private static final int GREEN=0xff087F67, DARK=0xff17372F, MUTED=0xff6B7D76, BG=0xffF3F6F2, LINE=0xffE5ECE6, AMBER=0xffA36B17;
    private RecoveryEngine engine;
    private final Handler main=new Handler(Looper.getMainLooper());
    private TextView headline,subtitle,badge,heroNote,deviceDetail,shizukuDetail,ksuDetail,environmentDetail,progressText,logText,recordStatus,recordMessage,actionNote;
    private Button primary,refresh,shizukuAction,ksuAction,details;
    private Switch allowShellSwitch;
    private TextView allowShellNote;
    private LinearLayout[] pages=new LinearLayout[3];
    private ScrollView[] scrolls=new ScrollView[3];
    private TextView[] tabs=new TextView[3];
    private LinearLayout actionBar,progressCard,firstUseCard;
    private boolean expanded;
    private int page;
    private String shownLog="";
    private final Shizuku.OnBinderReceivedListener received=()->{engine.invalidate();autoCheck();render();};
    private final Shizuku.OnBinderDeadListener dead=()->{engine.invalidate();engine.record("Shizuku 服务已停止；恢复入口暂不可用。");render();};
    private final Shizuku.OnRequestPermissionResultListener permission=(code,result)->{
        if(code!=51)return;
        if(result==PackageManager.PERMISSION_GRANTED){engine.record("用户已授予 Shizuku 权限。");engine.checkEnvironment();}
        else engine.record("Shizuku 权限未授予；可稍后在 Shizuku 的应用管理中开启。");
        render();
    };
    private final Runnable tick=new Runnable(){public void run(){render();main.postDelayed(this,1000);}};
    @Override public void onCreate(Bundle state){
        super.onCreate(state);engine=RecoveryEngine.get(this);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        buildUi();
        if(state!=null){expanded=state.getBoolean("details");showPage(state.getInt("page",0));}
        Shizuku.addBinderReceivedListenerSticky(received);
        Shizuku.addBinderDeadListener(dead);
        Shizuku.addRequestPermissionResultListener(permission);
    }
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putInt("page",page);state.putBoolean("details",expanded);}
    @Override protected void onResume(){super.onResume();autoCheck();main.removeCallbacks(tick);main.post(tick);}
    private void autoCheck(){if(engine.gate()==Readiness.Gate.ENVIRONMENT)engine.checkEnvironment();}
    @Override protected void onPause(){super.onPause();main.removeCallbacks(tick);}
    @Override protected void onDestroy(){
        main.removeCallbacks(tick);
        Shizuku.removeBinderReceivedListener(received);Shizuku.removeBinderDeadListener(dead);
        Shizuku.removeRequestPermissionResultListener(permission);super.onDestroy();
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView text(String value,int size,int color,boolean bold){
        TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);
        if(bold)v.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        v.setLineSpacing(dp(3),1);v.setIncludeFontPadding(false);return v;
    }
    private GradientDrawable shape(int color,int radius){GradientDrawable b=new GradientDrawable();b.setColor(color);b.setCornerRadius(dp(radius));return b;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private void gap(LinearLayout parent,int height){parent.addView(new Space(this),new LinearLayout.LayoutParams(1,dp(height)));}
    private void divider(LinearLayout parent){View v=new View(this);v.setBackgroundColor(LINE);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(1));lp.topMargin=dp(12);lp.bottomMargin=dp(12);parent.addView(v,lp);}
    private LinearLayout card(LinearLayout parent){
        LinearLayout c=column();c.setPadding(dp(18),dp(18),dp(18),dp(18));c.setBackground(shape(Color.WHITE,20));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(14);parent.addView(c,lp);return c;
    }
    private Button button(String label,View.OnClickListener listener,boolean solid){
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(15);b.setTypeface(Typeface.create("sans-serif-medium",0));
        b.setTextColor(solid?Color.WHITE:GREEN);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(dp(12),0,dp(12),0);
        b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(solid?0x33ffffff:0x22087f67),shape(solid?GREEN:0xffE8F1EB,14),null));
        b.setStateListAnimator(null);b.setOnClickListener(listener);return b;
    }
    private void full(LinearLayout parent,View view){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(50));lp.topMargin=dp(8);parent.addView(view,lp);}
    private TextView statusRow(LinearLayout parent,String label,View.OnClickListener action){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setMinimumHeight(dp(43));
        TextView name=text(label,14,DARK,true);row.addView(name,new LinearLayout.LayoutParams(0,-2,1));
        TextView value=text("检查中",13,MUTED,false);value.setGravity(Gravity.END);row.addView(value);
        row.setContentDescription(label+"状态");row.setOnClickListener(action);parent.addView(row,new LinearLayout.LayoutParams(-1,-2));return value;
    }
    private void title(LinearLayout parent,String heading,String detail){parent.addView(text(heading,22,DARK,true));gap(parent,7);parent.addView(text(detail,13,MUTED,false));gap(parent,20);}
    private void buildUi(){
        LinearLayout frame=column();frame.setBackgroundColor(BG);
        frame.setOnApplyWindowInsetsListener((view,insets)->{
            view.setPadding(0,insets.getSystemWindowInsetTop(),0,insets.getSystemWindowInsetBottom());return insets;
        });
        LinearLayout header=new LinearLayout(this);header.setPadding(dp(22),dp(14),dp(22),dp(14));header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon=new ImageView(this);int iconId=getResources().getIdentifier("ic_launcher","mipmap",getPackageName());
        if(iconId==0)iconId=getResources().getIdentifier("ic_launcher","drawable",getPackageName());icon.setImageResource(iconId);
        icon.setContentDescription("ANB Root 图标");header.addView(icon,new LinearLayout.LayoutParams(dp(42),dp(42)));
        LinearLayout brand=column();LinearLayout.LayoutParams blp=new LinearLayout.LayoutParams(0,-2,1);blp.leftMargin=dp(12);
        brand.addView(text("ANB Root",21,DARK,true));gap(brand,3);brand.addView(text("荣耀 80 Pro 直屏版",11,MUTED,false));header.addView(brand,blp);
        TextView version=text("2.2.0",11,GREEN,true);version.setPadding(dp(10),dp(6),dp(10),dp(6));version.setBackground(shape(0xffE1EDE5,12));version.setOnClickListener(v->showAbout());header.addView(version);frame.addView(header);
        FrameLayout content=new FrameLayout(this);frame.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        for(int i=0;i<3;i++){
            ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);
            LinearLayout body=column();body.setPadding(dp(22),dp(10),dp(22),dp(14));scroll.addView(body);
            pages[i]=body;scrolls[i]=scroll;content.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        }
        buildHome(pages[0]);buildRecords(pages[1]);buildHelp(pages[2]);
        actionBar=column();actionBar.setPadding(dp(22),dp(10),dp(22),dp(10));actionBar.setBackgroundColor(BG);
        primary=button("检查准备状态",v->primaryAction(),true);actionBar.addView(primary,new LinearLayout.LayoutParams(-1,dp(54)));
        actionNote=text("仅在本次开机有效",11,MUTED,false);actionNote.setGravity(Gravity.CENTER);gap(actionBar,7);actionBar.addView(actionNote);frame.addView(actionBar);
        LinearLayout navigation=new LinearLayout(this);navigation.setPadding(dp(12),dp(2),dp(12),dp(5));navigation.setBackgroundColor(Color.WHITE);
        for(int i=0;i<3;i++){
            final int index=i;TextView tab=text(new String[]{"首页","记录","帮助"}[i],14,MUTED,true);tab.setGravity(Gravity.CENTER);tab.setMinHeight(dp(48));
            tab.setOnClickListener(v->showPage(index));tabs[i]=tab;navigation.addView(tab,new LinearLayout.LayoutParams(0,dp(48),1));
        }
        frame.addView(navigation);setContentView(frame);showPage(0);
    }
    private void buildHome(LinearLayout root){
        LinearLayout hero=card(root);hero.setPadding(dp(22),dp(22),dp(22),dp(22));
        GradientDrawable background=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff123F35,0xff14634E});background.setCornerRadius(dp(24));hero.setBackground(background);
        badge=text("正在检查",11,0xffB8E4CF,true);hero.addView(badge);gap(hero,14);
        headline=text("准备你的 Root",26,Color.WHITE,true);hero.addView(headline);gap(hero,10);
        subtitle=text("",14,0xffE0EEE5,false);hero.addView(subtitle);gap(hero,17);
        heroNote=text("ANB-AN00 专用  ·  Shizuku 驱动",11,0xffA9CEB9,false);hero.addView(heroNote);
        firstUseCard=card(root);firstUseCard.addView(text("欢迎使用",16,DARK,true));gap(firstUseCard,6);
        firstUseCard.addView(text("先安装并启动 Shizuku，再准备 KernelSU。下方会逐项检查，并引导你完成首次设置。",13,MUTED,false));
        LinearLayout state=card(root);state.addView(text("准备状态",16,DARK,true));gap(state,9);
        deviceDetail=statusRow(state,"设备与系统",v->dialog("支持范围","荣耀 80 Pro 直屏版（ANB-AN00）\nMagicOS 9.0.0.230\n\n当前版本只支持这一已验证组合。",null,null));
        shizukuDetail=statusRow(state,"Shizuku",v->openPackage(RecoveryEngine.SHIZUKU,"https://shizuku.rikka.app/download/"));
        ksuDetail=statusRow(state,"KernelSU",v->openPackage(RecoveryEngine.KSU,"https://github.com/tiann/KernelSU/releases/tag/v3.3.0"));
        environmentDetail=statusRow(state,"执行环境",v->refreshEnvironment());
        divider(state);LinearLayout shortcuts=new LinearLayout(this);
        shizukuAction=button("Shizuku",v->openPackage(RecoveryEngine.SHIZUKU,"https://shizuku.rikka.app/download/"),false);
        ksuAction=button("KernelSU",v->openPackage(RecoveryEngine.KSU,"https://github.com/tiann/KernelSU/releases/tag/v3.3.0"),false);
        shortcuts.addView(shizukuAction,new LinearLayout.LayoutParams(0,dp(44),1));LinearLayout.LayoutParams k=new LinearLayout.LayoutParams(0,dp(44),1);k.leftMargin=dp(10);shortcuts.addView(ksuAction,k);state.addView(shortcuts);
        progressCard=card(root);progressCard.addView(text("本次进度",16,DARK,true));gap(progressCard,12);progressText=text("",14,MUTED,false);progressText.setLineSpacing(dp(8),1);progressCard.addView(progressText);
        LinearLayout options=card(root);options.addView(text("恢复选项",16,DARK,true));gap(options,10);
        allowShellSwitch=new Switch(this);allowShellSwitch.setText("允许调试工具使用 Root");allowShellSwitch.setTextSize(14);allowShellSwitch.setTextColor(DARK);
        allowShellSwitch.setMinHeight(dp(52));allowShellSwitch.setSwitchPadding(dp(12));
        allowShellSwitch.setChecked(engine.allowShellSelected());options.addView(allowShellSwitch,new LinearLayout.LayoutParams(-1,-2));
        gap(options,8);options.addView(text("开启后，ADB、Shizuku 等以调试身份运行的工具可直接获准使用 Root，无需逐项授权。普通应用仍由 KernelSU 单独管理。日常使用建议关闭。",13,MUTED,false));
        gap(options,10);allowShellNote=text("",12,AMBER,false);options.addView(allowShellNote);
        allowShellSwitch.setOnCheckedChangeListener((view,enabled)->{
            if(enabled==engine.allowShellSelected())return;
            if(!enabled){if(!engine.selectAllowShell(false))toast("暂时无法保存选项，请重新检查状态");render();return;}
            view.setChecked(false);
            new AlertDialog.Builder(this).setTitle("允许调试工具使用 Root？")
                    .setMessage("这会允许所有以 shell 身份运行的调试工具请求 Root，包括已授权电脑的 ADB 和通过 Shizuku 运行的工具，不再逐项询问。\n\n选项会被记住，在下一次恢复时生效。恢复成功后若要取消这项自动授权，需要完整重启后关闭选项并重新恢复。")
                    .setPositiveButton("允许",(d,w)->{if(!engine.selectAllowShell(true))toast("当前状态已变化，未开启选项");render();})
                    .setNegativeButton("保持关闭",null).show();
        });
        LinearLayout how=card(root);how.addView(text("重启后怎么做？",16,DARK,true));gap(how,10);
        how.addView(text("①  启动 Shizuku\n②  返回这里恢复 Root\n③  在 KernelSU 管理权限",13,MUTED,false));
        full(how,button("查看完整指南",v->showPage(2),false));
    }
    private void buildRecords(LinearLayout root){
        title(root,"运行记录","查看最近的执行状态，或保存诊断记录。" );
        LinearLayout summary=card(root);recordStatus=text("尚未开始",18,DARK,true);summary.addView(recordStatus);gap(summary,8);recordMessage=text("",13,MUTED,false);summary.addView(recordMessage);
        LinearLayout tools=card(root);tools.addView(text("排查与支持",16,DARK,true));gap(tools,6);
        tools.addView(text("遇到问题时先保存记录；不要连续重复启动恢复。",13,MUTED,false));
        full(tools,button("保存诊断记录",v->export(),false));
        details=button("展开详细记录",v->{expanded=!expanded;render();},false);full(tools,details);
        logText=text("",11,MUTED,false);logText.setTypeface(Typeface.MONOSPACE);logText.setTextIsSelectable(true);logText.setPadding(0,dp(16),0,0);tools.addView(logText);
        LinearLayout management=card(root);management.addView(text("KernelSU 状态未更新？",16,DARK,true));gap(management,6);
        management.addView(text("刷新只会重新打开管理器，不会再次执行 Root 恢复。",13,MUTED,false));full(management,button("刷新 KernelSU",v->engine.refreshManagerOnly(),false));
    }
    private void buildHelp(LinearLayout root){
        title(root,"使用帮助","每次开机，一次清晰的恢复流程。" );
        LinearLayout start=card(root);start.addView(text("第一次使用",17,DARK,true));gap(start,10);
        start.addView(text("1. 安装官方 Shizuku 与 KernelSU v3.3.0。\n2. 在 Shizuku 中通过无线调试配对并启动。\n3. 返回 ANB Root，允许使用 Shizuku。\n4. 检查通过后，完成首次准备并开始恢复。\n5. 如提示授权，在 KernelSU 中允许 ANB Root，然后返回验证。",14,MUTED,false));
        LinearLayout reboot=card(root);reboot.addView(text("每次重启之后",17,DARK,true));gap(reboot,10);
        reboot.addView(text("解锁手机，等待开机约 90 秒。重新启动 Shizuku，再回到 ANB Root 点击恢复。\n\n只有显示“Root 已验证”后，才确认本次权限可用。Shizuku 和临时 Root 都不会自动跨重启保持运行。",14,MUTED,false));
        LinearLayout faq=card(root);faq.addView(text("常见问题",17,DARK,true));gap(faq,6);
        helpRow(faq,"Shizuku 未运行或无法授权",()->dialog("重新连接 Shizuku","先在 Shizuku 中通过无线调试启动服务，再返回本 App。若曾拒绝授权，可在 Shizuku 的应用管理中重新允许 ANB Root。\n\n关闭开发者选项或撤销配对后，需要先在手机上重新开启调试、配对并启动。","打开 Shizuku",()->openPackage(RecoveryEngine.SHIZUKU,"https://shizuku.rikka.app/download/")));
        helpRow(faq,"KernelSU 显示“未安装”",()->dialog("刷新管理器状态","恢复完成后，旧的管理器进程可能仍显示过时状态。刷新会停止并重新打开 KernelSU，不会再次提权。","刷新 KernelSU",()->engine.refreshManagerOnly()));
        helpRow(faq,"恢复超时，或手机重启了",()->dialog("先确认状态","不要重复运行同一次恢复。先保存诊断记录；若本次状态无法确认，请完整重启手机，再启动 Shizuku 后重试。","查看记录",()->showPage(1)));
        helpRow(faq,"升级系统后还能使用吗？",()->dialog("系统版本支持","本版本仅适配 ANB-AN00 的 MagicOS 9.0.0.230。升级后若内核不匹配，App 会关闭恢复入口；需要重新确认适配，不能套用旧版文件。",null,null));
        helpRow(faq,"卸载 App 会立即解除 Root 吗？",()->dialog("临时 Root 的有效期","卸载 App 不会立即卸载当前已加载的内核模块。完整重启会结束本次临时 Root；之后不再启动恢复即可。",null,null));
        helpRow(faq,"允许调试工具使用 Root 是什么？",()->dialog("调试工具的 Root 权限","默认关闭。开启后，ADB、Shizuku 等以 shell 身份运行的工具可直接获准使用 Root，普通应用仍由 KernelSU 单独授权。\n\n该选择会被记住，只在恢复加载模块时设置。已有 Root 时不能即时切换；请完整重启手机，在恢复前修改。关闭选项仅取消此自动授权，不会撤销 KernelSU 中已有的其他授权。",null,null));
        LinearLayout privacy=card(root);privacy.addView(text("本地运行，由你掌控",17,DARK,true));gap(privacy,8);
        privacy.addView(text("App 不申请联网权限，不上传运行记录。诊断文件只会保存到你选择的位置。日志可能含设备版本和运行信息，分享前可以先查看。",13,MUTED,false));
        full(privacy,button("关于与开源许可",v->showAbout(),false));
    }
    private void helpRow(LinearLayout parent,String label,Runnable action){
        TextView row=text(label+"  ›",14,DARK,false);row.setPadding(0,dp(13),0,dp(13));row.setMinHeight(dp(48));row.setOnClickListener(v->action.run());parent.addView(row,new LinearLayout.LayoutParams(-1,-2));
    }
    private void dialog(String heading,String message,String action,Runnable task){
        AlertDialog.Builder builder=new AlertDialog.Builder(this).setTitle(heading).setMessage(message);
        if(action!=null)builder.setPositiveButton(action,(d,w)->task.run()).setNegativeButton("关闭",null);
        else builder.setPositiveButton("知道了",null);builder.show();
    }
    private void showAbout(){
        new AlertDialog.Builder(this).setTitle("ANB Root 2.2.0")
                .setMessage("荣耀 80 Pro 直屏版 · 临时 Root 助手\n适配 ANB-AN00 / MagicOS 9.0.0.230\n\n通过 Shizuku 提供执行环境，使用已验证的 GhostLock-H80GT 载荷与 KernelSU 管理权限。\n\n本版增加调试工具 Root 选项，默认关闭；原载荷与其余加载参数保持不变。")
                .setPositiveButton("关闭",null).setNeutralButton("开源许可",(d,w)->showLicenses()).show();
    }
    private void showLicenses(){
        try{String content;try(InputStream in=getAssets().open("THIRD_PARTY_NOTICES.txt");ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);content=new String(out.toByteArray(),"UTF-8");}
            TextView t=text(content,12,MUTED,false);t.setTextIsSelectable(true);t.setPadding(dp(20),dp(12),dp(20),dp(12));ScrollView s=new ScrollView(this);s.addView(t);
            new AlertDialog.Builder(this).setTitle("开源组件与许可").setView(s).setPositiveButton("关闭",null).show();
        }catch(Exception e){toast("暂时无法显示许可文件");}
    }
    private void showPage(int index){
        page=Math.max(0,Math.min(2,index));for(int i=0;i<3;i++){scrolls[i].setVisibility(i==page?View.VISIBLE:View.GONE);tabs[i].setTextColor(i==page?GREEN:MUTED);tabs[i].setBackground(i==page?shape(0xffE8F1EB,14):null);tabs[i].setSelected(i==page);}
        actionBar.setVisibility(page==0?View.VISIBLE:View.GONE);render();
    }
    private boolean setup(){return getSharedPreferences("setup",0).getBoolean("completed",false);}
    private void refreshEnvironment(){engine.invalidate();if(engine.gate()==Readiness.Gate.ENVIRONMENT)engine.checkEnvironment();else render();}
    private String humanError(String message){
        if(message.contains("90 秒")||message.contains("仍在启动"))return message;
        if(message.contains("Shizuku")||message.contains("未授予"))return "执行服务已断开或尚未授权。请先启动 Shizuku 并允许访问，再返回检查。";
        if(message.contains("另一次")||message.contains("同次"))return "已有恢复记录，暂时不要再次启动。请到记录页查看本次进度。";
        if(message.contains("5 分钟"))return "等待时间较长，尚未确认完成。请保存记录；不要连续重复恢复。";
        return "暂时无法确认恢复完成。请到记录页保存诊断，并按帮助中的说明处理。";
    }
    private void render(){
        if(headline==null||primary==null)return;
        Readiness.Gate gate=engine.gate();boolean busy=engine.busy.get()||engine.checking;
        deviceDetail.setText(engine.compatible()?"已匹配":"不受支持");deviceDetail.setTextColor(engine.compatible()?GREEN:AMBER);
        String shizuku="待安装";int shColor=AMBER;
        if(engine.installed(RecoveryEngine.SHIZUKU)){
            try {if(!Shizuku.pingBinder())shizuku="待启动";
                else if(Shizuku.getVersion()<13)shizuku="待更新";
                else if(Shizuku.checkSelfPermission()!=PackageManager.PERMISSION_GRANTED)shizuku="待授权";
                else if(Shizuku.getUid()!=2000)shizuku="需无线调试启动";
                else{shizuku="已连接";shColor=GREEN;}}
            catch(Exception e){shizuku="连接已断开";}
        }
        shizukuDetail.setText(shizuku);shizukuDetail.setTextColor(shColor);
        boolean ksu=engine.installed(RecoveryEngine.KSU);ksuDetail.setText(ksu?"已安装":"待安装");ksuDetail.setTextColor(ksu?GREEN:AMBER);
        environmentDetail.setText(engine.checking?"检测中…":engine.environmentReady?"已通过":"待检测");environmentDetail.setTextColor(engine.environmentReady?GREEN:MUTED);
        firstUseCard.setVisibility(setup()?View.GONE:View.VISIBLE);
        allowShellSwitch.setChecked(engine.allowShellSelected());
        allowShellSwitch.setEnabled(!busy&&!engine.modulePresent);
        allowShellNote.setText(engine.modulePresent?"本次 Root 已加载，不能即时更改。请完整重启后，在恢复前设置。":busy
                ?"正在处理，本次恢复选项已锁定。"
                :"默认关闭，会记住你的选择。已开始的恢复不会改用新选项；完整重启后按新选择恢复。");
        String[] state=describe(gate);String heroTitle=state[0],heroMessage=state[1],statusLabel=setup()?"本机状态":"首次准备";
        if(gate==Readiness.Gate.READY&&setup()){heroTitle="准备就绪";heroMessage=engine.modulePresent?"已发现 KernelSU。你可以直接验证当前 Root。":"所有条件已通过，点击下方按钮恢复本次权限。";}
        if(engine.phase==RecoveryEngine.Phase.SUCCESS){heroTitle="Root 已就绪";heroMessage=engine.message.contains("刷新未完成")?"Root 已验证，管理器状态待更新。可到记录页刷新 KernelSU。":"本次开机的 Root 已验证。你可以在 KernelSU 中管理权限。";statusLabel="已验证";}
        else if(engine.phase==RecoveryEngine.Phase.WAIT_AUTH){heroTitle="还差一步授权";heroMessage="请在 KernelSU 中允许 ANB Root 使用 Root，然后返回这里验证。";statusLabel="等待授权";}
        else if(engine.phase==RecoveryEngine.Phase.FAILED){heroTitle="本次尚未完成";heroMessage=humanError(engine.message);statusLabel="需要处理";}
        if(engine.checking){heroTitle="正在检查环境";heroMessage="请稍候，正在确认 Shizuku 提供的执行环境。";statusLabel="自动检查";}
        else if(engine.busy.get()){heroTitle=engine.title;heroMessage=engine.phase==RecoveryEngine.Phase.RUNNING?"请保持耐心，通常需要 1–3 分钟。不要重复启动；进度会持续显示。":engine.message;statusLabel="进行中";}
        headline.setText(heroTitle);subtitle.setText(heroMessage);badge.setText(statusLabel);
        heroNote.setText(engine.phase==RecoveryEngine.Phase.SUCCESS?"本次开机有效  ·  重启后需重新恢复":"ANB-AN00 专用  ·  Shizuku 驱动");
        primary.setEnabled(!busy && gate!=Readiness.Gate.DEVICE);primary.setAlpha(primary.isEnabled()?1f:.45f);
        if(busy)primary.setText(engine.checking?"正在检查…":"正在处理，请稍候");
        else if(gate!=Readiness.Gate.READY)primary.setText(state[2]);
        else if(!setup())primary.setText("完成首次准备");
        else if(engine.phase==RecoveryEngine.Phase.WAIT_AUTH)primary.setText("验证 Root 授权");
        else if(engine.phase==RecoveryEngine.Phase.SUCCESS)primary.setText("验证当前 Root");
        else primary.setText(engine.modulePresent?"验证当前 Root":"恢复 Root");
        actionNote.setText(busy?"前台通知会保留本次进度":gate==Readiness.Gate.READY?"仅在本次开机有效":"按提示完成准备后即可继续");
        shizukuAction.setEnabled(!engine.busy.get());ksuAction.setEnabled(!engine.busy.get()||engine.phase==RecoveryEngine.Phase.WAIT_AUTH);
        progressCard.setVisibility(engine.busy.get()||engine.phase==RecoveryEngine.Phase.SUCCESS||engine.phase==RecoveryEngine.Phase.WAIT_AUTH||engine.phase==RecoveryEngine.Phase.FAILED?View.VISIBLE:View.GONE);
        String[] stages={"检查状态","准备文件","恢复权限","完成加载","验证 Root"};StringBuilder progress=new StringBuilder();
        for(int i=0;i<stages.length;i++){String mark=engine.step>i?"✓":engine.step==i&&engine.busy.get()?"●":"○";progress.append(mark).append("  ").append(stages[i]);if(i==engine.step&&engine.busy.get())progress.append("  · 进行中");progress.append('\n');}
        progressText.setText(progress.toString().trim());recordStatus.setText(engine.title);recordMessage.setText(engine.message);
        details.setText(expanded?"收起详细记录":"展开详细记录");logText.setVisibility(expanded?View.VISIBLE:View.GONE);
        if(expanded){String current=engine.logs();if(!current.equals(shownLog)){logText.setText(current.isEmpty()?"暂无记录。完成检查或恢复后，会显示在这里。":current);shownLog=current;}}
        if(engine.busy.get())getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    private String[] describe(Readiness.Gate gate){
        switch(gate){
            case DEVICE:return new String[]{"当前版本不受支持","只支持 ANB-AN00 指定固件；不要更换其他机型的载荷。","不支持当前版本"};
            case SHIZUKU_INSTALL:return new String[]{"先安装 Shizuku","它负责提供本机执行权限。请从官方页面安装后返回。","获取 Shizuku"};
            case KSU_INSTALL:return new String[]{"先安装 KernelSU","检测到 Shizuku 后，还需要 KernelSU 管理器管理恢复后的 Root 授权。","获取 KernelSU"};
            case SHIZUKU_START:return new String[]{"启动 Shizuku","打开 Shizuku，通过无线调试配对并启动服务，再返回这里。每次重启后需重新启动。","打开 Shizuku"};
            case SHIZUKU_VERSION:return new String[]{"更新 Shizuku","执行服务需要 Shizuku 13 或更新版本。","更新 Shizuku"};
            case SHIZUKU_PERMISSION:return new String[]{"授予 Shizuku 权限","允许 ANB Root 使用 Shizuku。拒绝后仍可在 Shizuku 的应用管理中重新开启。","授予 Shizuku 权限"};
            case SHELL_MODE:return new String[]{"请用无线调试启动","当前服务以 Root 启动。请在 Shizuku 中停止服务，再通过无线调试启动，以确认重启后可用的执行方式。","打开 Shizuku"};
            case ENVIRONMENT:return new String[]{"检查执行环境","已连接并授权。再检查一次实际执行身份与进程限制。","检测执行环境"};
            default:return new String[]{setup()?"准备就绪":"准备条件已满足",setup()?"可以开始本次恢复。":"已检测两个应用、Shizuku 授权和执行环境。确认后开放恢复入口。","确认准备完成，进入恢复"};
        }
    }
    private void primaryAction(){
        Readiness.Gate gate=engine.gate();
        switch(gate){
            case SHIZUKU_INSTALL:case SHIZUKU_VERSION:openUri("https://shizuku.rikka.app/download/");return;
            case KSU_INSTALL:openUri("https://github.com/tiann/KernelSU/releases/tag/v3.3.0");return;
            case SHIZUKU_START:case SHELL_MODE:openPackage(RecoveryEngine.SHIZUKU,"https://shizuku.rikka.app/download/");return;
            case SHIZUKU_PERMISSION:
                try {
                    if(Shizuku.shouldShowRequestPermissionRationale())new AlertDialog.Builder(this).setTitle("重新开启权限")
                            .setMessage("请在 Shizuku → 已授权应用中给 ANB Root 开启权限，然后返回。").setPositiveButton("打开 Shizuku",(d,w)->openPackage(RecoveryEngine.SHIZUKU,"https://shizuku.rikka.app/download/")).setNegativeButton("稍后",null).show();
                    else Shizuku.requestPermission(51);
                }catch(Exception e){toast("Shizuku 连接已变化，请重新检查");}return;
            case ENVIRONMENT:engine.checkEnvironment();return;
            case DEVICE:return;
            default:break;
        }
        if(!setup()){
            if(getSharedPreferences("setup",0).edit().putBoolean("completed",true).commit())engine.record("首次准备完成：依赖、授权与执行环境已确认。");
            render();return;
        }
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
        { requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},52);return; }
        startForegroundService(new Intent(this,RecoveryService.class));render();
    }
    @Override public void onRequestPermissionsResult(int request,String[] names,int[] grants){
        super.onRequestPermissionsResult(request,names,grants);
        if(request==52 && setup() && engine.gate()==Readiness.Gate.READY)
            startForegroundService(new Intent(this,RecoveryService.class));
    }
    private void openPackage(String pkg,String fallback){
        Intent launch=getPackageManager().getLaunchIntentForPackage(pkg);
        if(launch!=null)try{startActivity(launch);return;}catch(Exception ignored){}
        openUri(fallback);
    }
    private void openUri(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception e){toast("没有可打开网页的应用");}}
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
    private void export(){
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE,"ANB-Root-诊断-"+new java.text.SimpleDateFormat("yyyyMMdd-HHmmss",java.util.Locale.US).format(new java.util.Date())+".txt");
        try{startActivityForResult(intent,53);}catch(Exception e){toast("当前无法打开保存页面");}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==53&&result==RESULT_OK&&data!=null&&data.getData()!=null){
            try(OutputStream out=getContentResolver().openOutputStream(data.getData())){
                if(out==null)throw new IOException();out.write(engine.logs().getBytes("UTF-8"));toast("诊断记录已保存");
            }catch(Exception e){toast("保存失败，请重试");}
        }
    }
}
