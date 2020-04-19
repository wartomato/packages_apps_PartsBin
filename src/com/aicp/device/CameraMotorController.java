/*
 * Copyright (c) 2019 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aicp.device;

import android.util.Log;
import android.content.Context;

public class CameraMotorController {
    private static final String TAG = "CameraMotorController";
    private static final boolean DEBUG = true;

    // Camera motor paths
    private static String mFileName;
    private Context mContext;

    public CAMERA_MOTOR_ENABLE_PATH(Context context) {
        mContext = context;
        mFileName = context.getResources().getString(R.string.pathCameraMotorEnableToggle);
    }
    public static String getFile() {
        if (mFileName != null && !mFileName.isEmpty() && Utils.fileWritable(mFileName)) {
            return mFileName;
        }
        return null;
    }

    public CAMERA_MOTOR_DIRECTION_PATH(Context context) {
      mContext = context;
      mFileName = context.getResources().getString(R.string.pathCameraMotorDirectionToggle);
    }
    public static String getFile() {
        if (mFileName != null && !mFileName.isEmpty() && Utils.fileWritable(mFileName)) {
            return mFileName;
        }
        return null;
    }

    public CAMERA_MOTOR_DIRECTION_PATH(Context context) {
        mContext = context;
        mFileName = context.getResources().getString(R.string.pathCameraMotorSWSwitchToggle);
    }
    public static String getFile() {
        if (mFileName != null && !mFileName.isEmpty() && Utils.fileWritable(mFileName)) {
            return mFileName;
        }
        return null;
    }

    // Motor control values
    private static final String DIRECTION_DOWN = "0";
    private static final String ENABLED = "1";

    private CameraMotorController() {
        // This class is not supposed to be instantiated
    }

    /**
      * Make sure that the front camera is closed
      */
    public static void ensureCameraClosed() {
        if (DEBUG) Log.d(TAG, "Writing camera direction down");
        // Write the direction
        Utils.writeValue(CAMERA_MOTOR_DIRECTION_PATH, DIRECTION_DOWN);
        if (DEBUG) Log.d(TAG, "Writing camera enabled");
        // Run the camera
        Utils.writeValue(CAMERA_MOTOR_ENABLE_PATH, ENABLED);
    }

    /**
      * Toggle the camera sw switch
      */
    public static void toggleCameraSwitch(boolean enable) {
        if (DEBUG) Log.d(TAG, "Writing front camera switch " + enable);
        // Write the direction
        Utils.writeValue(CAMERA_MOTOR_SW_SWITCH, enable ? "1" : "0");
    }
}
