// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.xiong.tbs_dynamic_plugin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;
import android.view.View;

import com.tencent.smtt.sdk.ValueCallback;

import java.util.Map;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.StandardMessageCodec;
import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.platform.PlatformViewFactory;

import static android.app.Activity.RESULT_OK;

public class WebViewFactory extends PlatformViewFactory implements PluginRegistry.ActivityResultListener {
    private final BinaryMessenger messenger;
    private final View containerView;
    public ValueCallback<Uri> mUploadMessage;
    public ValueCallback<Uri[]> mUploadMessageArray;
    private Activity mActivity;
    public final int FILECHOOSER_RESULTCODE = 1;
    public Uri fileUri;
    public Uri videoUri;

    WebViewFactory(BinaryMessenger messenger, View containerView, Activity mActivity) {
        super(StandardMessageCodec.INSTANCE);
        this.messenger = messenger;
        this.containerView = containerView;
        this.mActivity = mActivity;
    }

    @SuppressWarnings("unchecked")
    @Override
    public PlatformView create(Context context, int id, Object args) {
        Map<String, Object> params = (Map<String, Object>) args;
        return new FlutterWebView(context, messenger, id, params, containerView, this);
    }

    private long getFileSize(Uri fileUri) {
        Cursor returnCursor = mActivity.getContentResolver().query(fileUri, null, null, null, null);
        returnCursor.moveToFirst();
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        return returnCursor.getLong(sizeIndex);
    }

    private Uri[] getSelectedFiles(Intent data) {
        // we have one files selected
        if (data.getData() != null) {
            String dataString = data.getDataString();
            if (dataString != null) {
                return new Uri[]{Uri.parse(dataString)};
            }
        }
        // we have multiple files selected
        if (data.getClipData() != null) {
            final int numSelectedFiles = data.getClipData().getItemCount();
            Uri[] result = new Uri[numSelectedFiles];
            for (int i = 0; i < numSelectedFiles; i++) {
                result[i] = data.getClipData().getItemAt(i).getUri();
            }
            return result;
        }
        return null;
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent intent) {
        boolean handled = false;
        if (Build.VERSION.SDK_INT >= 21) {
            if (requestCode == FILECHOOSER_RESULTCODE) {
                Uri[] results = null;
                if (resultCode == Activity.RESULT_OK) {
                    if (fileUri != null && getFileSize(fileUri) > 0) {
                        results = new Uri[]{fileUri};
                    } else if (videoUri != null && getFileSize(videoUri) > 0) {
                        results = new Uri[]{videoUri};
                    } else if (intent != null) {
                        results = getSelectedFiles(intent);
                    }
                }
                if (mUploadMessageArray != null) {
                    mUploadMessageArray.onReceiveValue(results);
                    mUploadMessageArray = null;
                }
                handled = true;
            }
        } else {
            if (requestCode == FILECHOOSER_RESULTCODE) {
                Uri result = null;
                if (resultCode == RESULT_OK && intent != null) {
                    result = intent.getData();
                }
                if (mUploadMessage != null) {
                    mUploadMessage.onReceiveValue(result);
                    mUploadMessage = null;
                }
                handled = true;
            }
        }
        return handled;
    }

    public void setActivity(Activity mActivity) {
        this.mActivity = mActivity;
    }

    public Activity getActivity() {
        return mActivity;
    }

}
