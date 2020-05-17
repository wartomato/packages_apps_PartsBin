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
ifeq ($(TARGET_DEVICE),$(filter $(TARGET_DEVICE),guacamoleb hotdog))
    LOCAL_SRC_FILES += $(call all-java-files-under, src_7kh)
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

LOCAL_REQUIRED_MODULES := privapp_whitelist_com.aicp.device.xml

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)
LOCAL_MODULE := privapp_whitelist_com.aicp.device.xml
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/permissions
LOCAL_SRC_FILES := $(LOCAL_MODULE)
include $(BUILD_PREBUILT)

include $(call all-makefiles-under,$(LOCAL_PATH))
endif
