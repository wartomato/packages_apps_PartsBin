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

import android.content.Context;

public class NightModeSwitch {

    public static final String SETTINGS_KEY = DeviceSettings.KEY_SETTINGS_PREFIX + DeviceSettings.KEY_NIGHT_SWITCH;

    public static String getFile(Context context) {
        String fileName = context.getResources().getString(R.string.pathNightModeToggle);
        if (fileName != null && !fileName.isEmpty() && Utils.fileWritable(fileName)) {
            return fileName;
        }
        return null;
    }

    public static boolean isSupported(Context context) {
        return getFile(context) != null;
    }

    public static boolean isCurrentlyEnabled(Context context) {
        return Utils.getFileValueAsBoolean(getFile(context), false);
    }
}
