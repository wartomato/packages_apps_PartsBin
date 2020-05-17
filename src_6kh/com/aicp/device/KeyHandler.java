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
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.IAudioService;
import android.media.AudioManager;
import android.media.session.MediaSessionLegacyHelper;
import android.net.Uri;
import android.text.TextUtils;
import android.os.FileObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.HapticFeedbackConstants;
import android.view.WindowManagerGlobal;

import com.android.internal.util.ArrayUtils;
import com.android.internal.util.aicp.AicpUtils;
import com.android.internal.util.aicp.AicpVibe;
import com.android.internal.util.aicp.CustomKeyHandler;
import com.android.internal.statusbar.IStatusBarService;

public class KeyHandler implements CustomKeyHandler {

    private static final String TAG = "KeyHandler";
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_SENSOR = false;

    protected static final int GESTURE_REQUEST = 1;
    private static final int GESTURE_WAKELOCK_DURATION = 2000;
    private static final int GESTURE_HAPTIC_DURATION = 50;
    private static final String DT2W_CONTROL_PATH = "/proc/touchpanel/double_tap_enable";

    public static final String ACTION_UPDATE_SLIDER_POSITION
            = "com.aicp.device.UPDATE_SLIDER_POSITION";
    public static final String EXTRA_SLIDER_POSITION = "position";
    public static final String EXTRA_SLIDER_POSITION_VALUE = "position_value";

    private static final int GESTURE_W_SCANCODE = 246;
    private static final int GESTURE_M_SCANCODE = 247;
    private static final int GESTURE_S_SCANCODE = 248;
    private static final int GESTURE_CIRCLE_SCANCODE = 250;
    private static final int GESTURE_V_SCANCODE = 252;
    private static final int GESTURE_II_SCANCODE = 251;
    private static final int GESTURE_LEFT_V_SCANCODE = 253;
    private static final int GESTURE_RIGHT_V_SCANCODE = 254;
    private static final int GESTURE_RIGHT_SWIPE_SCANCODE = 63;
    private static final int GESTURE_LEFT_SWIPE_SCANCODE = 64;
    private static final int GESTURE_DOWN_SWIPE_SCANCODE = 65;
    private static final int GESTURE_UP_SWIPE_SCANCODE = 66;

    private static final int KEY_DOUBLE_TAP = 143;
    private static final int KEY_HOME = 102;
    private static final int KEY_BACK = 158;
    private static final int KEY_RECENTS = 580;
    private static final int KEY_SLIDER_TOP = 601;
    private static final int KEY_SLIDER_CENTER = 602;
    private static final int KEY_SLIDER_BOTTOM = 603;

    private static final int MIN_PULSE_INTERVAL_MS = 2500;
    private static final String DOZE_INTENT = "com.android.systemui.doze.pulse";
    private static final int HANDWAVE_MAX_DELTA_MS = 1000;
    private static final int POCKET_MIN_DELTA_MS = 5000;
    private static final int FP_GESTURE_LONG_PRESS = 305;

    public static final String CLIENT_PACKAGE_NAME = "com.oneplus.camera";
    public static final String CLIENT_PACKAGE_PATH = "/data/misc/omni/client_package_name";
    // TriStateUI Modes
    public static final int MODE_TOTAL_SILENCE = 600;
    public static final int MODE_ALARMS_ONLY = 601;
    public static final int MODE_PRIORITY_ONLY = 602;
    public static final int MODE_NONE = 603;
    public static final int MODE_VIBRATE = 604;
    public static final int MODE_RING = 605;
    // AICP additions: arbitrary value which hopefully doesn't conflict with upstream anytime soon
    public static final int MODE_SILENT = 620;
    public static final int MODE_FLASHLIGHT = 621;

    // Single tap key code
    private static final int KEY_SINGLE_TAP = 67;

    private static final int[] sSupportedGestures = new int[]{
        GESTURE_II_SCANCODE,
        GESTURE_CIRCLE_SCANCODE,
        GESTURE_V_SCANCODE,
        GESTURE_M_SCANCODE,
        GESTURE_S_SCANCODE,
        GESTURE_W_SCANCODE,
        GESTURE_LEFT_V_SCANCODE,
        GESTURE_RIGHT_V_SCANCODE,
        GESTURE_DOWN_SWIPE_SCANCODE,
        GESTURE_UP_SWIPE_SCANCODE,
        GESTURE_LEFT_SWIPE_SCANCODE,
        GESTURE_RIGHT_SWIPE_SCANCODE,
        KEY_DOUBLE_TAP,
        KEY_SLIDER_TOP,
        KEY_SLIDER_CENTER,
        KEY_SLIDER_BOTTOM
    };

