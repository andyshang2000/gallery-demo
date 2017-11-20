package com.zz.gallery;

import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.BinaryHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.TextHttpResponseHandler;

import cz.msebera.android.httpclient.Header;

public class HTTP {
    public static AsyncHttpClient mHttpc = new AsyncHttpClient();
    public static String TAG = "HTTP";

    public void get(String uri) {
        mHttpc.get(uri, null, new JsonHttpResponseHandler() {
        });
    }

    public void get(String uri, String savePath) {
        mHttpc.get(uri, new ImageResponseHandler(savePath));
    }

    public class ImageResponseHandler extends BinaryHttpResponseHandler {
        private String mSavePath;

        public ImageResponseHandler(String savePath) {
            super();
            mSavePath = savePath;
        }

        @Override
        public void onSuccess(int statusCode, Header[] headers, byte[] binaryData) {
            Log.i(TAG, "download image, file length " + binaryData.length);
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, byte[] binaryData, Throwable error) {
            Log.i(TAG, "download failed");
        }
    }
};