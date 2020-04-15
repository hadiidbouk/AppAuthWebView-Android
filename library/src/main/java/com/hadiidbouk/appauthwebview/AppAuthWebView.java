package com.hadiidbouk.appauthwebview;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ClientAuthentication;
import net.openid.appauth.ClientSecretBasic;
import net.openid.appauth.ClientSecretPost;
import net.openid.appauth.CodeVerifierUtil;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;
import net.openid.appauth.internal.Logger;

import org.json.JSONException;

import java.util.Date;
import java.util.UUID;

public class AppAuthWebView {

	private IAppAuthWebViewListener mAppAuthWebViewListener;
	private WebView mWebView;
	private AppAuthWebViewData mAppAuthWebViewData;
	private Context mContext;
	private long mConnectionTimeOut;
	private boolean isRedirect, isErrorReceived, isLoadingLayoutVisible;
	private ConnectionTimeoutHandler timeoutHandler = null;
	private static int PAGE_LOAD_PROGRESS = 0;
	private String mCodeVerifier;
	private boolean isLogout = false;
	private static String mSharedPrefenceName = null;
	private static boolean mUseClientSecretBasic = false;

	// From AppAuth Library
	private AuthorizationServiceConfiguration mAuthConfig;
	private AuthorizationRequest mAuthRequest;
	private AuthState mAuthState;
	private AuthorizationService mAuthService;

	public static class Builder {
		private IAppAuthWebViewListener mAuthWebViewListener;
		private WebView mWebView;
		private AppAuthWebViewData mAppAuthWebViewData;
		private Context mContext;
		private long mConnectionTimeOut;
		private String mCustomSharedPreferenceName = null;
		private boolean mUseClientSecretBasic = false;

		public Builder listener(IAppAuthWebViewListener authWebViewListener) {
			mAuthWebViewListener = authWebViewListener;
			return this;
		}

		public Builder webView(WebView webView) {
			mWebView = webView;
			mContext = webView.getContext();
			return this;
		}

		public Builder authData(AppAuthWebViewData appAuthWebViewData) {
			mAppAuthWebViewData = appAuthWebViewData;
			return this;
		}

		public Builder setConnectionTimeout(long timeout) {
			mConnectionTimeOut = timeout;
			return this;
		}

		public Builder setSharedPreferenceName(String sharedPrefenceName) {
			mCustomSharedPreferenceName = sharedPrefenceName;
			return this;
		}

		public Builder setUseClientSecretBasic(boolean value) {
			mUseClientSecretBasic = value;
			return this;
		}

		public AppAuthWebView build() {
			return new AppAuthWebView(
				mContext,
				mAuthWebViewListener,
				mWebView,
				mAppAuthWebViewData,
				mConnectionTimeOut,
				mCustomSharedPreferenceName,
				mUseClientSecretBasic
			);
		}
	}

	@SuppressLint("SetJavaScriptEnabled")
	private AppAuthWebView(Context context,
						   IAppAuthWebViewListener appAuthWebViewListener,
						   WebView webView,
						   AppAuthWebViewData appAuthWebViewData,
						   long connectionTimeOut,
						   String customSharedPreferenceName,
						   boolean useClientSecretBasic
	) {
		this.mAppAuthWebViewListener = appAuthWebViewListener;
		this.mWebView = webView;
		this.mAppAuthWebViewData = appAuthWebViewData;
		this.mConnectionTimeOut = connectionTimeOut == 0L ? 30000L : connectionTimeOut;
		this.mSharedPrefenceName = customSharedPreferenceName;
		this.mUseClientSecretBasic = useClientSecretBasic;
		mContext = context;

		mAuthConfig = new AuthorizationServiceConfiguration(
			Uri.parse(mAppAuthWebViewData.getAuthorizationEndpointUri()),
			Uri.parse(mAppAuthWebViewData.getTokenEndpointUri()),
			Uri.parse(mAppAuthWebViewData.getRegistrationEndpointUri())
		);

		AppAuthConfiguration.Builder appAuthConfigBuilder = new AppAuthConfiguration.Builder();
		appAuthConfigBuilder.setConnectionBuilder(AppAuthConnectionBuilderForTesting.INSTANCE);
		AppAuthConfiguration appAuthConfig = appAuthConfigBuilder.build();

		mAuthService = new AuthorizationService(mContext, appAuthConfig);

		AuthorizationRequest.Builder authRequestBuilder = new AuthorizationRequest
			.Builder(
			mAuthConfig,
			mAppAuthWebViewData.getClientId(),
			mAppAuthWebViewData.getResponseType(),
			Uri.parse(mAppAuthWebViewData.getRedirectLoginUri())
		)
			.setScope(mAppAuthWebViewData.getScope());

		if (mAppAuthWebViewData.isGenerateCodeVerifier()) {
			mCodeVerifier = CodeVerifierUtil.generateRandomCodeVerifier();
		} else {
			mCodeVerifier = null;
		}
		authRequestBuilder.setCodeVerifier(mCodeVerifier);

		mAuthRequest = authRequestBuilder.build();

		mWebView.setWebViewClient(new AppAuthWebViewClient());
		mWebView.getSettings().setJavaScriptEnabled(true);
	}

