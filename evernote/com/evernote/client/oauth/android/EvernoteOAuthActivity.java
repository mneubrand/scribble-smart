/*
 * Copyright 2012 Evernote Corporation
 * All rights reserved. 
 * 
 * Redistribution and use in source and binary forms, with or without modification, 
 * are permitted provided that the following conditions are met:
 *  
 * 1. Redistributions of source code must retain the above copyright notice, this 
 *    list of conditions and the following disclaimer.
 *     
 * 2. Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution.
 *  
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.evernote.client.oauth.android;

import org.scribe.builder.ServiceBuilder;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.evernote.client.oauth.EvernoteApi;
import com.evernote.client.oauth.EvernoteAuthToken;
import com.evernote.client.oauth.EvernoteSandboxApi;

/**
 * An Android Activity for authenticating to Evernote using OAuth.
 * Third parties should not need to use this class directly.
 */
public class EvernoteOAuthActivity extends Activity {

	private static final String TAG = "EvernoteOAuthActivity";

	static final String EXTRA_EVERNOTE_HOST = "EVERNOTE_HOST";
	static final String EXTRA_CONSUMER_KEY = "CONSUMER_KEY";
	static final String EXTRA_CONSUMER_SECRET = "CONSUMER_SECRET";

	static final String EXTRA_AUTH_TOKEN = "AUTH_TOKEN";
	static final String EXTRA_NOTESTORE_URL = "NOTESTORE_URL";
	static final String EXTRA_WEBAPI_URL_PREFIX = "WEBAPI_URL_PREFIX";
	static final String EXTRA_USERID = "USER_ID";

	private String evernoteHost = null;
	private String consumerKey = null;
	private String consumerSecret = null;
	private String requestToken = null;
	private String requestTokenSecret = null;

	static EvernoteAuthToken authToken = null;

	// Activity state variables
	static boolean startedAuthentication;
	private boolean receivedCallback = false;

	private WebView webView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			evernoteHost = savedInstanceState.getString("evernoteHost");
			consumerKey = savedInstanceState.getString("consumerKey");
			consumerSecret = savedInstanceState.getString("consumerSecret");
			requestToken = savedInstanceState.getString("requestToken");
			requestTokenSecret = savedInstanceState.getString("requestTokenSecret");
			startedAuthentication = savedInstanceState.getBoolean("startedAuthentication");
		}

		if (consumerKey == null) {
			Intent intent = getIntent();
			evernoteHost = intent.getStringExtra(EXTRA_EVERNOTE_HOST);
			consumerKey = intent.getStringExtra(EXTRA_CONSUMER_KEY);
			consumerSecret = intent.getStringExtra(EXTRA_CONSUMER_SECRET);
		}

		//Display webview
//		setTheme(android.R.style.Theme_NoDisplay);

		startedAuthentication = false;
		super.onCreate(savedInstanceState);


		// Let's display the progress in the activity title bar, like the
		// browser app does.
		getWindow().requestFeature(Window.FEATURE_PROGRESS);

		webView = new WebView(this);
		webView.setWebChromeClient(new WebChromeClient() {
			public void onProgressChanged(WebView view, int progress) {
				// Activities and WebViews measure progress with different scales.
				// The progress meter will automatically disappear when we reach 100%
				setProgress(progress * 1000);
			}
		});
		webView.setWebViewClient(new WebViewClient() {

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if (url.startsWith("en-neiti")) {
					authToken = completeAuth(Uri.parse(url));
					receivedCallback = true;
					finish();
					return true;
				}
				return super.shouldOverrideUrlLoading(view, url);
			}

		});
		webView.getSettings().setJavaScriptEnabled(true);
		webView.getSettings().setLoadsImagesAutomatically(true);
		LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		setContentView(webView, params);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putString("evernoteHost", evernoteHost);
		outState.putString("consumerKey", consumerKey);
		outState.putString("consumerSecret", consumerSecret);
		outState.putString("requestToken", requestToken);
		outState.putString("requestTokenSecret", requestTokenSecret);
		outState.putBoolean("startedAuthentication", startedAuthentication);
	}

	@Override
	protected void onResume() {
		super.onResume();

		if ((evernoteHost == null) || (consumerKey == null) || (consumerSecret == null)) {
			finish();
			return;
		}

		if (!startedAuthentication) {
			beginAuthentication();
			startedAuthentication = true;
		} else if (!receivedCallback) {
			authToken = null;
			finish();
			return;
		}
	}

	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Uri uri = intent.getData();
		if ((uri != null) && uri.getScheme().equals(getCallbackScheme())) {
			authToken = completeAuth(uri);
			receivedCallback = true;
			finish();
		}
	}

	private String getCallbackScheme() {
		return "en-" + consumerKey;
	}

	private OAuthService createService() {
		Class apiClass = EvernoteApi.class;
		if (evernoteHost.equals(EvernoteSandboxApi.evernoteHost)) {
			apiClass = EvernoteSandboxApi.class;
		}
		return new ServiceBuilder().provider(apiClass).apiKey(consumerKey).apiSecret(consumerSecret)
				.callback(getCallbackScheme() + "://callback").build();
	}

	/**
	 * Get a request token from the Evernote web service and send the user to a browser to authorize access.
	 */
	private void beginAuthentication() {
		try {
			OAuthService service = createService();
			Log.i(TAG, "Retrieving OAuth request token...");
			Token requestToken = service.getRequestToken();
			this.requestToken = requestToken.getToken();
			this.requestTokenSecret = requestToken.getSecret();

			// Open a browser to allow the user to authorize access to their account
			Log.i(TAG, "Redirecting user for authorization...");
			String url = service.getAuthorizationUrl(requestToken);
			
			//Don't open a browser but use internal web view instead
//			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
//			startActivity(intent);
			webView.loadUrl(url);
		} catch (OAuthException oax) {
			// TODO communicate this back to the caller
			Log.e(TAG, "Failed to obtain OAuth request token", oax);
			finish();
		} catch (Exception ex) {
			Log.e(TAG, "Failed to obtain OAuth request token", ex);
			finish();
		}
	}

	/**
	 * Called when the user authorizes or denies access and is redirected back to our app. If the user authorized
	 * access, this method exchanges our temporary request token for an access token that can be used to make
	 * authenticated API calls.
	 * 
	 * @param uri
	 *            The callback URL, which contains the mandatory oauth_verifier.
	 */
	private EvernoteAuthToken completeAuth(Uri uri) {
		EvernoteAuthToken accessToken = null;

		if (requestToken != null) {
			OAuthService service = createService();
			String verifierString = uri.getQueryParameter("oauth_verifier");
			if (verifierString == null || verifierString.length() == 0) {
				Log.i(TAG, "User did not authorize access");
			} else {
				Verifier verifier = new Verifier(verifierString);
				Log.i(TAG, "Retrieving OAuth access token...");
				try {
					Token reqToken = new Token(requestToken, requestTokenSecret);
					accessToken = (EvernoteAuthToken) service.getAccessToken(reqToken, verifier);
				} catch (OAuthException oax) {
					Log.e(TAG, "Failed to obtain OAuth access token", oax);
				} catch (Exception ex) {
					Log.e(TAG, "Failed to obtain OAuth access token", ex);
				}
			}
		} else {
			Log.d(TAG, "Unable to retrieve OAuth access token, no request token");
		}

		return accessToken;
	}
}
