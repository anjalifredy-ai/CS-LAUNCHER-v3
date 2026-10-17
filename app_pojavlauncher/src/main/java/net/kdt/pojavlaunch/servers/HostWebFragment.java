package net.kdt.pojavlaunch.servers;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;

/** Opens Aternos / Exaroton / Minehut / playit.gg inside Remom. */
public class HostWebFragment extends Fragment {
    public static final String TAG = "HostWebFragment";
    public static final String ARG_URL = "url";
    public static final String ARG_TITLE = "title";

    public HostWebFragment() {
        super(R.layout.fragment_host_web);
    }

    public static HostWebFragment aternos() {
        return of("https://aternos.org/", "Aternos");
    }

    public static HostWebFragment exaroton() {
        return of("https://exaroton.com/", "Exaroton");
    }

    public static HostWebFragment minehut() {
        return of("https://minehut.com/", "Minehut");
    }

    /** Free tunnel so local phone server is reachable by distant friends. */
    public static HostWebFragment playit() {
        return of("https://playit.gg/", "playit.gg tunnel");
    }

    private static HostWebFragment of(String url, String title) {
        HostWebFragment f = new HostWebFragment();
        Bundle b = new Bundle();
        b.putString(ARG_URL, url);
        b.putString(ARG_TITLE, title);
        f.setArguments(b);
        return f;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        String url = "https://playit.gg/";
        String title = "playit.gg";
        Bundle args = getArguments();
        if (args != null) {
            if (args.getString(ARG_URL) != null) url = args.getString(ARG_URL);
            if (args.getString(ARG_TITLE) != null) title = args.getString(ARG_TITLE);
        }

        TextView titleTv = root.findViewById(R.id.host_web_title);
        if (titleTv != null) titleTv.setText(title + " · in Remom");

        View back = root.findViewById(R.id.host_web_back);
        if (back != null) back.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        View done = root.findViewById(R.id.host_web_done);
        if (done != null) done.setOnClickListener(v -> {
            Toast.makeText(requireContext(),
                    "Public address copy karke Host → Tunnel box me paste karo",
                    Toast.LENGTH_LONG).show();
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        ProgressBar bar = root.findViewById(R.id.host_web_progress);
        WebView web = root.findViewById(R.id.host_web_view);
        if (web == null) return;

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);

        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (bar != null) {
                    bar.setProgress(newProgress);
                    bar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
                }
            }
        });
        web.loadUrl(url);
    }
}
