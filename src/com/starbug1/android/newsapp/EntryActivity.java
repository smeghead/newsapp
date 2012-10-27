package com.starbug1.android.newsapp;

import java.util.Timer;
import java.util.TimerTask;

import me.parappa.sdk.PaRappa;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.starbug1.android.newsapp.data.DatabaseHelper;
import com.starbug1.android.newsapp.data.NewsListItem;
import com.starbug1.android.newsapp.utils.AppUtils;
import com.starbug1.android.newsapp.utils.UrlUtils;

public class EntryActivity extends Activity {
	private static final String TAG = "EntryActivity";
	final Handler handler_ = new Handler();
	private NewsListItem currentItem_ = null;
	private DatabaseHelper dbHelper_ = null;
	private PaRappa parappa_;
	private WebView webview_;

	ProgressBar loading_ = null;

	private void setLoading(boolean start) {
		if (loading_ == null) {
			loading_ = (ProgressBar) findViewById(R.id.loading);
			if (loading_ == null) {
				Log.e(TAG, "no loading progressbar.");
				return;
			}
		}
		loading_.setVisibility(start ? ProgressBar.VISIBLE
				: ProgressBar.INVISIBLE);
	}

	public void startLoading() {
		setLoading(true);
	}

	public void stopLoading() {
		setLoading(false);
	}

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (AppUtils.DEBUG) {
			StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
					.detectDiskReads().detectDiskWrites().detectAll()
					.penaltyLog().build());
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
					.detectLeakedSqlLiteObjects().penaltyLog().penaltyDeath()
					.build());
		}
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);

		setContentView(R.layout.entry);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.window_title);

		dbHelper_ = new DatabaseHelper(this);

		startLoading();

		final String versionName = AppUtils.getVersionName(this);
		final TextView version = (TextView) this.findViewById(R.id.version);
		version.setText(versionName);

		final Intent intent = getIntent();
		currentItem_ = (NewsListItem) intent.getSerializableExtra("item");

		webview_ = (WebView) this.findViewById(R.id.entryView);

		webview_.setWebViewClient(new WebViewClient() {

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				final WebView v = view;
				Log.d("NewsDetailActivity", "onPageStarted url: " + url);
				final Timer timer = new Timer();
				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						Log.i(TAG, "timer taks");
						if (v.getContentHeight() > 0) {
							handler_.post(new Runnable() {
								@Override
								public void run() {
									stopLoading();
								}
							});
							timer.cancel();
						}
					}
				}, 1000, 1000);
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if (!url.startsWith("file")
						&& !UrlUtils.isSameDomain(view.getOriginalUrl(), url)) {
					final Intent intent = new Intent(Intent.ACTION_VIEW, Uri
							.parse(url));
					startActivity(intent);
					return true;
				}
				Log.d("NewsDetailActivity", "shouldOverrideUrlLoading url: "
						+ url);
				return super.shouldOverrideUrlLoading(view, url);
			}
		});
		final ImageView share = (ImageView) findViewById(R.id.image_share);
		share.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				parappa_.shareString(currentItem_.getTitle() + " "
						+ currentItem_.getLink() + " #"
						+ getResources().getString(R.string.app_name), "共有");
			}
		});

		final WebSettings ws = webview_.getSettings();
		ws.setBuiltInZoomControls(true);
		ws.setLoadWithOverviewMode(true);
		ws.setPluginsEnabled(true);
		ws.setUseWideViewPort(true);
		ws.setJavaScriptEnabled(true);
		ws.setAppCacheMaxSize(1024 * 1024 * 64); // 64MB
		ws.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
		ws.setDomStorageEnabled(true);
		ws.setAppCacheEnabled(true);
		webview_.setVerticalScrollbarOverlay(true);
		webview_.loadUrl(UrlUtils.mobileUrl(
				currentItem_.getLink(),
				this.getResources().getStringArray(
						R.array.arrays_mobile_url_orgin), this.getResources()
						.getStringArray(R.array.arrays_mobile_url_repleace)));
		startLoading();

		parappa_ = new PaRappa(this);

		initAdditional();
		AppUtils.onCreateAditional(this);
	}

	protected void initAdditional() {

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.entrymenu, menu);
		return true;
	}

	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		return super.onMenuOpened(featureId, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_reload) {
			WebView entryView = (WebView) this.findViewById(R.id.entryView);
			startLoading();
			entryView.reload();
		} else if (item.getItemId() == R.id.menu_share) {
			share();
		} else if (item.getItemId() == R.id.menu_notify_all) {
			shareAll();
		} else if (item.getItemId() == R.id.menu_review) {
			parappa_.gotoMarket();
		} else if (item.getItemId() == R.id.menu_favorite) {
			favorite();
		} else if (item.getItemId() == R.id.menu_support) {
			parappa_.startSupportActivity();
		}
		return super.onOptionsItemSelected(item);
	}

	protected void favorite() {
		if (currentItem_ == null)
			return;
		Log.d(TAG, "favorite id:" + currentItem_.getId());

		dbHelper_.favorite(this, currentItem_, null, true);
	}

	protected void share() {
		if (currentItem_ == null) {
			return;
		}
		parappa_.shareString(
				currentItem_.getTitle() + " " + currentItem_.getLink() + " #"
						+ getResources().getString(R.string.app_name),
				getResources().getString(R.string.share));
	}

	protected void shareAll() {
		parappa_.shareString(getResources()
				.getString(R.string.shareDescription)
				+ " #"
				+ getResources().getString(R.string.app_name), getResources()
				.getString(R.string.share));
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
			if (event.getAction() == KeyEvent.ACTION_DOWN) {
				Log.d(TAG, "dispatchKeyEvent");
				Log.d(TAG,
						"dispatchKeyEvent cGeanGoBack:" + webview_.canGoBack());
				if (webview_.canGoBack()) {
					webview_.goBack();
					return true;
				}
			}
		}
		return super.dispatchKeyEvent(event);
	}

}
