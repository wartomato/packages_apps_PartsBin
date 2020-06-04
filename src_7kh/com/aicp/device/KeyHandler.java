/*
 * Copyright (C) 2015-2016 The CyanogenMod Project
 * Copyright (C) 2017 The LineageOS Project
 * Copyright (C) 2020 Android Ice Cold Project
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

import static android.provider.Settings.Global.ZEN_MODE_OFF;
import static android.provider.Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS;
import static android.provider.Settings.Global.ZEN_MODE_ALARMS;
import static android.provider.Settings.Global.ZEN_MODE_NO_INTERRUPTIONS;

import android.app.ActivityManagerNative;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.IAudioService;
import android.media.AudioManager;
import android.media.session.MediaSessionLegacyHelper;
import android.text.TextUtils;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.util.Log;
import android.view.KeyEvent;
import android.view.HapticFeedbackConstants;

import com.android.internal.util.ArrayUtils;
import com.android.internal.util.aicp.AicpVibe;
import com.android.internal.util.aicp.CustomKeyHandler;
import com.android.internal.statusbar.IStatusBarService;

public class KeyHandler implements CustomKeyHandler {

    private static final String TAG = "KeyHandler";
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_SENSOR = false;

    private static final String KEY_CONTROL_PATH = "/proc/touchpanel/key_disable";

    protected final Context mContext;
    private final PowerManager mPowerManager;
    private EventHandler mEventHandler;
    private WakeLock mGestureWakeLock;
    private Handler mHandler = new Handler();
    private static boolean mButtonDisabled;
    private final NotificationManager mNotificationManager;
    private final AudioManager mAudioManager;
    private boolean mDispOn;
    private boolean mTorchState = false;
    private boolean mUseSliderTorch = false;

    private BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
         @Override
         public void onReceive(Context context, Intent intent) {
             if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                 mDispOn = true;
                 onDisplayOn();
             } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                 mDispOn = false;
                 onDisplayOff();
             }
         }
    };

    public KeyHandler(Context context) {
        mContext = context;
        mDispOn = true;
        mEventHandler = new EventHandler();
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mGestureWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "GestureWakeLock");
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        IntentFilter screenStateFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(mScreenStateReceiver, screenStateFilter);
    }

    private class EventHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        }
    }

    private boolean hasSetupCompleted() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.USER_SETUP_COMPLETE, 0) != 0;
    }

    @Override
    public boolean handleKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_UP) {
            return false;
        }
        if (!hasSetupCompleted()) {
            return false;
        }
        boolean isKeySupported = ArrayUtils.contains(Constants.sHandledGestures, event.getScanCode());
        if (isKeySupported) {
            int scanCode = event.getScanCode();
            if (DEBUG) Log.i(TAG, "scanCode=" + scanCode);
            int position = scanCode == Constants.KEY_SLIDER_TOP ? 2 :
                           scanCode == Constants.KEY_SLIDER_CENTER ? 1 : 0;

            if (scanCode == Constants.KEY_SINGLE_TAP) {
                launchDozePulse();
                return true;
            }
            doHandleSliderAction(position);
        }
        return isKeySupported;
    }

    @Override
    public boolean canHandleKeyEvent(KeyEvent event) {
        return ArrayUtils.contains(Constants.sSupportedGestures, event.getScanCode());
    }

    @Override
    public boolean isDisabledKeyEvent(KeyEvent event) {
        return false;
    }

    @Override
    public boolean isCameraLaunchEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_UP) {
            return false;
        }
            String value = getGestureValueForScanCode(event.getScanCode());
            return !TextUtils.isEmpty(value) && value.equals(AppSelectListPreference.CAMERA_ENTRY);
    }

    @Override
    public boolean isWakeEvent(KeyEvent event){
        if (event.getAction() != KeyEvent.ACTION_UP) {
            return false;
        }
        String value = getGestureValueForScanCode(event.getScanCode());
        if (!TextUtils.isEmpty(value) && value.equals(AppSelectListPreference.WAKE_ENTRY)) {
            if (DEBUG) Log.i(TAG, "isWakeEvent " + event.getScanCode() + value);
            return true;
        }
        return event.getScanCode() == Constants.KEY_DOUBLE_TAP;
    }

    @Override
    public Intent isActivityLaunchEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_UP) {
            return null;
        }
        String value = getGestureValueForScanCode(event.getScanCode());
        if (!TextUtils.isEmpty(value) && !value.equals(AppSelectListPreference.DISABLED_ENTRY)) {
            if (DEBUG) Log.i(TAG, "isActivityLaunchEvent " + event.getScanCode() + value);
            if (!launchSpecialActions(value)) {
                AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false, mContext,
                      DeviceSettings.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME, Constants.GESTURE_HAPTIC_DURATION);
                Intent intent = createIntent(value);
                return intent;
            }
        }
        return null;
    }

    private Intent createIntent(String value) {
        ComponentName componentName = ComponentName.unflattenFromString(value);
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        intent.setComponent(componentName);
        return intent;
    }

    private String getGestureValueForScanCode(int scanCode) {
      /* for the music playback gestures, just return the expected values */
        switch(scanCode) {
            case Constants.GESTURE_II_SCANCODE:
                return AppSelectListPreference.MUSIC_PLAY_ENTRY;
            case Constants.GESTURE_CIRCLE_SCANCODE:
                return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.DEVICE_GESTURE_MAPPING_1, UserHandle.USER_CURRENT);
            case Constants.GESTURE_V_SCANCODE:
                return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.DEVICE_GESTURE_MAPPING_2, UserHandle.USER_CURRENT);
            case Constants.GESTURE_M_SCANCODE:
                return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.DEVICE_GESTURE_MAPPING_3, UserHandle.USER_CURRENT);
            case Constants.GESTURE_LEFT_V_SCANCODE:
                return AppSelectListPreference.MUSIC_PREV_ENTRY;
            case Constants.GESTURE_RIGHT_V_SCANCODE:
                return AppSelectListPreference.MUSIC_NEXT_ENTRY;
            case Constants.GESTURE_DOWN_SWIPE_SCANCODE:
                return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.DEVICE_GESTURE_MAPPING_6, UserHandle.USER_CURRENT);
            case Constants.GESTURE_UP_SWIPE_SCANCODE:
                return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.DEVICE_GESTURE_MAPPING_7, UserHandle.USER_CURRENT);
            case Constants.GESTURE_LEFT_SWIPE_SCANCODE:
                return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.DEVICE_GESTURE_MAPPING_8, UserHandle.USER_CURRENT);
            case Constants.GESTURE_RIGHT_SWIPE_SCANCODE:
                return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.DEVICE_GESTURE_MAPPING_9, UserHandle.USER_CURRENT);
            case Constants.GESTURE_S_SCANCODE:
                return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.DEVICE_GESTURE_MAPPING_10, UserHandle.USER_CURRENT);
            case Constants.GESTURE_W_SCANCODE:
                return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.DEVICE_GESTURE_MAPPING_11, UserHandle.USER_CURRENT);
        }
        return null;
    }

    private int getSliderAction(int position) {
        String value = Settings.System.getStringForUser(mContext.getContentResolver(),
                    Settings.System.OMNI_BUTTON_EXTRA_KEY_MAPPING,
                    UserHandle.USER_CURRENT);
        final String defaultValue = DeviceSettings.SLIDER_DEFAULT_VALUE;

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

    private void doHandleSliderAction(int position) {
        int action = getSliderAction(position);
        int positionValue = 0;
        if (action == 0) {
            mNotificationManager.setZenMode(ZEN_MODE_OFF, null, TAG);
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL);
            mTorchState = false;
            positionValue = Constants.MODE_RING;
        } else if (action == 1) {
            mNotificationManager.setZenMode(ZEN_MODE_OFF, null, TAG);
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_VIBRATE);
            mTorchState = false;
            positionValue = Constants.MODE_VIBRATE;
        } else if (action == 2) {
            mNotificationManager.setZenMode(ZEN_MODE_OFF, null, TAG);
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_SILENT);
            mTorchState = false;
            positionValue = Constants.MODE_SILENT;
        } else if (action == 3) {
            mNotificationManager.setZenMode(ZEN_MODE_IMPORTANT_INTERRUPTIONS, null, TAG);
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL);
            mTorchState = false;
            positionValue = Constants.MODE_PRIORITY_ONLY;
        } else if (action == 4) {
            mNotificationManager.setZenMode(ZEN_MODE_ALARMS, null, TAG);
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL);
            mTorchState = false;
            positionValue = Constants.MODE_ALARMS_ONLY;
        } else if (action == 5) {
            mNotificationManager.setZenMode(ZEN_MODE_NO_INTERRUPTIONS, null, TAG);
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL);
            mTorchState = false;
            positionValue = Constants.MODE_TOTAL_SILENCE;
        } else if (action == 6) {
            mNotificationManager.setZenMode(ZEN_MODE_OFF, null, TAG);
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL);
            positionValue = Constants.MODE_FLASHLIGHT;
            mUseSliderTorch = true;
            mTorchState = true;
        }

        if (positionValue != 0) {
            sendUpdateBroadcast(position, positionValue);
        }

        if (mUseSliderTorch && action < 6) {
            launchSpecialActions(AppSelectListPreference.TORCH_ENTRY);
            mUseSliderTorch = false;
        } else if (mUseSliderTorch) {
            launchSpecialActions(AppSelectListPreference.TORCH_ENTRY);
        }

    }

    private void sendUpdateBroadcast(int position, int position_value) {
        Bundle extras = new Bundle();
        Intent intent = new Intent(Constants.ACTION_UPDATE_SLIDER_POSITION);
        extras.putInt(Constants.EXTRA_SLIDER_POSITION, position);
        extras.putInt(Constants.EXTRA_SLIDER_POSITION_VALUE, position_value);
        intent.putExtras(extras);
        mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
        intent.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        Log.d(TAG, "slider change to positon " + position
                            + " with value " + position_value);
    }

    private boolean launchSpecialActions(String value) {
        final boolean musicPlaybackEnabled = Settings.System.getIntForUser(mContext.getContentResolver(),
                "Settings.System."+DeviceSettings.GESTURE_MUSIC_PLAYBACK_SETTINGS_VARIABLE_NAME, 0, UserHandle.USER_CURRENT) == 1;
        /* handle music playback gesture if enabled */
        if (musicPlaybackEnabled) {
            switch (value) {
                case AppSelectListPreference.MUSIC_PLAY_ENTRY:
                    mGestureWakeLock.acquire(Constants.GESTURE_WAKELOCK_DURATION);
                    AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false, mContext,
                                  DeviceSettings.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                                  Constants.GESTURE_HAPTIC_DURATION);
                    dispatchMediaKeyWithWakeLockToAudioService(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                    return true;
                case AppSelectListPreference.MUSIC_NEXT_ENTRY:
                    if (isMusicActive()) {
                        mGestureWakeLock.acquire(Constants.GESTURE_WAKELOCK_DURATION);
                        AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false, mContext,
                                  DeviceSettings.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                                  Constants.GESTURE_HAPTIC_DURATION);
                        dispatchMediaKeyWithWakeLockToAudioService(KeyEvent.KEYCODE_MEDIA_NEXT);
                    }
                    return true;
                case AppSelectListPreference.MUSIC_PREV_ENTRY:
                    if (isMusicActive()) {
                        mGestureWakeLock.acquire(Constants.GESTURE_WAKELOCK_DURATION);
                        AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false, mContext,
                                  DeviceSettings.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                                  Constants.GESTURE_HAPTIC_DURATION);
                        dispatchMediaKeyWithWakeLockToAudioService(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                    }
                    return true;
            }
        }

        if (value.equals(AppSelectListPreference.TORCH_ENTRY)) {
            mGestureWakeLock.acquire(Constants.GESTURE_WAKELOCK_DURATION);
            IStatusBarService service = getStatusBarService();
            if (service != null) {
                try {
                    /*if (mUseSliderTorch) {
                        service.toggleCameraFlashState(mTorchState);
                        return true;
                    } else {*/
                        service.toggleCameraFlash();
                        AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false, mContext,
                                  DeviceSettings.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                                  Constants.GESTURE_HAPTIC_DURATION);
                        return true;
                   // }
                } catch (RemoteException e) {
                // do nothing.
               }
            }
        } else if (value.equals(AppSelectListPreference.AMBIENT_DISPLAY_ENTRY)) {
            AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false, mContext,
                        DeviceSettings.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                        Constants.GESTURE_HAPTIC_DURATION);
            launchDozePulse();
            return true;
        }
        return false;
    }

    private void onDisplayOn() {
        if (DEBUG) Log.i(TAG, "Display on");
    }

    private void onDisplayOff() {
        if (DEBUG) Log.i(TAG, "Display off");
    }

    private void launchDozePulse() {
        // Note: Only works with ambient display enabled.
        mContext.sendBroadcastAsUser(new Intent(Constants.DOZE_INTENT),
                new UserHandle(UserHandle.USER_CURRENT));
    }

    IStatusBarService getStatusBarService() {
        return IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
    }

    private void dispatchMediaKeyWithWakeLockToAudioService(int keycode) {
        if (ActivityManagerNative.isSystemReady()) {
            IAudioService audioService = getAudioService();
            if (audioService != null) {
                KeyEvent event = new KeyEvent(SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN,
                        keycode, 0);
                dispatchMediaKeyEventUnderWakelock(event);
                event = KeyEvent.changeAction(event, KeyEvent.ACTION_UP);
                dispatchMediaKeyEventUnderWakelock(event);
            }
        }
    }

    private void dispatchMediaKeyEventUnderWakelock(KeyEvent event) {
        if (ActivityManagerNative.isSystemReady()) {
            MediaSessionLegacyHelper.getHelper(mContext).sendMediaButtonEvent(event, true);
        }
    }

    boolean isMusicActive() {
        return mAudioManager.isMusicActive();
    }

    private IAudioService getAudioService() {
        IAudioService audioService = IAudioService.Stub
                .asInterface(ServiceManager.checkService(Context.AUDIO_SERVICE));
        if (audioService == null) {
            Log.w(TAG, "Unable to find IAudioService interface.");
        }
        return audioService;
    }
}
