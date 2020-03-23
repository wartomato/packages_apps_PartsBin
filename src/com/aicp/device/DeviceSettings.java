/*
* Copyright (C) 2016 The OmniROM Project
* Copyright (C) 2020 The Android Ice Cold Project
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.res.Resources;
import android.content.Intent;
import android.os.Bundle;
import androidx.preference.PreferenceFragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.util.Log;

import com.android.internal.util.aicp.PackageUtils;

public class DeviceSettings extends PreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    public static final String KEY_SYSTEM_VIBSTRENGTH = "vib_system_strength";
    public static final String KEY_CALL_VIBSTRENGTH = "vib_call_strength";
    public static final String KEY_NOTIF_VIBSTRENGTH = "vib_notif_strength";

    private static final String KEY_SLIDER_MODE_TOP = "slider_mode_top";
    private static final String KEY_SLIDER_MODE_CENTER = "slider_mode_center";
    private static final String KEY_SLIDER_MODE_BOTTOM = "slider_mode_bottom";

    private static final String KEY_BUTTON_CATEGORY = "category_buttons";
    private static final String KEY_GRAPHICS_CATEGORY = "category_graphics";
    private static final String KEY_VIBRATOR_CATEGORY = "category_vibrator";
    private static final String KEY_SLIDER_CATEGORY = "category_slider";
    private static final String KEY_DOZE_CATEGORY = "category_doze";

    public static final String KEY_SRGB_SWITCH = "srgb";
    public static final String KEY_HBM_SWITCH = "hbm";
    public static final String KEY_PROXI_SWITCH = "proxi";
    public static final String KEY_DCD_SWITCH = "dcd";
    public static final String KEY_DCI_SWITCH = "dci";
    public static final String KEY_WIDE_SWITCH = "wide";
    public static final String KEY_NIGHT_SWITCH = "night";
    public static final String KEY_ONEPLUSMODE_SWITCH = "oneplus";
    public static final String KEY_HWK_SWITCH = "hwk";

    public static final String SLIDER_DEFAULT_VALUE = "2,1,0";

    public static final String KEY_SETTINGS_PREFIX = "device_setting_";

    private VibratorSystemStrengthPreference mVibratorSystemStrength;
    private VibratorCallStrengthPreference mVibratorCallStrength;
    private VibratorNotifStrengthPreference mVibratorNotifStrength;
    private ListPreference mSliderModeTop;
    private ListPreference mSliderModeCenter;
    private ListPreference mSliderModeBottom;
    private static TwoStatePreference mHBMModeSwitch;
    private static TwoStatePreference mDCDModeSwitch;
    private static TwoStatePreference mHWKSwitch;


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.main, rootKey);

        boolean hasAlertSlider = getContext().getResources().
                getBoolean(com.android.internal.R.bool.config_hasAlertSlider);
        if (hasAlertSlider) {
            mSliderModeTop = (ListPreference) findPreference(KEY_SLIDER_MODE_TOP);
            mSliderModeTop.setOnPreferenceChangeListener(this);
            int sliderModeTop = getSliderAction(0);
            int valueIndex = mSliderModeTop.findIndexOfValue(String.valueOf(sliderModeTop));
            mSliderModeTop.setValueIndex(valueIndex);
            mSliderModeTop.setSummary(mSliderModeTop.getEntries()[valueIndex]);

            mSliderModeCenter = (ListPreference) findPreference(KEY_SLIDER_MODE_CENTER);
            mSliderModeCenter.setOnPreferenceChangeListener(this);
            int sliderModeCenter = getSliderAction(1);
            valueIndex = mSliderModeCenter.findIndexOfValue(String.valueOf(sliderModeCenter));
            mSliderModeCenter.setValueIndex(valueIndex);
            mSliderModeCenter.setSummary(mSliderModeCenter.getEntries()[valueIndex]);

            mSliderModeBottom = (ListPreference) findPreference(KEY_SLIDER_MODE_BOTTOM);
            mSliderModeBottom.setOnPreferenceChangeListener(this);
            int sliderModeBottom = getSliderAction(2);
            valueIndex = mSliderModeBottom.findIndexOfValue(String.valueOf(sliderModeBottom));
            mSliderModeBottom.setValueIndex(valueIndex);
            mSliderModeBottom.setSummary(mSliderModeBottom.getEntries()[valueIndex]);
        } else {
            PreferenceCategory sliderCategory = (PreferenceCategory) findPreference(KEY_SLIDER_CATEGORY);
            sliderCategory.getParent().removePreference(sliderCategory);
        }

        boolean hasDozePackage = PackageUtils.isDozePackageAvailable(getContext());
        if (!hasDozePackage) {
            PreferenceCategory dozeCategory = (PreferenceCategory) findPreference(KEY_DOZE_CATEGORY);
            dozeCategory.getParent().removePreference(dozeCategory);
        }

        mHWKSwitch = (TwoStatePreference) findPreference(KEY_HWK_SWITCH);
        if (mHWKSwitch != null && HWKSwitch.isSupported()) {
            mHWKSwitch.setEnabled(true);
            mHWKSwitch.setChecked(HWKSwitch.isCurrentlyEnabled());
            mHWKSwitch.setOnPreferenceChangeListener(new HWKSwitch(getContext()));
        } else {
            PreferenceCategory buttonsCategory = (PreferenceCategory) findPreference(KEY_BUTTON_CATEGORY);
            buttonsCategory.getParent().removePreference(buttonsCategory);
        }

        PreferenceCategory graphicsCategory = (PreferenceCategory) findPreference(KEY_GRAPHICS_CATEGORY);
        mHBMModeSwitch = (TwoStatePreference) findPreference(KEY_HBM_SWITCH);
        if (mHBMModeSwitch != null && HBMModeSwitch.isSupported(getContext())){
            mHBMModeSwitch.setEnabled(true);
            mHBMModeSwitch.setChecked(HBMModeSwitch.isCurrentlyEnabled(getContext()));
            mHBMModeSwitch.setOnPreferenceChangeListener(new HBMModeSwitch(getContext()));
        } else {
            graphicsCategory.removePreference(mHBMModeSwitch);
        }

        mDCDModeSwitch = (TwoStatePreference) findPreference(KEY_DCD_SWITCH);
        if (mDCDModeSwitch != null && DCDModeSwitch.isSupported(getContext())){
            mDCDModeSwitch.setEnabled(true);
            mDCDModeSwitch.setChecked(DCDModeSwitch.isCurrentlyEnabled(getContext()));
            mDCDModeSwitch.setOnPreferenceChangeListener(new DCDModeSwitch(getContext()));
        } else {
            graphicsCategory.removePreference(mDCDModeSwitch);
        }

        PreferenceCategory vibratorCategory = (PreferenceCategory) findPreference(KEY_VIBRATOR_CATEGORY);
        int countVibRemoved = 0;
        mVibratorSystemStrength = (VibratorSystemStrengthPreference) findPreference(KEY_SYSTEM_VIBSTRENGTH);
        if (mVibratorSystemStrength != null && mVibratorSystemStrength.isSupported()) {
            mVibratorSystemStrength.setEnabled(true);
        } else {
            mVibratorSystemStrength.getParent().removePreference(mVibratorSystemStrength);
            countVibRemoved += 1;
        }
        mVibratorCallStrength = (VibratorCallStrengthPreference) findPreference(KEY_CALL_VIBSTRENGTH);
        if (mVibratorCallStrength != null && mVibratorCallStrength.isSupported()) {
            mVibratorCallStrength.setEnabled(true);
        } else {
            mVibratorCallStrength.getParent().removePreference(mVibratorCallStrength);
            countVibRemoved += 1;
        }
        mVibratorNotifStrength = (VibratorNotifStrengthPreference) findPreference(KEY_NOTIF_VIBSTRENGTH);
        if (mVibratorNotifStrength != null && mVibratorNotifStrength.isSupported()) {
            mVibratorNotifStrength.setEnabled(true);
        } else {
            mVibratorNotifStrength.getParent().removePreference(mVibratorNotifStrength);
            countVibRemoved += 1;
        }
        if (countVibRemoved == 3) vibratorCategory.getParent().removePreference(vibratorCategory);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mSliderModeTop) {
            String value = (String) newValue;
            int sliderMode = Integer.valueOf(value);
            setSliderAction(0, sliderMode);
            int valueIndex = mSliderModeTop.findIndexOfValue(value);
            mSliderModeTop.setSummary(mSliderModeTop.getEntries()[valueIndex]);
        } else if (preference == mSliderModeCenter) {
            String value = (String) newValue;
            int sliderMode = Integer.valueOf(value);
            setSliderAction(1, sliderMode);
            int valueIndex = mSliderModeCenter.findIndexOfValue(value);
            mSliderModeCenter.setSummary(mSliderModeCenter.getEntries()[valueIndex]);
        } else if (preference == mSliderModeBottom) {
            String value = (String) newValue;
            int sliderMode = Integer.valueOf(value);
            setSliderAction(2, sliderMode);
            int valueIndex = mSliderModeBottom.findIndexOfValue(value);
            mSliderModeBottom.setSummary(mSliderModeBottom.getEntries()[valueIndex]);
        }
        return true;
    }

    private int getSliderAction(int position) {
        String value = Settings.System.getString(getContext().getContentResolver(),
                    Settings.System.OMNI_BUTTON_EXTRA_KEY_MAPPING);
        final String defaultValue = SLIDER_DEFAULT_VALUE;

        if (value == null) {
            value = defaultValue;
        } else if (value.indexOf(",") == -1) {
            value = defaultValue;
        }
        try {
            String[] parts = value.split(",");
            return Integer.valueOf(parts[position]);
        } catch (Exception e) {
        }
        return 0;
    }

    private void setSliderAction(int position, int action) {
        String value = Settings.System.getString(getContext().getContentResolver(),
                    Settings.System.OMNI_BUTTON_EXTRA_KEY_MAPPING);
        final String defaultValue = SLIDER_DEFAULT_VALUE;

        if (value == null) {
            value = defaultValue;
        } else if (value.indexOf(",") == -1) {
            value = defaultValue;
        }
        try {
            String[] parts = value.split(",");
            parts[position] = String.valueOf(action);
            String newValue = TextUtils.join(",", parts);
            Settings.System.putString(getContext().getContentResolver(),
                    Settings.System.OMNI_BUTTON_EXTRA_KEY_MAPPING, newValue);
        } catch (Exception e) {
        }
    }
}
