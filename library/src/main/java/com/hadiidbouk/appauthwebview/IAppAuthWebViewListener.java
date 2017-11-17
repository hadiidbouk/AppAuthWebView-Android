package com.hadiidbouk.appauthwebview;

import net.openid.appauth.AuthState;

public interface IAppAuthWebViewListener {


	void onUserAuthorize(AuthState authState);

	void showConnectionErrorLayout();

	void hideConnectionErrorLayout();

	void showLoadingLayout();

	void hideLoadingLayout();

	void onLogoutFinish();
}
