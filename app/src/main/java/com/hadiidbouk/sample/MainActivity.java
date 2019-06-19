package com.hadiidbouk.sample;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.hadiidbouk.appauthwebview.AppAuthWebView;
import com.hadiidbouk.appauthwebview.AppAuthWebViewData;
import com.hadiidbouk.appauthwebview.IAppAuthWebViewListener;

import net.openid.appauth.AuthState;

public class MainActivity extends AppCompatActivity implements IAppAuthWebViewListener {

    private AppAuthWebViewData mData;
    private FrameLayout mErrorLayout;
    private FrameLayout mLoadingLayout;
    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mData = new AppAuthWebViewData();
        mData.setClientId("native.code");
        mData.setDiscoveryUri("https://demo.identityserver.io/.well-known/openid-configuration");
        mData.setScope("openid profile email api offline_access");
        mData.setAuthorizationEndpointUri("https://demo.identityserver.io/connect/authorize");
        mData.setRedirectLoginUri("hadiidbouk-appAuthWebView://callback");
        mData.setTokenEndpointUri("https://demo.identityserver.io/connect/token");
        mData.setResponseType("code");
        mData.setGenerateCodeVerifier(true);

        //Todo: delete after refactoring the code
        mData.setRegistrationEndpointUri("");
        mData.setRedirectLogoutUri("");
        mData.setClientSecret("");

        mErrorLayout = findViewById(R.id.ErrorLayout);
        mLoadingLayout = findViewById(R.id.LoadingLayout);
        mWebView = findViewById(R.id.WebView);

        AppAuthWebView appAuthWebView = new AppAuthWebView
                .Builder()
                .webView(mWebView)
                .authData(mData)
                .listener(this)
                .build();

        appAuthWebView.performLoginRequest();
    }

    @Override public void onUserAuthorize(AuthState authState) {
        Toast.makeText(this, "Success!\n\nToken : " + authState.getIdToken(), Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override public void showConnectionErrorLayout() {
        mErrorLayout.setVisibility(View.VISIBLE);
    }

    @Override public void hideConnectionErrorLayout() {
        mErrorLayout.setVisibility(View.INVISIBLE);
    }

    @Override public void showLoadingLayout() {
        mLoadingLayout.setVisibility(View.VISIBLE);
    }

    @Override public void hideLoadingLayout() {
        mLoadingLayout.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onLogoutFinish() {

    }
}
