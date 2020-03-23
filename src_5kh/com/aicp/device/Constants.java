/*
 * Copyright (C) 2015 The CyanogenMod Project
 * Copyright (C) 2017 The LineageOS Project
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

public class Constants {

    // Default values
    public static final int KEY_VALUE_TOTAL_SILENCE = 0;
    public static final int KEY_VALUE_SILENT = 1;
    public static final int KEY_VALUE_PRIORTY_ONLY = 2;
    public static final int KEY_VALUE_VIBRATE = 3;
    public static final int KEY_VALUE_NORMAL = 4;

    // TriStateUI Modes
    public static final int MODE_TOTAL_SILENCE = 600;
    public static final int MODE_ALARMS_ONLY = 601;
    public static final int MODE_PRIORITY_ONLY = 602;
    public static final int MODE_NONE = 603;
    public static final int MODE_VIBRATE = 604;
    public static final int MODE_RING = 605;
    // AICP additions: arbitrary value which hopefully doesn't conflict with upstream anytime soon
    public static final int MODE_SILENT = 620;

    public static final int KEY_DOUBLE_TAP = 143;
    public static final int KEY_HOME = 102;
    public static final int KEY_BACK = 158;
    public static final int KEY_RECENTS = 580;
    public static final int KEY_SLIDER_TOP = 601;
    public static final int KEY_SLIDER_CENTER = 602;
    public static final int KEY_SLIDER_BOTTOM = 603;

    // Gesture constants
    public static final int GESTURE_RIGHT_SWIPE_SCANCODE = 63;
    public static final int GESTURE_LEFT_SWIPE_SCANCODE = 64;
    public static final int GESTURE_DOWN_SWIPE_SCANCODE = 65;
    public static final int GESTURE_UP_SWIPE_SCANCODE = 66;
    public static final int GESTURE_W_SCANCODE = 246;
    public static final int GESTURE_M_SCANCODE = 247;
    public static final int GESTURE_S_SCANCODE = 248;
    public static final int GESTURE_CIRCLE_SCANCODE = 250;
    public static final int GESTURE_II_SCANCODE = 251;
    public static final int GESTURE_V_SCANCODE = 252;
    public static final int GESTURE_LEFT_V_SCANCODE = 253;
    public static final int GESTURE_RIGHT_V_SCANCODE = 254;

    // FP Gesture constants
    public static final int FP_GESTURE_SWIPE_DOWN = 108;
    public static final int FP_GESTURE_SWIPE_UP = 103;
    public static final int FP_GESTURE_SWIPE_LEFT = 105;
    public static final int FP_GESTURE_SWIPE_RIGHT = 106;
    public static final int FP_GESTURE_LONG_PRESS = 305;

    public static final int MIN_PULSE_INTERVAL_MS = 2500;
    public static final String DOZE_INTENT = "com.android.systemui.doze.pulse";
    public static final int HANDWAVE_MAX_DELTA_MS = 1000;
    public static final int POCKET_MIN_DELTA_MS = 5000;
    public static final int GESTURE_REQUEST = 1;
    public static final int GESTURE_WAKELOCK_DURATION = 2000;
    public static final int GESTURE_HAPTIC_DURATION = 50;

    public static final String ACTION_UPDATE_SLIDER_POSITION
            = "com.aicp.device.UPDATE_SLIDER_POSITION";
    public static final String EXTRA_SLIDER_POSITION = "position";
    public static final String EXTRA_SLIDER_POSITION_VALUE = "position_value";

    public static final int[] sSupportedGestures5t = new int[]{
        GESTURE_II_SCANCODE,
        GESTURE_CIRCLE_SCANCODE,
        GESTURE_V_SCANCODE,
        GESTURE_LEFT_V_SCANCODE,
        GESTURE_RIGHT_V_SCANCODE,
        GESTURE_DOWN_SWIPE_SCANCODE,
        GESTURE_UP_SWIPE_SCANCODE,
        GESTURE_LEFT_SWIPE_SCANCODE,
        GESTURE_RIGHT_SWIPE_SCANCODE,
        GESTURE_M_SCANCODE,
        GESTURE_W_SCANCODE,
        GESTURE_S_SCANCODE,
        KEY_DOUBLE_TAP,
        KEY_SLIDER_TOP,
        KEY_SLIDER_CENTER,
        KEY_SLIDER_BOTTOM,
        FP_GESTURE_SWIPE_DOWN,
        FP_GESTURE_SWIPE_UP,
        FP_GESTURE_SWIPE_LEFT,
        FP_GESTURE_SWIPE_RIGHT,
        FP_GESTURE_LONG_PRESS,
    };

    public static final int[] sSupportedGestures = new int[]{
        GESTURE_II_SCANCODE,
        GESTURE_CIRCLE_SCANCODE,
        GESTURE_V_SCANCODE,
        GESTURE_LEFT_V_SCANCODE,
        GESTURE_RIGHT_V_SCANCODE,
        GESTURE_DOWN_SWIPE_SCANCODE,
        GESTURE_UP_SWIPE_SCANCODE,
        GESTURE_LEFT_SWIPE_SCANCODE,
        GESTURE_RIGHT_SWIPE_SCANCODE,
        GESTURE_M_SCANCODE,
        GESTURE_W_SCANCODE,
        GESTURE_S_SCANCODE,
        KEY_DOUBLE_TAP,
        KEY_SLIDER_TOP,
        KEY_SLIDER_CENTER,
        KEY_SLIDER_BOTTOM
    };

    public static final int[] sHandledGestures = new int[]{
        KEY_SLIDER_TOP,
        KEY_SLIDER_CENTER,
        KEY_SLIDER_BOTTOM
    };

    public static final int[] sProxiCheckedGestures = new int[]{
        GESTURE_II_SCANCODE,
        GESTURE_CIRCLE_SCANCODE,
        GESTURE_LEFT_V_SCANCODE,
        GESTURE_RIGHT_V_SCANCODE,
        GESTURE_DOWN_SWIPE_SCANCODE,
        GESTURE_UP_SWIPE_SCANCODE,
        GESTURE_LEFT_SWIPE_SCANCODE,
        GESTURE_RIGHT_SWIPE_SCANCODE,
        GESTURE_W_SCANCODE,
        GESTURE_M_SCANCODE,
        GESTURE_S_SCANCODE,
        GESTURE_V_SCANCODE,
        KEY_DOUBLE_TAP
    };
}