	@SuppressLint("SetJavaScriptEnabled")
	public void performLoginRequest() {
		isRedirect = false;
		isErrorReceived = false;
		mWebView.setWebViewClient(new AppAuthWebViewClient());
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.setWebChromeClient(new ChromeClient());

		if (!isLoadingLayoutVisible && !isLogout)
			mWebView.loadUrl("about:blank");

		mAppAuthWebViewListener.hideConnectionErrorLayout();

		mWebView.setVisibility(View.VISIBLE);

		Uri uri = mAuthRequest.toUri();

		String nonce = "";

		if (mAppAuthWebViewData.isNonceAdded()) {
			nonce = "&nonce=" + UUID.randomUUID().toString();
		}
		mWebView.loadUrl(uri.toString() + nonce);
	}

	public void performLogoutRequest() {
		AuthState authState;
		if(isSetSharedPreferenceName()){
			authState = getAuthState(mContext, mSharedPrefenceName);
		} else {
			authState = getAuthState(mContext);
		}
		if (authState == null)
			return;
		String idToken = authState.getIdToken();
		if (idToken != null) {
			Uri uri = Uri.parse(
				mAppAuthWebViewData.getEndSessionEndpointUri()
					+ "?id_token_hint="
					+ authState.getIdToken()
					+ "&post_logout_redirect_uri="
					+ mAppAuthWebViewData.getRedirectLogoutUri()
			);

			mWebView.loadUrl(uri.toString());
		}
	}

	public static void performRefreshTokenRequest(final Context context, AuthState authState, AppAuthWebViewData data) {

		AppAuthConfiguration.Builder appAuthConfigBuilder = new AppAuthConfiguration.Builder();
		appAuthConfigBuilder.setConnectionBuilder(AppAuthConnectionBuilderForTesting.INSTANCE);
		AppAuthConfiguration appAuthConfig = appAuthConfigBuilder.build();

		AuthorizationService authService = new AuthorizationService(context, appAuthConfig);

		final TokenRequest request = authState.createTokenRefreshRequest();

		authService.performTokenRequest(request, getClientAuthentication(data.getClientSecret()), new AuthorizationService.TokenResponseCallback() {
			@Override public void onTokenRequestCompleted(@Nullable TokenResponse response, @Nullable AuthorizationException ex) {
				if (ex != null) {
					ex.printStackTrace();
					return;
				}
				AppAuthWebView.updateAuthStateFromRefreshToken(context, response, ex);
			}
		});
	}

	@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
	private Intent extractResponseData(Uri responseUri) {
		if (responseUri.getQueryParameterNames().contains(AuthorizationException.PARAM_ERROR)) {
			return AuthorizationException.fromOAuthRedirect(responseUri).toIntent();
		} else {
			AuthorizationResponse response = new AuthorizationResponse.Builder(mAuthRequest)
				.fromUri(responseUri)
				.build();

			if (mAuthRequest.state == null &&
				response.state != null ||
				(mAuthRequest.state != null && !mAuthRequest.state.equals(response.state))) {
				Logger.warn("State returned in authorization response (%s) does not match state "
						+ "from request (%s) - discarding response",
					response.state,
					mAuthRequest.state);

				return AuthorizationException.AuthorizationRequestErrors.STATE_MISMATCH.toIntent();
			}

			return response.toIntent();
		}
	}

