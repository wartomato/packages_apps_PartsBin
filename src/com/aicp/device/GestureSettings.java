/*
* Copyright (C) 2017 The OmniROM Project
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*
*/
package com.aicp.device;

import static android.provider.Settings.Secure.SYSTEM_NAVIGATION_KEYS_ENABLED;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import androidx.preference.PreferenceFragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class GestureSettings extends PreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    public static final String TAG = "GestureSettings";
    public static final String KEY_PROXI_SWITCH = "proxi";
    public static final String KEY_OFF_SCREEN_GESTURE_FEEDBACK_SWITCH = "off_screen_gesture_feedback";
    public static final String KEY_MUSIC_START = "music_playback_gesture";
    public static final String KEY_CIRCLE_APP = "circle_gesture_app";
    public static final String KEY_DOWN_ARROW_APP = "down_arrow_gesture_app";
    public static final String KEY_MUSIC_TRACK_PREV = "left_arrow_gesture_app";
    public static final String KEY_MUSIC_TRACK_NEXT = "right_arrow_gesture_app";
    public static final String KEY_UP_ARROW_APP = "down_up_gesture_app";
    public static final String KEY_DOWN_SWIPE_APP = "down_swipe_gesture_app";
    public static final String KEY_UP_SWIPE_APP = "up_swipe_gesture_app";
    public static final String KEY_LEFT_SWIPE_APP = "left_swipe_gesture_app";
    public static final String KEY_RIGHT_SWIPE_APP = "right_swipe_gesture_app";
    public static final String KEY_M_GESTURE_APP = "gesture_m_app";
    public static final String KEY_S_GESTURE_APP = "gesture_s_app";
    public static final String KEY_W_GESTURE_APP = "gesture_w_app";
    public static final String KEY_FP_GESTURE_CATEGORY = "key_fp_gesture_category";
    public static final String KEY_FP_GESTURE_DEFAULT_CATEGORY = "gesture_settings";

    public static final String DEVICE_GESTURE_MAPPING_0 = "device_gesture_mapping_0_0";
    public static final String DEVICE_GESTURE_MAPPING_1 = "device_gesture_mapping_1_0";
    public static final String DEVICE_GESTURE_MAPPING_2 = "device_gesture_mapping_2_0";
    public static final String DEVICE_GESTURE_MAPPING_3 = "device_gesture_mapping_3_0";
    public static final String DEVICE_GESTURE_MAPPING_4 = "device_gesture_mapping_4_0";
    public static final String DEVICE_GESTURE_MAPPING_5 = "device_gesture_mapping_5_0";
    public static final String DEVICE_GESTURE_MAPPING_6 = "device_gesture_mapping_6_0";
    public static final String DEVICE_GESTURE_MAPPING_7 = "device_gesture_mapping_7_0";
    public static final String DEVICE_GESTURE_MAPPING_8 = "device_gesture_mapping_8_0";
    public static final String DEVICE_GESTURE_MAPPING_9 = "device_gesture_mapping_9_0";
    public static final String DEVICE_GESTURE_MAPPING_10 = "device_gesture_mapping_10_0";
    public static final String DEVICE_GESTURE_MAPPING_11 = "device_gesture_mapping_11_0";
    public static final String DEVICE_GESTURE_MAPPING_12 = "device_gesture_mapping_12_0";
    public static final String DEVICE_GESTURE_MAPPING_13 = "device_gesture_mapping_13_0";
    public static final String DEVICE_GESTURE_MAPPING_14 = "device_gesture_mapping_14_0";

    private TwoStatePreference mMusicPlaybackGestureSwitch;
    private TwoStatePreference mOffscreenGestureFeedbackSwitch;
    private AppSelectListPreference mDoubleSwipeApp;
    private AppSelectListPreference mCircleApp;
    private AppSelectListPreference mDownArrowApp;
    private AppSelectListPreference mUpArrowApp;
    private AppSelectListPreference mLeftArrowApp;
    private AppSelectListPreference mRightArrowApp;
    private AppSelectListPreference mDownSwipeApp;
    private AppSelectListPreference mUpSwipeApp;
    private AppSelectListPreference mLeftSwipeApp;
    private AppSelectListPreference mRightSwipeApp;
    private AppSelectListPreference mMGestureApp;
    private AppSelectListPreference mSGestureApp;
    private AppSelectListPreference mWGestureApp;
    private AppSelectListPreference mFPDownSwipeApp;
    private AppSelectListPreference mFPUpSwipeApp;
    private AppSelectListPreference mFPRightSwipeApp;
    private AppSelectListPreference mFPLeftSwipeApp;
    private PreferenceCategory fpGestures;
    private List<AppSelectListPreference.PackageItem> mInstalledPackages = new LinkedList<AppSelectListPreference.PackageItem>();
    private PackageManager mPm;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.gesture_settings, rootKey);
        mPm = getContext().getPackageManager();

        mOffscreenGestureFeedbackSwitch = (TwoStatePreference) findPreference(KEY_OFF_SCREEN_GESTURE_FEEDBACK_SWITCH);
        mOffscreenGestureFeedbackSwitch.setChecked(Settings.System.getInt(getContext().getContentResolver(),
                "Settings.System."+DeviceSettings.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME, 1) != 0);

        mMusicPlaybackGestureSwitch = (TwoStatePreference) findPreference(KEY_MUSIC_START);
        mMusicPlaybackGestureSwitch.setChecked(Settings.System.getInt(getContext().getContentResolver(),
                "Settings.System."+DeviceSettings.GESTURE_MUSIC_PLAYBACK_SETTINGS_VARIABLE_NAME, 1) != 0);
        final boolean musicPlaybackEnabled = Settings.System.getIntForUser(getContext().getContentResolver(),
                "Settings.System."+DeviceSettings.GESTURE_MUSIC_PLAYBACK_SETTINGS_VARIABLE_NAME, 0, UserHandle.USER_CURRENT) == 1;
        setMusicPlaybackGestureEnabled(musicPlaybackEnabled);

        mCircleApp = (AppSelectListPreference) findPreference(KEY_CIRCLE_APP);
        mCircleApp.setEnabled(isGestureSupported(KEY_CIRCLE_APP));
        String value = Settings.System.getString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_1);
        mCircleApp.setValue(value);
        mCircleApp.setOnPreferenceChangeListener(this);

        mDownArrowApp = (AppSelectListPreference) findPreference(KEY_DOWN_ARROW_APP);
        mDownArrowApp.setEnabled(isGestureSupported(KEY_DOWN_ARROW_APP));
        value = Settings.System.getString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_2);
        mDownArrowApp.setValue(value);
        mDownArrowApp.setOnPreferenceChangeListener(this);

        mMGestureApp = (AppSelectListPreference) findPreference(KEY_M_GESTURE_APP);
        mMGestureApp.setEnabled(isGestureSupported(KEY_M_GESTURE_APP));
        value = Settings.System.getString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_3);
        mMGestureApp.setValue(value);
        mMGestureApp.setOnPreferenceChangeListener(this);

        mDownSwipeApp = (AppSelectListPreference) findPreference(KEY_DOWN_SWIPE_APP);
        mDownSwipeApp.setEnabled(isGestureSupported(KEY_DOWN_SWIPE_APP));
        value = Settings.System.getString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_6);
        mDownSwipeApp.setValue(value);
        mDownSwipeApp.setOnPreferenceChangeListener(this);

        mUpSwipeApp = (AppSelectListPreference) findPreference(KEY_UP_SWIPE_APP);
        mUpSwipeApp.setEnabled(isGestureSupported(KEY_UP_SWIPE_APP));
        value = Settings.System.getString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_7);
        mUpSwipeApp.setValue(value);
        mUpSwipeApp.setOnPreferenceChangeListener(this);

        mLeftSwipeApp = (AppSelectListPreference) findPreference(KEY_LEFT_SWIPE_APP);
        mLeftSwipeApp.setEnabled(isGestureSupported(KEY_LEFT_SWIPE_APP));
        value = Settings.System.getString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_8);
        mLeftSwipeApp.setValue(value);
        mLeftSwipeApp.setOnPreferenceChangeListener(this);

        mRightSwipeApp = (AppSelectListPreference) findPreference(KEY_RIGHT_SWIPE_APP);
        mRightSwipeApp.setEnabled(isGestureSupported(KEY_RIGHT_SWIPE_APP));
        value = Settings.System.getString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_9);
        mRightSwipeApp.setValue(value);
        mRightSwipeApp.setOnPreferenceChangeListener(this);

        mSGestureApp = (AppSelectListPreference) findPreference(KEY_S_GESTURE_APP);
        mSGestureApp.setEnabled(isGestureSupported(KEY_S_GESTURE_APP));
        value = Settings.System.getString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_10);
        mSGestureApp.setValue(value);
        mSGestureApp.setOnPreferenceChangeListener(this);

        mWGestureApp = (AppSelectListPreference) findPreference(KEY_W_GESTURE_APP);
        mWGestureApp.setEnabled(isGestureSupported(KEY_W_GESTURE_APP));
        value = Settings.System.getString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_11);
        mWGestureApp.setValue(value);
        mWGestureApp.setOnPreferenceChangeListener(this);

        new FetchPackageInformationTask().execute();
    }

    private boolean areSystemNavigationKeysEnabled() {
        return Settings.Secure.getInt(getContext().getContentResolver(),
               Settings.Secure.SYSTEM_NAVIGATION_KEYS_ENABLED, 0) == 1;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mOffscreenGestureFeedbackSwitch) {
            Settings.System.putInt(getContext().getContentResolver(),
                    "Settings.System."+DeviceSettings.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME, mOffscreenGestureFeedbackSwitch.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mMusicPlaybackGestureSwitch) {
            Settings.System.putInt(getContext().getContentResolver(),
                    "Settings.System."+DeviceSettings.GESTURE_MUSIC_PLAYBACK_SETTINGS_VARIABLE_NAME, mMusicPlaybackGestureSwitch.isChecked() ? 1 : 0);
            setMusicPlaybackGestureEnabled(mMusicPlaybackGestureSwitch.isChecked());
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mCircleApp) {
            String value = (String) newValue;
            boolean gestureDisabled = value.equals(AppSelectListPreference.DISABLED_ENTRY);
            setGestureEnabled(KEY_CIRCLE_APP, !gestureDisabled);
            Settings.System.putString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_1, value);
        } else if (preference == mDownArrowApp) {
            String value = (String) newValue;
            boolean gestureDisabled = value.equals(AppSelectListPreference.DISABLED_ENTRY);
            setGestureEnabled(KEY_DOWN_ARROW_APP, !gestureDisabled);
            Settings.System.putString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_2, value);
        } else if (preference == mMGestureApp) {
            String value = (String) newValue;
            boolean gestureDisabled = value.equals(AppSelectListPreference.DISABLED_ENTRY);
            setGestureEnabled(KEY_M_GESTURE_APP, !gestureDisabled);
            Settings.System.putString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_3, value);
        } else if (preference == mDownSwipeApp) {
            String value = (String) newValue;
            boolean gestureDisabled = value.equals(AppSelectListPreference.DISABLED_ENTRY);
            setGestureEnabled(KEY_DOWN_SWIPE_APP, !gestureDisabled);
            Settings.System.putString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_6, value);
        } else if (preference == mUpSwipeApp) {
            String value = (String) newValue;
            boolean gestureDisabled = value.equals(AppSelectListPreference.DISABLED_ENTRY);
            setGestureEnabled(KEY_UP_SWIPE_APP, !gestureDisabled);
            Settings.System.putString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_7, value);
        } else if (preference == mLeftSwipeApp) {
            String value = (String) newValue;
            boolean gestureDisabled = value.equals(AppSelectListPreference.DISABLED_ENTRY);
            setGestureEnabled(KEY_LEFT_SWIPE_APP, !gestureDisabled);
            Settings.System.putString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_8, value);
        } else if (preference == mRightSwipeApp) {
            String value = (String) newValue;
            boolean gestureDisabled = value.equals(AppSelectListPreference.DISABLED_ENTRY);
            setGestureEnabled(KEY_RIGHT_SWIPE_APP, !gestureDisabled);
            Settings.System.putString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_9, value);
        } else if (preference == mSGestureApp) {
            String value = (String) newValue;
            boolean gestureDisabled = value.equals(AppSelectListPreference.DISABLED_ENTRY);
            setGestureEnabled(KEY_S_GESTURE_APP, !gestureDisabled);
            Settings.System.putString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_10, value);
        } else if (preference == mWGestureApp) {
            String value = (String) newValue;
            boolean gestureDisabled = value.equals(AppSelectListPreference.DISABLED_ENTRY);
            setGestureEnabled(KEY_W_GESTURE_APP, !gestureDisabled);
            Settings.System.putString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_11, value);
        }
        return true;
    }

    private void setMusicPlaybackGestureEnabled(boolean enabled) {
        boolean playbackSupported = isGestureSupported(KEY_MUSIC_START);
        boolean nextTrackSupported = isGestureSupported(KEY_MUSIC_TRACK_NEXT);
        boolean prevTrackSupported = isGestureSupported(KEY_MUSIC_TRACK_PREV);

        if (playbackSupported && nextTrackSupported && prevTrackSupported) {
            setGestureEnabled(KEY_MUSIC_START, enabled ? playbackSupported : false);
            setGestureEnabled(KEY_MUSIC_TRACK_NEXT, enabled ? nextTrackSupported : false);
            setGestureEnabled(KEY_MUSIC_TRACK_PREV, enabled ? prevTrackSupported : false);
        } else {
            Log.e(TAG,"Cannot write to music playback gesture files");
        }
    }

    public static String getGestureFile(String key) {
        switch(key) {
            case KEY_CIRCLE_APP:
                return "/proc/touchpanel/letter_o_enable"; //getContext().getResources().getString(R.string.pathGestureCircle); //"/proc/touchpanel/letter_o_enable";
            case KEY_MUSIC_START:
                return "/proc/touchpanel/double_swipe_enable"; //getContext().getResources().getString(R.string.pathGestureDoubleSwipe); //"/proc/touchpanel/double_swipe_enable";
            case KEY_DOWN_ARROW_APP:
                return "/proc/touchpanel/down_arrow_enable"; //getContext().getResources().getString(R.string.pathGestureDownArrow); //"/proc/touchpanel/down_arrow_enable";
            case KEY_MUSIC_TRACK_PREV:
                return "/proc/touchpanel/left_arrow_enable"; //getContext().getResources().getString(R.string.pathGestureLeftArrow); //"/proc/touchpanel/left_arrow_enable";
            case KEY_MUSIC_TRACK_NEXT:
                return "/proc/touchpanel/right_arrow_enable"; //getContext().getResources().getString(R.string.pathGestureRightArrow); //"/proc/touchpanel/right_arrow_enable";
            case KEY_DOWN_SWIPE_APP:
                return "/proc/touchpanel/down_swipe_enable"; //getContext().getResources().getString(R.string.pathGestureDownSwipe); //"/proc/touchpanel/down_swipe_enable";
            case KEY_UP_SWIPE_APP:
                return "/proc/touchpanel/up_swipe_enable"; //getContext().getResources().getString(R.string.pathGestureUpSwipe); //"/proc/touchpanel/up_swipe_enable";
            case KEY_LEFT_SWIPE_APP:
                return "/proc/touchpanel/left_swipe_enable"; //getContext().getResources().getString(R.string.pathGestureLeftSwipe); //"/proc/touchpanel/left_swipe_enable";
            case KEY_RIGHT_SWIPE_APP:
                return "/proc/touchpanel/right_swipe_enable"; //getContext().getResources().getString(R.string.pathGestureRightSwipe); //"/proc/touchpanel/right_swipe_enable";
            case KEY_M_GESTURE_APP:
                return "/proc/touchpanel/letter_m_enable"; //getContext().getResources().getString(R.string.pathGestureM); //"/proc/touchpanel/letter_m_enable";
            case KEY_S_GESTURE_APP:
                return "/proc/touchpanel/letter_s_enable"; //getContext().getResources().getString(R.string.pathGestureS); //"/proc/touchpanel/letter_s_enable";
            case KEY_W_GESTURE_APP:
                return "/proc/touchpanel/letter_w_enable"; //getContext().getResources().getString(R.string.pathGestureW); //"/proc/touchpanel/letter_w_enable";
        }
        return null;
    }

    private boolean isGestureSupported(String key) {
        return Utils.fileWritable(getGestureFile(key));
    }

    private void setGestureEnabled(String key, boolean enabled) {
        Utils.writeValue(getGestureFile(key), enabled ? "1" : "0");
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (!(preference instanceof AppSelectListPreference)) {
            super.onDisplayPreferenceDialog(preference);
            return;
        }
        DialogFragment fragment =
                AppSelectListPreference.AppSelectListPreferenceDialogFragment
                        .newInstance(preference.getKey());
        fragment.setTargetFragment(this, 0);
        fragment.show(getFragmentManager(), "dialog_preference");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mFPDownSwipeApp != null) {
            mFPDownSwipeApp.setEnabled(!areSystemNavigationKeysEnabled());
        }
        if (mFPUpSwipeApp != null) {
            mFPUpSwipeApp.setEnabled(!areSystemNavigationKeysEnabled());
        }
    }

    private void loadInstalledPackages() {
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> installedAppsInfo = mPm.queryIntentActivities(mainIntent, 0);

        for (ResolveInfo info : installedAppsInfo) {
            ActivityInfo activity = info.activityInfo;
            ApplicationInfo appInfo = activity.applicationInfo;
            ComponentName componentName = new ComponentName(appInfo.packageName, activity.name);
            CharSequence label = null;
            try {
                label = activity.loadLabel(mPm);
            } catch (Exception e) {
            }
            if (label != null) {
                final AppSelectListPreference.PackageItem item = new AppSelectListPreference.PackageItem(activity.loadLabel(mPm), 0, componentName);
                mInstalledPackages.add(item);
            }
        }
        Collections.sort(mInstalledPackages);
    }

    private class FetchPackageInformationTask extends AsyncTask<Void, Void, Void> {
        public FetchPackageInformationTask() {
        }

        @Override
        protected Void doInBackground(Void... params) {
            loadInstalledPackages();
            return null;
        }

        @Override
        protected void onPostExecute(Void feed) {
            mCircleApp.setPackageList(mInstalledPackages);
            mDownArrowApp.setPackageList(mInstalledPackages);
            mMGestureApp.setPackageList(mInstalledPackages);
            mDownSwipeApp.setPackageList(mInstalledPackages);
            mUpSwipeApp.setPackageList(mInstalledPackages);
            mLeftSwipeApp.setPackageList(mInstalledPackages);
            mRightSwipeApp.setPackageList(mInstalledPackages);
            mSGestureApp.setPackageList(mInstalledPackages);
            mWGestureApp.setPackageList(mInstalledPackages);
        }
    }
}
