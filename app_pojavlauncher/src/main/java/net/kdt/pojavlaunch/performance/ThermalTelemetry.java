package net.kdt.pojavlaunch.performance;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.SystemClock;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Locale;

/** Opt-in measurement only. It never adjusts Minecraft or renderer settings. */
public final class ThermalTelemetry {
    private final Context app;private final ArrayDeque<String> samples=new ArrayDeque<>();private HandlerThread thread;private Handler handler;private PowerManager power;private volatile int status=-1;private int sampleCounter;private PowerManager.OnThermalStatusChangedListener listener;
    private final Runnable sampler=new Runnable(){@Override public void run(){sample();if(handler!=null)handler.postDelayed(this,5000L);}};
    public ThermalTelemetry(Context context){app=context.getApplicationContext();}
    public void start(){if(!LauncherPreferences.DEFAULT_PREF.getBoolean("thermalTelemetry",false)||thread!=null)return;power=(PowerManager)app.getSystemService(Context.POWER_SERVICE);thread=new HandlerThread("CS-Thermal-Telemetry",android.os.Process.THREAD_PRIORITY_BACKGROUND);thread.start();handler=new Handler(thread.getLooper());if(Build.VERSION.SDK_INT>=29&&power!=null){listener=s->status=s;try{power.addThermalStatusListener(listener);}catch(Throwable ignored){}}handler.post(sampler);}
    public void stop(){Handler h=handler;if(h!=null)h.removeCallbacksAndMessages(null);if(Build.VERSION.SDK_INT>=29&&power!=null&&listener!=null)try{power.removeThermalStatusListener(listener);}catch(Throwable ignored){}handler=null;if(thread!=null){thread.quitSafely();thread=null;}writeSnapshot(true);}
    private void sample(){float headroom=Float.NaN;if(Build.VERSION.SDK_INT>=30&&power!=null)try{headroom=power.getThermalHeadroom(10);}catch(Throwable ignored){}String line=String.format(Locale.US,"%d,%d,%.4f,%s,%s,%b",SystemClock.elapsedRealtime(),status,headroom,cpuFrequencies(),gpuFrequency(),LauncherPreferences.PREF_SUSTAINED_PERFORMANCE);synchronized(samples){if(samples.size()>=720)samples.removeFirst();samples.addLast(line);}if(++sampleCounter%12==0)writeSnapshot(false);}
    private String cpuFrequencies(){StringBuilder b=new StringBuilder();int cores=Runtime.getRuntime().availableProcessors();for(int i=0;i<cores;i++){if(i>0)b.append('|');b.append(readNumber(new File("/sys/devices/system/cpu/cpu"+i+"/cpufreq/scaling_cur_freq")));}return b.toString();}
    private String gpuFrequency(){String[]p={"/sys/class/kgsl/kgsl-3d0/gpuclk","/sys/class/devfreq/gpufreq/cur_freq","/sys/class/devfreq/13000000.mali/cur_freq"};for(String x:p){String v=readNumber(new File(x));if(!"na".equals(v))return v;}return"na";}
    private String readNumber(File f){try{if(!f.isFile())return"na";String v=Tools.read(f.getAbsolutePath()).trim();return v.matches("[0-9]+")?v:"na";}catch(Throwable ignored){return"na";}}
    private void writeSnapshot(boolean clear){try{StringBuilder b=new StringBuilder("elapsed_ms,thermal_status,headroom_10s,cpu_freq_khz,gpu_freq,sustained_mode\n");synchronized(samples){for(String s:samples)b.append(s).append('\n');if(clear)samples.clear();}File out=new File(Tools.DIR_DATA,"performance/last_thermal.csv");File parent=out.getParentFile();if(parent!=null)parent.mkdirs();Tools.write(out.getAbsolutePath(),b.toString());}catch(Throwable ignored){}}
}
