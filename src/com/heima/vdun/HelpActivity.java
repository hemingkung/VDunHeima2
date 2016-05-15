package com.heima.vdun;

import com.heima.vdun.global.App;
import com.heima.vdun.global.GlobalConstants;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

/**
 * "帮助"页面
 * @author Kevin
 *
 */
public class HelpActivity extends Activity {

	    private WebView help_webview;
	    private ProgressBar help_pb;
	    private String help_url = GlobalConstants.HELP_URL;
	    
	    @Override
	    protected void onCreate(Bundle savedInstanceState) {
	    	super.onCreate(savedInstanceState);
	    	
	        requestWindowFeature(Window.FEATURE_NO_TITLE);
	        setContentView(R.layout.help);
	        
	        App app = (App) this.getApplication();
			app.addActivity(this);
	        
	        help_webview=(WebView)findViewById(R.id.wv_help);
	        help_pb = (ProgressBar) findViewById(R.id.pb_help);
	        help_webview.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
	        help_webview.loadUrl(help_url);
	        help_webview.setWebViewClient(new MyWebViewClient());
	    }
	    
	    private class MyWebViewClient extends WebViewClient {

	        @Override
	        public void onPageFinished(WebView view, String url) {
	            super.onPageFinished(view, url);
	            help_pb.setVisibility(View.GONE);
	        }

	        @Override
	        public boolean shouldOverrideUrlLoading(WebView view, String url) {
	            return super.shouldOverrideUrlLoading(view, url);
	        }
	    }    
	}
