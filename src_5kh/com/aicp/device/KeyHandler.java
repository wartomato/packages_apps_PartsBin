/*
* Copyright (C) 2016 The OmniROM Project
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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.os.Vibrator;
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

    private static final String KEY_CONTROL_PATH = "/proc/touchpanel/key_disable";
    private static final String FPC_CONTROL_PATH = "/sys/devices/soc/soc:fpc_fpc1020/proximity_state";
    private static final String FPC_KEY_CONTROL_PATH = "/sys/devices/soc/soc:fpc_fpc1020/key_disable";
    private static final String GOODIX_CONTROL_PATH = "/sys/devices/soc/soc:goodix_fp/proximity_state";

    private static final boolean sIsOnePlus5t = android.os.Build.DEVICE.equals("dumpling");

    protected final Context mContext;
    private final PowerManager mPowerManager;
    private EventHandler mEventHandler;
    private WakeLock mGestureWakeLock;
    private Handler mHandler = new Handler();
    private SettingsObserver mSettingsObserver;
    private static boolean mButtonDisabled;
    private final NotificationManager mNoMan;
    private final AudioManager mAudioManager;
    private SensorManager mSensorManager;
    private boolean mProxyIsNear;
    private boolean mUseProxiCheck;
    private Sensor mTiltSensor;
    private boolean mUseTiltCheck;
    private boolean mProxyWasNear;
    private long mProxySensorTimestamp;
    private boolean mUseWaveCheck;
    private Sensor mPocketSensor;
    private boolean mUsePocketCheck;
    private boolean mFPcheck;
    private boolean mDispOn;
    private boolean isFpgesture;
    private boolean mTorchState = false;
    private boolean mUseSliderTorch = false;
    private int mSliderPosition = -1;

    private SensorEventListener mProximitySensor = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            mProxyIsNear = event.values[0] == 1;
            if (DEBUG_SENSOR) Log.i(TAG, "mProxyIsNear = " + mProxyIsNear + " mProxyWasNear = " + mProxyWasNear);
            if (mUseProxiCheck) {
                if (!sIsOnePlus5t) {
                    if (Utils.fileWritable(FPC_CONTROL_PATH)) {
                        Utils.writeValue(FPC_CONTROL_PATH, mProxyIsNear ? "1" : "0");
                    }
                } else {
                    if (Utils.fileWritable(GOODIX_CONTROL_PATH)) {
                        Utils.writeValue(GOODIX_CONTROL_PATH, mProxyIsNear ? "1" : "0");
                    }
                }
            }
            if (mUseWaveCheck || mUsePocketCheck) {
                if (mProxyWasNear && !mProxyIsNear) {
                    long delta = SystemClock.elapsedRealtime() - mProxySensorTimestamp;
                    if (DEBUG_SENSOR) Log.i(TAG, "delta = " + delta);
                    if (mUseWaveCheck && delta < Constants.HANDWAVE_MAX_DELTA_MS) {
                        launchDozePulse();
                    }
                    if (mUsePocketCheck && delta > Constants.POCKET_MIN_DELTA_MS) {
                        launchDozePulse();
                    }
                }
                mProxySensorTimestamp = SystemClock.elapsedRealtime();
                mProxyWasNear = mProxyIsNear;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private SensorEventListener mTiltSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.values[0] == 1) {
                launchDozePulse();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(
                    Settings.Secure.HARDWARE_KEYS_DISABLE),
                    false, this);
            mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(
                    Settings.System.OMNI_DEVICE_PROXI_CHECK_ENABLED),
                    false, this);
            mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(
                    Settings.System.OMNI_DEVICE_FEATURE_SETTINGS),
                    false, this);
            update();
            updateDozeSettings();
        }

        @Override
        public void onChange(boolean selfChange) {
            update();
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.System.getUriFor(
                    Settings.System.OMNI_DEVICE_FEATURE_SETTINGS))){
                updateDozeSettings();
                return;
            }
            update();
        }

        public void update() {
            setButtonDisable(mContext);
            mUseProxiCheck = Settings.System.getIntForUser(
                    mContext.getContentResolver(), Settings.System.OMNI_DEVICE_PROXI_CHECK_ENABLED, 1,
                    UserHandle.USER_CURRENT) == 1;
        }
    }

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
        mSettingsObserver = new SettingsObserver(mHandler);
        mSettingsObserver.observe();
        mNoMan = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mTiltSensor = getSensor(mSensorManager, "com.oneplus.sensor.pickup");
        mPocketSensor = getSensor(mSensorManager, "com.oneplus.sensor.pocket");
        IntentFilter screenStateFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(mScreenStateReceiver, screenStateFilter);
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
        isFpgesture = false;
        boolean isKeySupported = ArrayUtils.contains(Constants.sHandledGestures, event.getScanCode());
        if (isKeySupported) {
            int scanCode = event.getScanCode();
            if (DEBUG) Log.i(TAG, "scanCode=" + scanCode);
            int position = scanCode == Constants.KEY_SLIDER_TOP ? 0 :
                    scanCode == Constants.KEY_SLIDER_CENTER ? 1 : 2;
            if (mSliderPosition != position) {
                mSliderPosition = position;
                doHandleSliderAction(position);
                switch(scanCode) {
                    case Constants.KEY_SLIDER_TOP:
                        if (DEBUG) Log.i(TAG, "KEY_SLIDER_TOP");
                        return true;
                    case Constants.KEY_SLIDER_CENTER:
                        if (DEBUG) Log.i(TAG, "KEY_SLIDER_CENTER");
                        return true;
                    case Constants.KEY_SLIDER_BOTTOM:
                        if (DEBUG) Log.i(TAG, "KEY_SLIDER_BOTTOM");
                        return true;
                }
            } // else: discard changes caused by a loose contact
        }

        if (DEBUG) Log.i(TAG, "nav_code=" + event.getScanCode());
        int fpcode = event.getScanCode();
        mFPcheck = canHandleKeyEvent(event);
        String value = getGestureValueForFPScanCode(fpcode);
        if (mFPcheck && mDispOn && !TextUtils.isEmpty(value) && !value.equals(AppSelectListPreference.DISABLED_ENTRY)){
            isFpgesture = true;
            if (!launchSpecialActions(value) && !isCameraLaunchEvent(event)) {
                    AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false,
                              mContext, DeviceSettings.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME, Constants.GESTURE_HAPTIC_DURATION);
                    Intent intent = createIntent(value);
                    if (DEBUG) Log.i(TAG, "intent = " + intent);
                    mContext.startActivity(intent);
            }
        }
        return isKeySupported;
    }

    @Override
    public boolean canHandleKeyEvent(KeyEvent event) {
        if (sIsOnePlus5t) {
            return ArrayUtils.contains(Constants.sSupportedGestures5t, event.getScanCode());
        } else {
            return ArrayUtils.contains(Constants.sSupportedGestures, event.getScanCode());
        }
    }

    @Override
    public boolean isDisabledKeyEvent(KeyEvent event) {
        boolean isProxyCheckRequired = mUseProxiCheck &&
                ArrayUtils.contains(Constants.sProxiCheckedGestures, event.getScanCode());
        if (mProxyIsNear && isProxyCheckRequired) {
            if (DEBUG) Log.i(TAG, "isDisabledKeyEvent: blocked by proxi sensor - scanCode=" + event.getScanCode());
            return true;
        }
        return false;
    }

    public static void setButtonDisable(Context context) {
        // we should never come here on the 5t but just to be sure
        if (!sIsOnePlus5t) {
            mButtonDisabled = Settings.Secure.getIntForUser(
                    context.getContentResolver(), Settings.Secure.HARDWARE_KEYS_DISABLE, 0,
                    UserHandle.USER_CURRENT) == 1;
            if (DEBUG) Log.i(TAG, "setButtonDisable=" + mButtonDisabled);
            if(mButtonDisabled) {
                Utils.writeValue(KEY_CONTROL_PATH, "1");
                Utils.writeValue(FPC_KEY_CONTROL_PATH, "1");
            }
            else {
                Utils.writeValue(KEY_CONTROL_PATH, "0");
                Utils.writeValue(FPC_KEY_CONTROL_PATH, "0");
            }
        }
    }

    @Override
    public boolean isCameraLaunchEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_UP) {
            return false;
        }
        if (mFPcheck) {
            String value = getGestureValueForFPScanCode(event.getScanCode());
            return !TextUtils.isEmpty(value) && value.equals(AppSelectListPreference.CAMERA_ENTRY);
        } else {
            String value = getGestureValueForScanCode(event.getScanCode());
            return !TextUtils.isEmpty(value) && value.equals(AppSelectListPreference.CAMERA_ENTRY);
        }
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
                AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false,
                          mContext, DeviceSettings.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME, Constants.GESTURE_HAPTIC_DURATION);
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
        if (enableProxiSensor()) {
            mSensorManager.unregisterListener(mProximitySensor, mPocketSensor);
            enableGoodix();
        }
        if (mUseTiltCheck) {
            mSensorManager.unregisterListener(mTiltSensorListener, mTiltSensor);
        }
    }

    private void enableGoodix() {
        if (sIsOnePlus5t) {
            if (Utils.fileWritable(GOODIX_CONTROL_PATH)) {
                Utils.writeValue(GOODIX_CONTROL_PATH, "0");
            }
        }
    }

    private void onDisplayOff() {
        if (DEBUG) Log.i(TAG, "Display off");
        if (enableProxiSensor()) {
            mProxyWasNear = false;
            mSensorManager.registerListener(mProximitySensor, mPocketSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
            mProxySensorTimestamp = SystemClock.elapsedRealtime();
        }
        if (mUseTiltCheck) {
            mSensorManager.registerListener(mTiltSensorListener, mTiltSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
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
            positionValue = Constants.MODE_RING;
        } else if (action == 1) {
            mNoMan.setZenMode(ZEN_MODE_OFF, null, TAG);
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_VIBRATE);
            mTorchState = false;
            positionValue = Constants.MODE_VIBRATE;
        } else if (action == 2) {
            mNoMan.setZenMode(ZEN_MODE_OFF, null, TAG);
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_SILENT);
            mTorchState = false;
            positionValue = Constants.MODE_SILENT;
        } else if (action == 3) {
            mNoMan.setZenMode(ZEN_MODE_IMPORTANT_INTERRUPTIONS, null, TAG);
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL);
            mTorchState = false;
            positionValue = Constants.MODE_PRIORITY_ONLY;
        } else if (action == 4) {
            mNoMan.setZenMode(ZEN_MODE_ALARMS, null, TAG);
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL);
            mTorchState = false;
            positionValue = Constants.MODE_ALARMS_ONLY;
        } else if (action == 5) {
            mNoMan.setZenMode(ZEN_MODE_NO_INTERRUPTIONS, null, TAG);
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL);
            mTorchState = false;
            positionValue = Constants.MODE_TOTAL_SILENCE;
        } else if (action == 6) {
            mNoMan.setZenMode(ZEN_MODE_OFF, null, TAG);
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL);
            positionValue = Constants.MODE_FLASHLIGHT;
            mUseSliderTorch = true;
            mTorchState = true;
        }
        if (positionValue != 0) {
            sendUpdateBroadcast(position, positionValue);
        }
        if (((!mProxyIsNear && mUseProxiCheck) || !mUseProxiCheck) && mUseSliderTorch && action < 4) {
            launchSpecialActions(AppSelectListPreference.TORCH_ENTRY);
            mUseSliderTorch = false;
        } else if (((!mProxyIsNear && mUseProxiCheck) || !mUseProxiCheck) && mUseSliderTorch) {
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
                    mGestureWakeLock.acquire(Constants.GESTURE_WAKELOCK_DURATION);
                    AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false, mContext,DeviceSettings.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,Constants.GESTURE_HAPTIC_DURATION);
                    dispatchMediaKeyWithWakeLockToAudioService(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                    return true;
                case AppSelectListPreference.MUSIC_NEXT_ENTRY:
                    if (isMusicActive()) {
                        mGestureWakeLock.acquire(Constants.GESTURE_WAKELOCK_DURATION);
                        AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false, mContext,DeviceSettings.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,Constants.GESTURE_HAPTIC_DURATION);
                        dispatchMediaKeyWithWakeLockToAudioService(KeyEvent.KEYCODE_MEDIA_NEXT);
                    }
                    return true;
                case AppSelectListPreference.MUSIC_PREV_ENTRY:
                    if (isMusicActive()) {
                        mGestureWakeLock.acquire(Constants.GESTURE_WAKELOCK_DURATION);
                        AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false, mContext,DeviceSettings.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,Constants.GESTURE_HAPTIC_DURATION);
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
                    if (mUseSliderTorch) {
                        service.toggleCameraFlashState(mTorchState);
                        AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false,
                                  mContext, DeviceSettings.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                                          Constants.GESTURE_HAPTIC_DURATION);
                        return true;
                    } else {
                        service.toggleCameraFlash();
                        AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false,
                                  mContext, DeviceSettings.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                                          Constants.GESTURE_HAPTIC_DURATION);
                        return true;
                    }
                } catch (RemoteException e) {
                // do nothing.
               }
           }
        } else if (value.equals(AppSelectListPreference.VOLUME_UP_ENTRY)) {
            AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false,
                      mContext, DeviceSettings.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                          Constants.GESTURE_HAPTIC_DURATION);
            mAudioManager.adjustSuggestedStreamVolume(AudioManager.ADJUST_RAISE,AudioManager.USE_DEFAULT_STREAM_TYPE,AudioManager.FLAG_SHOW_UI);
            return true;
        } else if (value.equals(AppSelectListPreference.VOLUME_DOWN_ENTRY)) {
            AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false,
                      mContext, DeviceSettings.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                          Constants.GESTURE_HAPTIC_DURATION);
            mAudioManager.adjustSuggestedStreamVolume(AudioManager.ADJUST_LOWER,AudioManager.USE_DEFAULT_STREAM_TYPE,AudioManager.FLAG_SHOW_UI);
            return true;
        } else if (value.equals(AppSelectListPreference.BROWSE_SCROLL_DOWN_ENTRY)) {
            AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false,
                      mContext, DeviceSettings.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                          Constants.GESTURE_HAPTIC_DURATION);
            AicpUtils.sendKeycode(KeyEvent.KEYCODE_PAGE_DOWN);
            return true;
        } else if (value.equals(AppSelectListPreference.BROWSE_SCROLL_UP_ENTRY)) {
            AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false,
                      mContext, DeviceSettings.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                          Constants.GESTURE_HAPTIC_DURATION);
            AicpUtils.sendKeycode(KeyEvent.KEYCODE_PAGE_UP);
            return true;
        } else if (value.equals(AppSelectListPreference.NAVIGATE_BACK_ENTRY)) {
            AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false,
                      mContext, DeviceSettings.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                          Constants.GESTURE_HAPTIC_DURATION);
            AicpUtils.sendKeycode(KeyEvent.KEYCODE_BACK);
            return true;
        } else if (value.equals(AppSelectListPreference.NAVIGATE_HOME_ENTRY)) {
            AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false,
                      mContext, DeviceSettings.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                          Constants.GESTURE_HAPTIC_DURATION);
            AicpUtils.sendKeycode(KeyEvent.KEYCODE_HOME);
            return true;
        } else if (value.equals(AppSelectListPreference.NAVIGATE_RECENT_ENTRY)) {
            AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false,
                      mContext, DeviceSettings.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                          Constants.GESTURE_HAPTIC_DURATION);
            AicpUtils.sendKeycode(KeyEvent.KEYCODE_APP_SWITCH);
            return true;
        }
        return false;
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

    private String getGestureValueForFPScanCode(int scanCode) {
        switch(scanCode) {
            case Constants.FP_GESTURE_SWIPE_DOWN:
                if (areSystemNavigationKeysEnabled() == false){
                    return Settings.System.getStringForUser(mContext.getContentResolver(),
                       GestureSettings.DEVICE_GESTURE_MAPPING_10, UserHandle.USER_CURRENT);
                }
                break;
            case Constants.FP_GESTURE_SWIPE_UP:
                if (areSystemNavigationKeysEnabled() == false){
                    return Settings.System.getStringForUser(mContext.getContentResolver(),
                       GestureSettings.DEVICE_GESTURE_MAPPING_11, UserHandle.USER_CURRENT);
                }
                break;
            case Constants.FP_GESTURE_SWIPE_LEFT:
                return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.DEVICE_GESTURE_MAPPING_12, UserHandle.USER_CURRENT);
            case Constants.FP_GESTURE_SWIPE_RIGHT:
                return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.DEVICE_GESTURE_MAPPING_13, UserHandle.USER_CURRENT);
            case Constants.FP_GESTURE_LONG_PRESS:
                return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.DEVICE_GESTURE_MAPPING_14, UserHandle.USER_CURRENT);
        }
        return null;
    }

    private boolean areSystemNavigationKeysEnabled() {
        return Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.SYSTEM_NAVIGATION_KEYS_ENABLED, 0, UserHandle.USER_CURRENT) == 1;
    }

    private void launchDozePulse() {
        if (DEBUG) Log.i(TAG, "Doze pulse");
        mContext.sendBroadcastAsUser(new Intent(Constants.DOZE_INTENT),
                new UserHandle(UserHandle.USER_CURRENT));
    }

    private boolean enableProxiSensor() {
        return mUsePocketCheck || mUseWaveCheck || mUseProxiCheck;
    }

    private void updateDozeSettings() {
        String value = Settings.System.getStringForUser(mContext.getContentResolver(),
                    Settings.System.OMNI_DEVICE_FEATURE_SETTINGS,
                    UserHandle.USER_CURRENT);
        if (DEBUG) Log.i(TAG, "Doze settings = " + value);
        if (!TextUtils.isEmpty(value)) {
            String[] parts = value.split(":");
            mUseWaveCheck = Boolean.valueOf(parts[0]);
            mUsePocketCheck = Boolean.valueOf(parts[1]);
            mUseTiltCheck = Boolean.valueOf(parts[2]);
        }
    }

    IStatusBarService getStatusBarService() {
        return IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
    }

    protected static Sensor getSensor(SensorManager sm, String type) {
        for (Sensor sensor : sm.getSensorList(Sensor.TYPE_ALL)) {
            if (type.equals(sensor.getStringType())) {
                return sensor;
            }
        }
        return null;
    }

    @Override
    public boolean getCustomProxiIsNear(SensorEvent event) {
        return event.values[0] == 1;
    }

    @Override
    public String getCustomProxiSensor() {
        return "com.oneplus.sensor.pocket";
    }

    /*private void vibe(){
        /*boolean doVibrate = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.OMNI_DEVICE_GESTURE_FEEDBACK_ENABLED, 0,
                UserHandle.USER_CURRENT) == 1;
        int owningUid;
        String owningPackage;

        owningUid = android.os.Process.myUid();
        owningPackage = mContext.getOpPackageName();
        VibrationEffect effect = VibrationEffect.get(VibrationEffect.EFFECT_HEAVY_CLICK);
        //mVibrator.vibrate(owningUid, owningPackage, effect, VIBRATION_ATTRIBUTES);
        //OmniVibe.performHapticFeedback(owningUid, owningPackage, effect, VIBRATION_ATTRIBUTES);

        //OmniVibe mOmniVibe = new OmniVibe();
        OmniVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false, mContext);
    }*/

}
