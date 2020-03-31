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

import android.content.ContentResolver;
import android.content.Context;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import android.database.ContentObserver;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Button;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

public class HeadphoneGainPreference extends Preference implements
        SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "HeadphoneGainPreference";

    protected static String SETTINGS_KEY = DeviceSettings.KEY_SETTINGS_PREFIX + DeviceSettings.KEY_HEADPHONE_GAIN;
    protected static String DEFAULT_VALUE;

    private static String mFileName;
    private int mMinValue = 0;
    private int mMaxValue = 1;
    private SeekBar mSeekBar;
    private int mOldStrength;


    public HeadphoneGainPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        // from sound/soc/codecs/wcd9335.c
        mFileName = context.getResources().getString(R.string.pathAudioHeadphoneGain);
        String defaultValue = Integer.toString(0);
        if (isSupported()) {
            mMinValue = (int) context.getResources().getInteger(R.integer.audioHeadphoneGainMin);
            mMaxValue = (int) context.getResources().getInteger(R.integer.audioHeadphoneGainMax);
            defaultValue = Integer.toString(context.getResources().getInteger(R.integer.audioHeadphoneGainDefault));
        }
        DEFAULT_VALUE = defaultValue;
        setLayoutResource(R.layout.preference_seek_bar);
        restore(context);
    }

    public static boolean isSupported() {
        if (mFileName != null && !mFileName.isEmpty()) {
            return Utils.fileWritable(mFileName);
        }
        return false;
    }

    public static String getFile(Context context){
        mFileName = context.getResources().getString(R.string.pathAudioHeadphoneGain);
        if(isSupported()) {
            return mFileName;
        }
        return null;
    }

    public String getValue(Context context) {
        String val = Utils.getFileValueDual(mFileName, DEFAULT_VALUE);
        return val;
    }

    private void setValue(String newValue) {
        Log.d(TAG, "setValue - mFileName " + mFileName + " - newValue " + newValue);
        Utils.writeValueDual(mFileName, newValue);
        Settings.System.putString(getContext().getContentResolver(), SETTINGS_KEY, newValue);
    }

    private void restore(Context context) {
        if (!isSupported()) {
            return;
        }
        String storedValue = Settings.System.getString(context.getContentResolver(), SETTINGS_KEY);
        if (storedValue == null) {
            storedValue = DEFAULT_VALUE;
        }
        Utils.writeValueDual(mFileName, storedValue);
    }

    public static String getDefaultValue() {
        return DEFAULT_VALUE;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromTouch) {
        setValue(String.valueOf(progress + mMinValue));
        Log.d(TAG, "onProgressChanged - progress " + progress + " - mMinValue " + mMinValue);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        mOldStrength = Integer.parseInt(getValue(getContext()));
        mSeekBar = (SeekBar) holder.findViewById(R.id.seekbar);
        mSeekBar.setMax(mMaxValue - mMinValue);
        mSeekBar.setProgress(mOldStrength - mMinValue);
        mSeekBar.setOnSeekBarChangeListener(this);
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        // NA
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        // NA
    }
}
