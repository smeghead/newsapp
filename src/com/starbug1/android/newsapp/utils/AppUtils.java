package com.starbug1.android.newsapp.utils;

import java.util.GregorianCalendar;
import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import android.view.MenuItem;

import com.starbug1.android.newsapp.ActivityProcessAditional;
import com.starbug1.android.newsapp.AlarmReceiver;
import com.starbug1.android.newsapp.AppException;

public class AppUtils {
	private static final String TAG = "AppUtils";
	public static final boolean DEBUG = false;
	
	public static String getVersionName(Context context) {
		PackageInfo packageInfo = null;
		try {
			packageInfo = context.getPackageManager().getPackageInfo(
					context.getPackageName(),
					PackageManager.GET_META_DATA);
			return "Version " + packageInfo.versionName;
		} catch (NameNotFoundException e) {
			Log.e(TAG, "failed to retreive version info.");
		}
		return "";
	}

	public static boolean isServiceRunning(Activity activity) {
		final ActivityManager activityManager = (ActivityManager) activity
				.getSystemService(Context.ACTIVITY_SERVICE);
		final List<RunningServiceInfo> services = activityManager
				.getRunningServices(Integer.MAX_VALUE);

		String serviceName = AppUtils.getServiceClass(activity).getCanonicalName();
		for (RunningServiceInfo info : services) {
			if (serviceName.equals(info.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
	
	public static Class<?> getServiceClass(Context context) {
		Class<?> serviceClass = null;
		try {
			serviceClass = Class.forName(context.getPackageName() + ".AppFetchFeedService");
		} catch (ClassNotFoundException e) {
			Log.e(TAG, "no service " + e.toString());
			throw new AppException("failed to get service class.");
		}
		return serviceClass;
	}
	
	public static void onCreateAditional(Activity activity) {
		String aditionalClassName = activity.getPackageName() + ".AppActivityProcessAditional";
		try {
			Class<? extends ActivityProcessAditional> aditionalClass = (Class<? extends ActivityProcessAditional>) Class.forName(aditionalClassName);
			ActivityProcessAditional aditional = aditionalClass.newInstance();
			aditional.onCreateAditional(activity);
		} catch (Exception e) {
			Log.i(TAG, "no class:" + aditionalClassName);
		}
	}
	
	public static void updateClowlIntervals(Context context, final int clowlIntervals) {
		final AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		final Intent alarmIntent = new Intent(context, AlarmReceiver.class);
		final PendingIntent sender = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		if (clowlIntervals != 0) {
			final GregorianCalendar calendar = new GregorianCalendar();
			alarmManager.setInexactRepeating(
					AlarmManager.RTC_WAKEUP,
					calendar.getTimeInMillis() + 1000 * 60 * 1,
					1000 * 60 * clowlIntervals,
					sender); 
		} else {
			alarmManager.cancel(sender);
		}
	}
}
