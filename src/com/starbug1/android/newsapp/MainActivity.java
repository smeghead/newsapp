package com.starbug1.android.newsapp;

import java.util.ArrayList;
import java.util.List;

import me.parappa.sdk.PaRappa;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.StrictMode;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.starbug1.android.newsapp.data.DatabaseHelper;
import com.starbug1.android.newsapp.data.NewsListItem;
import com.starbug1.android.newsapp.utils.AppUtils;

public class MainActivity extends AbstractActivity {
	private static final String TAG = "MudanewsActivity";

	private List<NewsListItem> items_;
	private int page_ = 0;
	private DatabaseHelper dbHelper_ = null;
	private NewsListAdapter adapter_;
	public boolean hasNextPage = true;
	public boolean gridUpdating = false;

	private FetchFeedService fetchFeedService_;
	private boolean isBound_;
	final Handler handler_ = new Handler();

	private final ServiceConnection connection_ = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// サービスにはIBinder経由で#getService()してダイレクトにアクセス可能
			fetchFeedService_ = ((FetchFeedService.FetchFeedServiceLocalBinder) service)
					.getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			fetchFeedService_ = null;
		}
	};

	void doBindService() {
		bindService(
				new Intent(MainActivity.this, AppUtils.getServiceClass(this)),
				connection_, Context.BIND_AUTO_CREATE);
		isBound_ = true;
	}

	void doUnbindService() {
		if (isBound_) {
			unbindService(connection_);
			isBound_ = false;
		}
	}

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

	protected Class<?> getEntryActivityClass() {
		return EntryActivity.class;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");
		if (AppUtils.DEBUG) {
			StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
					.detectDiskReads().detectDiskWrites().detectAll()
					.penaltyLog().build());
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
					.detectLeakedSqlLiteObjects().penaltyLog().penaltyDeath()
					.build());
		}
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);

		setContentView(R.layout.main);
		Log.d(TAG, "setContentView");
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.window_title);

		dbHelper_ = new DatabaseHelper(this);

		doBindService();
		Log.d(TAG, "bindService");

		page_ = 0;
		hasNextPage = true;
		items_ = new ArrayList<NewsListItem>();
		adapter_ = new NewsListAdapter(this, dbHelper_);

		final String versionName = AppUtils.getVersionName(this);
		final TextView version = (TextView) this.findViewById(R.id.version);
		version.setText(versionName);

		final GridView grid = (GridView) this.findViewById(R.id.grid);
		grid.setOnItemClickListener(new NewsGridEvents.NewsItemClickListener(
				this, dbHelper_, getEntryActivityClass()));

		grid.setOnItemLongClickListener(new NewsGridEvents.NewsItemLognClickListener(
				this, dbHelper_, R.class));
		Log.d(TAG, "grid setup");

		grid.setOnScrollListener(new OnScrollListener() {
			private boolean stayBottom = false;

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				switch (scrollState) {
				// スクロールしていない
				case OnScrollListener.SCROLL_STATE_IDLE:
				case OnScrollListener.SCROLL_STATE_FLING:
					if (stayBottom) {
						Log.d(TAG, "scrollY: " + grid.getHeight());
						// load more.

						if (!MainActivity.this.gridUpdating
								&& MainActivity.this.hasNextPage) {
							updateList(++page_);
						}
					}
					break;
				}
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {

				stayBottom = (totalItemCount == firstVisibleItem
						+ visibleItemCount);
			}
		});
		Log.d(TAG, "scroll");

		// 初回起動
		if (dbHelper_.entryIsEmpty()) {
			final TextView initialMessage = (TextView) this
					.findViewById(R.id.initialMessage);
			initialMessage.setVisibility(Button.VISIBLE);
			new Thread(new Runnable() {
				@Override
				public void run() {
					for (int i = 0; i < 10; i++) {
						try {
							Thread.sleep(500);
						} catch (Exception e) {
						}
						Log.d(TAG, "service:" + isBound_);
						if (isBound_ && fetchFeedService_ != null)
							break;
					}
					handler_.post(new Runnable() {
						@Override
						public void run() {
							fetchFeeds(true);
						}
					});
				}
			}).start();
		} else {
			Log.d(TAG, "updateList start.");
			updateList(page_);
			Log.d(TAG, "updateList end.");
		}

		final NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		manager.cancelAll();
		Log.d(TAG, "notify cancel");

		parappa_ = new PaRappa(this);
		Log.d(TAG, "parappa");

		initAdditional();
		AppUtils.onCreateAditional(this);
		Log.d(TAG, "aditional");
	}

	protected void initAdditional() {

	}

	private NewsCollectTask task_ = null;

	private int column_count_ = 1;

	private void setupGridColumns() {
		final DisplayMetrics metrics = new DisplayMetrics();
		this.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		final WindowManager w = getWindowManager();
		final Display d = w.getDefaultDisplay();
		int width = (int) (d.getWidth() / metrics.scaledDensity);
		column_count_ = (int) (width / (160 / 1.5));
		final GridView grid = (GridView) this.findViewById(R.id.grid);
		grid.setNumColumns(column_count_);
	}

	@Override
	public void resetGridInfo() {
		page_ = 0;
		hasNextPage = true;
		updateList(page_);
	}

	private void updateList(int page) {

		setupGridColumns();

		if (page_ == 0) {
			adapter_.clear();
		}
		final GridView grid = (GridView) this.findViewById(R.id.grid);
		task_ = new NewsCollectTask(this, dbHelper_, grid, adapter_);
		task_.execute(String.valueOf(page));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);
		return true;
	}

	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		return super.onMenuOpened(featureId, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_update_feeds) {
			fetchFeeds(false);
		} else if (item.getItemId() == R.id.menu_settings) {
			settings();
		} else if (item.getItemId() == R.id.menu_notify_all) {
			shareAll();
		} else if (item.getItemId() == R.id.menu_review) {
			parappa_.gotoMarket();
		} else if (item.getItemId() == R.id.menu_support) {
			parappa_.startSupportActivity();
		} else if (item.getItemId() == R.id.menu_favorites) {
			Intent intent = new Intent(this, FavoriteListActivity.class);
			this.startActivity(intent);
		}
		return super.onOptionsItemSelected(item);
	}

	protected void shareAll() {
		parappa_.shareString(getResources()
				.getString(R.string.shareDescription)
				+ " #"
				+ getResources().getString(R.string.app_name), getResources()
				.getString(R.string.share));
	}

	protected void settings() {
		final Intent intent = new Intent(this, AppPrefActivity.class);
		startActivity(intent);
	}

	protected void fetchFeeds(boolean isFirst) {
		final boolean first = isFirst;
		items_.clear();
		startLoading();

		new Thread() {
			@Override
			public void run() {
				final int count = fetchFeedService_.updateFeeds(first);
				handler_.post(new Runnable() {
					@Override
					public void run() {
						final TextView initialMessage = (TextView) findViewById(R.id.initialMessage);
						initialMessage.setVisibility(TextView.GONE);

						startLoading();
						page_ = 0;
						hasNextPage = true;
						items_.clear();
						updateList(page_);
						if (count == 0) {
							Toast.makeText(
									MainActivity.this,
									getResources().getString(
											R.string.no_newer_news),
									Toast.LENGTH_LONG).show();
						} else {
							Toast.makeText(
									MainActivity.this,
									String.format(
											getResources().getString(
													R.string.n_news_added),
											count), Toast.LENGTH_LONG).show();
						}
					}
				});
			}
		}.start();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		doUnbindService();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
	}

	@Override
	protected void onPause() {
		if (task_ != null) {
			task_.progresCancel();
		}
		super.onPause();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setupGridColumns();
	}

	@Override
	public DatabaseHelper getDbHelper() {
		return dbHelper_;
	}

	@Override
	public int getGridColumnCount() {
		return this.column_count_;
	}
}
