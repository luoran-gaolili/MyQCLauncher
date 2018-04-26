# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE    := libbitmaputils-jni
LOCAL_SRC_FILES := bitmaputils-jni.c
LOCAL_SHARED_LIBRARIES += libjnigraphics liblog libm
#LOCAL_LDLIBS += -ljnigraphics -llog -lm
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_SRC_FILES := \
	dcfdecoder_jni.cpp 
LOCAL_MODULE := liblauncher-dcfdecoder-jni
LOCAL_C_INCLUDES := $(JNI_H_INCLUDE)
LOCAL_C_INCLUDES += \
    external/skia/include/core \
    external/skia/include/effects \
    external/skia/include/images \
    external/skia/src/ports \
    external/skia/include/utils \
    $(TOP)/frameworks/base/core/jni/android/graphics \
    $(TOP)/frameworks/base/libs/hwui \
    $(TOP)/frameworks/av/include/drm \
    $(MTK_PATH_SOURCE)/frameworks/av/drm/include/drm
LOCAL_SHARED_LIBRARIES := \
    libcutils \
    libpixelflinger \
    libhardware \
    libutils \
    libhwui \
    libskia \
    libandroid_runtime \
    libdrmframework \
    libdrmmtkutil
LOCAL_PRELINK_MODULE := false
LOCAL_MODULE_TAGS := optional
include $(BUILD_SHARED_LIBRARY)
