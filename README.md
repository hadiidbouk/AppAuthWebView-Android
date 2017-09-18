# AppAuthWebView Android

Using [AppAuth-Android](https://github.com/openid/AppAuth-Android) with webview .

![](https://i.imgur.com/iTmRs0p.png)


## Installation


### Add the library to your dependencies

```compile 'com.github.hadiidbouk:AppAuthWebView-Android:1.0.0'```

**Also add this in your build.gradle (app)**

```
android {
   ...
    defaultConfig {
       ....
        manifestPlaceholders = [
                'appAuthRedirectScheme': ''
        ]

    }
 }
 ```
 
**And in the project build.gradle**


```allprojects {
      repositories {
            .. 
            maven { url 'https://jitpack.io' }
            }
     }
```

1. Create your layouts

```
<RelativeLayout
        	xmlns:android="http://schemas.android.com/apk/res/android"
        	xmlns:tools="http://schemas.android.com/tools"
        	android:layout_width="match_parent"
        	android:layout_height="match_parent">
        
        	<WebView
        		android:id="@+id/WebView"
        		android:layout_width="match_parent"
        		android:layout_height="match_parent"/>
        
        	<FrameLayout
        		android:id="@+id/LoadingLayout"
        		android:layout_width="match_parent"
        		android:layout_height="match_parent"
        		android:background="@android:color/holo_green_dark">
        		<TextView
        			android:layout_width="match_parent"
        			android:layout_height="wrap_content"
        			android:textColor="#FFF"
        			android:text="Loading .."
        			android:textSize="30sp"
        			android:layout_gravity="center"
        			android:gravity="center"
        			/>
        	</FrameLayout>
        
        	<FrameLayout
        		android:id="@+id/ErrorLayout"
        		android:layout_width="match_parent"
        		android:layout_height="match_parent"
        		android:background="@android:color/holo_red_dark"
        		>
        
        		<TextView
        			android:layout_width="match_parent"
        			android:layout_height="wrap_content"
        			android:text="Error"
        			android:textColor="#FFF"
        			android:textSize="30sp"
        			android:layout_gravity="center"
        			android:gravity="center"
        			/>
        	</FrameLayout>
        
        </RelativeLayout>
 ```
- Error Layout will be visible when there is a error received in webview .
- Loading Layout will be visible when waiting the page to be loaded.

2. Create your **AppAuthWebViewData** 

```
  AppAuthWebViewData data = new AppAuthWebViewData();
        		data.setClientId(");
        		data.setAuthorizationEndpointUri("");
        		data.setClientSecret("");
        		data.setDiscoveryUri("n");
        		data.setRedirectLoginUri("");
        		data.setRedirectLogoutUri("");
        		data.setScope("");
        		data.setTokenEndpointUri("");
        		data.setRegistrationEndpointUri("");
        		data.setResponseType("");
```


3. Implement **IAppAuthWebViewListener**

```
 public class MainActivity extends Activity implements IAppAuthWebViewListener {
            	.....
        
        @Override public void onUserAuthorize(AuthState authState) {
        		Toast.makeText(this, "Token : " + authState.getIdToken(), Toast.LENGTH_SHORT).show();
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
        }
    
  ```
  
  4. Perform Login Request 

```
     @Override
        	protected void onCreate(Bundle savedInstanceState) {
        		super.onCreate(savedInstanceState);
        		setContentView(R.layout.activity_main);
        
        	....
    
    		AppAuthWebView appAuthWebView = new AppAuthWebView
    			.Builder()
    			.webView(webView)
    			.authData(data)
    			.listener(this)
    			.build();
    
    		appAuthWebView.performLoginRequest();
    	}
```


### Working with tokens and refresh token

To access the id token or the access token you can simply call the static method  **AppAuthWebView.getAuthState(Context context)** to get the authstate object (already saved in shared preferences) and then access what you want in this object.


To Perform a refresh token request you can simply call the static method **AppAuthWebView.peroformRefreshTokenRequest(final Context context, AuthState authState, AppAuthWebViewData data)** and if the request success the AuthState will be updated in the shared preferences.



### If you are using a service in another process 

If you want to get the access token from this service you can do the following :

1. Create your class extends BroadcastReceiver ( example in Kotlin ) :

``` 
 override fun onReceive(p0: Context?, p1: Intent?) {

        if(p1!!.action.equals(AppAuthWebView.BROADCAST_RECEIVER_ACTION)) {
            val token: String = p1.getStringExtra(AppAuthWebView.ACCESS_TOKEN)
            
            //do what you want here 
            MyApp.TOKEN = token
        }
    }

```

2. Register to the broadcast by adding this to your manifest :

```
		<receiver android:name=".MyBroadcastReceiver">
			<intent-filter>
				<action android:name="com.hadiidbouk.AppAuthWebView.AccessTokenAction"/>
			</intent-filter>
		</receiver>
```

