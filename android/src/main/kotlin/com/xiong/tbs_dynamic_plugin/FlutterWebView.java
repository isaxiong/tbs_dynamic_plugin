// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.xiong.tbs_dynamic_plugin;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.tencent.smtt.export.external.interfaces.WebResourceRequest;
import com.tencent.smtt.sdk.ValueCallback;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebStorage;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.platform.PlatformView;


public class FlutterWebView implements PlatformView, MethodCallHandler {
    private static final String JS_CHANNEL_NAMES_FIELD = "javascriptChannelNames";
    private final WebViewFactory webViewFactory;
    private final WebView webView;
    private final MethodChannel methodChannel;
    private final FlutterWebViewClient flutterWebViewClient;
    private final Handler platformThreadHandler;

    // Verifies that a url opened by `Window.open` has a secure url.
    private class FlutterWebChromeClient extends WebChromeClient {
        @Override
        public boolean onCreateWindow(
                final WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
            final WebViewClient webViewClient =
                    new WebViewClient() {
                        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                        @Override
                        public boolean shouldOverrideUrlLoading(
                                @NonNull WebView view, @NonNull WebResourceRequest request) {
                            final String url = request.getUrl().toString();
                            if (!flutterWebViewClient.shouldOverrideUrlLoading(
                                    FlutterWebView.this.webView, request)) {
                                webView.loadUrl(url);
                            }
                            return true;
                        }

                        @Override
                        public boolean shouldOverrideUrlLoading(WebView view, String url) {
                            if (!flutterWebViewClient.shouldOverrideUrlLoading(
                                    FlutterWebView.this.webView, url)) {
                                webView.loadUrl(url);
                            }
                            return true;
                        }
                    };

            final WebView newWebView = new WebView(view.getContext());
            newWebView.setWebViewClient(webViewClient);

            final WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(newWebView);
            resultMsg.sendToTarget();

            return true;
        }

        @Override
        public void onProgressChanged(WebView view, int progress) {
            flutterWebViewClient.onLoadingProgress(progress);
        }

        @Keep
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)//For Android 3.0+
        public void openFileChooser(ValueCallback uploadMsg) {
            webViewFactory.mUploadMessage = uploadMsg;
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/*");
            webViewFactory.getActivity().startActivityForResult(Intent.createChooser(i, "File " +
                            "Chooser"),
                    webViewFactory.FILECHOOSER_RESULTCODE);
        }

        @Keep
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)//For Android 3.0+
        public void openFileChooser(ValueCallback uploadMsg, String acceptType) {
            webViewFactory.mUploadMessage = uploadMsg;
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            webViewFactory.getActivity().startActivityForResult(
                    Intent.createChooser(i, "File Browser"),
                    webViewFactory.FILECHOOSER_RESULTCODE);
        }


        @Override
        @Keep
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)//For Android 4.1
        public void openFileChooser(ValueCallback<Uri> uploadMsg,
                                    String acceptType, String capture) {
            webViewFactory.mUploadMessage = uploadMsg;
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/*");
            webViewFactory.getActivity().startActivityForResult(Intent.createChooser(i, "File " +
                    "Chooser"), webViewFactory.FILECHOOSER_RESULTCODE);
        }

        @Override
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)//For Android 5.0+
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                         FileChooserParams fileChooserParams) {
            if (webViewFactory.mUploadMessageArray != null) {
                webViewFactory.mUploadMessageArray.onReceiveValue(null);
            }
            webViewFactory.mUploadMessageArray = filePathCallback;

            final String[] acceptTypes = getSafeAcceptedTypes(fileChooserParams);
            List<Intent> intentList = new ArrayList<Intent>();
            webViewFactory.fileUri = null;
            webViewFactory.videoUri = null;
            if (acceptsImages(acceptTypes)) {
                Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                webViewFactory.fileUri = getOutputFilename(MediaStore.ACTION_IMAGE_CAPTURE);
                takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, webViewFactory.fileUri);
                intentList.add(takePhotoIntent);
            }
            if (acceptsVideo(acceptTypes)) {
                Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                webViewFactory.videoUri = getOutputFilename(MediaStore.ACTION_VIDEO_CAPTURE);
                takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, webViewFactory.videoUri);
                intentList.add(takeVideoIntent);
            }
            Intent contentSelectionIntent;
            if (Build.VERSION.SDK_INT >= 21) {
                final boolean allowMultiple =
                        fileChooserParams.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE;
                contentSelectionIntent = fileChooserParams.createIntent();
                contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple);
            } else {
                contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("*/*");
            }
            Intent[] intentArray = intentList.toArray(new Intent[intentList.size()]);

            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
            webViewFactory.getActivity().startActivityForResult(chooserIntent,
                    webViewFactory.FILECHOOSER_RESULTCODE);

