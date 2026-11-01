package com.anb.root;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.*;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import java.io.*;
import rikka.shizuku.Shizuku;

/**
 * Onboarding, status and records. The activity is a renderer: what to show comes from
 * {@link StatusModel}, why it failed from {@link FailureText}, and the export from
 * {@link Diagnostics}. Rendering is event driven; only a running recovery ticks a clock.
 */
public final class MainActivity extends Activity {
    private static final int PAGE_HOME=0, PAGE_RECORDS=1, PAGE_HELP=2;
    private static final int REQ_SHIZUKU=51, REQ_NOTIFICATIONS=52, REQ_EXPORT=53;
    private static final long PRIMARY_DEBOUNCE_MS=800;
    private static final String[] STAGE_LABELS={"检查状态","准备文件","恢复权限","完成加载","验证 Root"};

    private RecoveryEngine engine;
    private final Handler main=new Handler(Looper.getMainLooper());

    private int cBg,cSurface,cLine,cText,cMuted,cBrand,cBrandSoft,cBrandTint,cOnBrand,cWarn,cWarnSoft,
            cHeroFrom,cHeroTo,cHeroText,cHeroMuted,cHeroBadge,cRipple,cRippleOnBrand,cNavBg;

    private TextView headline,subtitle,badge,heroNote,deviceDetail,shizukuDetail,ksuDetail,environmentDetail,
            recordStatus,recordMessage,actionNote,errorDetail,errorGuidance,elapsedText,versionChip,logText,checkCount;
    private Button primary,shizukuAction,ksuAction;
    private TextView details;
    private Switch allowShellSwitch;
    private TextView allowShellNote;
    private LinearLayout[] pages=new LinearLayout[3];
    private ScrollView[] scrolls=new ScrollView[3];
    private LinearLayout[] tabItems=new LinearLayout[3];
    private ImageView[] tabIcons=new ImageView[3];
    private TextView[] tabLabels=new TextView[3];
    private TextView[] stageMarks=new TextView[5];
    private TextView[] stageStates=new TextView[5];
    private LinearLayout actionBar,progressCard,errorCard,allowShellRow;
    private View[] statusRows=new View[4];
    private View[] statusDots=new View[4];

    private boolean expanded,updatingSwitch,ticking;
    private int page;
    private String shownLog="";
    private long lastPrimaryTap,busySince;
    private int lastAnnouncedStep=-1;
    private RecoveryEngine.Phase lastPhase;

    private final Shizuku.OnBinderReceivedListener received=()->{engine.invalidate();autoCheck();refresh();};
    private final Shizuku.OnBinderDeadListener dead=()->{
        engine.invalidate();engine.record("Shizuku 服务已停止；恢复入口暂不可用。");refresh();};
    private final Shizuku.OnRequestPermissionResultListener permission=(code,result)->{
        if(code!=REQ_SHIZUKU)return;
        if(result==PackageManager.PERMISSION_GRANTED){engine.record("用户已授予 Shizuku 权限。");engine.checkEnvironment();}
        else engine.record("Shizuku 权限未授予；可稍后在 Shizuku 的应用管理中开启。");
        refresh();
    };

