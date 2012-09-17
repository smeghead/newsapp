package com.starbug1.android.newsapp.data;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.starbug1.android.newsapp.AppException;
import com.starbug1.android.newsapp.FetchFeedService;
import com.starbug1.android.newsapp.MainActivity;
import com.starbug1.android.newsapp.R;

public class DatabaseHelper extends SQLiteOpenHelper {
	private static final String TAG = "DatabaseHelper";
	public static final int MAX_ENTRIES_PER_PAGE = 30;

	public DatabaseHelper(Context context) {
		super(context, "mudanews.db", null, 4);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.beginTransaction();
		try {
			db.execSQL("create table feeds ( " + "  id integer primary key,"
					+ "  source text not null," + "  title text not null,"
					+ "  link text not null," + "  description text not null,"
					+ "  category text," + "  published_at datetime,"
					+ "  deleted bool not null default 0,"
					+ "  created_at datetime not null" + ")");
			db.execSQL("create table images ( " + "  id integer primary key,"
					+ "  feed_id integer not null," + "  image text,"
					+ "  created_at datetime not null" + ")");
			db.execSQL("create table view_logs ( "
					+ "  id integer primary key,"
					+ "  feed_id integer not null,"
					+ "  created_at datetime not null" + ")");
			db.execSQL("create table favorites ( "
					+ "  id integer primary key,"
					+ "  feed_id integer not null,"
					+ "  created_at datetime not null" + ")");
			db.execSQL("create index if not exists feeds_idx on feeds (id, deleted)");
			db.execSQL("create index if not exists favorites_idx on favorites (id, feed_id)");
			db.execSQL("create index if not exists images_idx on images (id, feed_id)");			
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int currentVersion, int newVersion) {
		// debug
		db.beginTransaction();
		try {
			if (currentVersion < 2 && newVersion == 2) {
				db.execSQL("create table favorites ( "
						+ "  id integer primary key,"
						+ "  feed_id integer not null,"
						+ "  created_at datetime not null" + ")");
			} else if (currentVersion < 4 && newVersion == 4) {
				db.execSQL("create index if not exists feeds_idx on feeds (id, deleted)");
				db.execSQL("create index if not exists favorites_idx on favorites (id, feed_id)");
				db.execSQL("create index if not exists images_idx on images (id, feed_id)");			
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public synchronized boolean entryIsEmpty() {
		SQLiteDatabase db = null;
		Cursor cu = null;
		try {
			db = getReadableDatabase();
			cu = db.rawQuery("select count(*) from feeds", new String[] {});
			cu.moveToFirst();
			if (cu.getInt(0) == 0) {
				Log.w("NewsParserTask", "no feed.");
				return true;
			}
			return false;
		} finally {
			if (cu != null)
				cu.close();
			if (db != null)
				db.close();
		}
	}

	public synchronized void favorite(Context context, final NewsListItem item, View v,
			final boolean add) {
		final SQLiteDatabase db = this.getWritableDatabase();
		try {
			
			db.execSQL(
					add
						? "insert into favorites (feed_id, created_at) values (?, current_timestamp)"
						: "delete from favorites where feed_id = ?",
					new String[] { String.valueOf(item.getId()) });
			item.setFavorite(add);
			if (v != null) {
				final ImageView favorite = (ImageView) v
						.findViewById(R.id.favorite);
				favorite.setImageResource(add
						? android.R.drawable.btn_star_big_on
						: android.R.drawable.btn_star_big_off);
			}
			if (add) {
				Toast.makeText(context, item.getTitle() + "をお気に入りにしました", Toast.LENGTH_LONG).show();
			}
		} catch (Exception e) {
			Log.e("MudanewsActivity", "failed to update entry action.");
		} finally {
			db.close();
		}
	}

	public synchronized int registerItems(List<NewsListItem> list, int registerCount,
			final DatabaseHelper helper, FetchFeedService service) {
		SQLiteDatabase db = helper.getWritableDatabase();
		Date now = new Date();
//		db.execSQL("delete from feeds");
//		db.execSQL("delete from images");
		
		Cursor c = null;
		try {
			for (NewsListItem item : list) {
				c = db.rawQuery("select id from feeds where link = ?", new String[]{item.getLink()});
				int count = c.getCount();
				c.close(); c = null;
				if (count > 0) continue; //同じ リンクURLのエントリがあったら、取り込まない。
				
				item = service.fetchImage(item);

				ContentValues values = new ContentValues();
		        values.put("source", item.getSource());
		        values.put("title", item.getTitle());
		        values.put("link", item.getLink());
		        values.put("description", item.getDescription());
		        values.put("category", item.getCategory());
		        values.put("published_at", item.getPublishedAt());
		        values.put("created_at", now.getTime());
		        long id = db.insert("feeds", null, values);

		        registerCount++;

		        if (item.getImage() == null) {
		        	continue;
		        }
		        values = new ContentValues();
		        values.put("feed_id", id);
		        values.put("image", item.getImage());
		        values.put("created_at", now.getTime());
		        db.insert("images", null, values);
			}
			db.close();
			return registerCount;
		} finally {
			if (c != null) c.close();
			if (db != null && db.isOpen()) db.close();
		}
	}
	
	public synchronized List<NewsListItem> getItems(final List<NewsListItem> result, MainActivity activity, int page) {
		SQLiteDatabase db = null;
		Cursor c = null;
		try {
			db = this.getReadableDatabase();

			Log.d(TAG, "doInBackground getWritableDatabase");
			c = db.rawQuery(
					"select f.id, f.title, f.description, f.link, f.source, count(v.id), fav.id " +
					"from feeds as f " +
					"left join view_logs as v on v.feed_id = f.id " +
					"left join favorites as fav on fav.feed_id = f.id " +
					"where f.deleted = 0 " +
					"group by f.id " +
 					"order by published_at desc " + 
					"limit ? " +
					"offset ?", new String[]{
							String.valueOf(MAX_ENTRIES_PER_PAGE + 1), 
							String.valueOf(page * MAX_ENTRIES_PER_PAGE)}
			);
			c.moveToFirst();
			if (c.getCount() == 0) {
				activity.hasNextPage = false;
				return result;
			}
			for (int i = 0, len = c.getCount(); i < len; i++) {
				final NewsListItem item = new NewsListItem();
				item.setId(Integer.parseInt(c.getString(0)));
				item.setTitle(c.getString(1));
				item.setDescription(c.getString(2));
				item.setLink(c.getString(3));
				item.setSource(c.getString(4));
				item.setViewCount(c.getInt(5));
				item.setFavorite(c.getInt(6) > 0);
				result.add(item);
				c.moveToNext();
			}
			for (NewsListItem item : result) {
				Cursor cu = null;
				try {
					cu = db.rawQuery("select image from images where feed_id = ?", new String[]{String.valueOf(item.getId())});
					cu.moveToFirst();
					if (cu.getCount() != 1) {
						Log.w("NewsParserTask", "no image.");
						continue;
					}
					item.setImage(cu.getBlob(0));
				} finally {
					if (cu != null) cu.close();
				}
			}
			Log.d(TAG, "doInBackground got records");
			return result;
		} catch(Exception e) {
			Log.e(TAG, e.getMessage());
			throw new AppException("failed to background task.", e);
		} finally {
			if (c != null) c.close();
			if (db != null) db.close();
		}
	}
	
	public synchronized void viewLog(final NewsListItem item) {
		final SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL(
				"insert into view_logs (feed_id, created_at) values (?, current_timestamp)",
				new String[] { String.valueOf(item.getId()) });
		db.close();
	}

	public synchronized void addFavorite(final NewsListItem item) {
		final SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL(
				"insert into favorites (feed_id, created_at) values (?, current_timestamp)",
				new String[] { String.valueOf(item.getId()) });
		db.close();
	}

	public synchronized void deleteItem(final NewsListItem item) {
		final SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL(
				"update feeds set deleted = 1 where id = ?",
				new String[] { String.valueOf(item.getId()) });
		db.close();
	}
	public synchronized List<FavoriteMonth> getFavoriteItems(
			final List<FavoriteMonth> result, final List<Date> months) {
		SQLiteDatabase db = null;
		Cursor c = null;
		try {
			db = this.getReadableDatabase();

			try {
				c = db.rawQuery(
						"select strftime('%Y-%m', fav.created_at, 'localtime') as month, count(f.id) " +
						"from favorites as fav " +
						"inner join feeds as f on fav.feed_id = f.id " + 
						"where f.deleted = 0 " + 
						"group by month " +
						"order by month desc",
						new String[0]
				);
				c.moveToFirst();
				if (c.getCount() == 0) {
					return result;
				}
				for (int i = 0, len = c.getCount(); i < len; i++) {
					String month = c.getString(0);
					Log.d(TAG, month + ":" + c.getInt(1));
					String[] dates = month.split("-");
					Calendar cal = Calendar.getInstance();
					cal.set(Integer.parseInt(dates[0], 10), Integer.parseInt(dates[1], 10) - 1, 1, 0, 0, 0);
					months.add(cal.getTime());
					c.moveToNext();
				}
			} finally {
				c.close();
			}

			final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
			
			//  月毎にお気に入りを取得する。
			for (Date begin : months) {
				final List<NewsListItem> items = new ArrayList<NewsListItem>();
				final Calendar cal = Calendar.getInstance();
				cal.setTime(begin);
				cal.add(Calendar.MONTH, 1);
				final Date end = new Date(cal.getTimeInMillis());
				Log.d(TAG, "between " + begin + " and " + end);
				
				try {
					c = db.rawQuery(
							"select f.id, f.title, f.description, f.link, f.source, count(v.id), fav.id " +
							"from feeds as f " +
							"left join view_logs as v on v.feed_id = f.id " +
							"left join favorites as fav on fav.feed_id = f.id " +
							"where f.deleted = 0 and fav.created_at between ? and ? " +
							"group by f.id " +
		 					"order by fav.created_at desc ", new String[]{
									String.valueOf(dateFormat.format(begin)), 
									String.valueOf(dateFormat.format(end))}
					);
					c.moveToFirst();
					if (c.getCount() == 0) {
						continue;
					}
					for (int i = 0, len = c.getCount(); i < len; i++) {
						final NewsListItem item = new NewsListItem();
						item.setId(Integer.parseInt(c.getString(0)));
						item.setTitle(c.getString(1));
						item.setDescription(c.getString(2));
						item.setLink(c.getString(3));
						item.setSource(c.getString(4));
						item.setViewCount(c.getInt(5));
						item.setFavorite(c.getInt(6) > 0);
						items.add(item);
						c.moveToNext();
					}
				} finally {
					c.close();
				}
				for (NewsListItem item : items) {
					Cursor cu = null;
					try {
						cu = db.rawQuery("select image from images where feed_id = ?", new String[]{String.valueOf(item.getId())});
						cu.moveToFirst();
						if (cu.getCount() != 1) {
							Log.w("NewsParserTask", "no image.");
							continue;
						}
						item.setImage(cu.getBlob(0));
					} finally {
						if (cu != null) cu.close();
					}
				}
				result.add(new FavoriteMonth(begin, items));
			}
			
			return result;
		} catch(Exception e) {
			Log.e("NewsParserTask", e.getMessage());
			throw new AppException("failed to background task.", e);
		} finally {
			if (c != null) c.close();
			if (db != null) db.close();
		}
	}
}