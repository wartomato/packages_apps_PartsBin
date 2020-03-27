# PartsBin
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](./LICENSE.txt)

**A configurable collection of settings for OnePlus phones.**

Currently supporting

 op5: Cheeseburger

 op5t: Dumpling

 op6: Enchilada

 op7: Guacamoleb

Idea is to manage device specific settings from a common code base while allowing
features/implementations unique to individual devices to coexist with common
ones. Feature availability is controlled by adding various overlays to the device
tree for this package. Overlays are mostly sysfs paths controlling that particular
feature. This application must have proper file access and sepolicy security contexts
to those sysfs paths.

Resources are used from https://github.com/eyosen/packages_resources_deviceparts

Currently supported features include:

*Toggles*

HWKSwitch: Hardware keys swap toggle (supported on devices hw nav keys)

HBMSwitch: High Brightness Mode toggle with configurable off-on values

DCDSwitch: DC Dimming toggle

Panel Modes: sRGB, DCI-P3, WideColor, OnePlus

Vibration Modes: System, Calls, Notifications
*if any vibration is used, 3 integer vibrator overlays **must** be defined

*ScreenOffGestures*

Single Tap - to display ambient mode

Music Control - Play/Pause, skip to prev or next track

O-W-M-S Gestures - configurable

Left-Right-Up-Down Swipes - configurable

Down Arrow Gesture - configurable

*AlertSlider*

Top-Center-Bottom positions with position bubbles

Possible selections:

Ringer: Ring, vibrate, silent

Zen mode: priority only, alarms only, do not disturb

Flashlight

**Configurable overlays.**

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
