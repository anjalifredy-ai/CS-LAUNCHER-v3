package net.kdt.pojavlaunch.performance;

import android.content.Context;
import android.os.Process;

import net.kdt.pojavlaunch.Tools;

import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;

/** Cross-process lightweight marker preventing launcher work during an active game session. */
public final class GameSessionState {
    private GameSessionState(){}
    private static File marker(){return new File(Tools.DIR_DATA,"performance/game_session.json");}
    public static synchronized void markActive(Context context){try{File f=marker();File parent=f.getParentFile();if(parent!=null)parent.mkdirs();JSONObject o=new JSONObject();o.put("pid",Process.myPid());o.put("started",System.currentTimeMillis());o.put("process",context.getPackageName()+":game");File tmp=new File(parent,"game_session.tmp");Tools.write(tmp.getAbsolutePath(),o.toString());if(f.exists())f.delete();if(!tmp.renameTo(f)){org.apache.commons.io.FileUtils.copyFile(tmp,f);tmp.delete();}}catch(Throwable ignored){}}
    public static synchronized void clear(){try{File f=marker();if(!f.isFile())return;JSONObject o=new JSONObject(Tools.read(f.getAbsolutePath()));if(o.optInt("pid",-1)==Process.myPid())f.delete();}catch(Throwable ignored){}}
    public static boolean isGameActive(){try{File f=marker();if(!f.isFile())return false;JSONObject o=new JSONObject(Tools.read(f.getAbsolutePath()));int pid=o.optInt("pid",-1);long age=System.currentTimeMillis()-o.optLong("started",0);if(pid<=0||age<0||age>12L*60L*60L*1000L){f.delete();return false;}File cmd=new File("/proc/"+pid+"/cmdline");if(!cmd.isFile()){f.delete();return false;}ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(FileInputStream in=new FileInputStream(cmd)){byte[]b=new byte[128];int n;while((n=in.read(b))>0)bytes.write(b,0,n);}String process=new String(bytes.toByteArray(),StandardCharsets.UTF_8).replace("\u0000","");boolean active=process.endsWith(":game")||process.contains(":game");if(!active)f.delete();return active;}catch(Throwable ignored){return false;}}
}
