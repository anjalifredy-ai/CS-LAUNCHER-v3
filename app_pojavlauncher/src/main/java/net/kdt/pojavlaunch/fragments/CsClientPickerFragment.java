package net.kdt.pojavlaunch.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.LruCache;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.PojavApplication;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.UiMotion;
import net.kdt.pojavlaunch.csclient.CsClientCatalog;
import net.kdt.pojavlaunch.csclient.CsClientSelection;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class CsClientPickerFragment extends Fragment {
    public static final String TAG="CS_CLIENT_PICKER";
    private static final LruCache<String,Bitmap> ICONS=new LruCache<String,Bitmap>(8*1024){
        @Override protected int sizeOf(String key,Bitmap value){return Math.max(1,value.getByteCount()/1024);}
    };
    private String mc;private boolean pack;private LinearLayout list;private ProgressBar loading;private EditText search;
    private final List<RowBinding> bindings=new ArrayList<>();

    public CsClientPickerFragment(){super(R.layout.fragment_cs_client_picker);}
    @Override public void onCreate(@Nullable Bundle state){super.onCreate(state);mc=requireArguments().getString("mc");pack=requireArguments().getBoolean("pack");}

    @Override public void onViewCreated(@NonNull View root,@Nullable Bundle state){
        list=root.findViewById(R.id.csc_picker_list);loading=root.findViewById(R.id.csc_picker_loading);search=root.findViewById(R.id.csc_picker_search);
        ((TextView)root.findViewById(R.id.csc_picker_title)).setText(pack?"Choose one modpack":"Choose mods");
        ((TextView)root.findViewById(R.id.csc_picker_subtitle)).setText("Minecraft "+mc+" • Fabric compatible only");
        root.findViewById(R.id.csc_picker_back).setOnClickListener(v->back());root.findViewById(R.id.csc_picker_done).setOnClickListener(v->back());
        root.findViewById(R.id.csc_picker_search_go).setOnClickListener(v->load(search.getText().toString()));
        search.setOnEditorActionListener((v,a,e)->{load(search.getText().toString());return true;});
        UiMotion.pressFeedback(root.findViewById(R.id.csc_picker_back),root.findViewById(R.id.csc_picker_done),root.findViewById(R.id.csc_picker_search_go));
        LinearLayout page=(LinearLayout)root;enter(page.getChildAt(0),0,-8);enter(page.getChildAt(1),55,-6);load("");
    }

    private void load(String query){
        loading.setVisibility(View.VISIBLE);list.removeAllViews();bindings.clear();
        PojavApplication.sExecutorService.execute(()->{
            try{
                List<CsClientCatalog.ProjectEntry> results=CsClientCatalog.search(query,mc,pack);
                Tools.runOnUiThread(()->{
                    if(!isAdded())return;loading.setVisibility(View.GONE);long delay=0;
                    for(CsClientCatalog.ProjectEntry entry:results){
                        RowBinding binding=row(entry);bindings.add(binding);list.addView(binding.root);
                        binding.root.setAlpha(0f);binding.root.setTranslationY(dp(8));
                        binding.root.animate().alpha(1f).translationY(0).setStartDelay(delay).setDuration(210).start();delay+=22;
                    }
                });
            }catch(Exception ignored){Tools.runOnUiThread(()->{if(isAdded()){loading.setVisibility(View.GONE);Toast.makeText(requireContext(),"Search failed",Toast.LENGTH_SHORT).show();}});}
        });
    }

    private RowBinding row(CsClientCatalog.ProjectEntry entry){
        LinearLayout root=new LinearLayout(requireContext());root.setOrientation(LinearLayout.HORIZONTAL);root.setGravity(Gravity.CENTER_VERTICAL);
        root.setPadding(dp(10),dp(8),dp(10),dp(8));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(76));lp.bottomMargin=dp(8);root.setLayoutParams(lp);

        ImageView icon=new ImageView(requireContext());icon.setScaleType(ImageView.ScaleType.CENTER_CROP);icon.setClipToOutline(true);icon.setBackgroundResource(R.drawable.bg_csc_icon);
        icon.setPadding(dp(3),dp(3),dp(3),dp(3));root.addView(icon,new LinearLayout.LayoutParams(dp(50),dp(50)));loadIcon(icon,entry.iconUrl,entry.modpack);

        LinearLayout textBox=new LinearLayout(requireContext());textBox.setOrientation(LinearLayout.VERTICAL);textBox.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams textLp=new LinearLayout.LayoutParams(0,-1,1);textLp.leftMargin=dp(11);textLp.rightMargin=dp(8);root.addView(textBox,textLp);
        TextView title=new TextView(requireContext());title.setTextColor(0xffffffff);title.setTextSize(12);title.setTypeface(null,1);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);title.setIncludeFontPadding(false);
        TextView desc=new TextView(requireContext());desc.setText(entry.description);desc.setTextColor(0xff898b92);desc.setTextSize(9);desc.setMaxLines(2);desc.setEllipsize(android.text.TextUtils.TruncateAt.END);desc.setIncludeFontPadding(false);desc.setLineSpacing(0,1.05f);
        textBox.addView(title);textBox.addView(desc);

        TextView check=new TextView(requireContext());check.setGravity(Gravity.CENTER);check.setTextSize(18);root.addView(check,new LinearLayout.LayoutParams(dp(38),dp(38)));
        RowBinding binding=new RowBinding(root,title,check,entry);binding.render(false);
        root.setOnClickListener(v->{
            if(pack)CsClientSelection.modpack=selected(entry)?null:entry;else CsClientSelection.toggleMod(entry);
            for(RowBinding b:bindings)b.render(true);
        });
        UiMotion.pressFeedback(root);return binding;
    }

    private void loadIcon(ImageView view,String url,boolean isPack){
        view.setImageResource(isPack?R.drawable.ic_cs_modpack:R.drawable.ic_cs_mods);
        if(url==null||url.isEmpty())return;view.setTag(url);Bitmap cached=ICONS.get(url);if(cached!=null){view.setPadding(0,0,0,0);view.setImageBitmap(cached);return;}
        PojavApplication.sExecutorService.execute(()->{
            HttpURLConnection connection=null;
            try{connection=(HttpURLConnection)new URL(url).openConnection();connection.setConnectTimeout(7000);connection.setReadTimeout(9000);connection.setRequestProperty("User-Agent","CS-Launcher/3");
                try(InputStream input=connection.getInputStream()){Bitmap bitmap=BitmapFactory.decodeStream(input);if(bitmap!=null){ICONS.put(url,bitmap);Tools.runOnUiThread(()->{if(isAdded()&&url.equals(view.getTag())){view.setPadding(0,0,0,0);view.setImageBitmap(bitmap);view.setAlpha(0f);view.animate().alpha(1f).setDuration(180).start();}});}}
            }catch(Exception ignored){}finally{if(connection!=null)connection.disconnect();}
        });
    }

    private final class RowBinding{
        final LinearLayout root;final TextView title,check;final CsClientCatalog.ProjectEntry entry;
        RowBinding(LinearLayout root,TextView title,TextView check,CsClientCatalog.ProjectEntry entry){this.root=root;this.title=title;this.check=check;this.entry=entry;}
        void render(boolean animate){boolean yes=selected(entry);root.setBackgroundResource(yes?R.drawable.bg_csc_manifest_selected:R.drawable.bg_csc_manifest_row);
            title.setText(entry.title);check.setText(yes?"✓":"+");check.setTextColor(yes?0xffffffff:0xffa0a2aa);
            if(animate){check.setScaleX(.75f);check.setScaleY(.75f);check.setAlpha(.4f);check.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(150).start();}}
    }

    private boolean selected(CsClientCatalog.ProjectEntry e){return pack?CsClientSelection.modpack!=null&&CsClientSelection.modpack.id.equals(e.id):CsClientSelection.contains(e.id);}
    private void back(){requireActivity().getSupportFragmentManager().popBackStack();}
    private void enter(View v,long delay,float y){v.setAlpha(0f);v.setTranslationY(dp((int)y));v.animate().alpha(1f).translationY(0).setStartDelay(delay).setDuration(240).start();}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