            return true;
        }

        private String[] getSafeAcceptedTypes(FileChooserParams params) {

            // the getAcceptTypes() is available only in api 21+
            // for lower level, we ignore it
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return params.getAcceptTypes();
            }

            final String[] EMPTY = {};
            return EMPTY;
        }

        private Boolean acceptsImages(String[] types) {
            return isArrayEmpty(types) || arrayContainsString(types, "image");
        }

        private Boolean acceptsVideo(String[] types) {
            return isArrayEmpty(types) || arrayContainsString(types, "video");
        }

        private Boolean isArrayEmpty(String[] arr) {
            // when our array returned from getAcceptTypes() has no values set from the
            // webview
            // i.e. <input type="file" />, without any "accept" attr
            // will be an array with one empty string element, afaik
            return arr.length == 0 || (arr.length == 1 && arr[0].length() == 0);
        }

        private Boolean arrayContainsString(String[] array, String pattern) {
            for (String content : array) {
                if (content.contains(pattern)) {
                    return true;
                }
            }
            return false;
        }

        private Uri getOutputFilename(String intentType) {
            String prefix = "";
            String suffix = "";

            if (intentType == MediaStore.ACTION_IMAGE_CAPTURE) {
                prefix = "image-";
                suffix = ".jpg";
            } else if (intentType == MediaStore.ACTION_VIDEO_CAPTURE) {
                prefix = "video-";
                suffix = ".mp4";
            }

            String packageName = webViewFactory.getActivity().getPackageName();
            File capturedFile = null;
            try {
                capturedFile = createCapturedFile(prefix, suffix);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return FileProvider.getUriForFile(webViewFactory.getActivity(), packageName +
                    ".fileprovider", capturedFile);
        }

        private File createCapturedFile(String prefix, String suffix) throws IOException {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = prefix + "_" + timeStamp;
            File storageDir = webViewFactory.getActivity().getExternalFilesDir(null);
            return File.createTempFile(imageFileName, suffix, storageDir);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @SuppressWarnings("unchecked")
    FlutterWebView(
            final Context context,
            BinaryMessenger messenger,
            int id,
            Map<String, Object> params,
            View containerView, WebViewFactory webViewFactory) {

        DisplayListenerProxy displayListenerProxy = new DisplayListenerProxy();
        DisplayManager displayManager =
                (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        displayListenerProxy.onPreWebViewInitialization(displayManager);

        Boolean usesHybridComposition = (Boolean) params.get("usesHybridComposition");
        webView =
                (usesHybridComposition)
                        ? new WebView(context)
                        : new InputAwareWebView(context, containerView);

        displayListenerProxy.onPostWebViewInitialization(displayManager);

        this.webViewFactory = webViewFactory;
        platformThreadHandler = new Handler(context.getMainLooper());

        //移除有风险的WebView系统隐藏接口漏洞
        webView.removeJavascriptInterface("searchBoxJavaBridge_");
        webView.removeJavascriptInterface("accessibility");
        webView.removeJavascriptInterface("accessibilityTraversal");

        // Allow local storage.
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setPluginState(WebSettings.PluginState.ON);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        // Multi windows is set with FlutterWebChromeClient by default to handle internal bug:
        // b/159892679.
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBlockNetworkImage(false);
        webView.getSettings().setGeolocationEnabled(false);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setSupportMultipleWindows(true);

        // 允许webview对本机文件的操作
        webView.getSettings().setAllowFileAccess(true);
//        webView.getSettings().setAllowContentAccess(true);
//        webView.getSettings().setAllowFileAccessFromFileURLs(false);
//        webView.getSettings().setAllowUniversalAccessFromFileURLs(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webView.getSettings().setMixedContentMode(webView.getSettings().getMixedContentMode());// 设置兼容模式在https下显示http的图片资源
        }

        webView.setWebChromeClient(new FlutterWebChromeClient());

        methodChannel = new MethodChannel(messenger, "com.xiong.tbs_dynamic/x5webview_" + id);
        methodChannel.setMethodCallHandler(this);

        flutterWebViewClient = new FlutterWebViewClient(methodChannel);

        Map<String, Object> settings = (Map<String, Object>) params.get("settings");
        if (settings != null) {
            applySettings(settings);
        }

        if (params.containsKey(JS_CHANNEL_NAMES_FIELD)) {
            List<String> names = (List<String>) params.get(JS_CHANNEL_NAMES_FIELD);
            if (names != null) {
                registerJavaScriptChannelNames(names);
            }
        }

        Integer autoMediaPlaybackPolicy = (Integer) params.get("autoMediaPlaybackPolicy");
        if (autoMediaPlaybackPolicy != null) {
            updateAutoMediaPlaybackPolicy(autoMediaPlaybackPolicy);
        }
        if (params.containsKey("userAgent")) {
            String userAgent = (String) params.get("userAgent");
            updateUserAgent(userAgent);
        }
        if (params.containsKey("initialUrl")) {
            String url = (String) params.get("initialUrl");
            webView.loadUrl(url);
        }
    }

    @Override
    public View getView() {
        return webView;
    }

    @Override
    // This is overriding a method that hasn't rolled into stable Flutter yet. Including the
    // annotation would cause compile time failures in versions of Flutter too old to include the
    // new
    // method. However leaving it raw like this means that the method will be ignored in old
    // versions
    // of Flutter but used as an override anyway wherever it's actually defined.
    // TODO(mklim): Add the @Override annotation once flutter/engine#9727 rolls to stable.
    public void onInputConnectionUnlocked() {
        if (webView instanceof InputAwareWebView) {
            ((InputAwareWebView) webView).unlockInputConnection();
        }
    }

    @Override
    // This is overriding a method that hasn't rolled into stable Flutter yet. Including the
    // annotation would cause compile time failures in versions of Flutter too old to include the
    // new
    // method. However leaving it raw like this means that the method will be ignored in old
    // versions
    // of Flutter but used as an override anyway wherever it's actually defined.
    // TODO(mklim): Add the @Override annotation once flutter/engine#9727 rolls to stable.
    public void onInputConnectionLocked() {
        if (webView instanceof InputAwareWebView) {
            ((InputAwareWebView) webView).lockInputConnection();
        }
    }

    @Override
    // This is overriding a method that hasn't rolled into stable Flutter yet. Including the
    // annotation would cause compile time failures in versions of Flutter too old to include the
    // new
    // method. However leaving it raw like this means that the method will be ignored in old
    // versions
    // of Flutter but used as an override anyway wherever it's actually defined.
    // TODO(mklim): Add the @Override annotation once stable passes v1.10.9.
    public void onFlutterViewAttached(View flutterView) {
        if (webView instanceof InputAwareWebView) {
            ((InputAwareWebView) webView).setContainerView(flutterView);
        }
    }

    @Override
    // This is overriding a method that hasn't rolled into stable Flutter yet. Including the
    // annotation would cause compile time failures in versions of Flutter too old to include the
    // new
    // method. However leaving it raw like this means that the method will be ignored in old
    // versions
    // of Flutter but used as an override anyway wherever it's actually defined.
    // TODO(mklim): Add the @Override annotation once stable passes v1.10.9.
    public void onFlutterViewDetached() {
        if (webView instanceof InputAwareWebView) {
            ((InputAwareWebView) webView).setContainerView(null);
        }
    }

    @Override
    public void onMethodCall(MethodCall methodCall, Result result) {
        switch (methodCall.method) {
            case "loadUrl":
                loadUrl(methodCall, result);
                break;
            case "updateSettings":
                updateSettings(methodCall, result);
                break;
            case "canGoBack":
                canGoBack(result);
                break;
            case "canGoForward":
                canGoForward(result);
                break;
            case "goBack":
                goBack(result);
                break;
            case "goForward":
                goForward(result);
                break;
            case "reload":
                reload(result);
                break;
            case "currentUrl":
                currentUrl(result);
                break;
            case "evaluateJavascript":
                evaluateJavaScript(methodCall, result);
                break;
            case "addJavascriptChannels":
                addJavaScriptChannels(methodCall, result);
                break;
            case "removeJavascriptChannels":
                removeJavaScriptChannels(methodCall, result);
                break;
            case "clearCache":
                clearCache(result);
                break;
            case "getTitle":
                getTitle(result);
                break;
            case "scrollTo":
                scrollTo(methodCall, result);
                break;
            case "scrollBy":
                scrollBy(methodCall, result);
                break;
            case "getScrollX":
                getScrollX(result);
                break;
            case "getScrollY":
                getScrollY(result);
                break;
            default:
                result.notImplemented();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadUrl(MethodCall methodCall, Result result) {
        Map<String, Object> request = (Map<String, Object>) methodCall.arguments;
        String url = (String) request.get("url");
        Map<String, String> headers = (Map<String, String>) request.get("headers");
        if (headers == null) {
            headers = Collections.emptyMap();
        }
        webView.loadUrl(url, headers);
        result.success(null);
    }

    private void canGoBack(Result result) {
        result.success(webView.canGoBack());
    }

    private void canGoForward(Result result) {
        result.success(webView.canGoForward());
    }

    private void goBack(Result result) {
        if (webView.canGoBack()) {
            webView.goBack();
        }
        result.success(null);
    }

    private void goForward(Result result) {
        if (webView.canGoForward()) {
            webView.goForward();
        }
        result.success(null);
    }

    private void reload(Result result) {
        webView.reload();
        result.success(null);
    }

    private void currentUrl(Result result) {
        result.success(webView.getUrl());
    }

    @SuppressWarnings("unchecked")
    private void updateSettings(MethodCall methodCall, Result result) {
        applySettings((Map<String, Object>) methodCall.arguments);
        result.success(null);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void evaluateJavaScript(MethodCall methodCall, final Result result) {
        String jsString = (String) methodCall.arguments;
        if (jsString == null) {
            throw new UnsupportedOperationException("JavaScript string cannot be null");
        }
        webView.evaluateJavascript(
                jsString,
                new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        result.success(value);
                    }
                });
    }

    @SuppressWarnings("unchecked")
    private void addJavaScriptChannels(MethodCall methodCall, Result result) {
        List<String> channelNames = (List<String>) methodCall.arguments;
        registerJavaScriptChannelNames(channelNames);
        result.success(null);
    }

    @SuppressWarnings("unchecked")
    private void removeJavaScriptChannels(MethodCall methodCall, Result result) {
        List<String> channelNames = (List<String>) methodCall.arguments;
        for (String channelName : channelNames) {
            webView.removeJavascriptInterface(channelName);
        }
        result.success(null);
    }

    private void clearCache(Result result) {
        webView.clearCache(true);
        WebStorage.getInstance().deleteAllData();
        result.success(null);
    }

    private void getTitle(Result result) {
        result.success(webView.getTitle());
    }

    private void scrollTo(MethodCall methodCall, Result result) {
        Map<String, Object> request = methodCall.arguments();
        int x = (int) request.get("x");
        int y = (int) request.get("y");

        webView.scrollTo(x, y);

        result.success(null);
    }

    private void scrollBy(MethodCall methodCall, Result result) {
        Map<String, Object> request = methodCall.arguments();
        int x = (int) request.get("x");
        int y = (int) request.get("y");

        webView.scrollBy(x, y);
        result.success(null);
    }

    private void getScrollX(Result result) {
        result.success(webView.getScrollX());
    }

    private void getScrollY(Result result) {
        result.success(webView.getScrollY());
    }

    private void applySettings(Map<String, Object> settings) {
        for (String key : settings.keySet()) {
            switch (key) {
                case "jsMode":
                    Integer mode = (Integer) settings.get(key);
                    if (mode != null) {
                        updateJsMode(mode);
                    }
                    break;
                case "hasNavigationDelegate":
                    final boolean hasNavigationDelegate = (boolean) settings.get(key);

                    final WebViewClient webViewClient =
                            flutterWebViewClient.createWebViewClient(hasNavigationDelegate);

                    webView.setWebViewClient(webViewClient);
                    break;
                case "debuggingEnabled":
                    final boolean debuggingEnabled = (boolean) settings.get(key);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        webView.setWebContentsDebuggingEnabled(debuggingEnabled);
                    }
                    break;
                case "hasProgressTracking":
                    flutterWebViewClient.hasProgressTracking = (boolean) settings.get(key);
                    break;
                case "gestureNavigationEnabled":
                    break;
                case "userAgent":
                    updateUserAgent((String) settings.get(key));
                    break;
                case "allowsInlineMediaPlayback":
                    // no-op inline media playback is always allowed on Android.
                    break;
                default:
                    throw new IllegalArgumentException("Unknown WebView setting: " + key);
            }
        }
    }

    private void updateJsMode(int mode) {
        switch (mode) {
            case 0: // disabled
                webView.getSettings().setJavaScriptEnabled(false);
                break;
            case 1: // unrestricted
                webView.getSettings().setJavaScriptEnabled(true);
                break;
            default:
                throw new IllegalArgumentException("Trying to set unknown JavaScript mode: " + mode);
        }
    }

    private void updateAutoMediaPlaybackPolicy(int mode) {
        // This is the index of the AutoMediaPlaybackPolicy enum, index 1 is always_allow, for all
        // other values we require a user gesture.
        boolean requireUserGesture = mode != 1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            webView.getSettings().setMediaPlaybackRequiresUserGesture(requireUserGesture);
        }
    }

    private void registerJavaScriptChannelNames(List<String> channelNames) {
        for (String channelName : channelNames) {
            webView.addJavascriptInterface(
                    new JavaScriptChannel(methodChannel, channelName, platformThreadHandler),
                    channelName);
        }
    }

    private void updateUserAgent(String userAgent) {
        webView.getSettings().setUserAgentString(userAgent);
    }

    @Override
    public void dispose() {
        methodChannel.setMethodCallHandler(null);
        if (webView instanceof InputAwareWebView) {
            ((InputAwareWebView) webView).dispose();
        }
        webView.clearCache(true);
        webView.clearHistory();
        webView.clearFormData();
        webView.removeAllViewsInLayout();
        webView.removeAllViews();
        webView.destroy();
    }
}
