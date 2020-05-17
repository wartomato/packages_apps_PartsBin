LOCAL_PATH := $(call my-dir)
ifeq ($(TARGET_DEVICE),$(filter $(TARGET_DEVICE),cheeseburger dumpling enchilada fajita guacamoleb hammerhead hotdog))
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_STATIC_ANDROID_LIBRARIES := \
    androidx.core_core \
    androidx.preference_preference \
    AicpGear-preference \
    AicpGear-util

LOCAL_SRC_FILES := $(call all-java-files-under, src)
ifeq ($(TARGET_DEVICE),$(filter $(TARGET_DEVICE),cheeseburger dumpling))
    LOCAL_SRC_FILES += $(call all-java-files-under, src_5kh)
endif
ifeq ($(TARGET_DEVICE),$(filter $(TARGET_DEVICE),enchilada fajita))
    LOCAL_SRC_FILES += $(call all-java-files-under, src_6kh)
endif
ifeq ($(TARGET_DEVICE),$(filter $(TARGET_DEVICE),guacamole guacamoleb hotdog))
    LOCAL_SRC_FILES += $(call all-java-files-under, src_7kh)
endif

ifeq ($(TARGET_DEVICE),$(filter $(TARGET_DEVICE),guacamole hotdog))
LOCAL_STATIC_JAVA_LIBRARIES := \
    vendor.oneplus.camera.CameraHIDL-V1.0-java
endif

LOCAL_PACKAGE_NAME := PartsBin
LOCAL_CERTIFICATE := platform
LOCAL_PRIVATE_PLATFORM_APIS := true
LOCAL_PRIVILEGED_MODULE := true
LOCAL_RESOURCE_DIR := \
    $(LOCAL_PATH)/res \
    $(TOP)/packages/resources/deviceparts/res

LOCAL_USE_AAPT2 := true

LOCAL_JAVA_LIBRARIES := telephony-common

LOCAL_PROGUARD_ENABLED := disabled
LOCAL_DEX_PREOPT := false

include frameworks/base/packages/SettingsLib/common.mk

include $(BUILD_PACKAGE)

include $(call all-makefiles-under,$(LOCAL_PATH))
endif