    private static final int[] sProxiCheckedGestures = new int[]{
        GESTURE_II_SCANCODE,
        GESTURE_CIRCLE_SCANCODE,
        GESTURE_V_SCANCODE,
        GESTURE_M_SCANCODE,
        GESTURE_S_SCANCODE,
        GESTURE_W_SCANCODE,
        GESTURE_LEFT_V_SCANCODE,
        GESTURE_RIGHT_V_SCANCODE,
        GESTURE_DOWN_SWIPE_SCANCODE,
        GESTURE_UP_SWIPE_SCANCODE,
        GESTURE_LEFT_SWIPE_SCANCODE,
        GESTURE_RIGHT_SWIPE_SCANCODE,
        KEY_DOUBLE_TAP
    };

    protected final Context mContext;
    private final PowerManager mPowerManager;
    private EventHandler mEventHandler;
    private WakeLock mGestureWakeLock;
    private static boolean mButtonDisabled;
    private final NotificationManager mNoMan;
    private final AudioManager mAudioManager;
    private boolean mProxyIsNear;
    private boolean mDispOn;
    private boolean mRestoreUser;
    private boolean mUseSliderTorch = false;
    private boolean mTorchState = false;

    private BroadcastReceiver mSystemStateReceiver = new BroadcastReceiver() {
         @Override
         public void onReceive(Context context, Intent intent) {
             if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                 mDispOn = true;
                 onDisplayOn();
             } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                 mDispOn = false;
                 onDisplayOff();
             } else if (intent.getAction().equals(Intent.ACTION_USER_SWITCHED)) {
                int userId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, UserHandle.USER_NULL);
                if (userId == UserHandle.USER_SYSTEM && mRestoreUser) {
                    if (DEBUG) Log.i(TAG, "ACTION_USER_SWITCHED to system");
                    Startup.restoreAfterUserSwitch(context);
                } else {
                    mRestoreUser = true;
                }
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
        mNoMan = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        IntentFilter systemStateFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        systemStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        systemStateFilter.addAction(Intent.ACTION_USER_SWITCHED);
        mContext.registerReceiver(mSystemStateReceiver, systemStateFilter);
        (new UEventObserver() {
            @Override
            public void onUEvent(UEventObserver.UEvent event) {
                try {
                    String state = event.get("STATE");
                    boolean ringing = state.contains("USB=0");
                    boolean silent = state.contains("(null)=0");
                    boolean vibrate = state.contains("USB_HOST=0");
                    Log.v(TAG, "Got ringing = " + ringing + ", silent = " + silent + ", vibrate = " + vibrate);
                    if(ringing && !silent && !vibrate)
                        doHandleSliderAction(2);
                    if(silent && !ringing && !vibrate)
                        doHandleSliderAction(0);
                    if(vibrate && !silent && !ringing)
                        doHandleSliderAction(1);
                } catch(Exception e) {
                    Log.d(TAG, "Failed parsing uevent", e);
                }

            }
        }).startObserving("DEVPATH=/devices/platform/soc/soc:tri_state_key");
    }

    private boolean hasSetupCompleted() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.USER_SETUP_COMPLETE, 0) != 0;
    }

    private class EventHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        }
    }

    @Override
    public boolean handleKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_UP) {
            return false;
        }

        if (!hasSetupCompleted()) {
            return false;
        }

        int scanCode = event.getScanCode();
        if (scanCode == KEY_SINGLE_TAP) {
            launchDozePulse();
            return false;
        }

        return false;
    }

    @Override
    public boolean canHandleKeyEvent(KeyEvent event) {
            return ArrayUtils.contains(sSupportedGestures, event.getScanCode());
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
        return event.getScanCode() == KEY_DOUBLE_TAP;
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
                AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false, mContext, DeviceSettings.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,GESTURE_HAPTIC_DURATION);
                Intent intent = createIntent(value);
                return intent;
            }
        }
        return null;
    }

    private IAudioService getAudioService() {
        IAudioService audioService = IAudioService.Stub
                .asInterface(ServiceManager.checkService(Context.AUDIO_SERVICE));
        if (audioService == null) {
            Log.w(TAG, "Unable to find IAudioService interface.");
        }
        return audioService;
    }

    boolean isMusicActive() {
        return mAudioManager.isMusicActive();
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

    private void onDisplayOn() {
        if (DEBUG) Log.i(TAG, "Display on");
    }

    private void onDisplayOff() {
        if (DEBUG) Log.i(TAG, "Display off");
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
            mNoMan.setZenMode(ZEN_MODE_OFF, null, TAG);
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL);
            mTorchState = false;
            positionValue = MODE_RING;
        } else if (action == 1) {
            mNoMan.setZenMode(ZEN_MODE_OFF, null, TAG);
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_VIBRATE);
            mTorchState = false;
            positionValue = MODE_VIBRATE;
        } else if (action == 2) {
            mNoMan.setZenMode(ZEN_MODE_OFF, null, TAG);
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_SILENT);
            mTorchState = false;
            positionValue = MODE_SILENT;
        } else if (action == 3) {
            mNoMan.setZenMode(ZEN_MODE_IMPORTANT_INTERRUPTIONS, null, TAG);
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL);
            mTorchState = false;
            positionValue = MODE_PRIORITY_ONLY;
        } else if (action == 4) {
            mNoMan.setZenMode(ZEN_MODE_ALARMS, null, TAG);
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL);
            mTorchState = false;
            positionValue = MODE_ALARMS_ONLY;
        } else if (action == 5) {
            mNoMan.setZenMode(ZEN_MODE_NO_INTERRUPTIONS, null, TAG);
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL);
            mTorchState = false;
            positionValue = MODE_TOTAL_SILENCE;
        } else if (action == 6) {
            mNoMan.setZenMode(ZEN_MODE_OFF, null, TAG);
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL);
            positionValue = MODE_FLASHLIGHT;
            mUseSliderTorch = true;
            mTorchState = true;
        }

        if (positionValue != 0) {
            sendUpdateBroadcast(position, positionValue);
        }

        if (!mProxyIsNear && mUseSliderTorch && action < 4) {
            launchSpecialActions(AppSelectListPreference.TORCH_ENTRY);
            mUseSliderTorch = false;
        } else if (!mProxyIsNear && mUseSliderTorch) {
            launchSpecialActions(AppSelectListPreference.TORCH_ENTRY);
        }

    }

    private void sendUpdateBroadcast(int position, int position_value) {
        Bundle extras = new Bundle();
        Intent intent = new Intent(ACTION_UPDATE_SLIDER_POSITION);
        extras.putInt(EXTRA_SLIDER_POSITION, position);
        extras.putInt(EXTRA_SLIDER_POSITION_VALUE, position_value);
        intent.putExtras(extras);
        mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
        intent.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        Log.d(TAG, "slider change to positon " + position
                            + " with value " + position_value);
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

    private boolean launchSpecialActions(String value) {
        final boolean musicPlaybackEnabled = Settings.System.getIntForUser(mContext.getContentResolver(),
                "Settings.System."+DeviceSettings.GESTURE_MUSIC_PLAYBACK_SETTINGS_VARIABLE_NAME, 0, UserHandle.USER_CURRENT) == 1;
        /* handle music playback gesture if enabled */
        if (musicPlaybackEnabled) {
            switch (value) {
                case AppSelectListPreference.MUSIC_PLAY_ENTRY:
                    mGestureWakeLock.acquire(GESTURE_WAKELOCK_DURATION);
                    AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false, mContext,DeviceSettings.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,GESTURE_HAPTIC_DURATION);
                    dispatchMediaKeyWithWakeLockToAudioService(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                    return true;
                case AppSelectListPreference.MUSIC_NEXT_ENTRY:
                    if (isMusicActive()) {
                        mGestureWakeLock.acquire(GESTURE_WAKELOCK_DURATION);
                        AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false, mContext,DeviceSettings.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,GESTURE_HAPTIC_DURATION);
                        dispatchMediaKeyWithWakeLockToAudioService(KeyEvent.KEYCODE_MEDIA_NEXT);
                    }
                    return true;
                case AppSelectListPreference.MUSIC_PREV_ENTRY:
                    if (isMusicActive()) {
                        mGestureWakeLock.acquire(GESTURE_WAKELOCK_DURATION);
                        AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false, mContext,DeviceSettings.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,GESTURE_HAPTIC_DURATION);
                        dispatchMediaKeyWithWakeLockToAudioService(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                    }
                    return true;
            }
        }

        if (value.equals(AppSelectListPreference.TORCH_ENTRY)) {
            mGestureWakeLock.acquire(GESTURE_WAKELOCK_DURATION);
            IStatusBarService service = getStatusBarService();
            if (service != null) {
                try {
                    /*if (mUseSliderTorch) {
                        service.toggleCameraFlashState(mTorchState);
                        return true;
                    } else {*/
                        service.toggleCameraFlash();
                        AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false, mContext,DeviceSettings.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,GESTURE_HAPTIC_DURATION);
                        return true;
                   // }
                } catch (RemoteException e) {
                // do nothing.
               }
            }
        } else if (value.equals(AppSelectListPreference.AMBIENT_DISPLAY_ENTRY)) {
            AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false, mContext,DeviceSettings.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,GESTURE_HAPTIC_DURATION);
            launchDozePulse();
            return true;
        }
        return false;
    }

    private String getGestureValueForScanCode(int scanCode) {
      /* for the music playback gestures, just return the expected values */
        switch(scanCode) {
            case GESTURE_II_SCANCODE:
                return AppSelectListPreference.MUSIC_PLAY_ENTRY;
            case GESTURE_CIRCLE_SCANCODE:
                return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.DEVICE_GESTURE_MAPPING_1, UserHandle.USER_CURRENT);
            case GESTURE_V_SCANCODE:
                return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.DEVICE_GESTURE_MAPPING_2, UserHandle.USER_CURRENT);
            case GESTURE_M_SCANCODE:
                return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.DEVICE_GESTURE_MAPPING_3, UserHandle.USER_CURRENT);
            case GESTURE_LEFT_V_SCANCODE:
                return AppSelectListPreference.MUSIC_PREV_ENTRY;
            case GESTURE_RIGHT_V_SCANCODE:
                return AppSelectListPreference.MUSIC_NEXT_ENTRY;
            case GESTURE_DOWN_SWIPE_SCANCODE:
                return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.DEVICE_GESTURE_MAPPING_6, UserHandle.USER_CURRENT);
            case GESTURE_UP_SWIPE_SCANCODE:
                return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.DEVICE_GESTURE_MAPPING_7, UserHandle.USER_CURRENT);
            case GESTURE_LEFT_SWIPE_SCANCODE:
                return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.DEVICE_GESTURE_MAPPING_8, UserHandle.USER_CURRENT);
            case GESTURE_RIGHT_SWIPE_SCANCODE:
                return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.DEVICE_GESTURE_MAPPING_9, UserHandle.USER_CURRENT);
            case GESTURE_S_SCANCODE:
                return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.DEVICE_GESTURE_MAPPING_10, UserHandle.USER_CURRENT);
            case GESTURE_W_SCANCODE:
                return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.DEVICE_GESTURE_MAPPING_11, UserHandle.USER_CURRENT);
        }
        return null;
    }

    private void launchDozePulse() {
        // Note: Only works with ambient display enabled.
        if (DEBUG) Log.i(TAG, "Doze pulse");
        mContext.sendBroadcastAsUser(new Intent(DOZE_INTENT),
                new UserHandle(UserHandle.USER_CURRENT));
    }

    protected static Sensor getSensor(SensorManager sm, String type) {
        for (Sensor sensor : sm.getSensorList(Sensor.TYPE_ALL)) {
            if (type.equals(sensor.getStringType())) {
                return sensor;
            }
        }
        return null;
    }

    IStatusBarService getStatusBarService() {
        return IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
    }

    @Override
    public boolean getCustomProxiIsNear(SensorEvent event) {
        return event.values[0] == 1;
    }

    @Override
    public String getCustomProxiSensor() {
        return "oneplus.sensor.pocket";
    }

    private class ClientPackageNameObserver extends FileObserver {

        public ClientPackageNameObserver(String file) {
            super(CLIENT_PACKAGE_PATH, MODIFY);
        }

        @Override
        public void onEvent(int event, String file) {
            String pkgName = Utils.getFileValue(CLIENT_PACKAGE_PATH, "0");
            /*if (event == FileObserver.MODIFY) {
                try {
                    Log.d(TAG, "client_package" + file + " and " + pkgName);
                    mProvider = IOnePlusCameraProvider.getService();
                    mProvider.setPackageName(pkgName);
                } catch (RemoteException e) {
                    Log.e(TAG, "setPackageName error", e);
                }
            }*/
        }
    }
}