	private class AppAuthWebViewClient extends WebViewClient {

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {

			timeoutHandler = new ConnectionTimeoutHandler();
			timeoutHandler.execute();

			super.onPageStarted(view, url, favicon);
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {

			if (url.toLowerCase().equals(mAppAuthWebViewData.getRedirectLogoutUri().toLowerCase())) {
				isLogout = true;
				mAppAuthWebViewListener.onLogoutFinish();
				return true;
			} else if (url.toLowerCase().contains("logout")) {
				isLogout = true;
				return false;
			} else if (url.toLowerCase().startsWith(mAppAuthWebViewData.getRedirectLoginUri().toLowerCase())) {

				mAppAuthWebViewListener.hideConnectionErrorLayout();
				mAppAuthWebViewListener.showLoadingLayout();

				if (timeoutHandler != null)
					timeoutHandler.cancel(true);

				isRedirect = true;

				Intent intent = extractResponseData(Uri.parse(url));

				final AuthorizationResponse resp = AuthorizationResponse.fromIntent(intent);
				AuthorizationException ex = AuthorizationException.fromIntent(intent);

				setAuthState(resp, ex);

				if (resp != null) {

					TokenRequest.Builder tokenRequestBuilder = new TokenRequest
						.Builder(mAuthConfig, mAppAuthWebViewData.getClientId());

					tokenRequestBuilder
						.setAuthorizationCode(resp.authorizationCode)
						.setRedirectUri(Uri.parse(mAppAuthWebViewData.getRedirectLoginUri()))
						.setCodeVerifier(mCodeVerifier);


					TokenRequest tokenRequest = tokenRequestBuilder.build();

					mAuthService.performTokenRequest(tokenRequest, getClientAuthentication(mAppAuthWebViewData.getClientSecret()), new AuthorizationService.TokenResponseCallback() {
						@Override public void onTokenRequestCompleted(@Nullable TokenResponse response, @Nullable AuthorizationException ex) {

							if (ex == null) {
								// no exception received
								updateAuthState(response, ex);

								mAppAuthWebViewListener.onUserAuthorize(mAuthState);

								AuthorizationRequest authorizationRequest = new AuthorizationRequest
									.Builder(mAuthConfig,
									mAppAuthWebViewData.getClientId(),
									mAppAuthWebViewData.getResponseType(),
									Uri.parse(mAppAuthWebViewData.getRedirectLoginUri()))
									.setScope(mAppAuthWebViewData.getScope())
									.build();

								setAuthorizationRequest(authorizationRequest);
							} else {
								mAppAuthWebViewListener.showConnectionErrorLayout();
							}
						}
					});
				} else {
					mAppAuthWebViewListener.showConnectionErrorLayout();
				}
			}
			isLogout = false;
			return false;
		}

		@Override public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);

			if(isLogout) {
				view.setVisibility(View.INVISIBLE);
			}
			if (timeoutHandler != null)
				timeoutHandler.cancel(true);


		}

		@Override
		public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {

			isErrorReceived = true;

			if (!isRedirect) {
				mWebView.stopLoading();
				mAppAuthWebViewListener.showConnectionErrorLayout();
				mWebView.setVisibility(View.INVISIBLE);
			}
			super.onReceivedHttpError(view, request, errorResponse);

		}

		@Override
		public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {

			isErrorReceived = true;

			if (!isRedirect) {
				mWebView.stopLoading();
				mAppAuthWebViewListener.showConnectionErrorLayout();
				mWebView.setVisibility(View.INVISIBLE);
			}
			super.onReceivedSslError(view, handler, error);

		}

		@Override
		public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {

			isErrorReceived = true;

			if (!isRedirect) {
				mWebView.stopLoading();
				mAppAuthWebViewListener.showConnectionErrorLayout();
				mWebView.setVisibility(View.INVISIBLE);
			}
			super.onReceivedError(view, errorCode, description, failingUrl);

		}

