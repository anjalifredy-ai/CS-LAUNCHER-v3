package net.kdt.pojavlaunch.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.UiMotion;
import net.kdt.pojavlaunch.csclient.CsClientCatalog;
import net.kdt.pojavlaunch.csclient.CsClientInstallView;
import net.kdt.pojavlaunch.csclient.CsClientInstaller;
import net.kdt.pojavlaunch.csclient.CsClientSelection;

import java.util.ArrayList;

public class CsClientBuilderFragment extends Fragment {
    public static final String TAG="CS_CLIENT_BUILDER";
    private CsClientCatalog.VersionEntry version;
    private LinearLayout chips;
    private TextView create,status;
    private CsClientInstallView installVisual;
    private ScrollView scroll;

    public CsClientBuilderFragment(){super(R.layout.fragment_cs_client_builder);}

    @Override public void onCreate(@Nullable Bundle state){
        super.onCreate(state);
        version=(CsClientCatalog.VersionEntry)requireArguments().getSerializable("version");
    }

    @Override public void onViewCreated(@NonNull View root,@Nullable Bundle state){
        chips=root.findViewById(R.id.csc_selection_chips);
        create=root.findViewById(R.id.csc_create_profile);
        status=root.findViewById(R.id.csc_install_status);
        installVisual=root.findViewById(R.id.csc_install_visual);
        scroll=root.findViewById(R.id.csc_builder_scroll);
        ((TextView)root.findViewById(R.id.csc_builder_eyebrow)).setText("CS CLIENT • MINECRAFT "+version.minecraft);
        ((TextView)root.findViewById(R.id.csc_builder_core)).setText("Blaze "+version.build+" • Fabric Loader + Fabric API");
        root.findViewById(R.id.csc_builder_back).setOnClickListener(v->requireActivity().getSupportFragmentManager().popBackStack());
        root.findViewById(R.id.csc_browse_mods).setOnClickListener(v->openPicker(false));
        root.findViewById(R.id.csc_browse_pack).setOnClickListener(v->openPicker(true));
        create.setOnClickListener(v->install());
        UiMotion.pressFeedback(root.findViewById(R.id.csc_builder_back),root.findViewById(R.id.csc_browse_mods),root.findViewById(R.id.csc_browse_pack),create);
        refresh();
        enter(root.findViewById(R.id.csc_builder_header),0,0,-8);
        enter(root.findViewById(R.id.csc_browse_mods),55,-10,0);
        enter(root.findViewById(R.id.csc_browse_pack),85,10,0);
        enter(create,125,0,8);
    }

    @Override public void onResume(){super.onResume();if(chips!=null)refresh();}
    @Override public void onDestroyView(){if(installVisual!=null)installVisual.stop();super.onDestroyView();}

    private void openPicker(boolean pack){
        Bundle b=new Bundle();b.putString("mc",version.minecraft);b.putBoolean("pack",pack);
        Tools.swapFragment(requireActivity(),CsClientPickerFragment.class,CsClientPickerFragment.TAG,b);
    }

    private void refresh(){
        chips.removeAllViews();
        addChip("✓  Fabric API  •  always included",null,0);
        int delay=25;
        if(CsClientSelection.modpack!=null){
            addChip("MODPACK  •  "+CsClientSelection.modpack.title,()->{CsClientSelection.modpack=null;refresh();},delay);delay+=25;
        }
        for(CsClientCatalog.ProjectEntry e:new ArrayList<>(CsClientSelection.mods)){
            addChip("MOD  •  "+e.title,()->{CsClientSelection.toggleMod(e);refresh();},delay);delay+=25;
        }
    }

    private void addChip(String text,@Nullable Runnable remove,long delay){
        TextView v=new TextView(requireContext());
        v.setText(text+(remove==null?"":"     ×"));v.setTextColor(0xFFFFFFFF);v.setTextSize(10);
        v.setGravity(android.view.Gravity.CENTER_VERTICAL);v.setPadding(dp(13),0,dp(13),0);
        v.setBackgroundResource(R.drawable.bg_cs_client_transparent);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(40));lp.bottomMargin=dp(7);v.setLayoutParams(lp);
        if(remove!=null)v.setOnClickListener(x->{v.animate().alpha(0f).translationX(dp(10)).setDuration(130).withEndAction(remove).start();});
        chips.addView(v);enter(v,delay,-7,0);
    }

    private void install(){
        create.setEnabled(false);create.animate().alpha(.42f).setDuration(150).start();
        installVisual.setVisibility(View.VISIBLE);installVisual.setAlpha(0f);installVisual.setScaleX(.97f);installVisual.setScaleY(.97f);
        installVisual.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(260).start();
        installVisual.setInstallProgress(1,"Preparing isolated profile");installVisual.start();
        scroll.post(()->scroll.smoothScrollTo(0,installVisual.getBottom()));
        CsClientInstaller.install(requireContext(),version,new ArrayList<>(CsClientSelection.mods),CsClientSelection.modpack,
                new CsClientInstaller.Callback(){
                    public void onStatus(String text,int percent){
                        if(!isAdded())return;status.setText(text);installVisual.setInstallProgress(percent,text);
                    }
                    public void onDone(boolean ok,String msg){
                        if(!isAdded())return;installVisual.setInstallProgress(ok?100:0,msg);
                        create.setEnabled(true);create.animate().alpha(1f).setDuration(150).start();
                        Toast.makeText(requireContext(),msg,Toast.LENGTH_LONG).show();
                        if(ok){CsClientSelection.clear();installVisual.postDelayed(()->{if(isAdded())Tools.backToMainMenu(requireActivity());},550);}
                    }
                });
    }

    private void enter(View v,long delay,float x,float y){
        v.setAlpha(0f);v.setTranslationX(dp((int)x));v.setTranslationY(dp((int)y));
        v.animate().alpha(1f).translationX(0).translationY(0).setStartDelay(delay).setDuration(240).start();
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
