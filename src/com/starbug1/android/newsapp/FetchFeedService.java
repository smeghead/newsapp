/**
 * 
 */
package com.starbug1.android.newsapp;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParser;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.util.Xml;

import com.starbug1.android.newsapp.data.DatabaseHelper;
import com.starbug1.android.newsapp.data.NewsListItem;
import com.starbug1.android.newsapp.utils.AppUtils;
import com.starbug1.android.newsapp.utils.FileDownloader;
import com.starbug1.android.newsapp.utils.FlushedInputStream;
import com.starbug1.android.newsapp.utils.InternetStatus;
import com.starbug1.android.newsapp.utils.MetaDataUtil;
import com.starbug1.android.newsapp.utils.UrlUtils;

/**
 * @author smeghead
 * 
 */
public abstract class FetchFeedService extends Service {
	public static final String ACTION = "mudanews fetch feed Service";
	private static final String TAG = "FetchFeedService";
	private boolean isRunning = false;
	SharedPreferences sharedPreferences_ = null;

	protected abstract List<Feed> getFeeds();

	protected static abstract class Feed {
		public String name = "";
		public String url = "";
		public Pattern imageUrlIgnorePattern;

		public Feed(String name, String url) {
			this.name = name;
			this.url = url;
		}

		public Feed(String name, String url, String imageUrlIgnorePattern) {
			this.name = name;
			this.url = url;
			this.imageUrlIgnorePattern = Pattern.compile(imageUrlIgnorePattern);
		}

		public abstract String getImageUrl(String content, NewsListItem item);
	}

	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate");
		Log.d(TAG, "resource: " + this.getResources().getClass().toString());

		sharedPreferences_ = PreferenceManager
				.getDefaultSharedPreferences(this);
		final int clowlIntervals = Integer.parseInt(sharedPreferences_
				.getString("clowl_intervals", "60"));
		AppUtils.updateClowlIntervals(this, clowlIntervals);
		super.onCreate();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Log.d(TAG, "onStart");

		new Thread(new Runnable() {
			@Override
			public void run() {
				fetchFeeds();
			}
		}).start();

