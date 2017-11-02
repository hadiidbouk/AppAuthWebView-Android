package com.hadiidbouk.appauthwebview;

/**
 * Created by Hadi on 6/29/2017.
 */

public class AppAuthWebViewData {

	private String clientId;
	private String clientSecret;
	private String redirectLoginUri;
	private String redirectLogoutUri;
	private String scope;
	private String discoveryUri;
	private String authorizationEndpointUri;
	private String tokenEndpointUri;
	private String registrationEndpointUri;
	private String responseType;
	private String endSessionEndpointUri;
	private boolean isNonceAdded;
	private boolean generateCodeVerifier = true;

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	public String getRedirectLoginUri() {
		return redirectLoginUri;
	}

	public void setRedirectLoginUri(String redirectLoginUri) {
		this.redirectLoginUri = redirectLoginUri;
	}

	public String getRedirectLogoutUri() {
		return redirectLogoutUri;
	}

	public void setRedirectLogoutUri(String redirectLogoutUri) {
		this.redirectLogoutUri = redirectLogoutUri;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public String getDiscoveryUri() {
		return discoveryUri;
	}

	public void setDiscoveryUri(String discoveryUri) {
		this.discoveryUri = discoveryUri;
	}

	public String getAuthorizationEndpointUri() {
		return authorizationEndpointUri;
	}

	public void setAuthorizationEndpointUri(String authorizationEndpointUri) {
		this.authorizationEndpointUri = authorizationEndpointUri;
	}

	public String getTokenEndpointUri() {
		return tokenEndpointUri;
	}

	public void setTokenEndpointUri(String tokenEndpointUri) {
		this.tokenEndpointUri = tokenEndpointUri;
	}

	public String getRegistrationEndpointUri() {
		return registrationEndpointUri;
	}

	public void setRegistrationEndpointUri(String registrationEndpointUri) {
		this.registrationEndpointUri = registrationEndpointUri;
	}

	public String getResponseType() {
		return responseType;
	}

	public void setResponseType(String responseType) {
		this.responseType = responseType;
	}

	public String getEndSessionEndpointUri() {
		return endSessionEndpointUri;
	}

	public void setEndSessionEndpointUri(String endSessionEndpointUri) {
		this.endSessionEndpointUri = endSessionEndpointUri;
	}

	public boolean isNonceAdded() {
		return isNonceAdded;
	}

	public void setNonceAdded(boolean nonceAdded) {
		isNonceAdded = nonceAdded;
	}

	public boolean isGenerateCodeVerifier() {
		return generateCodeVerifier;
	}

	public void setGenerateCodeVerifier(boolean generateCodeVerifier) {
		this.generateCodeVerifier = generateCodeVerifier;
	}
}
