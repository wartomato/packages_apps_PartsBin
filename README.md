# PartsBin
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](./LICENSE.txt)

*A configurable collection of settings for OnePlus phones.*

### Currently supported devices:

  * OnePlus 5:      cheeseburger
  * OnePlus 5T:     dumpling
  * OnePlus 6:      enchilada
  * OnePlus 7:      guacamoleb
  * OnePlus 7 Pro:  guacamole
  * OnePlus 7T Pro: hotdog

### Description

Based on OMNIROMs DeviceParts package, the idea is to manage device specific
settings from a common code base while allowing features/implementations
to be unique to individual devices to coexist with common ones.
Feature availability is controlled by adding various overlays to the device
tree for this package. Overlays are mostly sysfs paths controlling that particular
feature.
This application must have proper file access and sepolicy security contexts to those sysfs paths.

Resource strings are used from https://github.com/AICP/packages_resources_deviceparts

### Currently supported features include:

**Toggles**

	HWKSwitch: Hardware keys swap toggle (supported on devices hw nav keys)

	SoundTuner: En-/disable the proprietary SoundTuner

	HBMSwitch: High Brightness Mode toggle with configurable off-on values

	DCDSwitch: DC Dimming toggle

	Displaypanel Color Modes: sRGB, DCI-P3, WideColor, OnePlus, Night

	Display Refreshrate: Automatic, Manuak (60Hz, 90Hz)

	Vibration Modes: System, Calls, Notifications
	[Note: If any vibration is used, 3 integer vibrator overlays must be defined.]

	Fastcharge: En-/diable USB 3.0 charging with max. 900mA

**Screen-Off Gestures (with optional haptic feedback)**

	Single Tap - to display ambient mode

	Music Control - Play/Pause ("||"), skip to previous ("<") or next (">") track

	O-W-M-S-V-A Gestures - configurable

	Left-Right-Up-Down Swipes - configurable

**Fingerprintreader Swiping Gestures**

	Left-Right-Up-Down Fingerprint-Swipes - configurable
	[Note: Down-Swipe can also be disabled in favour of the AOSP build-in gesture.]

**AlertSlider**

	Top-Center-Bottom positions with position bubbles

	Possible selections:

	  - Ringer: ring, vibrate, silent
	  - Zen mode: priority only, alarms only, do not disturb
	  - Flashlight

**Configurable overlays**

	\<!-- Whether the device has hardware navigation buttons (true/false) -->

	\<bool name="config_device_has_hw_nav_buttons">\</bool>

	\<!-- Whether the device supports the prebuilt SoundTuner (true/false) -->

	\<bool name="config_device_supports_soundtuner">\</bool>

	\<!-- Whether device supports switching display refreshrates (true/false) -->

	\<bool name="config_device_supports_switch_refreshrate">\</bool>

	\<!-- Whether device supports disabling hwkeys -->

	\<string name="pathHWKToggle">\</string>

	\<!-- Path to devices single-tap toggle file -->

	\<string name="pathSTapToggle">\</string>

	\<!-- Path to devices High Brigness Mode toggle file -->

	\<string name="pathHBMModeToggle">\</string>

	\<string name="hbmOFF">"0"\</string>

	\<string name="hbmON">"1"\</string>

	\<!-- Path to devices High Brigness Mode toggle file -->

	\<string name="pathOnePlusModeToggle">\</string>

	\<!-- Path to devices SRGBMode toggle file -->

	\<string name="pathSRGBModeToggle">\</string>

	\<!-- Path to devices DCIMode toggle file -->

	\<string name="pathDCIModeToggle">\</string>

	\<!-- Path to devices Nightmode toggle file -->

	\<string name="pathNightModeToggle">\</string>

	\<!-- Path to devices DCDMode toggle file -->

	\<string name="pathDCDModeToggle">\</string>

	\<!-- Path to devices WideMode toggle file -->

	\<string name="pathWideModeToggle">\</string>

	\<!-- Whether device allow changing system vibrationlevels -->

	\<string name="pathSystemVibStrength">\</string>

	\<!-- Whether device allow changing calls vibrationlevels -->

	\<string name="pathCallVibStrength">\</string>

	\<!-- Whether device allow changing notification vibrationlevels -->

	\<string name="pathNotifVibStrength">\</string>

	\<!-- Device vibrator min-max-default values -->

	\<integer name="vibratorMinMV">\</integer>

	\<integer name="vibratorMaxMV">\</integer>

	\<integer name="vibratorDefaultMV">\</integer>

	\<!-- Path to the needed CameraMotorController nodes, if the device
	      has set TARGET_MOTORIZED_CAMERA := true in its BoardConfig.mk -->

	\<string name="pathCameraMotorEnableToggle">\</string>

	\<string name="pathCameraMotorDirectionToggle">\</string>

	\<string name="pathCameraMotorSWSwitchToggle">\</string>