		@TargetApi(android.os.Build.VERSION_CODES.M)
		@Override
		public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError rerr) {
			// Redirect to deprecated method, so you can use it in all SDK versions
			onReceivedError(view, rerr.getErrorCode(), rerr.getDescription().toString(), req.getUrl().toString());
		}
	}

	private class ConnectionTimeoutHandler extends AsyncTask<Void, Void, String> {

		private static final String PAGE_LOADED = "PAGE_LOADED";
		private static final String CONNECTION_TIMEOUT = "CONNECTION_TIMEOUT";

		private Date startTime = new Date();
		private Date currentTime = new Date();
		private Boolean loaded = false;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			startTime = new Date();
			PAGE_LOAD_PROGRESS = 0;
		}

		@Override
		protected void onPostExecute(String s) {

			if (isErrorReceived && !isRedirect) {

				mAppAuthWebViewListener.showConnectionErrorLayout();

				mWebView.setVisibility(View.INVISIBLE);

				mWebView.stopLoading();

			} else {

				if (isRedirect || isLogout) {
					mAppAuthWebViewListener.showLoadingLayout();
					isLoadingLayoutVisible = true;
				} else {

					mAppAuthWebViewListener.hideLoadingLayout();

					isLoadingLayoutVisible = false;

					mAppAuthWebViewListener.hideConnectionErrorLayout();

					mWebView.setVisibility(View.VISIBLE);
				}

			}
		}

		@Override
		protected String doInBackground(Void... voids) {

			while (!loaded) {

				currentTime = new Date();

				if (PAGE_LOAD_PROGRESS != 100
					&& (currentTime.getTime() - startTime.getTime()) > mConnectionTimeOut) {
					isErrorReceived = true;
					return CONNECTION_TIMEOUT;
				} else if (PAGE_LOAD_PROGRESS == 100) {
					loaded = true;
				}
			}
			return PAGE_LOADED;
		}
	}

	private class ChromeClient extends WebChromeClient {

		@Override
		public void onProgressChanged(WebView view, int newProgress) {
			PAGE_LOAD_PROGRESS = newProgress;

			if (PAGE_LOAD_PROGRESS == 100) {
				if (!isRedirect && !isLogout) {
					mAppAuthWebViewListener.hideLoadingLayout();
					isLoadingLayoutVisible = false;
				}
			} else {
				mAppAuthWebViewListener.showLoadingLayout();
				isLoadingLayoutVisible = true;
			}
			super.onProgressChanged(view, newProgress);
		}
	}

	private void setAuthState(AuthorizationResponse response, AuthorizationException ex) {
		if (mAuthState == null)
			mAuthState = new AuthState(response, ex);

//		PreferenceManager.getDefaultSharedPreferences(mContext).edit().putString("AuthState", mAuthState.jsonSerializeString()).apply();
		getSharedPreferences(mContext).edit().putString("AuthState", mAuthState.jsonSerializeString()).apply();

	}

	private void updateAuthState(TokenResponse response, AuthorizationException ex) {
		if (mAuthState != null) {
			mAuthState.update(response, ex);
			getSharedPreferences(mContext).edit().putString("AuthState", mAuthState.jsonSerializeString()).apply();
		}
	}

	public static void updateAuthState(Context context, String authStateJsonString) {
		getSharedPreferences(context).edit().putString("AuthState", authStateJsonString).apply();
	}

	private void setAuthorizationRequest(AuthorizationRequest request) {
		if (request != null)
			getSharedPreferences(mContext).edit().putString("AuthRequest", request.jsonSerializeString()).apply();
		else
			getSharedPreferences(mContext).edit().putString("AuthRequest", null).apply();
	}

	private static void updateAuthStateFromRefreshToken(Context context, TokenResponse response, AuthorizationException ex) {
		AuthState authState = getAuthState(context);
		if (authState != null) {
			authState.update(response, ex);
			getSharedPreferences(context).edit().putString("AuthState", authState.jsonSerializeString()).apply();
			Intent intent = new Intent();
			intent.setAction(BROADCAST_RECEIVER_ACTION);
			intent.putExtra(AUTH_STATE_JSON, authState.jsonSerializeString());
			context.sendBroadcast(intent);
		}
	}

	public static AuthState getAuthState(Context context) {
		String authStateString = PreferenceManager.getDefaultSharedPreferences(context).getString("AuthState", null);
		if (authStateString != null) {
			try {
				return AuthState.jsonDeserialize(authStateString);
			} catch (JSONException e) {
				e.printStackTrace();
				return null;
			}
		}
		return null;
	}

	public static AuthState getAuthState(Context context, String customSharedPreferenceName) {
		if(customSharedPreferenceName != null && !customSharedPreferenceName.isEmpty()){
			String authStateString = context.getSharedPreferences(customSharedPreferenceName, Context.MODE_PRIVATE).getString("AuthState", null);
			if (authStateString != null) {
				try {
					return AuthState.jsonDeserialize(authStateString);
				} catch (JSONException e) {
					e.printStackTrace();
					return null;
				}
			}
		}
		return null;
	}

	private static SharedPreferences getSharedPreferences(Context context){
		if(isSetSharedPreferenceName()){
			return context.getSharedPreferences(mSharedPrefenceName, Context.MODE_PRIVATE);
		} else {
			return  PreferenceManager.getDefaultSharedPreferences(context);
		}
	}

	private static boolean isSetSharedPreferenceName(){
		return mSharedPrefenceName!= null && !mSharedPrefenceName.isEmpty();
	}

	private static ClientAuthentication getClientAuthentication(String clientSecret){
		if(mUseClientSecretBasic){
			return new ClientSecretBasic(clientSecret);
		} else {
			return new ClientSecretPost(clientSecret);
		}
	}

	public static final String BROADCAST_RECEIVER_ACTION = "com.hadiidbouk.AppAuthWebView.AccessTokenAction";
	public static final String AUTH_STATE_JSON = "AUTH_STATE_JSON";
}