    private final Runnable busyTick=new Runnable(){public void run(){
        if(isFinishing())return;
        if(!engine.busy.get()){ticking=false;refresh();return;}
        updateProgress();
        if(engine.phase!=lastPhase){lastPhase=engine.phase;refresh();}
        main.postDelayed(this,1000);
    }};

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        engine=RecoveryEngine.get(this);
        resolvePalette();
        applySystemBars();
        buildUi();
        if(state!=null){
            expanded=state.getBoolean("details");
            showPage(state.getInt("page",PAGE_HOME));
        }else showPage(PAGE_HOME);
        Shizuku.addBinderReceivedListenerSticky(received);
        Shizuku.addBinderDeadListener(dead);
        Shizuku.addRequestPermissionResultListener(permission);
    }

    @Override protected void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);state.putInt("page",page);state.putBoolean("details",expanded);
    }

    @Override protected void onResume(){
        super.onResume();
        autoCheck();
        refresh();
        main.removeCallbacks(busyTick);
        if(engine.busy.get())main.post(busyTick);
    }

    @Override protected void onPause(){ticking=false;main.removeCallbacks(busyTick);super.onPause();}

    @Override protected void onDestroy(){
        main.removeCallbacks(busyTick);
        Shizuku.removeBinderReceivedListener(received);Shizuku.removeBinderDeadListener(dead);
        Shizuku.removeRequestPermissionResultListener(permission);
        super.onDestroy();
    }

    @Override public void onBackPressed(){
        if(page!=PAGE_HOME){showPage(PAGE_HOME);return;}
        super.onBackPressed();
    }

    // ---------------------------------------------------------------- palette and system bars

    private void resolvePalette(){
        cBg=color(R.color.bg);cSurface=color(R.color.surface);cLine=color(R.color.line);
        cText=color(R.color.text);cMuted=color(R.color.text_muted);cBrand=color(R.color.brand);
        cBrandSoft=color(R.color.brand_soft);cBrandTint=color(R.color.brand_tint);cOnBrand=color(R.color.on_brand);
        cWarn=color(R.color.warn);cWarnSoft=color(R.color.warn_soft);
        cHeroFrom=color(R.color.hero_from);cHeroTo=color(R.color.hero_to);cHeroText=color(R.color.hero_text);
        cHeroMuted=color(R.color.hero_muted);cHeroBadge=color(R.color.hero_badge);
        cRipple=color(R.color.ripple);cRippleOnBrand=color(R.color.ripple_on_brand);cNavBg=color(R.color.nav_bg);
    }

    private int color(int id){return getColor(id);}

    private boolean nightMode(){
        return (getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
    }

    private void applySystemBars(){
        Window window=getWindow();
        window.setStatusBarColor(cBg);window.setNavigationBarColor(cBg);
        int flags=View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        if(!nightMode())flags|=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        window.getDecorView().setSystemUiVisibility(flags);
    }

    // ---------------------------------------------------------------- type scale and view helpers

    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}

    private TextView text(String value,float size,int color,boolean medium){
        TextView v=new TextView(this);v.setText(value);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP,size);v.setTextColor(color);
        if(medium)v.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        v.setLineSpacing(dp(4),1);v.setIncludeFontPadding(false);return v;
    }

    /** Section heading inside a card: 17sp, medium, tight bottom rhythm. */
    private TextView section(String title){return text(title,17,cText,true);}

    /** Body copy: 14sp with a 1.35 line height, which is what makes paragraphs readable. */
    private TextView body(String value){return text(value,14,cMuted,false);}

    /** Overline label: 11sp, wide tracking, used for the hero eyebrow. */
    private TextView overline(String value,int color){
        TextView v=text(value,11,color,true);v.setLetterSpacing(0.08f);return v;
    }

    private GradientDrawable shape(int fill,int radius){
        GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));return d;
    }

    private GradientDrawable outline(int fill,int radius,int strokeColor){
        GradientDrawable d=shape(fill,radius);d.setStroke(dp(1),strokeColor);return d;
    }

    private Drawable ripple(int rippleColor,Drawable content){
        return new RippleDrawable(ColorStateList.valueOf(rippleColor),content,null);
    }

    private View dot(int color){
        View v=new View(this);
        v.setBackground(shape(color,99));
        return v;
    }

    private void paintDot(View dot,int color){
        if(dot!=null)dot.setBackground(shape(color,99));
    }

    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}

    private void gap(LinearLayout parent,int height){parent.addView(new Space(this),new LinearLayout.LayoutParams(1,dp(height)));}

    private void divider(LinearLayout parent,int leftInset){
        View v=new View(this);v.setBackgroundColor(cLine);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(1));
        lp.leftMargin=dp(leftInset);parent.addView(v,lp);
    }

    private LinearLayout card(LinearLayout parent){
        LinearLayout c=column();c.setPadding(dp(20),dp(18),dp(20),dp(18));
        c.setBackground(shape(cSurface,18));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(12);
        parent.addView(c,lp);return c;
    }

    private Button button(String label,View.OnClickListener listener,boolean solid){
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP,15);
        b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        b.setTextColor(solid?cOnBrand:cBrand);
        b.setMinHeight(0);b.setMinimumHeight(dp(48));
        b.setPadding(dp(16),dp(12),dp(16),dp(12));
        b.setBackground(ripple(solid?cRippleOnBrand:cRipple,shape(solid?cBrand:cBrandTint,14)));
        b.setStateListAnimator(null);b.setOnClickListener(listener);return b;
    }

    private void full(LinearLayout parent,View view){
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.topMargin=dp(10);parent.addView(view,lp);
    }

    /**
     * One checklist entry: status dot, label, value and a chevron, separated by dividers.
     * The dot carries the state colour so the value text can stay neutral and readable.
     */
    private TextView statusRow(LinearLayout parent,int index,String label,View.OnClickListener action){
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(56));
        row.setBackground(ripple(cRipple,null));
        row.setPadding(0,0,0,0);

        View marker=dot(cMuted);
        LinearLayout.LayoutParams dlp=new LinearLayout.LayoutParams(dp(8),dp(8));
        dlp.rightMargin=dp(14);row.addView(marker,dlp);
        statusDots[index]=marker;

        TextView name=text(label,15,cText,false);
        row.addView(name,new LinearLayout.LayoutParams(0,-2,1));

        TextView value=text("检查中",13,cMuted,true);
        value.setGravity(Gravity.END);row.addView(value);

        TextView chevron=text("›",18,cMuted,false);
        LinearLayout.LayoutParams clp=new LinearLayout.LayoutParams(-2,-2);
        clp.leftMargin=dp(8);row.addView(chevron,clp);

        row.setFocusable(true);row.setClickable(true);row.setOnClickListener(action);
        statusRows[index]=row;
        parent.addView(row,new LinearLayout.LayoutParams(-1,-2));
        return value;
    }

    private void title(LinearLayout parent,String heading,String detail){
        parent.addView(text(heading,22,cText,true));gap(parent,6);
        parent.addView(body(detail));gap(parent,18);
    }

    /** Numbered step list: a fixed number gutter keeps long Chinese lines scannable. */
    private void steps(LinearLayout parent,String[] items){
        for(int i=0;i<items.length;i++){
            LinearLayout row=new LinearLayout(this);
            row.setGravity(Gravity.TOP);
            TextView number=text(String.valueOf(i+1),13,cBrand,true);
            LinearLayout.LayoutParams nlp=new LinearLayout.LayoutParams(dp(22),-2);
            row.addView(number,nlp);
            TextView copy=body(items[i]);copy.setTextColor(cText);
            row.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
            LinearLayout.LayoutParams rlp=new LinearLayout.LayoutParams(-1,-2);
            if(i>0)rlp.topMargin=dp(10);
            parent.addView(row,rlp);
        }
    }

    private void helpRow(LinearLayout parent,String label,Runnable action){
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(52));
        row.setBackground(ripple(cRipple,null));
        row.setPadding(0,dp(12),0,dp(12));
        row.setFocusable(true);
        TextView name=text(label,15,cText,false);
        row.addView(name,new LinearLayout.LayoutParams(0,-2,1));
        row.addView(text("›",18,cMuted,false));
        row.setOnClickListener(v->action.run());
        parent.addView(row,new LinearLayout.LayoutParams(-1,-2));
    }

    // ---------------------------------------------------------------- layout

    private void buildUi(){
        LinearLayout frame=column();frame.setBackgroundColor(cBg);
        frame.setOnApplyWindowInsetsListener((view,insets)->{
            view.setPadding(0,insets.getSystemWindowInsetTop(),0,insets.getSystemWindowInsetBottom());
            return insets;
        });
        frame.addView(buildHeader());

        FrameLayout content=new FrameLayout(this);
        frame.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        for(int i=0;i<3;i++){
            ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);
            LinearLayout body=column();
            body.setPadding(dp(20),dp(6),dp(20),dp(16));scroll.addView(body);
            pages[i]=body;scrolls[i]=scroll;content.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        }
        buildHome(pages[PAGE_HOME]);buildRecords(pages[PAGE_RECORDS]);buildHelp(pages[PAGE_HELP]);

        actionBar=column();actionBar.setPadding(dp(20),dp(8),dp(20),dp(8));actionBar.setBackgroundColor(cBg);
        primary=button("检查准备状态",v->primaryAction(),true);
        primary.setSingleLine(true);
        actionBar.addView(primary,new LinearLayout.LayoutParams(-1,-2));
        actionNote=text("仅在本次开机有效",11,cMuted,false);
        actionNote.setGravity(Gravity.CENTER);actionNote.setSingleLine(true);gap(actionBar,6);actionBar.addView(actionNote);
        frame.addView(actionBar);

        LinearLayout navigation=new LinearLayout(this);
        navigation.setPadding(dp(10),dp(4),dp(10),dp(4));navigation.setBackgroundColor(cNavBg);
        String[] labels={"首页","记录","帮助"};
        int[] icons={R.drawable.ic_tab_home,R.drawable.ic_tab_records,R.drawable.ic_tab_help};
        for(int i=0;i<3;i++){
            final int index=i;
            LinearLayout item=column();item.setGravity(Gravity.CENTER_HORIZONTAL);
            item.setBackground(ripple(cRipple,null));item.setFocusable(true);
            ImageView icon=new ImageView(this);icon.setImageResource(icons[i]);
            icon.setColorFilter(cMuted);
            item.addView(icon,new LinearLayout.LayoutParams(dp(22),dp(22)));
            TextView label=text(labels[i],11,cMuted,true);label.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lblp=new LinearLayout.LayoutParams(-2,-2);lblp.topMargin=dp(3);
            item.addView(label,lblp);
            item.setContentDescription(labels[i]+"，第 "+(i+1)+" 项，共 3 项");
            item.setOnClickListener(v->showPage(index));
            tabItems[i]=item;tabIcons[i]=icon;tabLabels[i]=label;
            navigation.addView(item,new LinearLayout.LayoutParams(0,dp(56),1));
        }
        frame.addView(navigation);
        setContentView(frame);
    }

    private View buildHeader(){
        LinearLayout header=new LinearLayout(this);
        header.setPadding(dp(20),dp(12),dp(20),dp(10));header.setGravity(Gravity.CENTER_VERTICAL);

        ImageView icon=new ImageView(this);
        int iconId=getResources().getIdentifier("ic_launcher","mipmap",getPackageName());
        if(iconId==0)iconId=getResources().getIdentifier("ic_launcher","drawable",getPackageName());
        icon.setImageResource(iconId);
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        icon.setContentDescription(null);
        header.addView(icon,new LinearLayout.LayoutParams(dp(36),dp(36)));

        LinearLayout brand=column();
        LinearLayout.LayoutParams blp=new LinearLayout.LayoutParams(0,-2,1);blp.leftMargin=dp(11);
        brand.addView(text("ANB Root",19,cText,true));
        LinearLayout.LayoutParams blp2=new LinearLayout.LayoutParams(-2,-2);blp2.topMargin=dp(1);
        brand.addView(text("荣耀 80 Pro 直屏版",11,cMuted,false),blp2);
        header.addView(brand,blp);

        versionChip=text(versionName(),11,cBrand,true);
        versionChip.setPadding(dp(10),dp(6),dp(10),dp(6));
        versionChip.setBackground(shape(cBrandSoft,10));
        versionChip.setMinHeight(dp(28));
        versionChip.setContentDescription("版本 "+versionName()+"，点按查看关于");
        versionChip.setOnClickListener(v->showAbout());
        header.addView(versionChip);
        return header;
    }

    private void buildHome(LinearLayout root){
        LinearLayout hero=card(root);
        hero.setPadding(dp(22),dp(20),dp(22),dp(20));
        GradientDrawable background=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{cHeroFrom,cHeroTo});
        background.setCornerRadius(dp(22));hero.setBackground(background);

        LinearLayout eyebrowRow=new LinearLayout(this);eyebrowRow.setGravity(Gravity.CENTER_VERTICAL);
        eyebrowRow.addView(dot(cHeroBadge),new LinearLayout.LayoutParams(dp(6),dp(6)));
        LinearLayout.LayoutParams elp=new LinearLayout.LayoutParams(-2,-2);elp.leftMargin=dp(7);
        badge=overline("首次准备",cHeroBadge);
        eyebrowRow.addView(badge,elp);
        hero.addView(eyebrowRow);gap(hero,12);

        headline=text("准备你的 Root",24,cHeroText,true);
        headline.setLineSpacing(dp(2),1);hero.addView(headline);gap(hero,8);
        subtitle=text("",13,cHeroMuted,false);subtitle.setLineSpacing(dp(5),1);hero.addView(subtitle);gap(hero,14);
        heroNote=text("ANB-AN00 · MagicOS 9.0.0.230",11,cHeroMuted,false);
        hero.addView(heroNote);

        errorCard=card(root);errorCard.setVisibility(View.GONE);
        errorCard.setBackground(outline(cWarnSoft,18,cWarn));
        errorCard.addView(section("需要处理"));gap(errorCard,8);
        errorDetail=text("",13,cText,false);errorDetail.setTextIsSelectable(true);errorCard.addView(errorDetail);
        gap(errorCard,4);
        errorGuidance=body("");errorCard.addView(errorGuidance);
        LinearLayout errorActions=new LinearLayout(this);gap(errorCard,12);
        Button save=button("保存诊断记录",v->export(),false);
        Button help=button("查看帮助",v->showPage(PAGE_HELP),false);
        errorActions.addView(save,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout.LayoutParams hlp=new LinearLayout.LayoutParams(0,-2,1);hlp.leftMargin=dp(10);
        errorActions.addView(help,hlp);errorCard.addView(errorActions);

        LinearLayout state=card(root);
        LinearLayout stateHead=new LinearLayout(this);stateHead.setGravity(Gravity.CENTER_VERTICAL);
        stateHead.addView(section("准备状态"),new LinearLayout.LayoutParams(0,-2,1));
        checkCount=text("",12,cMuted,false);checkCount.setGravity(Gravity.END);
        stateHead.addView(checkCount);
        state.addView(stateHead);gap(state,4);

        deviceDetail=statusRow(state,0,"设备与系统",v->dialog("支持范围",
                "荣耀 80 Pro 直屏版（ANB-AN00）\nMagicOS 9.0.0.230\n\n当前版本只支持这一已验证组合。",null,null));
        divider(state,22);
        shizukuDetail=statusRow(state,1,"Shizuku",v->openPackage(RecoveryEngine.SHIZUKU,
                "https://shizuku.rikka.app/download/"));
        divider(state,22);
        ksuDetail=statusRow(state,2,"KernelSU",v->openPackage(RecoveryEngine.KSU,
                "https://github.com/tiann/KernelSU/releases/tag/v3.3.0"));
        divider(state,22);
        environmentDetail=statusRow(state,3,"执行环境",v->refreshEnvironment());

        LinearLayout shortcuts=new LinearLayout(this);gap(state,14);
        shizukuAction=button("打开 Shizuku",v->openPackage(RecoveryEngine.SHIZUKU,
                "https://shizuku.rikka.app/download/"),false);
        ksuAction=button("打开 KernelSU",v->openPackage(RecoveryEngine.KSU,
                "https://github.com/tiann/KernelSU/releases/tag/v3.3.0"),false);
        shortcuts.addView(shizukuAction,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout.LayoutParams klp=new LinearLayout.LayoutParams(0,-2,1);klp.leftMargin=dp(10);
        shortcuts.addView(ksuAction,klp);
        state.addView(shortcuts);

        progressCard=card(root);
        LinearLayout progressHead=new LinearLayout(this);progressHead.setGravity(Gravity.CENTER_VERTICAL);
        progressHead.addView(section("本次进度"),new LinearLayout.LayoutParams(0,-2,1));
        elapsedText=text("",12,cMuted,false);
        progressHead.addView(elapsedText);
        progressCard.addView(progressHead);gap(progressCard,12);
        for(int i=0;i<5;i++){
            LinearLayout row=new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);row.setMinimumHeight(dp(32));
            TextView mark=text("○",13,cMuted,true);
            mark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            mark.setWidth(dp(22));row.addView(mark);
            TextView label=text(STAGE_LABELS[i],13,cMuted,false);
            row.addView(label,new LinearLayout.LayoutParams(0,-2,1));
            TextView stateText=text("等待",12,cMuted,false);
            stateText.setGravity(Gravity.END);row.addView(stateText);
            stageMarks[i]=mark;stageStates[i]=stateText;
            progressCard.addView(row,new LinearLayout.LayoutParams(-1,-2));
        }

        LinearLayout options=card(root);
        options.addView(section("恢复选项"));gap(options,6);
        allowShellRow=new LinearLayout(this);
        allowShellRow.setGravity(Gravity.CENTER_VERTICAL);allowShellRow.setMinimumHeight(dp(52));
        allowShellRow.setBackground(ripple(cRipple,null));allowShellRow.setFocusable(true);
        allowShellRow.addView(text("允许调试工具使用 Root",15,cText,false),
                new LinearLayout.LayoutParams(0,-2,1));
        allowShellSwitch=new Switch(this);
        allowShellSwitch.setMinHeight(dp(48));
        allowShellSwitch.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        allowShellSwitch.setChecked(engine.allowShellSelected());
        allowShellRow.addView(allowShellSwitch);
        allowShellRow.setOnClickListener(v->allowShellSwitch.toggle());
        options.addView(allowShellRow,new LinearLayout.LayoutParams(-1,-2));
        gap(options,4);
        allowShellNote=text("",12,cWarn,false);allowShellNote.setLineSpacing(dp(4),1);
        options.addView(allowShellNote);
        allowShellSwitch.setOnCheckedChangeListener((view,enabled)->{
            if(updatingSwitch)return;
            if(enabled==engine.allowShellSelected())return;
            if(!enabled){if(!engine.selectAllowShell(false))toast("暂时无法保存选项，请重新检查状态");refresh();return;}
            view.setChecked(false);
            new AlertDialog.Builder(this).setTitle("允许调试工具使用 Root？")
                    .setMessage("这会允许所有以 shell 身份运行的调试工具请求 Root，包括已授权电脑的 ADB 和通过 Shizuku 运行的工具，不再逐项询问。\n\n选项会被记住，在下一次恢复时生效。恢复成功后若要取消这项自动授权，需要完整重启后关闭选项并重新恢复。")
                    .setPositiveButton("允许",(d,w)->{
                        if(!engine.selectAllowShell(true))toast("当前状态已变化，未开启选项");refresh();})
                    .setNegativeButton("保持关闭",null).show();
        });
    }

    private void buildRecords(LinearLayout root){
        title(root,"运行记录","查看最近一次执行的状态，或保存诊断记录。");
        LinearLayout summary=card(root);
        recordStatus=text("尚未开始",18,cText,true);summary.addView(recordStatus);
        gap(summary,6);
        recordMessage=body("");summary.addView(recordMessage);

        LinearLayout tools=card(root);
        tools.addView(section("排查与支持"));gap(tools,4);
        tools.addView(body("遇到问题时先保存记录；不要连续重复启动恢复。"));
        LinearLayout actions=new LinearLayout(this);gap(tools,14);
        actions.addView(button("保存诊断记录",v->export(),false),new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout.LayoutParams clp=new LinearLayout.LayoutParams(0,-2,1);clp.leftMargin=dp(10);
        actions.addView(button("复制记录",v->copyLog(),false),clp);
        tools.addView(actions);

        details=text("",14,cBrand,true);
        details.setPadding(dp(2),dp(14),dp(2),dp(14));details.setMinHeight(dp(48));
        details.setBackground(ripple(cRipple,null));
        details.setOnClickListener(v->{expanded=!expanded;refresh();});
        tools.addView(details,new LinearLayout.LayoutParams(-1,-2));
        logText=text("",11,cMuted,false);
        logText.setTypeface(Typeface.MONOSPACE);
        logText.setTextIsSelectable(true);
        logText.setPadding(0,dp(4),0,dp(4));
        logText.setVisibility(View.GONE);
        tools.addView(logText,new LinearLayout.LayoutParams(-1,-2));

        LinearLayout management=card(root);
        management.addView(section("KernelSU 状态未更新？"));gap(management,4);
        management.addView(body("刷新只会重新打开管理器，不会再次执行 Root 恢复。"));
        full(management,button("刷新 KernelSU",v->engine.refreshManagerOnly(),false));
    }

    private void buildHelp(LinearLayout root){
        title(root,"使用帮助","每次开机，一次清晰的恢复流程。");

        LinearLayout start=card(root);
        start.addView(section("第一次使用"));gap(start,12);
        steps(start,new String[]{
                "安装官方 Shizuku 与 KernelSU v3.3.0。",
                "在 Shizuku 中通过无线调试配对并启动。",
                "返回 ANB Root，允许使用 Shizuku。",
                "检查通过后，完成首次准备并开始恢复。",
                "如提示授权，在 KernelSU 中允许 ANB Root，然后返回验证。"});

        LinearLayout reboot=card(root);
        reboot.addView(section("每次重启之后"));gap(reboot,12);
        steps(reboot,new String[]{
                "解锁手机，等待开机约 90 秒。",
                "重新启动 Shizuku。",
                "回到 ANB Root 点击恢复，看到“Root 已验证”才算成功。"});
        gap(reboot,10);
        reboot.addView(body("Shizuku 和临时 Root 都不会跨重启保持运行。"));

        LinearLayout faq=card(root);
        faq.addView(section("常见问题"));gap(faq,2);
        helpRow(faq,"Shizuku 未运行或无法授权",()->dialog("重新连接 Shizuku","先在 Shizuku 中通过无线调试启动服务，再返回本 App。若曾拒绝授权，可在 Shizuku 的应用管理中重新允许 ANB Root。\n\n关闭开发者选项或撤销配对后，需要先在手机上重新开启调试、配对并启动。","打开 Shizuku",()->openPackage(RecoveryEngine.SHIZUKU,"https://shizuku.rikka.app/download/")));
        divider(faq,0);
        helpRow(faq,"KernelSU 显示“未安装”",()->dialog("刷新管理器状态","恢复完成后，旧的管理器进程可能仍显示过时状态。刷新会停止并重新打开 KernelSU，不会再次提权。","刷新 KernelSU",()->engine.refreshManagerOnly()));
        divider(faq,0);
        helpRow(faq,"恢复超时，或手机重启了",()->dialog("先确认状态","不要重复运行同一次恢复。先保存诊断记录；若本次状态无法确认，请完整重启手机，再启动 Shizuku 后重试。","查看记录",()->showPage(PAGE_RECORDS)));
        divider(faq,0);
        helpRow(faq,"升级系统后还能使用吗？",()->dialog("系统版本支持","本版本仅适配 ANB-AN00 的 MagicOS 9.0.0.230。升级后若内核不匹配，App 会关闭恢复入口；需要重新确认适配，不能套用旧版文件。",null,null));
        divider(faq,0);
        helpRow(faq,"卸载 App 会立即解除 Root 吗？",()->dialog("临时 Root 的有效期","卸载 App 不会立即卸载当前已加载的内核模块。完整重启会结束本次临时 Root；之后不再启动恢复即可。",null,null));
        divider(faq,0);
        helpRow(faq,"允许调试工具使用 Root 是什么？",()->dialog("调试工具的 Root 权限","默认关闭。开启后，ADB、Shizuku 等以 shell 身份运行的工具可直接获准使用 Root，普通应用仍由 KernelSU 单独授权。\n\n该选择会被记住，只在恢复加载模块时设置。已有 Root 时不能即时切换；请完整重启手机，在恢复前修改。关闭选项仅取消此自动授权，不会撤销 KernelSU 中已有的其他授权。",null,null));

        LinearLayout privacy=card(root);
        privacy.addView(section("本地运行，由你掌控"));gap(privacy,8);
        privacy.addView(body("App 不申请联网权限，不上传运行记录。诊断文件只会保存到你选择的位置。日志可能含设备版本和运行信息，分享前可以先查看。"));
        full(privacy,button("关于与开源许可",v->showAbout(),false));
    }

    // ---------------------------------------------------------------- rendering

    private void refresh(){
        if(headline==null||primary==null)return;
        Readiness.Gate gate=engine.gate();
        boolean busy=engine.busy.get(),checking=engine.checking,setup=setup();
        StatusModel.Snapshot snapshot=StatusModel.of(gate,engine.phase.name(),busy,checking,setup,engine.modulePresent);

        set(badge,badgeText(snapshot.badge));
        set(headline,headlineText(snapshot.headline));
        set(subtitle,bodyText(snapshot.headline,busy));
        set(heroNote,engine.phase==RecoveryEngine.Phase.SUCCESS
                ?"ANB-AN00 · 本次开机有效，重启后需重新恢复"
                :"ANB-AN00 · MagicOS 9.0.0.230");

        boolean compatible=engine.compatible();
        set(deviceDetail,compatible?"已匹配":"不受支持");
        paintDot(statusDots[0],compatible?cBrand:cWarn);

        String shizuku="待安装";int shizukuColor=cWarn;boolean shizukuReady=false;
        if(engine.installed(RecoveryEngine.SHIZUKU)){
            try{
                if(!Shizuku.pingBinder())shizuku="待启动";
                else if(Shizuku.getVersion()<13)shizuku="待更新";
                else if(Shizuku.checkSelfPermission()!=PackageManager.PERMISSION_GRANTED)shizuku="待授权";
                else if(Shizuku.getUid()!=2000)shizuku="需无线调试启动";
                else{shizuku="已连接";shizukuColor=cBrand;shizukuReady=true;}
            }catch(Exception e){shizuku="连接已断开";}
        }
        set(shizukuDetail,shizuku);shizukuDetail.setTextColor(shizukuColor);
        paintDot(statusDots[1],shizukuReady?cBrand:cWarn);

        boolean ksu=engine.installed(RecoveryEngine.KSU);
        set(ksuDetail,ksu?"已安装":"待安装");ksuDetail.setTextColor(ksu?cBrand:cWarn);
        paintDot(statusDots[2],ksu?cBrand:cWarn);

        set(environmentDetail,checking?"检测中…":engine.environmentReady?"已通过":"待检测");
        environmentDetail.setTextColor(engine.environmentReady?cBrand:cMuted);
        paintDot(statusDots[3],engine.environmentReady?cBrand:cMuted);

        int ready=(compatible?1:0)+(shizukuReady?1:0)+(ksu?1:0)+(engine.environmentReady?1:0);
        set(checkCount,ready+"/4");

        updateRowDescriptions();
        updatingSwitch=true;
        allowShellSwitch.setChecked(engine.allowShellSelected());
        updatingSwitch=false;
        boolean switchEnabled=!busy&&!checking&&!engine.modulePresent;
        if(allowShellSwitch.isEnabled()!=switchEnabled)allowShellSwitch.setEnabled(switchEnabled);
        set(allowShellNote,engine.modulePresent
                ?"本次 Root 已加载，不能即时更改。请完整重启后，在恢复前设置。"
                :busy||checking?"正在处理，本次恢复选项已锁定。"
                :"开启后 ADB、Shizuku 等调试工具可直接使用 Root；日常建议关闭。");

        boolean failed=engine.phase==RecoveryEngine.Phase.FAILED;
        errorCard.setVisibility(failed?View.VISIBLE:View.GONE);
        if(failed){
            set(errorDetail,engine.message);
            set(errorGuidance,guidanceText(engine.message));
        }

        boolean showProgress=busy||engine.phase==RecoveryEngine.Phase.SUCCESS
                ||engine.phase==RecoveryEngine.Phase.WAIT_AUTH||failed;
        progressCard.setVisibility(showProgress?View.VISIBLE:View.GONE);
        updateProgress();

        set(recordStatus,engine.title);
        set(recordMessage,engine.message);

        set(primary,primaryLabel(snapshot.action,busy,checking));
        enable(primary,!busy&&!checking&&snapshot.action!=StatusModel.Action.NONE);
        set(actionNote,actionNoteText(busy,checking,gate));

        String logDump=engine.logs();
        int logLines=logDump.isEmpty()?0:logDump.split("\n").length;
        set(details,expanded?"收起详细记录"
                :logLines==0?"展开详细记录":"展开详细记录 · "+logLines+" 行");
        logText.setVisibility(expanded?View.VISIBLE:View.GONE);
        if(expanded){
            String current=logDump;
            if(!current.equals(shownLog)){
                set(logText,current.isEmpty()?"暂无记录。完成检查或恢复后，会显示在这里。":current);
                shownLog=current;
            }
        }

        if(busy){
            if(!getWindow().getDecorView().getKeepScreenOn())
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if(busySince==0)busySince=SystemClock.elapsedRealtime();
            if(!ticking){
                ticking=true;
                main.removeCallbacks(busyTick);main.post(busyTick);
            }
        }else{
            busySince=0;
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            main.removeCallbacks(busyTick);
            set(elapsedText,"");
        }
        lastPhase=engine.phase;
    }

    private void updateRowDescriptions(){
        describeRow(0,"设备与系统",String.valueOf(deviceDetail.getText()),"点按查看支持范围");
        describeRow(1,"Shizuku",String.valueOf(shizukuDetail.getText()),"点按打开 Shizuku");
        describeRow(2,"KernelSU",String.valueOf(ksuDetail.getText()),"点按打开 KernelSU");
        describeRow(3,"执行环境",String.valueOf(environmentDetail.getText()),"点按重新检测");
    }

    private void describeRow(int index,String label,String value,String hint){
        View row=statusRows[index];
        if(row==null)return;
        row.setContentDescription(label+"："+value+"，"+hint);
    }

    private void updateProgress(){
        boolean busy=engine.busy.get();
        boolean failed=engine.phase==RecoveryEngine.Phase.FAILED;
        boolean waiting=engine.phase==RecoveryEngine.Phase.WAIT_AUTH;
        int step=Math.max(0,Math.min(5,engine.step));
        for(int i=0;i<5;i++){
            boolean done=step>i;
            boolean active=step==i&&!done;
            String mark=done?"✓":active&&(busy||waiting||failed)?"●":"○";
            set(stageMarks[i],mark);
            stageMarks[i].setTextColor(done?cBrand:active&&(busy||waiting||failed)?cBrand:cMuted);
            String state=done?"完成"
                    :active&&busy?"进行中"
                    :active&&waiting?"待授权"
                    :active&&failed?"已中断"
                    :"等待";
            set(stageStates[i],state);
            stageStates[i].setTextColor(done?cBrand:active&&(busy||waiting||failed)?cWarn:cMuted);
        }
        if(busy){
            long seconds=busySince==0?0:Math.max(0,(SystemClock.elapsedRealtime()-busySince)/1000);
            set(elapsedText,String.format(java.util.Locale.CHINA,"已用 %d:%02d",seconds/60,seconds%60));
            if(step!=lastAnnouncedStep){
                lastAnnouncedStep=step;
                if(progressCard.getVisibility()==View.VISIBLE)
                    progressCard.announceForAccessibility(STAGE_LABELS[Math.min(4,step)]+"，进行中");
            }
        }else{
            set(elapsedText,"");
            lastAnnouncedStep=-1;
        }
    }

    private void set(TextView view,String value){
        if(view==null)return;
        String text=value==null?"":value;
        if(!text.equals(String.valueOf(view.getText())))view.setText(text);
    }

    private void enable(View view,boolean enabled){
        if(view==null)return;
        if(view.isEnabled()==enabled)return;
        view.setEnabled(enabled);view.setAlpha(enabled?1f:.45f);
    }

    private String badgeText(StatusModel.Badge badge){
        switch(badge){
            case UNSUPPORTED:return "不受支持";
            case FIRST_RUN:return "首次准备";
            case CHECKING:return "自动检查";
            case WORKING:return "进行中";
            case WAIT_AUTH:return "等待授权";
            case VERIFIED:return "已验证";
            case ATTENTION:return "需要处理";
            default:return "本机状态";
        }
    }

    private String headlineText(StatusModel.Headline headline){
        switch(headline){
            case UNSUPPORTED:return "当前版本不受支持";
            case INSTALL_SHIZUKU:return "先安装 Shizuku";
            case INSTALL_KSU:return "先安装 KernelSU";
            case START_SHIZUKU:return "启动 Shizuku";
            case UPDATE_SHIZUKU:return "更新 Shizuku";
            case GRANT_SHIZUKU:return "授予 Shizuku 权限";
            case SHELL_MODE:return "请用无线调试启动";
            case CHECK_ENV:return engine.checking?"正在检查环境":"检查执行环境";
            case FIRST_SETUP:return "准备条件已满足";
            case WORKING:return engine.title;
            case WAIT_AUTH:return "还差一步授权";
            case SUCCESS:return "Root 已就绪";
            case FAILED:return "本次尚未完成";
            default:return setup()?"准备就绪":"准备条件已满足";
        }
    }

    private String bodyText(StatusModel.Headline headline,boolean busy){
        switch(headline){
            case UNSUPPORTED:return "只支持 ANB-AN00 指定固件；不要更换其他机型的载荷。";
            case INSTALL_SHIZUKU:return "它负责提供本机执行权限。请从官方页面安装后返回。";
            case INSTALL_KSU:return "检测到 Shizuku 后，还需要 KernelSU 管理器管理恢复后的 Root 授权。";
            case START_SHIZUKU:return "打开 Shizuku，通过无线调试配对并启动服务，再返回这里。每次重启后需重新启动。";
            case UPDATE_SHIZUKU:return "执行服务需要 Shizuku 13 或更新版本。";
            case GRANT_SHIZUKU:return "允许 ANB Root 使用 Shizuku。拒绝后仍可在 Shizuku 的应用管理中重新开启。";
            case SHELL_MODE:return "当前服务以 Root 启动。请在 Shizuku 中停止服务，再通过无线调试启动，以确认重启后可用的执行方式。";
            case CHECK_ENV:return busy?"正在确认 Shizuku 提供的执行环境。":"已连接并授权。再检查一次实际执行身份与进程限制。";
            case FIRST_SETUP:return "已检测两个应用、Shizuku 授权和执行环境。确认后开放恢复入口。";
            case WORKING:return engine.phase==RecoveryEngine.Phase.RUNNING
                    ?"请保持耐心，通常需要 1–3 分钟。不要重复启动；进度会持续显示。":engine.message;
            case WAIT_AUTH:return "请在 KernelSU 中允许 ANB Root 使用 Root，然后返回这里验证。";
            case SUCCESS:return engine.message.contains("刷新未完成")
                    ?"Root 已验证，管理器状态待更新。可到记录页刷新 KernelSU。"
                    :"本次开机的 Root 已验证。你可以在 KernelSU 中管理权限。";
            case FAILED:return "已停止本次执行。请按下方说明处理，不要重复启动。";
            default:return setup()?"所有条件已通过，点击下方按钮恢复本次权限。":"已检测两个应用、Shizuku 授权和执行环境。";
        }
    }

    private String guidanceText(String message){
        switch(FailureText.classify(message)){
            case BOOT_WAIT:return "系统仍在启动。请解锁并等到桌面稳定（约 90 秒）后，再点一次。";
            case CONCURRENT_RUN:return "已有一次恢复在运行。请等待它结束，不要并发启动。";
            case REBOOTED:return "手机在恢复过程中重启了。解锁后重新启动 Shizuku，再返回检查。";
            case TIMEOUT:return "等待超时，尚未确认完成。请先保存诊断记录，不要连续重试；必要时完整重启后再试。";
            case PERMISSION:return "权限未授予或已撤销。请在 Shizuku 的应用管理中重新允许 ANB Root。";
            case SHIZUKU:return "执行服务不可用。请重新启动 Shizuku（无线调试）后，再检查一次。";
            case COMPATIBILITY:return "当前设备或系统版本不在支持范围内。不要套用其他机型的文件。";
            case PAYLOAD:return "内置载荷校验失败。请重新安装本版本，不要使用被修改过的文件。";
            case STORAGE:return "存储不可用或无法写入。请解锁手机、确认空间后重试。";
            case PREPARE:return "前置条件未完成。请按首页提示逐项检查后再试。";
            default:return "暂时无法确认具体原因。请保存诊断记录，并按帮助页的说明处理。";
        }
    }

    private String primaryLabel(StatusModel.Action action,boolean busy,boolean checking){
        if(busy)return "正在处理，请稍候";
        if(checking)return "正在检查…";
        switch(action){
            case SHIZUKU_DOWNLOAD:return "获取 Shizuku";
            case KSU_DOWNLOAD:return "获取 KernelSU";
            case OPEN_SHIZUKU:return "打开 Shizuku";
            case REQUEST_SHIZUKU:return "授予 Shizuku 权限";
            case CHECK_ENVIRONMENT:return "检测执行环境";
            case FINISH_SETUP:return "完成首次准备";
            case RESTORE:return "恢复 Root";
            case VERIFY:return engine.phase==RecoveryEngine.Phase.WAIT_AUTH?"验证 Root 授权":"验证当前 Root";
            case SAVE_DIAGNOSTICS:return "保存诊断记录";
            default:return "暂不可用";
        }
    }

    private String actionNoteText(boolean busy,boolean checking,Readiness.Gate gate){
        if(busy||checking)return "前台通知会保留本次进度";
        if(gate==Readiness.Gate.READY)return "仅在本次开机有效";
        return "按提示完成准备后即可继续";
    }

    private void showPage(int index){
        page=Math.max(0,Math.min(2,index));
        for(int i=0;i<3;i++){
            scrolls[i].setVisibility(i==page?View.VISIBLE:View.GONE);
            boolean selected=i==page;
            tabLabels[i].setTextColor(selected?cBrand:cMuted);
            tabIcons[i].setColorFilter(selected?cBrand:cMuted);
            tabItems[i].setSelected(selected);
            tabItems[i].setContentDescription(tabLabels[i].getText()+"，第 "+(i+1)+" 项，共 3 项，"
                    +(selected?"已选中":"未选中"));
        }
        actionBar.setVisibility(page==PAGE_HOME?View.VISIBLE:View.GONE);
        refresh();
    }

    // ---------------------------------------------------------------- actions

    private void autoCheck(){
        if(engine.gate()==Readiness.Gate.ENVIRONMENT&&!engine.checking)engine.checkEnvironment();
    }

    private void refreshEnvironment(){
        engine.invalidate();
        if(engine.gate()==Readiness.Gate.ENVIRONMENT)engine.checkEnvironment();
        else refresh();
    }

    private void primaryAction(){
        long now=SystemClock.elapsedRealtime();
        if(now-lastPrimaryTap<PRIMARY_DEBOUNCE_MS)return;
        lastPrimaryTap=now;

        Readiness.Gate gate=engine.gate();
        StatusModel.Snapshot snapshot=StatusModel.of(gate,engine.phase.name(),engine.busy.get(),
                engine.checking,setup(),engine.modulePresent);
        switch(snapshot.action){
            case SHIZUKU_DOWNLOAD:
                openPackage(RecoveryEngine.SHIZUKU,"https://shizuku.rikka.app/download/");return;
            case KSU_DOWNLOAD:
                openUri("https://github.com/tiann/KernelSU/releases/tag/v3.3.0");return;
            case OPEN_SHIZUKU:
                openPackage(RecoveryEngine.SHIZUKU,"https://shizuku.rikka.app/download/");return;
            case REQUEST_SHIZUKU:
                requestShizuku();return;
            case CHECK_ENVIRONMENT:
                refreshEnvironment();return;
            case FINISH_SETUP:
                if(getSharedPreferences("setup",0).edit().putBoolean("completed",true).commit())
                    engine.record("首次准备完成：依赖、授权与执行环境已确认。");
                refresh();return;
            case SAVE_DIAGNOSTICS:
                export();return;
            case RESTORE:
            case VERIFY:
                startRecovery();return;
            default:
                return;
        }
    }

    private void requestShizuku(){
        try{
            if(Shizuku.shouldShowRequestPermissionRationale()){
                new AlertDialog.Builder(this).setTitle("重新开启权限")
                        .setMessage("请在 Shizuku → 已授权应用中给 ANB Root 开启权限，然后返回。")
                        .setPositiveButton("打开 Shizuku",(d,w)->openPackage(RecoveryEngine.SHIZUKU,
                                "https://shizuku.rikka.app/download/"))
                        .setNegativeButton("稍后",null).show();
            }else Shizuku.requestPermission(REQ_SHIZUKU);
        }catch(Exception e){toast("Shizuku 连接已变化，请重新检查");}
    }

    private void startRecovery(){
        if(engine.busy.get()||engine.checking){toast("正在处理，请稍候");refresh();return;}
        if(!setup()){toast("请先完成首次准备");refresh();return;}
        if(engine.gate()!=Readiness.Gate.READY){toast("当前状态已变化，请按首页提示重新准备");refresh();return;}
        if(Build.VERSION.SDK_INT>=33
                &&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED
                &&!getSharedPreferences("ui",0).getBoolean("notif_asked",false)){
            getSharedPreferences("ui",0).edit().putBoolean("notif_asked",true).apply();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQ_NOTIFICATIONS);
            return;
        }
        launchService();
    }

    private void launchService(){
        busySince=SystemClock.elapsedRealtime();
        startForegroundService(new Intent(this,RecoveryService.class));
        main.postDelayed(this::refresh,250);
        main.postDelayed(this::refresh,1200);
    }

    @Override public void onRequestPermissionsResult(int request,String[] names,int[] grants){
        super.onRequestPermissionsResult(request,names,grants);
        if(request!=REQ_NOTIFICATIONS)return;
        if(engine.gate()==Readiness.Gate.READY)launchService();
        else{toast("状态已变化，请重新检查");refresh();}
    }

    private void openPackage(String pkg,String fallback){
        Intent launch=getPackageManager().getLaunchIntentForPackage(pkg);
        if(launch!=null)try{startActivity(launch);return;}catch(Exception ignored){}
        openUri(fallback);
    }

    private void openUri(String url){
        try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}
        catch(Exception e){toast("没有可打开网页的应用");}
    }

    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}

    private boolean setup(){return getSharedPreferences("setup",0).getBoolean("completed",false);}

    // ---------------------------------------------------------------- dialogs, export, about

    private void dialog(String heading,String message,String action,Runnable task){
        AlertDialog.Builder builder=new AlertDialog.Builder(this).setTitle(heading).setMessage(message);
        if(action!=null)builder.setPositiveButton(action,(d,w)->task.run()).setNegativeButton("关闭",null);
        else builder.setPositiveButton("知道了",null);
        builder.show();
    }

    private void export(){
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE,"ANB-Root-诊断-"
                +new java.text.SimpleDateFormat("yyyyMMdd-HHmmss",java.util.Locale.US).format(new java.util.Date())+".txt");
        try{startActivityForResult(intent,REQ_EXPORT);}catch(Exception e){toast("当前无法打开保存页面");}
    }

    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=REQ_EXPORT||result!=RESULT_OK||data==null||data.getData()==null)return;
        try(OutputStream out=getContentResolver().openOutputStream(data.getData())){
            if(out==null)throw new IOException();
            out.write(report().getBytes("UTF-8"));toast("诊断记录已保存");
        }catch(Exception e){toast("保存失败，请重试");}
    }

    private String report(){
        String stamp=new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",java.util.Locale.CHINA)
                .format(new java.util.Date());
        return Diagnostics.build(versionName(),versionCode(),android.os.Build.MODEL,RecoveryEngine.KERNEL,
                engine.phase.name(),"未知",stamp,engine.logs());
    }

    private void copyLog(){
        try{
            ClipboardManager clipboard=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("ANB Root 记录",engine.logs()));
            toast("记录已复制");
        }catch(Exception e){toast("复制失败");}
    }

    private String versionName(){
        try{return getPackageManager().getPackageInfo(getPackageName(),0).versionName;}
        catch(Exception e){return "2.3.0";}
    }

    private int versionCode(){
        try{return getPackageManager().getPackageInfo(getPackageName(),0).versionCode;}
        catch(Exception e){return 0;}
    }

    private void showAbout(){
        new AlertDialog.Builder(this).setTitle("ANB Root "+versionName())
                .setMessage("荣耀 80 Pro 直屏版 · 临时 Root 助手\n适配 ANB-AN00 / MagicOS 9.0.0.230\n\n"
                        +"通过 Shizuku 提供执行环境，使用已验证的 GhostLock-H80GT 载荷与 KernelSU 管理权限。\n\n"
                        +"本版为界面与可访问性更新，恢复链与载荷未改动。")
                .setPositiveButton("关闭",null)
                .setNeutralButton("开源许可",(d,w)->showLicenses()).show();
    }

    private void showLicenses(){
        try{
            String content;
            try(InputStream in=getAssets().open("THIRD_PARTY_NOTICES.txt");
                ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] buffer=new byte[4096];int read;
                while((read=in.read(buffer))!=-1)out.write(buffer,0,read);
                content=new String(out.toByteArray(),"UTF-8");
            }
            TextView view=text(content,12,cMuted,false);
            view.setTextIsSelectable(true);view.setPadding(dp(20),dp(12),dp(20),dp(12));
            ScrollView scroll=new ScrollView(this);scroll.addView(view);
            new AlertDialog.Builder(this).setTitle("开源组件与许可").setView(scroll)
                    .setPositiveButton("关闭",null).show();
        }catch(Exception e){toast("暂时无法显示许可文件");}
    }
}
