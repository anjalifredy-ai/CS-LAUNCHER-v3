package net.kdt.pojavlaunch.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;import androidx.annotation.Nullable;import androidx.fragment.app.Fragment;
import net.kdt.pojavlaunch.PojavApplication;import net.kdt.pojavlaunch.R;import net.kdt.pojavlaunch.Tools;import net.kdt.pojavlaunch.UiMotion;import net.kdt.pojavlaunch.csclient.CsClientCatalog;import net.kdt.pojavlaunch.csclient.CsClientSelection;
import java.util.List;

public class CsClientVersionsFragment extends Fragment {
 public static final String TAG="CS_CLIENT_VERSIONS";
 public CsClientVersionsFragment(){super(R.layout.fragment_cs_client_versions);}
 @Override public void onViewCreated(@NonNull View root,@Nullable Bundle state){LinearLayout box=root.findViewById(R.id.csc_versions_container);ProgressBar load=root.findViewById(R.id.csc_versions_loading);root.findViewById(R.id.csc_versions_back).setOnClickListener(v->requireActivity().getSupportFragmentManager().popBackStack());UiMotion.pressFeedback(root.findViewById(R.id.csc_versions_back));PojavApplication.sExecutorService.execute(()->{try{List<CsClientCatalog.VersionEntry> list=CsClientCatalog.fetchLatestByMinecraft();Tools.runOnUiThread(()->{if(!isAdded())return;load.setVisibility(View.GONE);long delay=0;for(CsClientCatalog.VersionEntry e:list){View card=makeCard(e);card.setAlpha(0f);card.setTranslationY(12f*getResources().getDisplayMetrics().density);box.addView(card);card.animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(260).start();delay+=32;}});}catch(Exception e){Tools.runOnUiThread(()->{load.setVisibility(View.GONE);Toast.makeText(requireContext(),"Could not load Blaze Client versions",Toast.LENGTH_LONG).show();});}});}
 private View makeCard(CsClientCatalog.VersionEntry e){LinearLayout card=new LinearLayout(requireContext());card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(16),dp(13),dp(16),dp(13));card.setBackgroundResource(R.drawable.bg_cs_client_transparent);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(92));lp.bottomMargin=dp(10);card.setLayoutParams(lp);TextView tag=t("MINECRAFT • FABRIC",9,0xFF22C9CB,true);TextView title=t(e.minecraft,20,0xFFFFFFFF,true);TextView sub=t("Blaze "+e.build+"  •  latest compatible build",10,0xFF999BA2,false);card.addView(tag);card.addView(title);card.addView(sub);card.setOnClickListener(v->{CsClientSelection.clear();Bundle b=new Bundle();b.putSerializable("version",e);Tools.swapFragment(requireActivity(),CsClientBuilderFragment.class,CsClientBuilderFragment.TAG,b);});UiMotion.pressFeedback(card);return card;}
 private TextView t(String s,int sp,int c,boolean bold){TextView v=new TextView(requireContext());v.setText(s);v.setTextSize(sp);v.setTextColor(c);if(bold)v.setTypeface(null,1);return v;}private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}