package com.starbug1.android.newsapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Window;
import android.widget.TextView;

import com.starbug1.android.newsapp.utils.AppUtils;

public class AppPrefActivity extends PreferenceActivity {
	private static final String TAG = "AppPrefActivity";
	public static final String NEEDS_REFRESH = "needs_refresh";

	private ListPreference clowlIntervalsPref_;
	private ListPreference thumbnailSizePref_;
	private SharedPreferences sharedPref_;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pref);
		addPreferencesFromResource(R.xml.pref);

		final String versionName = AppUtils.getVersionName(this);
		final TextView version = (TextView) this.findViewById(R.id.version);
		version.setText(versionName);

		sharedPref_ = PreferenceManager.getDefaultSharedPreferences(this);
		clowlIntervalsPref_ = (ListPreference) findPreference("clowl_intervals");

		clowlIntervalsPref_.setSummary(getStringByValue(clowlIntervalsPref_,
				sharedPref_.getString("clowl_intervals", "60")));
		clowlIntervalsPref_
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						preference.setSummary(getStringByValue(
								clowlIntervalsPref_, newValue.toString()));
						// 更新間隔の更新
						AppUtils.updateClowlIntervals(AppPrefActivity.this,
								Integer.parseInt(newValue.toString()));
						return true;
					}
				});

		thumbnailSizePref_ = (ListPreference) findPreference("thumbnail_size");
		thumbnailSizePref_.setSummary(getStringByValue(thumbnailSizePref_,
				sharedPref_.getString("thumbnail_size", "200")));
		thumbnailSizePref_
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						Log.d(TAG,
								"thumbnail size changed:" + newValue.toString());
						preference.setSummary(getStringByValue(
								thumbnailSizePref_, newValue.toString()));
						// サムネイルサイズの変更を反映する
						Intent intent = new Intent();
						intent.putExtra(NEEDS_REFRESH, true);
						AppPrefActivity.this.setResult(RESULT_OK, intent);
						return true;
					}
				});
	}

	public String getStringByValue(ListPreference listPref, String value) {
		final CharSequence[] strings = listPref.getEntries();
		final CharSequence[] values = listPref.getEntryValues();
		int index = -1;
		for (int i = 0, len = values.length; i < len; i++) {
			if (value.equals(values[i])) {
				index = i;
				break;
			}
		}
		if (index == -1 || index > strings.length - 1) {
			return "";
		}
		return strings[index].toString();
	}

}