		super.onStart(intent, startId);
	}

	protected Class<?> getMainActivity() {
		return MainActivity.class;
	}

	private void fetchFeeds() {
		if (isRunning)
			return;

		final int interval = Integer.parseInt(sharedPreferences_.getString(
				"clowl_intervals", "60"));
		if (interval == 0) {
			Log.d(TAG, "not need to clowl.");
			return;
		}
		final boolean nightClowl = sharedPreferences_.getBoolean(
				"pref_night_clowl", false);
		final int hour = new Date().getHours();
		if (!nightClowl && hour < 7) {
			Log.d(TAG, "this is night. zzz");
			return;
		}

		if (!isRunning) {
			try {
				isRunning = true;

				Log.d(TAG, "fetch feeds start.....");
				final int count = updateFeeds(false);

				if (count > 0) {
					final NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
					final Notification notification = new Notification(
							MetaDataUtil.getApplicationIconId(this),
							getResources().getString(R.string.news_has_arrived),
							System.currentTimeMillis());
					if (sharedPreferences_
							.getBoolean("pref_notify_sound", true)) {
						notification.sound = Settings.System.DEFAULT_NOTIFICATION_URI;
					}

					final Intent intent = new Intent(FetchFeedService.this,
							getMainActivity());
					final PendingIntent contentIntent = PendingIntent
							.getActivity(FetchFeedService.this, 0, intent, 0);
					notification.setLatestEventInfo(
							getApplicationContext(),
							getResources().getString(R.string.app_name),
							String.format(
									getResources().getString(
											R.string.n_news_added), count),
							contentIntent);
					manager.notify(R.string.app_name, notification);
				}
			} finally {
				isRunning = false;
			}
		}
	}

	private class FeedTask implements Callable<Integer> {

		public final Feed feed;
		public final String url;

		public FeedTask(Feed feed, String url) {
			this.feed = feed;
			this.url = url;
		}

		@Override
		public Integer call() throws Exception {
			return Integer.valueOf(registerNews(parseXml(this.feed, this.url)));
		}

	}

	private static boolean isWorking = false;

	public int updateFeeds(boolean isFirst) {
		if (!InternetStatus.isConnected(getApplicationContext())) {
			Log.i(TAG, "no connection.");
			return 0;
		}
		if (isWorking) {
			Log.i(TAG, "already working another task.");
			return 0;
		}
		int count = 0;
		final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(
				10);
		final ThreadPoolExecutor exec = new ThreadPoolExecutor(5, 5, 0,
				TimeUnit.MILLISECONDS, queue);

		// 結果を受け取るリスト
		final List<Future<Integer>> result = new ArrayList<Future<Integer>>();

		try {
			isWorking = true;
			// キューへの順次追加
			try {
				for (Feed f : this.getFeeds()) {
					if (queue.remainingCapacity() > 0) {
						Log.d(TAG, "register feedtask:" + f.name);
						result.add(exec.submit(new FeedTask(f, f.url)));
					} else {
						Log.d(TAG, "waiting:" + f.name);
						Thread.sleep(1000);
					}
					// 初回は1サイトだけを登録するので、ここでbreak
					if (isFirst)
						break;
				}
			} catch (InterruptedException e) {
				Log.e(TAG, "queue error " + e.getMessage());
				throw new RuntimeException(e);
			} finally {
				exec.shutdown();
			}

			// 結果表示
			for (int i = 0; i < result.size(); i++) {
				try {
					Log.d(TAG, "retrieve result");
					count += result.get(i).get();
				} catch (InterruptedException e) {
					Log.e(TAG, "queue error " + e.getMessage());
					throw new RuntimeException(e);
				} catch (ExecutionException e) {
					Log.e(TAG, "queue error " + e.getMessage());
					throw new RuntimeException(e);
				}
			}
			Log.d(TAG, "fetched");
		} catch (Exception e) {
			Log.e("NewsParserTask", e.toString());
			throw new AppException("failed to background task.", e);
		} finally {
			isWorking = false;
		}
		return count;
	}

	private Feed getFeed(String source) {
		for (Feed f : this.getFeeds()) {
			if (f.name.equals(source)) {
				return f;
			}
		}
		throw new AppException("invalid feed");
	}

	protected boolean isValidItem(NewsListItem item) {
		if (item.getTitle().toString().indexOf("［PR］") == -1
				&& item.getTitle().toString().indexOf("PR:") == -1
				&& item.getTitle().toString().indexOf("ヘッドラインニュース") == -1) {
			return true;
		}
		return false;
	}

	private List<NewsListItem> parseXml(Feed feed, String urlString) {
		final XmlPullParser parser = Xml.newPullParser();
		final List<NewsListItem> list = new ArrayList<NewsListItem>(20);

		InputStream is = null;
		try {
			final URL url = new URL(urlString);
			final URLConnection conn = url.openConnection();
			conn.setConnectTimeout(1000 * 5);
			is = conn.getInputStream();

			parser.setInput(is, null);
			int eventType = parser.getEventType();
			NewsListItem currentItem = null;
			DOC: while (eventType != XmlPullParser.END_DOCUMENT) {
				String tag = null;
				switch (eventType) {
				case XmlPullParser.START_TAG:
					tag = parser.getName();
					if (tag.equals("item")) {
						currentItem = new NewsListItem();
						currentItem.setSource(feed.name);
					} else if (currentItem != null) {
						if (tag.equals("title")) {
							currentItem.setTitle(parser.nextText());
						} else if (tag.equals("description")) {
							String text = parser.nextText();
							currentItem.setDescription(text);
							if (currentItem.getImageUrl() == null) {
								String imageUrl = pickupUrl(text);
								if (imageUrl.length() > 0) {
									if (feed.imageUrlIgnorePattern == null
											|| !feed.imageUrlIgnorePattern
													.matcher(imageUrl).find()) {
										currentItem.setImageUrl(imageUrl);
									}
								}
							}
						} else if (tag.equals("link")) {
							currentItem.setLink(parser.nextText());
						} else if (tag.equals("subject")) {
							currentItem.setCategory(parser.nextText());
						} else if (tag.equals("encoded")) {
							if (currentItem.getImageUrl() == null) {
								String imageUrl = pickupUrl(parser.nextText());
								if (imageUrl.length() > 0) {
									if (feed.imageUrlIgnorePattern == null
											|| !feed.imageUrlIgnorePattern
													.matcher(imageUrl).find()) {
										currentItem.setImageUrl(imageUrl);
									}
								}
							}
						} else if (tag.equals("date")) {
							String dateExp = parser.nextText();
							currentItem.setPublishedAt(parseDate(dateExp));
						} else if (tag.equals("pubDate")) {
							String dateExp = parser.nextText();
							currentItem.setPublishedAt(parseDate(dateExp));
						} else if (tag.equals("thumbnail")) {
							for (int i = 0; i < parser.getAttributeCount(); i++) {
								if (parser.getAttributeName(i).equals("url")) {
									String imageUrl = parser
											.getAttributeValue(i);
									if (feed.imageUrlIgnorePattern == null
											|| !feed.imageUrlIgnorePattern
													.matcher(imageUrl).find()) {
										currentItem.setImageUrl(imageUrl);
										break;
									}
								}
							}
						}
					}

					break;
				case XmlPullParser.END_TAG:
					tag = parser.getName();
					if (tag.equals("item")) {
						if (this.isValidItem(currentItem)) {
							list.add(currentItem);
							if (list.size() > 10) {
								break DOC;
							}
						}
					}
					break;
				}
				eventType = parser.next();
			}
		} catch (Exception e) {
			Log.e("NewsParserTask", e.getMessage());
			// 処理は継続する
		} finally {
			try {
				if (is != null)
					is.close();
			} catch (Exception e) {
			}
		}
		return list;
	}

	private String pickupUrl(String src) {
		final Pattern p = Pattern.compile("<img.*src=\"([^\"]*)\"",
				Pattern.MULTILINE);
		final Matcher m = p.matcher(src);
		if (!m.find()) {
			return "";
		}
		return m.group(1);
	}

	private long parseDate(String src) {
		Date d = null;
		try {
			final SimpleDateFormat format = new SimpleDateFormat(
					"yyyy-MM-dd'T'HH:mm:ssZ");
			d = format.parse(src);
		} catch (Exception e) {
			Log.d(TAG, e.getMessage());
		}
		if (d != null) {
			return d.getTime();
		}
		try {
			final SimpleDateFormat format = new SimpleDateFormat(
					"EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
			d = format.parse(src);
		} catch (Exception e) {
			Log.d(TAG, e.getMessage());
		}
		if (d != null) {
			return d.getTime();
		}
		Log.e(TAG, "failed to parseDate : " + src);
		return new Date().getTime();
	}

	public NewsListItem fetchImage(NewsListItem item) { // //OutOfMemory
														// 対策として同期化した。
		Log.d(TAG, "id:" + item.getId() + " title: " + item.getTitle());
		Log.d(TAG, "link:" + item.getLink());
		String imageUrl = item.getImageUrl();
		Log.d(TAG, "imageUrl:" + imageUrl);
		if (imageUrl == null || imageUrl.length() == 0) {
			Log.d(TAG, "imageUrl is empty");
			try {
				// GIGAZINE はfeedにimageのURLが無いので直に取得しにいく。
				String content = FileDownloader.download(item.getLink());

				final Feed feed = this.getFeed(item.getSource());
				imageUrl = feed.getImageUrl(content, item);
				if (imageUrl == null) {
					Log.d(TAG, "imageUrl dose not exist. gave up");
					return item;
				}
			} catch (AppException e) {
				Log.e(TAG, "failed to download content.");
				return item;
			}
		}
		InputStream is = null;
		FlushedInputStream fis = null;
		try {
			final URL url = new URL(imageUrl);
			URLConnection urlConn = url.openConnection();
			urlConn.setRequestProperty("Referer",
					UrlUtils.findSchemaDomain(imageUrl));
			is = urlConn.getInputStream();
			fis = new FlushedInputStream(is);
			final Bitmap image = BitmapFactory.decodeStream(fis);

			int heightOrg = image.getHeight(), widthOrg = image.getWidth();
			int height = 0, width = 0;
			int x = 0, y = 0;

			height = width = Math.min(widthOrg, heightOrg);

			if (heightOrg > widthOrg) {
				// 縦長
				y = Math.abs(heightOrg - widthOrg) / 2;
			} else {
				// 横長
				x = Math.abs(heightOrg - widthOrg) / 2;
			}

			final Matrix matrix = new Matrix();
			matrix.postScale(250f / width, 250f / height);
			final Bitmap scaledBitmap = Bitmap.createBitmap(image, x, y, width,
					height, matrix, true);
			final ByteArrayOutputStream stream = new ByteArrayOutputStream();
			try {
				scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream);
				item.setImage(stream.toByteArray());
				Log.d(TAG, "setImage");
			} finally {
				stream.close();
			}
		} catch (Exception e) {
			Log.e(TAG, "[Exception] failed to get image." + e.getMessage()
					+ " " + imageUrl);
		} catch (OutOfMemoryError e) {
			Log.e(TAG, "[Exception] failed to get image." + e.getMessage()
					+ " " + imageUrl);
		} finally {
			try {
				if (fis != null)
					fis.close();
				if (is != null)
					is.close();
			} catch (Exception e) {
			}
		}
		return item;
	}

	private int registerNews(List<NewsListItem> list) {
		int registerCount = 0;
		final DatabaseHelper helper = new DatabaseHelper(this);
		return helper.registerItems(list, registerCount, helper, this);
	}

	public class FetchFeedServiceLocalBinder extends Binder {
		// サービスの取得
		public FetchFeedService getService() {
			return FetchFeedService.this;
		}
	}

	// Binderの生成
	private final IBinder binder_ = new FetchFeedServiceLocalBinder();

	@Override
	public IBinder onBind(Intent intent) {
		Log.i(TAG, "onBind" + ": " + intent);
		return binder_;
	}

	@Override
	public void onRebind(Intent intent) {
		Log.i(TAG, "onRebind" + ": " + intent);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.i(TAG, "onUnbind" + ": " + intent);

		// onUnbindをreturn trueでoverrideすると次回バインド時にonRebildが呼ばれる
		return true;
	}
}
