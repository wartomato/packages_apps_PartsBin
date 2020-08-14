# PartsBin
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](./LICENSE.txt)

*A configurable collection of settings for OnePlus phones.*

### Currently supported devices:

  * OnePlus 5:      cheeseburger
  * OnePlus 5T:     dumpling
  * OnePlus 6:      enchilada
  * OnePlus 6T:     fajita
  * OnePlus 7:      guacamoleb
  * OnePlus 7 Pro:  guacamole
  * OnePlus 7T Pro: hotdog
  * Hammerhead:     Google Nexus 5

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

**Toggles and Paths**

_Note that defining paths needs support in the kernel! Thus the features might not work, if you are using a custom kernel._

	HWKSwitch: Hardware keys swap toggle (supported on devices hw nav keys)

	Offscreen-gestures: Only set to true, if the device supports Screengestures while the display is off.

	Doubletap to wake (DT2W): En-/disable waking up the display by tapping on it two times.

	Sweep to wake (S2W): En-/disable waking up the display by swiping from left to right in the lower section.

	SoundTuner: En-/disable the proprietary SoundTuner (OnePlus specific)

	HBMSwitch: High Brightness Mode toggle with configurable off-on values with additional QS tile

	DCDSwitch: DC-Dimming toggle with additional QS tile

	Displaypanel Color Modes: sRGB, DCI-P3, WideColor, OnePlus, Nightmode. With additional QS tile

	Display Refreshrate: Automatic, Manual (60Hz, 90Hz) with additional QS tile

	Vibration Modes: System, Calls, Notifications
	[Note: For a vibration to work the corresponding integer vibrator overlays must be defined.]

	Fastcharge: En-/disable USB charging with max. 900mA

**Screen-Off Gestures (with optional haptic feedback)**

	SingleTap - to display ambient mode

	Music Control - Play/Pause ("||"), skip to previous ("<") or next (">") track

	Letter "O", "W", "M", "S", "V", "A" Gestures - configurable

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

	\<!-- Whether the device supports offscreen-gestures (true/false) -->
	\<bool name="config_device_supports_gestures">\</bool>

	\<!-- Whether the device supports the prebuilt SoundTuner (true/false) -->
	\<bool name="config_device_supports_soundtuner">\</bool>

	\<!-- Whether device supports switching display refreshrates (true/false) -->
	\<bool name="config_device_supports_switch_refreshrate">\</bool>

    \<!-- Show/hide the QS tile, if device supports DC Dimming or not. Default value is false. -->
    \<bool name="DCDSwitch_tile">false</bool>

    \<!-- Show/hide the QS tile, if device supports switching display refreshrates or not. Default value is false. -->
    \<bool name="RefreshrateSwitch_tile">false</bool>

    \<!-- Show/hide the QS tile, if device supports switching Displaymodes or not. Default value is false. -->
    \<bool name="PanelModeSwitch_tile">false</bool>

    \<!-- Show/hide the QS tile, if device supports switching HighBrightness mode or not. Default value is false. -->
    \<bool name="HBMModeSwitch_tile">false</bool>

	\<!-- Whether device supports disabling hwkeys -->
	\<string name="pathHWKToggle">\</string>

	\<!-- Path to devices single-tap toggle file -->
	\<string name="pathSTapToggle">\</string>

	\<!-- Path to devices doubletap to wake toggle file -->
	\<string name="pathDoubleTapToWakeToggle">\</string>

	\<!-- Path to devices sweep to wake toggle file -->
	\<string name="pathSweepToWakeToggle">\</string>

	\<!-- Path to devices High Brightness Mode toggle file -->
	\<string name="pathHBMModeToggle">\</string>
	\<string name="hbmOFF">"0"\</string>
	\<string name="hbmON">"1"\</string>

	\<!-- Path to devices OnePlus Mode toggle file -->
	\<string name="pathOnePlusModeToggle">\</string>

	\<!-- Path to devices SRGBMode toggle file -->
	\<string name="pathSRGBModeToggle">\</string>

	\<!-- Path to devices DCI-P3 Mode toggle file -->
	\<string name="pathDCIModeToggle">\</string>

	\<!-- Path to devices Nightmode toggle file -->
	\<string name="pathNightModeToggle">\</string>

	\<!-- Path to devices DC-Dimming Mode toggle file -->
	\<string name="pathDCDModeToggle">\</string>

	\<!-- Path to devices WideMode toggle file -->
	\<string name="pathWideModeToggle">\</string>

	\<!-- Path to devices system vibrationlevels -->
	\<string name="pathSystemVibStrength">\</string>

	\<!-- Path to devices calls vibrationlevels -->
	\<string name="pathCallVibStrength">\</string>

	\<!-- Path to devices notification vibrationlevels -->
	\<string name="pathNotifVibStrength">\</string>

	\<!-- Device vibrator min-max-default values -->
	\<integer name="vibratorMinMV">\</integer>
	\<integer name="vibratorMaxMV">\</integer>
	\<integer name="vibratorDefaultMV">\</integer>
