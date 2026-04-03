package io.chordynaut.app;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.URLUtil;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private static final String APP_URL = "https://appassets.androidplatform.net/assets/www/index.html";
    private static final int REQ_OPEN_DOCUMENT = 4201;

    private WebView webView;
    private ValueCallback<Uri[]> pendingFileChooser;

    private static final class InstrumentWebView extends WebView {
        InstrumentWebView(Context context) {
            super(context);
        }

        private void keepTouchStreamOwned() {
            ViewParent parent = getParent();
            if (parent != null) {
                parent.requestDisallowInterceptTouchEvent(true);
            }
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            keepTouchStreamOwned();
            return super.dispatchTouchEvent(event);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            keepTouchStreamOwned();
            return super.onTouchEvent(event);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            keepTouchStreamOwned();
            super.onDraw(canvas);
        }

        @Override
        public boolean performLongClick() {
            return false;
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new InstrumentWebView(this);
        FrameLayout root = new FrameLayout(this);
        root.addView(
            webView,
            new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        );
        setContentView(root);
        root.setBackgroundColor(Color.parseColor("#0a0a0f"));

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);
        settings.setLoadsImagesAutomatically(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);
        settings.setTextZoom(100);

        webView.setBackgroundColor(Color.parseColor("#0a0a0f"));
        webView.setLongClickable(false);
        webView.setHapticFeedbackEnabled(false);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setOnLongClickListener(v -> true);
        webView.setOnTouchListener((v, event) -> {
            ViewParent parent = v.getParent();
            if (parent != null) {
                parent.requestDisallowInterceptTouchEvent(true);
            }
            forwardTouchFrameToPage(event, v.getWidth(), v.getHeight());
            return false;
        });

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
            .build();

        webView.setWebViewClient(new WebViewClientCompat() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                return assetLoader.shouldInterceptRequest(Uri.parse(url));
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("appassets.androidplatform.net".equals(uri.getHost())) {
                    return false;
                }
                if (!request.isForMainFrame()) {
                    return false;
                }
                openExternal(uri);
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                WebView webView,
                ValueCallback<Uri[]> filePathCallback,
                FileChooserParams fileChooserParams
            ) {
                if (pendingFileChooser != null) {
                    pendingFileChooser.onReceiveValue(null);
                }
                pendingFileChooser = filePathCallback;
                String[] mimeTypes = fileChooserParams != null
                    ? fileChooserParams.getAcceptTypes()
                    : new String[0];
                if (mimeTypes == null || mimeTypes.length == 0) {
                    mimeTypes = new String[]{"*/*"};
                }
                try {
                    Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    pick.addCategory(Intent.CATEGORY_OPENABLE);
                    pick.setType("*/*");
                    pick.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                    startActivityForResult(pick, REQ_OPEN_DOCUMENT);
                    return true;
                } catch (ActivityNotFoundException e) {
                    pendingFileChooser = null;
                    filePathCallback.onReceiveValue(null);
                    return false;
                }
            }
        });

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                try {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    request.setMimeType(mimeType);
                    request.addRequestHeader("User-Agent", userAgent);
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType));
                    DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    if (manager != null) {
                        manager.enqueue(request);
                    } else {
                        openExternal(Uri.parse(url));
                    }
                } catch (Exception e) {
                    openExternal(Uri.parse(url));
                }
            }
        });

        webView.loadUrl(APP_URL);
    }

    private void forwardTouchFrameToPage(MotionEvent event, int viewWidth, int viewHeight) {
        if (viewWidth <= 0 || viewHeight <= 0) return;

        int actionMasked = event.getActionMasked();
        int actionIndex = event.getActionIndex();
        JSONArray touches = new JSONArray();

        if (actionMasked != MotionEvent.ACTION_CANCEL) {
            for (int i = 0; i < event.getPointerCount(); i++) {
                if ((actionMasked == MotionEvent.ACTION_UP || actionMasked == MotionEvent.ACTION_POINTER_UP) && i == actionIndex) {
                    continue;
                }
                try {
                    JSONObject touch = new JSONObject();
                    touch.put("id", event.getPointerId(i));
                    touch.put("x", Math.max(0d, Math.min(1d, event.getX(i) / (double) viewWidth)));
                    touch.put("y", Math.max(0d, Math.min(1d, event.getY(i) / (double) viewHeight)));
                    touches.put(touch);
                } catch (JSONException ignored) {
                }
            }
        }

        try {
            JSONObject payload = new JSONObject();
            payload.put("touches", touches);
            final String script =
                "window.ChordynautNativeTouchBridge&&window.ChordynautNativeTouchBridge.updateTouchFrame("
                    + JSONObject.quote(payload.toString()) + ");";
            webView.post(() -> webView.evaluateJavascript(script, null));
        } catch (JSONException ignored) {
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_OPEN_DOCUMENT || pendingFileChooser == null) {
            return;
        }
        Uri uri = (resultCode == RESULT_OK && data != null) ? data.getData() : null;
        if (uri != null) {
            pendingFileChooser.onReceiveValue(new Uri[]{uri});
        } else {
            pendingFileChooser.onReceiveValue(null);
        }
        pendingFileChooser = null;
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (pendingFileChooser != null) {
            pendingFileChooser.onReceiveValue(null);
            pendingFileChooser = null;
        }
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    private void openExternal(Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException ignored) {
        }
    }
}
