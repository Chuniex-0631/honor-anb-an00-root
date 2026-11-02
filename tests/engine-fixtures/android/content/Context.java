package android.content;
import java.io.*;import java.util.*;import android.content.pm.PackageManager;import android.content.res.AssetManager;
public class Context {
 private final File root;private final AssetManager assets;private final Map<String,SharedPreferences> prefs=new HashMap<>();
 public Context(File root,File assets){this.root=root;root.mkdirs();this.assets=new AssetManager(assets);}
 public Context getApplicationContext(){return this;} public PackageManager getPackageManager(){return new PackageManager();}
 public synchronized SharedPreferences getSharedPreferences(String name,int mode){return prefs.computeIfAbsent(name,n->new SharedPreferences());}
 public File getFilesDir(){File f=new File(root,"files");f.mkdirs();return f;}
 public File getExternalFilesDir(String ignored){File f=new File(root,"external");f.mkdirs();return f;}
 public AssetManager getAssets(){return assets;}
}