package com.starbug1.android.newsapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.Toast;

import com.starbug1.android.newsapp.data.DatabaseHelper;
import com.starbug1.android.newsapp.data.NewsListItem;

public class NewsGridEvents {

	public static class NewsItemClickListener implements OnItemClickListener {

		private final AbstractActivity activity_;
		private final Class<?> entryClass_;
		private final DatabaseHelper dbHelper_;

		public NewsItemClickListener(AbstractActivity activity,
				DatabaseHelper dbHelper, Class<?> entryClass) {
			activity_ = activity;
			dbHelper_ = dbHelper;
			entryClass_ = entryClass;
		}

		@Override
		public void onItemClick(AdapterView<?> adapter, View view,
				int position, long id) {
			final NewsListItem item = (NewsListItem) adapter
					.getItemAtPosition(position);

			dbHelper_.viewLog(item);

			item.setViewCount(item.getViewCount() + 1);
			final ImageView newIcon = (ImageView) view
					.findViewById(R.id.newEntry);
			newIcon.setVisibility(ImageView.GONE);

			final Intent entryIntent = new Intent(activity_, entryClass_);
			entryIntent.putExtra("item", item);
			activity_.startActivity(entryIntent);

		}
	}

	public static class NewsItemLognClickListener implements
			AdapterView.OnItemLongClickListener {
		private static final String TAG = "NewsItemLognClickListener";
		private final AbstractActivity activity_;
		private final DatabaseHelper dbHelper_;

		public NewsItemLognClickListener(AbstractActivity activity,
				DatabaseHelper dbHelper, Class<?> resourceClass) {
			activity_ = activity;
			dbHelper_ = dbHelper;
		}

		@Override
		public boolean onItemLongClick(AdapterView<?> adapter, View viewArg,
				int position, long arg3) {
			final View v = viewArg;
			final NewsListItem item = (NewsListItem) adapter
					.getItemAtPosition(position);
			// Integer item_index = (Integer)v.getTag() - 1;
			final AlertDialog.Builder ad = new AlertDialog.Builder(activity_);
			ad.setTitle(activity_.getResources().getString(
					R.string.actions_of_entry));
			ad.setItems(R.array.arrays_entry_actions,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Log.d("NewsListAdapter",
									"longclickmenu selected id:" + item.getId());
							final String processName = activity_.getResources()
									.getStringArray(
											R.array.arrays_entry_action_values)[which];
							try {
								if ("share".equals(processName)) {
									// 共有
									activity_.parappa_.shareString(
											item.getTitle()
													+ " "
													+ item.getLink()
													+ " #"
													+ activity_
															.getResources()
															.getString(
																	R.string.app_name),
											"共有");
								} else if ("make_favorite".equals(processName)) {
									// お気に入り
									dbHelper_.addFavorite(item);
									item.setFavorite(true);
									final ImageView favorite = (ImageView) v
											.findViewById(R.id.favorite);
									favorite.setImageResource(android.R.drawable.btn_star_big_on);
									Toast.makeText(
											activity_,
											String.format(
													activity_
															.getResources()
															.getString(
																	R.string.favorited_it),
													item.getTitle()),
											Toast.LENGTH_LONG).show();
								} else if ("make_read".equals(processName)) {
									// 既読にする
									dbHelper_.viewLog(item);
									item.setViewCount(item.getViewCount() + 1);
									final ImageView newIcon = (ImageView) v
											.findViewById(R.id.newEntry);
									newIcon.setVisibility(ImageView.GONE);
									Toast.makeText(
											activity_,
											String.format(
													activity_
															.getResources()
															.getString(
																	R.string.marked_it_as_read),
													item.getTitle()),
											Toast.LENGTH_LONG).show();
								} else if ("delete".equals(processName)) {
									// 削除
									dbHelper_.deleteItem(item);
									final MainActivity a = (MainActivity) activity_;
									a.resetGridInfo();
									Toast.makeText(
											activity_,
											String.format(
													activity_
															.getResources()
															.getString(
																	R.string.removed_it),
													item.getTitle()),
											Toast.LENGTH_LONG).show();
								}
							} catch (Exception e) {
								Log.e(TAG, "failed to update entry action.", e);
							}
						}

					});
			final AlertDialog alert = ad.create();
			alert.show();
			return true;
		}
	}
}
