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

import android.content.Context;
import android.provider.Settings;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;

public class HWKSwitch implements OnPreferenceChangeListener {

    public static final String SETTINGS_KEY = DeviceSettings.KEY_SETTINGS_PREFIX + DeviceSettings.KEY_HWK_SWITCH;

    private static String mFileName;
    private Context mContext;

    public HWKSwitch(Context context) {
        mContext = context;
        mFileName = context.getResources().getString(R.string.pathHWKToggle);
    }

    public static String getFile() {
        if (mFileName != null && !mFileName.isEmpty() && Utils.fileWritable(mFileName)) {
            return mFileName;
        }
        return null;
    }

    public static boolean isSupported() {
        return getFile() != null;
    }

    public static boolean isCurrentlyEnabled() {
        return Utils.getFileValueAsBoolean(getFile(), false);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Boolean enabled = (Boolean) newValue;
        Settings.System.putInt(mContext.getContentResolver(), SETTINGS_KEY, enabled ? 1 : 0);
        Utils.writeValue(getFile(), enabled ? "1" : "0");
        return true;
    }
}
