package com.zebra.enterpriseservices;

import android.os.Bundle;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;

public class LicenceActivity extends AppCompatActivity {

    WebView mWebView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_licence);

        mWebView = (WebView)findViewById(R.id.wvLicence);
        mWebView.loadUrl("file:///android_asset/licence.html");
    }

}
