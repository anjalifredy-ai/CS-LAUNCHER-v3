package net.kdt.pojavlaunch.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;

/**
 * CS Launcher V3 — In-Launcher WebView Browser for Ely.by.
 * Provides the same seamless browser experience as Microsoft / Mojang OAuth login,
 * allowing users to register an Ely.by account, verify email, or manage skins directly
 * inside the launcher.
 */
public class ElybyBrowserFragment extends Fragment {
    public static final String TAG = "ELYBY_BROWSER_FRAGMENT";

    private WebView mWebView;

    public ElybyBrowserFragment() {
        super(R.layout.fragment_elyby_browser);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mWebView = view.findViewById(R.id.elyby_browser_webview);

        View btnBack = view.findViewById(R.id.btn_elyby_browser_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (mWebView != null && mWebView.canGoBack()) {
                    mWebView.goBack();
                } else {
                    navigateBack();
                }
            });
        }

        View btnRefresh = view.findViewById(R.id.btn_elyby_browser_refresh);
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> {
                if (mWebView != null) mWebView.reload();
            });
        }

        if (mWebView != null) {
            WebSettings settings = mWebView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setLoadWithOverviewMode(true);
            settings.setUseWideViewPort(true);
            mWebView.setWebViewClient(new WebViewClient());
            mWebView.setWebChromeClient(new WebChromeClient());
            mWebView.loadUrl("https://ely.by/login");
        }
    }

    private void navigateBack() {
        Fragment parent = getParentFragment();
        if (parent instanceof MainMenuFragment) {
            ((MainMenuFragment) parent).clearRightPane();
        } else {
            Tools.swapFragment(requireActivity(), SelectAuthFragment.class, SelectAuthFragment.TAG, null);
        }
    }
}
